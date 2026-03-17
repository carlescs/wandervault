package cat.company.wandervault.ui.util

private const val BYTES_PER_KB = 1_024L
private const val BYTES_PER_MB = 1_024L * 1_024L

/**
 * Formats [bytes] as a human-readable size string (B / KB / MB).
 */
internal fun formatBytes(bytes: Long): String = when {
    bytes < BYTES_PER_KB -> "$bytes B"
    bytes < BYTES_PER_MB -> "${bytes / BYTES_PER_KB} KB"
    else -> "%.1f MB".format(bytes.toFloat() / BYTES_PER_MB.toFloat())
}
