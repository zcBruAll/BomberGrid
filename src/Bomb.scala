case class Bomb(x: Int, y: Int, dropTime: Long, duration: Long = 3500){
  def hasExploded(currentTime: Long): Boolean = {
    currentTime >= dropTime + duration
  }
}
