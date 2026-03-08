package dev.hippodid.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ClerkTenantResolver}.
 */
class ClerkTenantResolverTest {

    private ClerkTenantResolver resolver;

    // 32-byte secret for HS256
    private static final byte[] SECRET = "test-secret-key-for-unit-tests-1234".getBytes();

    @BeforeEach
    void setUp() {
        resolver = new ClerkTenantResolver();
    }

    private String buildJwt(String sub, String orgId) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(sub)
                .expirationTime(new Date(System.currentTimeMillis() + 3_600_000));
        if (orgId != null) {
            claims.claim("org_id", orgId);
        }
        SignedJWT jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims.build());
        jwt.sign(new MACSigner(SECRET));
        return jwt.serialize();
    }

    @Test
    void resolveOrgId_presentWhenOrgClaimed() throws Exception {
        String token = buildJwt("user_abc", "org_xyz");
        Optional<String> orgId = resolver.resolveOrgId(token);
        assertThat(orgId).hasValue("org_xyz");
    }

    @Test
    void resolveOrgId_emptyWhenNoOrgClaim() throws Exception {
        String token = buildJwt("user_abc", null);
        Optional<String> orgId = resolver.resolveOrgId(token);
        assertThat(orgId).isEmpty();
    }

    @Test
    void resolveUserId_extractsSubject() throws Exception {
        String token = buildJwt("user_abc123", null);
        Optional<String> userId = resolver.resolveUserId(token);
        assertThat(userId).hasValue("user_abc123");
    }

    @Test
    void resolveTenantId_prefersOrgOverUser() throws Exception {
        String token = buildJwt("user_abc", "org_xyz");
        Optional<String> tenantId = resolver.resolveTenantId(token);
        assertThat(tenantId).hasValue("org_xyz");
    }

    @Test
    void resolveTenantId_fallsBackToUserWhenNoOrg() throws Exception {
        String token = buildJwt("user_abc", null);
        Optional<String> tenantId = resolver.resolveTenantId(token);
        assertThat(tenantId).hasValue("user_abc");
    }

    @Test
    void invalidTokenReturnsEmpty() {
        Optional<String> orgId = resolver.resolveOrgId("not.a.jwt");
        assertThat(orgId).isEmpty();
    }

    @Test
    void emptyTokenReturnsEmpty() {
        assertThat(resolver.resolveOrgId("")).isEmpty();
        assertThat(resolver.resolveUserId("")).isEmpty();
        assertThat(resolver.resolveTenantId("")).isEmpty();
    }
}
