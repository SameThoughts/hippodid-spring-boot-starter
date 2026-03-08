package dev.hippodid.client.model;

/**
 * Supported export formats for character memory export.
 *
 * <p>Used with {@link dev.hippodid.client.CharacterHandle#export(ExportFormat, java.nio.file.Path)}.
 */
public enum ExportFormat {

    /** Markdown format (.md) — human-readable, suitable for import into other AI tools. */
    MARKDOWN("text/markdown", "md"),

    /** JSON format — machine-readable, suitable for programmatic processing. */
    JSON("application/json", "json");

    private final String mediaType;
    private final String fileExtension;

    ExportFormat(String mediaType, String fileExtension) {
        this.mediaType = mediaType;
        this.fileExtension = fileExtension;
    }

    /** The MIME type accepted in the {@code Accept} header. */
    public String mediaType() {
        return mediaType;
    }

    /** The typical file extension for files in this format. */
    public String fileExtension() {
        return fileExtension;
    }
}
