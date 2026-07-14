package ar.motorfar.app.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Genera GPX 1.1 a partir de una sesión de ruta y lo comparte por share intent. */
object GpxExporter {

    private fun isoTimestamp(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(millis))
    }

    fun buildGpx(points: List<RoutePoint>, trackName: String): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"Baqueano\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        append("  <trk>\n    <name>").append(trackName).append("</name>\n    <trkseg>\n")
        for (p in points) {
            append("      <trkpt lat=\"").append(p.latitude).append("\" lon=\"").append(p.longitude).append("\">\n")
            append("        <time>").append(isoTimestamp(p.timestamp)).append("</time>\n")
            append("      </trkpt>\n")
        }
        append("    </trkseg>\n  </trk>\n</gpx>\n")
    }

    /** [fileSlug] debe ser seguro para nombre de archivo (sin ":", espacios, etc). */
    fun shareGpx(context: Context, points: List<RoutePoint>, trackName: String, fileSlug: String) {
        val gpx = buildGpx(points, trackName)
        val dir = File(context.cacheDir, "gpx").apply { mkdirs() }
        val file = File(dir, "$fileSlug.gpx")
        file.writeText(gpx)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Compartir ruta").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
