🎮 Multiplayer Game Suite

A Java-based multiplayer game suite built using socket programming. The system includes a central game server and multiple clients that can connect to play games in real time.


🚀 Features

- ✅ Multiplayer support over a local or remote network
- ✅ Real-time interaction between players
- ✅ Modular game logic structure
- ✅ Clean object-oriented Java code
- ✅ Lightweight and easy to run


🛠️ Technologies Used

- Java (Core + Sockets)
- Java Threads
- Object-Oriented Programming (OOP)


📁 Project Structure

MultiplayerGameSuite/
├── GameClient.java       # Client-side code
├── GameServer.java       # Server-side logic
├── GameLogic.java        # Shared game logic between server and clients


🧪 How to Run:

1. Compile all `.java` files:

   ```bash
   javac *.java
   ```

2. Start the server (in one terminal window):

   ```bash
   java GameServer
   ```

3. Start the client(s) (in separate terminal windows):

   ```bash
   java GameClient
   ```

4. The clients will connect to the server and can start interacting based on the implemented game logic.

⚠️ By default, it uses `localhost`. To run over LAN, update the IP in `GameClient.java`.


🎯 Example Use Cases

- Simple multiplayer games like Rock-Paper-Scissors
- Turn-based card or logic games
- Teaching socket communication in Java


👤 Author

- Praneeth N R
   M.Sc. Data Science, Coimbatore Institute of Technology


📬 Contact

For any queries, feel free to reach out:
- 📧 Email: praneethnr0505@gmail.com
- 💼 LinkedIn: www.linkedin.com/in/praneeth-n-r-96a079340
