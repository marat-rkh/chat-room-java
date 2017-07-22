package chatroom.server;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.*;

// @ThreadSafe
public class MessagesBroadcaster {
    private final ConcurrentMap<ChatUser, AsyncMessageSender> users =
            new ConcurrentHashMap<>();

    public void register(ChatUser user, OutputStream userOS) {
        users.put(user, new AsyncMessageSender(userOS));
        System.out.println("Registered user: " + user.getIP());
    }

    public void unregister(ChatUser user) {
        AsyncMessageSender sender = users.remove(user);
        if(sender != null) {
            sender.shutdown();
            System.out.println("Unregistered user: " + user.getIP());
        }
    }

    public void unregisterAll() {
        for(ChatUser user : users.keySet()) {
            unregister(user);
        }
    }

    public void broadcast(String message) {
        for(AsyncMessageSender sender : users.values()) {
            sender.send(message);
        }
    }

    private static class AsyncMessageSender {
        private final PrintWriter messageWriter;
        // This one MUST be SingleThreadExecutor as
        // OutputStream is not thread safe
        private final ExecutorService messageSender = Executors.newSingleThreadExecutor();

        private AsyncMessageSender(OutputStream userOS) {
            this.messageWriter = new PrintWriter(userOS, true);
        }

        void send(String message) {
            messageSender.execute(() -> {
                messageWriter.println(message);
            });
        }

        void shutdown() {
            shutdownExecutor(messageSender);
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
}
