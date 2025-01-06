import hevs.graphics.FunGraphics

import java.awt.Color
import java.awt.event.{KeyAdapter, KeyEvent}
import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Motor extends App {
    // LAN variables
    val port = 9235                     // Port of the game communication       92 : Uranium atomic number - 235 : Uranium 235 is used in nuclear bombs
    var in: BufferedReader = _          // messages coming in  - destined to this
    var out: PrintWriter = _            // messages coming out - destined to the other part
    var serverSocket: ServerSocket = _  // Server socket
    var clientSocket: Socket = _        // Client socket
    var isHost = true                   // Boolean if the current instance is the host or not

    // Game variables
    var room: Room = _
    val cellSize = 40
    val diameter = (cellSize / 1.5).floor.toInt
    var fg: FunGraphics = _
    var isPlaying = true
    var gameInitialized = false

    println("Enter your choice:\nH - Host a game (Host)\nC - Join a game (Client)")
    scala.io.StdIn.readLine().toUpperCase match {
        case "H" => startHost()
        case "C" => startClient()
        case _ => println("Invalid choice, exiting...")
    }

    /**
     * Start the game setup and loop as the host (Player1)
     */
    def startHost(): Unit = {
        isHost = true
        serverSocket = new ServerSocket(port)
        val localIp = java.net.InetAddress.getLocalHost.getHostAddress
        println(s"Hosting on IP: $localIp:$port")

        println("Waiting for a client to connect...")

        clientSocket = serverSocket.accept()
        println("Client connected")

        initCommunication()

        initGame()
    }

    /**
     * Start the game setup and loop as the client (Player 2)
     */
    def startClient(): Unit = {
        isHost = false
        println("Enter the host's IP address: ")
        val hostIP = scala.io.StdIn.readLine()

        clientSocket = new Socket(hostIP, port)
        println(s"Connected to the server at $hostIP:$port")

        initCommunication()

        while (!gameInitialized)
            Thread.sleep(100)
        startGame()
    }

    /**
     * Initialize the discussion between the host and the client
     */
    def initCommunication(): Unit = {
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
        out = new PrintWriter(clientSocket.getOutputStream, true)

        listen()
    }

    /**
     * Send a message to the other player
     * @param s The message to be sent
     */
    def send(s: String): Unit = {
        println(s"Sent: $s")
        out.println(s)
    }

    /**
     * Listen for a new game info
     */
    def listen(): Unit = {
        Future {
            while (true) {
                val msg = in.readLine()
                if (msg == null || msg.equalsIgnoreCase("exit")) {
                    println("Other player disconnected")
                    if (isHost)
                        serverSocket.close()
                    else
                        clientSocket.close()
                    isPlaying = false
                    return
                }
                println(s"Received: $msg")
                updateGame(msg)
            }
        }
    }

    def initGame(): Unit = {
        room = new Room(20, 15)
        room.generateRoom()

        room.movePlayer(room.getPlayer(1), 0, 0)
        room.movePlayer(room.getPlayer(2), 14, 14)

        send(s"INIT${room.toString}")

        startGame()
    }

    /**
     * Start the game loop
     */
    def startGame(): Unit = {
        fg = new FunGraphics(cellSize * room.width, cellSize * room.height, "BomberGrid")

        fg.setKeyManager(new KeyAdapter {
            override def keyPressed(e: KeyEvent): Unit = {
                val keyToMove = Map(
                    'w' -> 1,
                    'd' -> 2,
                    's' -> 4,
                    'a' -> 8
                )

                val moveToVerify = keyToMove.getOrElse(e.getKeyChar, 0)

                if (moveToVerify != 0) {
                    val player = room.getPlayer(if (isHost) 1 else 2)
                    val move = room.tryMove(player, moveToVerify)
                    if (move._1)
                        send(s"UPDTMOVE${player.playerId};${move._2}:${move._3}")
                }

                if (e.getKeyCode == KeyEvent.VK_SPACE) {
                    val player = room.getPlayer(if (isHost) 1 else 2)
                    if (player.canDrop) {
                        val pos = player.getPos
                        val timestamp = System.currentTimeMillis()
                        val bomb = Bomb(pos._1, pos._2, timestamp)

                        room.addBomb(bomb)

                        send(s"UPDTDROP${pos._1}:${pos._2};$timestamp")
                    }
                }
            }
        })

        while (isPlaying) {
            room.checkExplosions()

            displayGame()

            fg.syncGameLogic(60)
        }
    }

    /**
     * Update the game state with the received message
     * @param msg The message used to update the game
     */
    def updateGame(msg: String): Unit = {
        if (msg.startsWith("INIT")) {
            // INIT4x5;19:0:1:5:0-2:0:0:0:6-...
            val msgInfo = msg.substring(4).split(";")
            val dim = msgInfo(0).split("x")
            room = new Room(dim(0).toInt, dim(1).toInt)

            val r = msgInfo(1).split("-").map(_.split(":").map(_.toInt))
            for (i <- r.indices;
                 j <- r(i).indices) {
                if ((r(i)(j) & 16) == 16) {
                    r(i)(j) -= 16
                    room.movePlayer(room.getPlayer(1), i, j)
                }
                if ((r(i)(j) & 32) == 32) {
                    r(i)(j) -= 32
                    room.movePlayer(room.getPlayer(2), i, j)
                }
                room.getRoom(i)(j).buildWalls(r(i)(j))
            }
            gameInitialized = true
        } else if (msg.startsWith("UPDT")) {
            val newMsg = msg.substring(4)
            if (newMsg.startsWith("MOVE")) {
                // UPDTMOVE1;3:4
                val moveInfo = newMsg.substring(4).split(";")
                val playerId = moveInfo(0).toInt
                val pos = moveInfo(1).split(":").map(_.toInt)
                room.movePlayer(room.getPlayer(playerId), pos(0), pos(1))
            } else if (newMsg.startsWith("DROP")) {
                // UPDTDROP3:4;12335781
                val dropInfo = newMsg.substring(4).split(";")
                val pos = dropInfo(0).split(":").map(_.toInt)
                val timeDropped = dropInfo(1).toLong
                room.addBomb(Bomb(pos(0), pos(1), timeDropped))
            }
        } else if (msg.startsWith("WIN")) {
            // WIN2
            val winnerId = msg.substring(3)
            isPlaying = false
        } else {
            println(s"Incorrect message, skipping it: $msg")
        }
    }

    /**
     * End the game with a winner
     * @param winnerId The winner of the game
     */
    def endGame(winnerId: Int): Unit = {
        send(f"WIN$winnerId")
        isPlaying = false
        // TODO: Handle further end of game
    }

    /**
     * Render the game with FunGraphics
     */
    def displayGame(): Unit = {
        fg.frontBuffer.synchronized {
            fg.clear(Color.WHITE)
            for (i <- 0 until room.width;
                 j <- 0 until room.height) {
                val x = cellSize * i
                val y = cellSize * j

                fg.setColor(Color.BLACK)
                val walls = room.getRoom(i)(j).getWalls
                if ((walls & 1) != 0)       // Upper wall
                    fg.drawLine(x, y, x + cellSize, y)
                if ((walls & 2) != 0)       // Right wall
                    fg.drawLine(x + cellSize, y, x + cellSize, y + cellSize)
                if ((walls & 4) != 0)       // Bottom wall
                    fg.drawLine(x, y + cellSize, x + cellSize, y + cellSize)
                if ((walls & 8) != 0)       // Left wall
                    fg.drawLine(x, y, x, y + cellSize)

                room.getActiveBombs.foreach { bomb =>
                    val x = bomb.x * cellSize + cellSize / 4
                    val y = bomb.y * cellSize + cellSize / 4
                    fg.setColor(Color.RED)
                    fg.drawFilledCircle(x, y, cellSize / 2)
                }

                val posP1 = room.getPlayer(1).getPos
                fg.setColor(Color.CYAN)
                fg.drawFilledCircle(posP1._1 * cellSize + (cellSize - diameter) / 2, posP1._2 * cellSize + (cellSize - diameter) / 2, diameter)
                val posP2 = room.getPlayer(2).getPos
                fg.setColor(Color.ORANGE)
                fg.drawFilledCircle(posP2._1 * cellSize + (cellSize - diameter) / 2, posP2._2 * cellSize + (cellSize - diameter) / 2, diameter)
            }
        }
    }
}