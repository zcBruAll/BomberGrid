import hevs.graphics.FunGraphics
import hevs.graphics.utils.GraphicsBitmap

import java.awt.image.{BufferedImage, RescaleOp}
import java.awt.{Color, Font}

object GraphicMotor {
	private val defaultFont = new Font("SansSerif", Font.BOLD, 24)
	private val defaultColor = Color.BLACK

	/**
	 * Render the game with FunGraphics
	 */
	def displayGame(fg: FunGraphics, room: Room, cellSize: Int, playerId: Int): Unit = {
		fg.clear(Color.LIGHT_GRAY)
		val player = room.getPlayer(playerId)
		val posP = player.getPos

		for (i <- 0 until room.width;
				 j <- 0 until room.height) {
			val x = 25 + cellSize * i
			val y = 45 + cellSize * j

			val distCell = math.sqrt(math.pow(posP._1 - i, 2) + math.pow(posP._2 - j, 2)).toInt
			if (distCell < 4 && !room.isLineOfSightBlocked(posP._1, posP._2, i, j)) {
				fg.drawTransformedPicture(x + cellSize / 2, y + cellSize / 2, 0, cellSize / Motor.dirtImg(4 - distCell).getHeight, Motor.dirtImg(4 - distCell))
			} else {
				fg.setColor(Color.BLACK)
				fg.drawFillRect(x, y, cellSize, cellSize)
			}

			// val distCell = math.sqrt(math.pow(posP._1 - i, 2) + math.pow(posP._2 - j, 2)).toInt
			if (distCell < 4 && !room.isLineOfSightBlocked(posP._1, posP._2, i, j)) {
				fg.setColor(Color.WHITE)

				val walls = room.getRoom(i)(j).getWalls
				if ((walls & 1) != 0) { // Upper wall
					fg.drawFillRect(x, y, cellSize, 2)
				}
				if ((walls & 2) != 0) { // Right wall
					fg.drawFillRect(x + cellSize - 2, y, 2, cellSize)
				}
				if ((walls & 4) != 0) { // Bottom wall
					fg.drawFillRect(x, y + cellSize - 2, cellSize, 2)
				}
				if ((walls & 8) != 0) { // Left wall
					fg.drawFillRect(x, y, 2, cellSize)
				}
			}

			room.spawnRadar()
			room.checkRadarPickup(playerId)


			//-------------------- Radar----------------------
			room.getActiveRadar.foreach { radar =>
				val centerX = 25 + radar.x * cellSize + cellSize/2
				val centerY = 45 + radar.y * cellSize + cellSize/2

				val distCell = math.sqrt(math.pow(posP._1 - radar.x, 2) + math.pow(posP._2 - radar.y, 2)).toInt
				if (distCell < 4 && !room.isLineOfSightBlocked(posP._1, posP._2, radar.x, radar.y)) {

					if (distCell < 4 && !room.isLineOfSightBlocked(posP._1, posP._2, radar.x, radar.y)) {
						fg.drawTransformedPicture(centerX, centerY, 0, cellSize*0.8 / Motor.radarImg(4 - distCell).getHeight, Motor.radarImg(4 - distCell))

					}
				}
			}

			// Draw radar ping effect if active
			room.getRadarPingInfo(playerId).foreach { case (opacity, pos) =>
				val x = 25 + pos._1 * cellSize
				val y = 45 + pos._2 * cellSize

				// Create semi-transparent red color based on opacity
				val alpha = (opacity * 255).toInt
				val pingColor = new Color(255, 0, 0, alpha)
				fg.setColor(pingColor)
				fg.drawFilledCircle(x + cellSize/2, y + cellSize/2, cellSize/3)
			}

		}

		room.getActiveBombs.foreach { bomb =>
			val x = 25 + bomb.x * cellSize
			val y = 45 + bomb.y * cellSize

			val distCell = math.sqrt(math.pow(posP._1 - bomb.x, 2) + math.pow(posP._2 - bomb.y, 2)).toInt
			if (distCell < 4 && !room.isLineOfSightBlocked(posP._1, posP._2, bomb.x, bomb.y)) {
				fg.drawTransformedPicture(x + cellSize / 2, y + cellSize / 2, 0, cellSize / Motor.bombImg(4 - distCell).getHeight, Motor.bombImg(4 - distCell))
			}
		}

		for (i <- 1 to 2) {
			val pos = room.getPlayer(i).getPos
			val x = 25 + pos._1 * cellSize
			val y = 45 + pos._2 * cellSize
			val distCellP = math.sqrt(math.pow(posP._1 - pos._1, 2) + math.pow(posP._2 - pos._2, 2)).toInt
			if (distCellP < 4 && !room.isLineOfSightBlocked(posP._1, posP._2, pos._1, pos._2)) {
				fg.drawTransformedPicture(x + cellSize / 2, y + cellSize / 2, 0, cellSize / (if (i == 1) Motor.player1Img else Motor.player2Img)(4 - distCellP).getHeight, (if (i == 1) Motor.player1Img else Motor.player2Img)(4 - distCellP))
			}
		}

		val lifePercent = player.life
		drawRectangle(fg, fg.width - 25 - 100 - 4, 45 - 15, 100 + 4, 10, Color.WHITE, 2, Color.BLACK, (Color.LIGHT_GRAY, Color.LIGHT_GRAY, Color.LIGHT_GRAY, Color.LIGHT_GRAY), (5, 5, 5, 5))
		drawRectangle(fg, fg.width - 25 - 100 - 2, 45 - 15 + 2, lifePercent, 6, if (lifePercent > 75) Color.GREEN else if (lifePercent > 25) Color.ORANGE else Color.RED, 0, Color.BLACK, if (lifePercent == 100) (Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK) else (Color.BLACK, Color.WHITE, Color.BLACK, Color.WHITE), (3, 3, 3, 3))

		val nextDrop = player.cooldown - (System.currentTimeMillis() - player.lastDropped)
		val seconds: Int = math.max((nextDrop / 1000).toInt, 0)
		val milliseconds: Int = math.max((nextDrop % 1000).toInt, 0)
		val text = f"$seconds:$milliseconds%03d"
		val textSize = fg.getStringSize(text, Motor.fontSubtitle)
		fg.drawString(fg.width - 25 - textSize.getWidth.toInt, 45 - 10 - 10, text, Motor.fontSubtitle, Color.BLACK)
	}

	def drawRectangle(fg: FunGraphics, x: Int, y: Int, width: Int, height: Int, background: Color, borderWidth: Int = 1, borderColor: Color = Color.WHITE, borderBackgroundColor: (Color, Color, Color, Color) = (Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE), borderRadius: (Int, Int, Int, Int) = (0, 0, 0, 0)): Unit = {
		val radius: (Int, Int, Int, Int) = (math.min(borderRadius._1, math.min(width, height) / 2), math.min(borderRadius._2, math.min(width, height) / 2), math.min(borderRadius._3, math.min(width, height) / 2), math.min(borderRadius._4, math.min(width, height) / 2))
		fg.setColor(background)
		fg.drawFillRect(x, y, width, height)

		if (radius._1 > 0) {
			if (borderWidth > 0) {
				drawRectangle(fg, x, y, width, height, borderColor, 0, borderRadius = radius, borderBackgroundColor = borderBackgroundColor)
				drawRectangle(fg, x + borderWidth, y + borderWidth, width - 2 * borderWidth, height - 2 * borderWidth, background, 0, borderRadius = (radius._1 - borderWidth, radius._2 - borderWidth, radius._3 - borderWidth, radius._4 - borderWidth), borderBackgroundColor = (borderColor, borderColor, borderColor, borderColor))
			} else {
				fg.setColor(borderBackgroundColor._1)
				fg.drawFillRect(x, y, radius._1, radius._1)

				fg.setColor(borderBackgroundColor._2)
				fg.drawFillRect(x + width - radius._2, y, radius._2, radius._2)

				fg.setColor(borderBackgroundColor._3)
				fg.drawFillRect(x, y + height - radius._3, radius._3, radius._3)

				fg.setColor(borderBackgroundColor._4)
				fg.drawFillRect(x + width - radius._4, y + height - radius._4, radius._4, radius._4)

				fg.setColor(background)
				fg.drawFilledCircle(x + width - 2 * radius._1, y, radius._1 * 2)
				fg.drawFilledCircle(x, y, radius._2 * 2)
				fg.drawFilledCircle(x, y + height - 2 * radius._3, radius._3 * 2)
				fg.drawFilledCircle(x + width - 2 * radius._3, y + height - 2 * radius._3, radius._3 * 2)
			}
		} else if (borderWidth > 0) {
			fg.setColor(borderColor)

			fg.drawFillRect(x, y, width, borderWidth)
			fg.drawFillRect(x, y + height - borderWidth, width, borderWidth)
			fg.drawFillRect(x, y, borderWidth, height)
			fg.drawFillRect(x + width - borderWidth, y, borderWidth, height)
		}
	}

	def drawButton(fg: FunGraphics, x: Int, y: Int, width: Int, height: Int, t: String, background: Color, foreground: Color, borderWidth: Int = 1, borderColor: Color, borderRadius: Int = 0, font: Font = defaultFont): Unit = {
		drawRectangle(fg, x, y, width, height, background, borderWidth, borderColor, borderRadius = (borderRadius, borderRadius, borderRadius, borderRadius))

		drawCenteredString(fg, t, x, y, width, height, font, foreground)
	}

	def drawCenteredString(fg: FunGraphics, s: String, x: Int, y: Int, width: Int, height: Int, font: Font = defaultFont, color: Color = defaultColor): Unit = {
		val msgSize = fg.getStringSize(s, font)
		val dx = (width - msgSize.getWidth.toInt) / 2
		val dy = height - ((height - msgSize.getHeight.toInt) / 2) - 4
		fg.drawString(x + dx, y + dy, s, font, color)
	}

	def drawTextbox(fg: FunGraphics, x: Int, y: Int, width: Int, height: Int, t: String, background: Color, foreground: Color, borderWidth: Int = 1, borderColor: Color, font: Font = defaultFont): Unit = {
		fg.setColor(background)
		fg.drawFillRect(x, y, width, height)

		if (borderWidth > 0) {
			fg.setColor(borderColor)

			fg.drawFillRect(x, y, width, borderWidth)
			fg.drawFillRect(x, y + height - borderWidth, width, borderWidth)
			fg.drawFillRect(x, y, borderWidth, height)
			fg.drawFillRect(x + width - borderWidth, y, borderWidth, height)
		}

		val dy = height - ((height - fg.getStringSize(t, font).getHeight.toInt) / 2) - 4
		fg.drawString(x + 10, y + dy, t, font, foreground)
	}

	def computeImgByLuminosity(path: String, maxLevel: Int = 4): Array[GraphicsBitmap] = {
		val mainImg = new GraphicsBitmap(path)
		val imgArray: Array[GraphicsBitmap] = Array.fill(maxLevel+1)(new GraphicsBitmap(path))
		for (i <- 1 to maxLevel+1) {
			val scale = .5f + (i - 2.5f) / 2.5f
			val filteredImg = new BufferedImage(mainImg.mBitmap.getWidth, mainImg.mBitmap.getHeight, mainImg.mBitmap.getType)

			val rescaleOp = new RescaleOp(scale, 0f, null)
			rescaleOp.filter(mainImg.mBitmap, filteredImg)

			imgArray(i-1).mBitmap = filteredImg
		}
		imgArray
	}
}
