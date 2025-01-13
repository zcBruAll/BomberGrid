case class RadarPickup(x: Int, y: Int, timestamp: Long) {
    val lifetime = 15000 // 30 seconds for final version but 15 for #debug
    def hasExpired(currentTime: Long): Boolean = currentTime - timestamp > lifetime
}