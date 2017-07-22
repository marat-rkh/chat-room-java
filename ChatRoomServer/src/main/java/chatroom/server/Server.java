package chatroom.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

// @NotThreadSafe
public final class Server {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ServerSocket serverSocket;
    private final ConnectionsHandler handler;

    private final Object lifecycleLock = new Object();
    // @GuardedBy("lifecycleLock")
    private boolean isStopped = false;

    public Server(int portNumber, Supplier<ConnectionsHandler> handlerFactory) throws IOException {
        serverSocket = new ServerSocket(portNumber);
        this.handler = handlerFactory.get();
    }

    public void start() {
        executor.execute(() -> {
            try {
                while(true) {
                    Socket clientSocket = serverSocket.accept();
                    synchronized(lifecycleLock) {
                        if(isStopped) {
                            clientSocket.close();
                            return;
                        }
                        handler.handle(clientSocket);
                    }
                }
            } catch (IOException e) {
                System.out.println("Server is down");
            }
        });
    }

    public void stop() throws IOException {
        synchronized(lifecycleLock) {
            if(isStopped) {
                return;
            }
            handler.closeConnections();
            isStopped = true;
        }
        serverSocket.close();
        shutdownExecutor(executor);
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
}
