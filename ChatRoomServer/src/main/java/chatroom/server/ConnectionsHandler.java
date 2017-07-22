package chatroom.server;

import java.io.IOException;
import java.net.Socket;

public interface ConnectionsHandler {
    void handle(Socket socket);
    void closeConnections() throws IOException;
}
