package chatroom.server;

import java.io.IOException;

public class ChatRoomServer {
    public static void main(String[] args) {
        System.out.println("Welcome to ChatRoom server");
        try {
            Server server = new Server(8888, UserMessagesHandler::new);
            server.start();
            System.out.println("Server started");
            System.out.println("Press ENTER to stop the server...");
            System.in.read();
            server.stop();
        } catch (IOException e) {
            System.out.println("Error:");
            System.out.println(e.getMessage());
        }
    }
}
