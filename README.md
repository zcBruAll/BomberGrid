# BomberGrid
BomberGrid is a 2D grid based game working on LAN. It was made in Scala using the FunGraphics library.
Players navigate a bomber through a grid and stragetically drop bombs to try to defeat the opponent.

## Table of Content
1. Features
2. Setup
3. Gameplay instructions
4. How the project works

## Features
- **Grid based navigation**: Move your bomber through the grid strategically.
- **Bomb mechanics**: Place your bomb to defeat your opponent
- **Graphics and Inputs**: Using the FunGraphics library to display and manage inputs

## Setup
### Prerequisites
- IntelliJ IDEA installed
- Scala plugin in IntelliJ IDEA
- Git installed either on IntelliJ or cmd
- A PC supporting at least 3 threads (Should be the case but we never know)

### Installation
- Clone the repository
  - Using Terminal (cmd):
    - Navigate to the destination folder using `cd`
    - clone the repo using this command: `git clone https://github.com/zcBruAll/BomberGrid.git`
    - Open the project folder with IntelliJ
  - Using IntelliJ
    - TASKS
- In IntelliJ
  - Ensure you're on branch `main`
  - Add the `fungraphics-1.5.15.jar` library to the project
    - Right click on the file > Add as library
  - Set the `src` directory as the `Source Root`
    - Right click on the directory > Set directory as > Source Root  

## Gameplay instructions
- Start 2 instances of the project
  - Either on 2 different PC in the same network
  - Either on a single PC. IntelliJ might not let you start 2 instance of the same project so you'll need to clone in another directory the repository
- One of the instance has to host the game and the other to join it:
  - The host gives its local IP to the client (Displayed when click on `Host a game`)
  - The client enter the host local IP (After clicking on the `Join a game` button)
- When connected the game start directly
- Move in the grid with `W`, `A`, `S`, `D` keys
- Drop a bomb with `SPACE_BAR` key
- Try not to get exploded by the bombs (you're not protected against your own bombs) while trying to explode the other player
  - Your life bar is displayed on the top right of the game map
  - The cooldown before dropping another bomb is displayed just under the life bar
- The game end when one of the 2 player's life reach 0
- To replay, repeat from the second task
 
If one of the player quits the game before it ended, the other player gent sent back to the main menu page

## How the project works

### LAN functionality
The hierarchy is a Master-Slave structure. One of the player is the host and the other one is the client.
<details>
  <summary>The game is hosted on the host computer on port 9235. Can you guess why?</summary>
  Uranium atomic number is 92 and one of its isotope: Uranium-235 (Used in nuclear bombs)
</details>

To make the connection, we use the Socket and ServerSocket from java.net. When the connection is initialized, it creates an infinite loop in another thread listening to incoming messages. This way, the game can be directly udpated and latence is greatly decreased.  
The messages are formed from, at least, 1 keyword which defines the type of the message
- Game initialization message: Before the game start, the host generate the room and send it to the client to let him generate the same room to play in.  
  The message looks like this: `INIT3x4;25:8:12-1:0:4-1:0:4-3:2:38`
  - Start with the keyword: `INIT`
  - Followed by 2 numbers defining the size of the grid separated by an `x`
  - A semi-colon `;` to separate the size of the room from its content
  - Then the content of the room that is formed like this:
    - Columns info are separated from a tiret `-` meaning a new column
    - Each column have their cells separated by a colon `:`
    - The value of the cells is the sum of its elements where each one has a different binary value:
      - **1** `0b1`: Top wall
      - **2** `0b10`: Right wall
      - **4** `0b100`: Bottom wall
      - **8** `0b1000`: Left wall
      - **16** `0b10000`: Player 1
      - **32** `0b100000`: Player 2
- Game update message: The moment the game start, players can move freely in the room and also can drop bombs at any time so we need to inform the other instance of the new game status.
  The message can look like this: `UPDTMOVE1;2:3` or `UPDTDROP2:3;1736515596`
  - Start with the keyword: `UPDT`
  - Based on the action:
    - If the one of the player moved:
      - Another keyword follows: `MOVE`
      - The id of the player that moved (1 for the host, 2 for the client)
      - A semicolon separating the player's id from the new coordinates
      - coordinates of the new position separated by a colon `:`
    - If a bomb is dropped:
      - Another keyword follows: `DROP`
      - The coordinates of the position, separated by a colon `:`, where the bomb is dropped follows
      - The timestamp of when the bomb was dropped
- Win of the game: When one of the instance calculates a kill, it sends a packet with the game winner
  The message can be this: `WIN2`
  - Formed by the keyword `WIN` and followed by the id of the player that won
- Disconnection of the game: It can happen that one of the 2 player disconnects from the game while it's not over. When this happen the connection needs to be closed and the game to be ended.
  The message is the keword: `EXIT`

### Front-end
An infinite loop in a separated thread manage the entire graphic interface. A page id is used to know what needs to be displayed. The GUI is refreshed at a rate of 60 fps.  
Buttons and textbox are simulated by mixing the mouse and key listeners with the FunGraphics library. Except the text, the plane in the main menu, the bombers and the bombs nothing is more than mix of rectangles and circles.

### Back-end
More practically, the room is made of a 2D `Array` of the custom type `Cell`. Each of these cells have 2 Int values: one for the walls, another for the players id. As explained eariler in the game initialization message, they're just sum of binary values that follows a strict rule:
- **1** `0b1`: Top wall
- **2** `0b10`: Right wall
- **4** `0b100`: Bottom wall
- **8** `0b1000`: Left wall
- **16** `0b10000`: Player 1
- **32** `0b100000`: Player 2
`toInt` and `toString` function returns the sum of the 2 values. Since the player id can only by 1 or 2, it's multiplied by 16 to output the correct binary value.
Everywhere in the game, the walls are manipulated by using their binary value

Blasts damage are first calculated based on their distance from each player. Damage goes from 25 to 75 within an area of maximum 3. The closer the bomber, the greater the damage.  
Calculated the same way as the fog, bombs can't damage through walls. To check if a wall was between the player and the bomb we followed these steps:
- Take the equation of the line (where values are the index in the grid) between the player and the bomb
- Retrieve each of the around cells that have at least one wall and share a point with the line
- Calculate again the equation of the line but with the size of the cells displayed in the GUI
- Check if any of the walls of the retrieved cells share a point with the line, if so, the wall obstructs the blast or vision

The fog around the player is calculated by the distance from the player to the targeted cell. The player has a Field Of View (Fov) of maximum 5.  
As it was said earlier, the vision obstruction is calculated in the same way as blasts (Why wouldn't they?).  
Since the floor, the bombers and bombs are images and it didn't seemed that FunGraphics can draw rgba pixels, a little trick was used.
As for the buttons and textboxes, luminosity is fake (In our game at least, in real-life it's a real thing). The luminosity is faked by displaying a precise image.
For each element, they each have 5 different images (one for each level of luminosity), based on the result of the distance calculation, a specific image is displayed.
For example here are 3 of the images for the ground:
{Floor at lowest luminosity} - {Floor at mid luminosity} - {Floor at full luminosity}
