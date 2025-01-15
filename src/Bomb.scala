/**
 * A bomb object
 *
 * @param x        The position in the room along the X-axis
 * @param y        The position in the room along the Y-axis
 * @param dropTime Timestamp for when the bomb has been dropped
 * @param duration The duration, in ms, for the bomb before exploding
 */
case class Bomb(x: Int, y: Int, dropTime: Long, duration: Long = 3500) {

  /**
   * Return if a bomb has exploded based on its drop time and duration
   *
   * @param currentTime The current timestamp
   * @return `true` if the bomb should have already exploded, `false` otherwise
   */
  def hasExploded(currentTime: Long): Boolean = {
    currentTime >= dropTime + duration
  }
}
