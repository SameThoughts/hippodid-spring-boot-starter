# hippodid-spring-boot-starter

[![CI](https://github.com/SameThoughts/hippodid-spring-boot-starter/actions/workflows/ci.yml/badge.svg)](https://github.com/SameThoughts/hippodid-spring-boot-starter/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Spring Boot auto-configuration for the [HippoDid](https://hippodid.com) AI character memory API.

Add persistent, searchable memory to your AI agents and Spring applications in three lines of configuration.

## Installation

Available via [GitHub Packages](https://github.com/SameThoughts/hippodid-spring-boot-starter/packages). Maven Central publishing is planned.

### Maven

Add the GitHub Packages repository and dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/SameThoughts/hippodid-spring-boot-starter</url>
    </repository>
</repositories>

<dependency>
    <groupId>dev.hippodid</groupId>
    <artifactId>hippodid-spring-boot-starter</artifactId>
    <version>1.2.0</version>
</dependency>
```

GitHub Packages requires authentication. Add to your `~/.m2/settings.xml`:

```xml
<server>
    <id>github</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>YOUR_GITHUB_TOKEN</password> <!-- needs read:packages scope -->
</server>
```

### Gradle

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/SameThoughts/hippodid-spring-boot-starter")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

implementation 'dev.hippodid:hippodid-spring-boot-starter:1.2.0'
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

// Create with specific memory mode
CharacterInfo verbatimAgent = hippodid.characters()
    .create("Verbatim Agent", "Stores memories as-is", MemoryMode.VERBATIM);

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
List<MemoryInfo> mems = handle.memories().add("User prefers dark mode and vim keybindings");

// Add memory — direct write (Starter+ tier, no AI processing)
MemoryInfo mem = handle.memories().addDirect(
        "Prefers Go over Java for backend services",
        "decisions",
        0.9  // salience [0.0, 1.0]
);

// Export all memories to a file
Path file = handle.export(ExportFormat.MARKDOWN, Path.of("agent-memory.md"));

// Set memory mode
handle.setMemoryMode(MemoryMode.VERBATIM);
```

### `hippodid.characters(id).agentConfig()` — Per-character agent config

```java
// Get agent config
AgentConfig config = hippodid.characters("id").agentConfig().get();
config.systemPrompt();   // Optional<String>
config.preferredModel();  // Optional<String>
config.temperature();     // Optional<Double>

// Set agent config
hippodid.characters("id").agentConfig().set(Map.of(
    "systemPrompt", "You are a helpful assistant",
    "preferredModel", "gpt-4o",
    "temperature", 0.7
));

// Delete agent config
hippodid.characters("id").agentConfig().delete();
```

### `hippodid.characters(id).clone()` — Clone a character

```java
// Clone with all options
CloneResult clone = hippodid.characters("source-id").clone("My Clone",
    CloneOptions.builder()
        .copyMemories(true)
        .copyTags(true)
        .agentConfigOverride(Map.of("temperature", 0.5))
        .build());

System.out.println("New ID: " + clone.characterId());
System.out.println("Memories copied: " + clone.memoriesCopied());

// Clone with defaults (tags copied, memories not)
CloneResult simple = hippodid.characters("source-id").clone("Quick Clone",
    CloneOptions.defaults());
```

### `hippodid.characters(id).assembleContext()` — Client-side context assembly

Fetches profile, relevant memories, and agent config, then formats into a prompt:

```java
AssembledContext ctx = hippodid.characters("agent-id")
    .assembleContext("user preferences",
        ContextOptions.builder()
            .strategy(AssemblyStrategy.CONVERSATIONAL)
            .maxContextTokens(4096)
            .recencyWeight(0.7)
            .topK(15)
            .build());

String prompt = ctx.formattedPrompt();   // Ready-to-use prompt
int tokens = ctx.tokenEstimate();         // Estimated token count
String sysPrompt = ctx.systemPrompt();    // From agentConfig or generated
List<MemoryResult> mems = ctx.memories(); // Retrieved memories
```

Available strategies: `DEFAULT`, `CONVERSATIONAL`, `TASK_FOCUSED`, `CONCIERGE`, `MATCHING`.

### `hippodid.templates()` — Character templates (Sprint 15)

```java
// Create a template
hippodid.templates().create("Employee", "Employee template",
    List.of(Map.of("categoryName", "skills", "purpose", "Track skills")),
    List.of(Map.of("sourceColumn", "name", "targetField", "name")));

// List, get, update, delete
hippodid.templates().list();
hippodid.templates().get("template-id");
hippodid.templates().update("template-id", "New Name", "New desc", null, null);
hippodid.templates().delete("template-id");

// Preview and clone
hippodid.templates().preview("template-id", Map.of("name", "John", "role", "Engineer"));
hippodid.templates().clone("template-id");
```

### `hippodid.batch()` — Batch character creation (Sprint 16)

```java
// Create characters from data rows
Map<String, Object> job = hippodid.batch().create("template-id",
    List.of(
        Map.of("name", "Alice", "role", "Engineer"),
        Map.of("name", "Bob", "role", "Designer")),
    "name",    // externalIdColumn
    "SKIP",    // onConflict: SKIP, UPDATE, ERROR
    false);    // dryRun

// Check job status (typed)
BatchJob status = hippodid.jobs().status("job-id");
System.out.println(status.status());  // PROCESSING
status.progress().ifPresent(p ->
    System.out.println(p.processed() + "/" + p.total()
        + " (" + p.succeeded() + " ok, " + p.failed() + " failed)"));
```

### `hippodid.agentConfigTemplates()` — Reusable agent config presets (Sprint 17)

```java
// Create a preset
hippodid.agentConfigTemplates().create("Friendly Bot", Map.of(
    "systemPrompt", "You are friendly and helpful",
    "temperature", 0.8));

// List all presets
hippodid.agentConfigTemplates().list();
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

## Links

- [Full documentation](https://docs.hippodid.com/spring-boot-starter)
- [API reference](https://docs.hippodid.com/api-reference)
- [Tiers & limits](https://docs.hippodid.com/tiers)
- [GitHub](https://github.com/SameThoughts/hippodid-spring-boot-starter)
