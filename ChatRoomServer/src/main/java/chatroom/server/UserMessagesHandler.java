package chatroom.server;

import java.io.*;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;

// @ThreadSafe
public class UserMessagesHandler implements ConnectionsHandler {
    private final ExecutorService messageReaders = Executors.newCachedThreadPool();
    private final MessagesBroadcaster messagesBroadcaster = new MessagesBroadcaster();

    private final Set<Socket> clientSockets = Collections.newSetFromMap(
            new ConcurrentHashMap<>()
    );

    @Override
    public void handle(Socket socket) {
        clientSockets.add(socket);
        try {
            ChatUser user = new ChatUser(socket);
            messagesBroadcaster.register(user, socket.getOutputStream());
            ReadTask readTask = new ReadTask(user, socket.getInputStream(), messagesBroadcaster);
            messageReaders.execute(readTask);
        } catch(IOException e) {
            String ip = socket.getInetAddress().getHostAddress();
            System.err.println("Failed to handle client connection from " + ip);
        }
    }

    @Override
    public void closeConnections() throws IOException {
        for(Socket s : clientSockets) {
            s.close();
        }
        clientSockets.clear();
        messagesBroadcaster.unregisterAll();
        shutdownExecutor(messageReaders);
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if(!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if(!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Failed to shutdown executor");
                }
            }
        } catch(InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class ReadTask implements Runnable {
        private final ChatUser user;
        private final InputStream userIS;
        private final MessagesBroadcaster messagesBroadcaster;

        private ReadTask(ChatUser user, InputStream userIS,
                         MessagesBroadcaster messagesBroadcaster) {
            this.user = user;
            this.userIS = userIS;
            this.messagesBroadcaster = messagesBroadcaster;
        }

        @Override
        public void run() {
            try(BufferedReader in = new BufferedReader(new InputStreamReader(userIS))) {
                String input;
                while ((input = in.readLine()) != null) {
                    messagesBroadcaster.broadcast(input);
                }
            } catch(IOException e) {
                messagesBroadcaster.unregister(user);
            }
        }
    }
}
