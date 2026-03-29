# P2P Chat Application

A peer-to-peer (P2P) chat system consisting of a Kotlin/JavaFX desktop client and a Node.js signaling server (STUN/Signaling) for peer discovery.

## 📁 Project Structure

The repository is divided into two main parts:

- **`app/`**: A multi-module Gradle project for the chat client.
  - `:chat-ui`: JavaFX-based user interface.
  - `:chat-network`: Peer discovery logic and UDP-based communication.
  - `:chat-common`: Shared data models (messages, peer info) and configuration logic.
  - `:utils`: Helper functions and shared utilities.
  - `buildSrc`: Shared build logic for Kotlin/JVM projects.
- **`stun-server/`**: A simple Node.js UDP signaling server to facilitate peer discovery between clients.

## 🛠️ Requirements

### Chat Client (`app/`)
- **JDK 23** (specified in `build.gradle.kts` toolchain).
- **Gradle** (Wrapper included).

### Signaling Server (`stun-server/`)
- **Node.js** (v14+ recommended).
- **npm** (for managing development dependencies).

## 🚀 Setup & Run

### 1. Signaling Server
The server must be running for clients to discover each other.

```powershell
cd stun-server
npm install
npm start
```
By default, it listens for UDP packets on port **5000**.

### 2. Chat Client
Run the JavaFX application using the Gradle wrapper.

```powershell
cd app
./gradlew run
```

Other useful Gradle tasks:
- `./gradlew build`: Build the application.
- `./gradlew check`: Run all tests.
- `./gradlew clean`: Clean build outputs.

## ⚙️ Configuration

### App Configuration
Client settings are managed in `app/config.properties`:

| Property | Description | Default |
|----------|-------------|---------|
| `stun.server.address` | The address of the signaling server. | `http://localhost` |
| `stun.server.port` | The UDP port of the signaling server. | `5000` |
| `user.name` | Display name for the user. | `Stefan` |

> **TODO:** Support environment variables for overriding configuration properties.

## 🧪 Testing

- **Unit Tests:** Located in `app/utils/src/test`.
- Run tests using:
  ```powershell
  ./gradlew test
  ```

## 📜 Scripts

| Directory | Command | Description |
|-----------|---------|-------------|
| `app/` | `./gradlew run` | Starts the JavaFX client. |
| `stun-server/` | `npm start` | Starts the UDP signaling server. |
| `stun-server/` | `npm run dev` | Starts the server with `nodemon` for development. |

## ⚖️ License

**TODO:** Add license information (e.g., MIT, Apache-2.0).
