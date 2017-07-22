package chatroom.server;

import java.net.Socket;

/**
 * Wrapper for user connection socket. Used to protect
 * socket from incorrect concurrent access.
 */
public class ChatUser {
    private final Socket socket;

    public ChatUser(Socket socket) {
        this.socket = socket;
    }

    public String getIP() { return socket.getInetAddress().toString(); }
}
