package ar.motorfar.app.ui.compose.state

data class PoiMarker(
    val alias: String,
    val lat: Double,
    val lon: Double,
    val label: String,
    val receivedAtMs: Long
)
