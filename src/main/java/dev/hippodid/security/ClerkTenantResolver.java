package dev.hippodid.security;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import java.text.ParseException;
import java.util.Optional;

/**
 * Extracts tenant information from a Clerk JWT for multi-tenant HippoDid usage.
 *
 * <p>When your application uses Clerk for user authentication, this resolver
 * extracts the Clerk organization ID ({@code org_id} claim) from the JWT so you
 * can construct per-tenant HippoDid requests.
 *
 * <p>This bean is <strong>not</strong> auto-created — instantiate it where needed:
 *
 * <pre>{@code
 * @Bean
 * public ClerkTenantResolver clerkTenantResolver() {
 *     return new ClerkTenantResolver();
 * }
 * }</pre>
 *
 * <h3>Usage in a controller</h3>
 * <pre>{@code
 * @PostMapping("/memories")
 * public ResponseEntity<Void> addMemory(
 *         @RequestHeader("Authorization") String authHeader,
 *         @RequestBody MemoryRequest request) {
 *
 *     String token = authHeader.replace("Bearer ", "");
 *     String tenantId = clerkTenantResolver
 *         .resolveOrgId(token)
 *         .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
 *
 *     // Use tenantId to scope calls (tenant isolation handled by HippoDid server)
 *     hippodid.characters(request.characterId())
 *         .memories()
 *         .add(request.content());
 *
 *     return ResponseEntity.ok().build();
 * }
 * }</pre>
 *
 * <p><strong>Note:</strong> This resolver only <em>reads</em> the JWT claims — it does
 * NOT validate the JWT signature. Signature validation should be performed upstream
 * (e.g., by a Spring Security filter or gateway). Use this only after the token has
 * already been validated.
 *
 * <p>Requires {@code nimbus-jose-jwt} on the classpath (included as optional dependency).
 */
public class ClerkTenantResolver {

    private static final String CLAIM_ORG_ID = "org_id";
    private static final String CLAIM_USER_ID = "sub";

    /**
     * Extracts the Clerk organization ID from the JWT.
     *
     * <p>The {@code org_id} claim is present when the user is acting in the context
     * of a Clerk organization (tenant). It is absent for personal accounts.
     *
     * @param jwtToken raw JWT string (without "Bearer " prefix)
     * @return the org_id claim value, or empty if not present (personal account)
     */
    public Optional<String> resolveOrgId(String jwtToken) {
        return parseClaim(jwtToken, CLAIM_ORG_ID);
    }

    /**
     * Extracts the Clerk user ID (subject) from the JWT.
     *
     * <p>The subject ({@code sub} claim) is the Clerk user ID, format: {@code user_...}.
     *
     * @param jwtToken raw JWT string (without "Bearer " prefix)
     * @return the user ID, or empty if the token cannot be parsed
     */
    public Optional<String> resolveUserId(String jwtToken) {
        return parseClaim(jwtToken, CLAIM_USER_ID);
    }

    /**
     * Resolves the effective tenant identifier.
     *
     * <p>Returns the organization ID if present (org-scoped session), otherwise
     * falls back to the user ID (personal account acting as single-member tenant).
     *
     * @param jwtToken raw JWT string (without "Bearer " prefix)
     * @return the tenant ID (org_id or sub), or empty if neither is present
     */
    public Optional<String> resolveTenantId(String jwtToken) {
        return resolveOrgId(jwtToken)
                .or(() -> resolveUserId(jwtToken));
    }

    private Optional<String> parseClaim(String jwtToken, String claimName) {
        try {
            JWT jwt = JWTParser.parse(jwtToken);
            Object claim = jwt.getJWTClaimsSet().getClaim(claimName);
            return Optional.ofNullable(claim).map(Object::toString);
        } catch (ParseException e) {
            return Optional.empty();
        }
    }
}
