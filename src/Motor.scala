import GraphicMotor._
import hevs.graphics.FunGraphics

import java.awt.{Color, Font, Rectangle}
import java.awt.event.{KeyAdapter, KeyEvent, MouseAdapter, MouseEvent}
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
    var isPlaying = true
    var gameInitialized = false

    // Front-end variables
    val cellSize = 40
    val diameter = (cellSize / 1.5).floor.toInt
    val fg: FunGraphics = new FunGraphics(50 + cellSize * 20, 50 + cellSize * 15, "BomberGrid")
    val menuWidth = 300

    displayMenu("Start menu")

    def displayMenu(t: String): Unit = {
        val hostButton = new Rectangle((fg.width - menuWidth) / 2, fg.height / 2 - 105, 300, 50)
        val joinButton = new Rectangle((fg.width - menuWidth) / 2, fg.height / 2 - 35, 300, 50)
        val exitButton = new Rectangle((fg.width - menuWidth) / 2, fg.height / 2 + 35, 300, 50)

        fg.clear()

        fg.addMouseListener(new MouseAdapter {
            override def mouseClicked(e: MouseEvent): Unit = {
                val x = e.getX
                val y = e.getY

                if (hostButton.contains(x, y))
                    startHost()
                else if (joinButton.contains(x, y))
                    startClient()
                else if (exitButton.contains(x, y))
                    quit()
            }
        })

        val fontTitle = new Font("SansSerif", Font.BOLD, 36)
        val fontText = new Font("SansSerif", Font.BOLD, 24)

        drawCenteredString(fg, t, (fg.width - menuWidth) / 2, fg.height / 2 - 175, 300, 50, fontTitle)

        drawButton(fg, hostButton.x, hostButton.y, hostButton.width, hostButton.height, "Host a game", Color.CYAN, Color.BLACK, 2, Color.BLACK, fontText)
        drawButton(fg, joinButton.x, joinButton.y, joinButton.width, joinButton.height, "Join a game", Color.CYAN, Color.BLACK, 2, Color.BLACK, fontText)
        drawButton(fg, exitButton.x, exitButton.y, exitButton.width, exitButton.height, "Quit", Color.CYAN, Color.BLACK, 2, Color.BLACK, fontText)
    }

    /**
     * Start the game setup and loop as the host (Player1)
     */
    def startHost(): Unit = {
        isHost = true
        serverSocket = new ServerSocket(port)
        val localIp = java.net.InetAddress.getLocalHost.getHostAddress

        fg.clear()

        val fontTitle = new Font("SansSerif", Font.PLAIN, 24)
        val fontIp = new Font("SansSerif", Font.BOLD, 24)
        val fontSubtitle = new Font("SansSerif", Font.PLAIN, 18)

        drawCenteredString(fg, "Hosting on:", (fg.width - menuWidth) / 2, fg.height / 2 - 45, 300, 24, fontTitle)
        drawCenteredString(fg, s"$localIp", (fg.width - menuWidth) / 2, fg.height / 2 - 15, 300, 24, fontIp)
        drawCenteredString(fg, "Waiting for a client", (fg.width - menuWidth) / 2, fg.height / 2 + 15, 300, 18, fontSubtitle)

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

        fg.clear()

        var hostIp = ""

        val fontInstruction = new Font("SansSerif", Font.PLAIN, 24)
        drawCenteredString(fg, "Enter the host IP", (fg.width - menuWidth) / 2, fg.height / 2 - 85, 300, 30, fontInstruction)
        drawButton(fg, (fg.width - menuWidth) / 2, fg.height / 2 - 25, 300, 50, s"$hostIp", Color.WHITE, Color.BLACK, 2, Color.BLACK, fontInstruction)

        println("Enter the host's IP address: ")
        val hostIP = scala.io.StdIn.readLine()

        clientSocket = new Socket(hostIP, port)
        println(s"Connected to the server at $hostIP:$port")

        initCommunication()

        while (!gameInitialized)
            Thread.sleep(100)
        startGame()
    }

    def quit(): Unit = {
        System.exit(0)
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
        fg.clear()

        fg.mainFrame.addKeyListener(new KeyAdapter {
            override def keyPressed(e: KeyEvent): Unit = {
                if (isPlaying) {
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
            }
        })

        fg.mainFrame.requestFocus()

        while (isPlaying) {
            room.checkExplosions()

            displayGame(fg, room, cellSize, diameter)

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
}