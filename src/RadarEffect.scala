case class RadarEffect(ownerPlayerId: Int, startTime: Long) {
  val duration = 15000 // 15 seconds of radar effect
  val pingInterval = 5000 // ping every 5 seconds
  val pingFadeDuration = 1000 // ping fade duration in ms

  def isActive(currentTime: Long): Boolean = currentTime - startTime < duration
  def shouldPing(currentTime: Long): Boolean = {
    val timeElapsed = currentTime - startTime
    timeElapsed % pingInterval == 0
  }
  def getPingOpacity(currentTime: Long): Float = {
    val timeSinceLastPing = (currentTime - startTime) % pingInterval
    if (timeSinceLastPing < pingFadeDuration) {
      1.0f - (timeSinceLastPing.toFloat / pingFadeDuration)
    } else 0.0f
  }
}