package dev.hippodid.client.model;

/**
 * Current tenant tier information.
 *
 * <p>Returned by the HippoDid health indicator and accessible via
 * {@link dev.hippodid.health.HippoDidHealthIndicator}.
 *
 * <p>Tiers: {@code FREE}, {@code STARTER}, {@code DEVELOPER}, {@code BUSINESS}.
 */
public record TierInfo(
        String tier,
        int maxCharacters,
        long currentCharacterCount,
        int maxMembers,
        int maxApiKeys,
        int minSyncIntervalSeconds,
        boolean aiExtractionAvailable,
        boolean directWriteAvailable,
        boolean importPipelineAvailable,
        boolean teamSharingEnabled) {}
