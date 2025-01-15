/**
 * Represents a radar effect bonus that reveals enemy positions
 *
 * @param ownerPlayerId The ID of the player who activated the radar
 * @param startTime     The timestamp when the radar effect was activated
 */
case class RadarEffect(ownerPlayerId: Int, startTime: Long) {
  val duration = 15000 // 15 seconds of radar effect
  val pingInterval = 5000 // ping every 5 seconds
  val pingFadeDuration = 1000 // ping fading duration

  /**
   * Check if the radar effect is still active
   *
   * @param currentTime Current game timestamp
   * @return True if the effect hasn't expired yet
   */
  def isActive(currentTime: Long): Boolean = currentTime - startTime < duration

  /**
   * Calculate the current opacity of the radar ping effect
   * Opacity fades from 1.0 to 0.0 over the pingFadeDuration
   *
   * @param currentTime Current game timestamp
   * @return Opacity value between 0.0 and 1.0
   */
  def getPingOpacity(currentTime: Long): Float = {
    val timeSinceLastPing = (currentTime - startTime) % pingInterval
    if (timeSinceLastPing < pingFadeDuration) {
      1.0f - (timeSinceLastPing.toFloat / pingFadeDuration)
    } else 0.0f
  }
}