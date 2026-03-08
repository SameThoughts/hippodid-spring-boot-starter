# hippodid-spring-boot-starter

[![Maven Central](https://img.shields.io/maven-central/v/dev.hippodid/hippodid-spring-boot-starter)](https://central.sonatype.com/artifact/dev.hippodid/hippodid-spring-boot-starter)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Spring Boot auto-configuration for the [HippoDid](https://hippodid.com) AI character memory API.

Add persistent, searchable memory to your AI agents and Spring applications in three lines of configuration.

## Installation

> **Publishing status:** This artifact is not yet published to Maven Central. For now, clone and `mvn install -Plocal` to use it locally. Maven Central publishing is planned — see [TODO](#todo).

### Maven

```xml
<dependency>
    <groupId>dev.hippodid</groupId>
    <artifactId>hippodid-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'dev.hippodid:hippodid-spring-boot-starter:1.0.0'
```

## Configuration

```yaml
hippodid:
  api-key: hd_key_your_key_here         # Required — get yours at hippodid.com
  character-id: your-character-uuid      # Optional — default character for defaultCharacter()
  base-url: https://api.hippodid.com    # Optional — default shown
```

Get your API key from the [HippoDid dashboard](https://hippodid.com) or via `GET /v1/tier` after signing up.

## Quick Start

Once configured, inject `HippoDidClient` into any Spring bean:

```java
@Service
public class AgentMemoryService {

    private final HippoDidClient hippodid;

    public AgentMemoryService(HippoDidClient hippodid) {
        this.hippodid = hippodid;
    }

    // Store a memory (AI extracts structure automatically)
    public void remember(String agentId, String observation) {
        hippodid.characters(agentId)
                .memories()
                .add(observation);
    }

    // Recall relevant memories
    public List<MemoryResult> recall(String agentId, String query) {
        return hippodid.characters(agentId)
                .search(query, SearchOptions.defaults())
                .memories();
    }
}
```

## API Reference

### `hippodid.characters()` — Tenant-level operations

```java
// Create a character
CharacterInfo agent = hippodid.characters()
    .create("My Agent", "Personal AI assistant");

// List all characters
List<CharacterInfo> all = hippodid.characters().list();
```

### `hippodid.characters(id)` — Character-specific operations

```java
CharacterHandle handle = hippodid.characters("your-character-uuid");

// Search memories (semantic search)
List<MemoryResult> results = handle
    .search("user UI preferences", SearchOptions.defaults())
    .memories();

// Filtered search
List<MemoryResult> filtered = handle
    .search("technical decisions",
            SearchOptions.builder()
                .topK(5)
                .categories(List.of("decisions", "skills"))
                .build())
    .memories();

// Add memory — AI extraction (Starter+ tier)
MemoryInfo mem = handle.memories().add("User prefers dark mode and vim keybindings");

// Add memory — direct write (Starter+ tier, no AI processing)
MemoryInfo mem = handle.memories().addDirect(
        "Prefers Go over Java for backend services",
        "decisions",
        0.9  // salience [0.0, 1.0]
);

// Export all memories to a file
Path file = handle.export(ExportFormat.MARKDOWN, Path.of("agent-memory.md"));
```

### `hippodid.defaultCharacter()` — Use configured default character

```java
// Uses hippodid.character-id from application.yml
hippodid.defaultCharacter().memories().add("Some observation");
```

### `hippodid.tier()` — Check tier and limits

```java
TierInfo tier = hippodid.tier();
System.out.println("Tier: " + tier.tier());                       // DEVELOPER
System.out.println("Characters: " + tier.currentCharacterCount()  // 5
    + "/" + tier.maxCharacters());                                 // 30
```

## Error Handling

All methods throw `HippoDidException` (unchecked) on API errors:

```java
try {
    hippodid.characters().create("My Agent", null);
} catch (HippoDidException e) {
    System.err.println("Status: " + e.statusCode());  // e.g., 429
    System.err.println("Type: " + e.errorType());     // e.g., CharacterLimitExceeded
    System.err.println("Message: " + e.getMessage());
}
```

Common error types:

| Type | HTTP | Description |
|------|------|-------------|
| `Unauthorized` | 401 | Invalid or missing API key |
| `CharacterNotFound` | 404 | Character ID does not exist |
| `CharacterLimitExceeded` | 429 | Tier's `maxCharacters` reached |
| `TierLimitExceeded` | 429 | Rate limit or AI ops quota exceeded |
| `TierFeatureNotAvailable` | 403 | Feature not available on current tier |

## Health Indicator

When `spring-boot-starter-actuator` is on the classpath, a health indicator is auto-registered:

```
GET /actuator/health/hippoDid
```

```json
{
  "status": "UP",
  "details": {
    "tier": "DEVELOPER",
    "characters": "5/30",
    "aiExtraction": true,
    "teamSharing": true,
    "baseUrl": "https://api.hippodid.com"
  }
}
```

## Multi-Tenant Apps (Clerk)

For applications using Clerk for authentication, `ClerkTenantResolver` extracts the tenant
organization ID from the user's JWT:

```java
@Bean
public ClerkTenantResolver clerkTenantResolver() {
    return new ClerkTenantResolver();
}

// In your controller:
@PostMapping("/memories")
public ResponseEntity<Void> addMemory(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody MemoryRequest req) {

    String token = authHeader.replace("Bearer ", "");
    String tenantId = clerkTenantResolver
            .resolveTenantId(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

    // HippoDid server enforces tenant isolation via your API key + server-side auth
    hippodid.characters(req.characterId()).memories().add(req.content());
    return ResponseEntity.ok().build();
}
```

> **Note:** `ClerkTenantResolver` reads JWT claims only — it does **not** validate the JWT
> signature. Validate the signature upstream (e.g., Spring Security filter).

## Testing

In tests, supply a mock WebClient to avoid real HTTP calls:

```java
@BeforeEach
void setUp() throws IOException {
    mockServer = new MockWebServer();
    mockServer.start();

    HippoDidProperties props = new HippoDidProperties();
    props.setApiKey("hd_key_test");
    props.setBaseUrl(mockServer.url("/").toString());

    WebClient webClient = WebClient.builder()
            .baseUrl(props.getBaseUrl())
            .defaultHeader("Authorization", "Bearer " + props.getApiKey())
            .build();

    client = new HippoDidClient(props, webClient);
}
```

Or use `@SpringBootTest` with properties override:

```java
@SpringBootTest
@TestPropertySource(properties = {
    "hippodid.api-key=hd_key_test",
    "hippodid.base-url=http://localhost:${mockserver.port}"
})
class MyServiceTest { ... }
```

## Requirements

- Java 21+
- Spring Boot 3.3+

## License

Apache 2.0 — see [LICENSE](LICENSE).

## TODO

- [ ] Publish to Maven Central (GPG signing + Sonatype Central Portal already configured in pom.xml)
- [ ] Set up GitHub Actions CI for automated testing on PRs
- [ ] Add GitHub Packages publishing as interim distribution channel

## Links

- [Full documentation](https://docs.hippodid.com/spring-boot-starter)
- [API reference](https://docs.hippodid.com/api-reference)
- [Tiers & limits](https://docs.hippodid.com/tiers)
- [GitHub](https://github.com/SameThoughts/hippodid-spring-boot-starter)
