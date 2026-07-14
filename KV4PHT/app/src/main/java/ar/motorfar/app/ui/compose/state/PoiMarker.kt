package ar.motorfar.app.ui.compose.state

data class PoiMarker(
    val label: String,
    val lat: Double,
    val lon: Double,
    val fromAlias: String,
    val receivedAtMs: Long
)
