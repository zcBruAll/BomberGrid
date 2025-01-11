import hevs.graphics.FunGraphics

import java.awt.{Color, Font}

object GraphicMotor {
	private val defaultFont = new Font("SansSerif", Font.BOLD, 24)
	private val defaultColor = Color.BLACK

	/**
	 * Render the game with FunGraphics
	 */
	def displayGame(fg: FunGraphics, room: Room, cellSize: Int, playerId: Int): Unit = {
		fg.clear(Color.LIGHT_GRAY)
		for (i <- 0 until room.width;
				 j <- 0 until room.height) {
			val x = 25 + cellSize * i
			val y = 25 + cellSize * j

			fg.setColor(Color.BLACK)
			val walls = room.getRoom(i)(j).getWalls
			if ((walls & 1) != 0) // Upper wall
				fg.drawLine(x, y, x + cellSize, y)
			if ((walls & 2) != 0) // Right wall
				fg.drawLine(x + cellSize, y, x + cellSize, y + cellSize)
			if ((walls & 4) != 0) // Bottom wall
				fg.drawLine(x, y + cellSize, x + cellSize, y + cellSize)
			if ((walls & 8) != 0) // Left wall
				fg.drawLine(x, y, x, y + cellSize)
		}

		room.getActiveBombs.foreach { bomb =>
			val x = 25 + bomb.x * cellSize + cellSize / 4
			val y = 25 + bomb.y * cellSize + cellSize / 4
			fg.setColor(Color.RED)

			val bombSize = Motor.bombImg.getWidth
			val scale = (cellSize * .75D) / bombSize
			val ds = ((bombSize * scale) / 3).toInt
			fg.drawTransformedPicture(x + ds, y + ds, 0, scale, Motor.bombImg)
		}

		val posP1 = room.getPlayer(1).getPos
		val p1Size = Motor.player1Img.getHeight
		val p1Scale = cellSize / p1Size.toDouble
		fg.drawTransformedPicture(50 + posP1._1 * cellSize + (cellSize - (p1Size * p1Scale).toInt) / 2, 50 + posP1._2 * cellSize + (cellSize - (p1Size * p1Scale).toInt) / 2, 0, p1Scale, Motor.player1Img)

		val posP2 = room.getPlayer(2).getPos
		val p2Size = Motor.player1Img.getHeight
		val p2Scale = cellSize / p1Size.toDouble
		fg.drawTransformedPicture(50 + posP2._1 * cellSize + (cellSize - (p2Size * p2Scale).toInt) / 2, 50 + posP2._2 * cellSize + (cellSize - (p2Size * p2Scale).toInt) / 2, 0, p2Scale, Motor.player2Img)

		val player = room.getPlayer(playerId)
		val lifePercent = player.life
		drawRectangle(fg, 25 + room.width * cellSize - 100 - 10 - 4, 25 + 10 + 4, 100 + 4, 10, Color.WHITE, 2, Color.BLACK, (Color.LIGHT_GRAY, Color.LIGHT_GRAY, Color.LIGHT_GRAY, Color.LIGHT_GRAY), (5, 5, 5, 5))
		drawRectangle(fg, 25 + room.width * cellSize - 100 - 10 - 2, 25 + 10 + 6, lifePercent, 6, if (lifePercent > 75) Color.GREEN else if (lifePercent > 25) Color.ORANGE else Color.RED, 0, Color.BLACK, if (lifePercent == 100) (Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK) else (Color.BLACK, Color.WHITE, Color.BLACK, Color.WHITE), (3, 3, 3, 3))
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
}
