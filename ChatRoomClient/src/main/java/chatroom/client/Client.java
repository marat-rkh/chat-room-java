package chatroom.client;

import java.io.*;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void run(String hostName) {
        try(
                Socket socket = new Socket(hostName, 8888);
                Scanner serverReader = new Scanner(socket.getInputStream());
                PrintWriter serverWriter = new PrintWriter(socket.getOutputStream());
                ReadTask readTask = new ReadTask(serverReader);
                BufferedReader userInputReader = new BufferedReader(
                    new InputStreamReader(System.in));
        ) {
            System.out.println("> Welcome to ChatRoom client!");
            System.out.println("> Connection to server is successful");
            executor.execute(readTask);
            String userName = getUserName(userInputReader);
            System.out.println("> Say hello! (to exit type ':q')");
            userInputLoop(userName, userInputReader, serverWriter);
        } catch (IOException e) {
            System.err.println("Failed to run client");
            System.err.println(e.getMessage());
        }
        shutdownExecutor(executor);
    }

    private String getUserName(BufferedReader userInputReader) throws IOException {
        System.out.println("> Pick a nickname:");
        return userInputReader.readLine();
    }

    private void userInputLoop(String userName,
                               BufferedReader userInputReader,
                               PrintWriter serverWriter) throws IOException {
        String message;
        while((message = userInputReader.readLine()) != null) {
            if(message.equals(":q")) {
                return;
            }
            serverWriter.println(userName + ": " + message);
            if(serverWriter.checkError()) {
                System.err.println("> You have been disconnected from the server");
                return;
            }
        }
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if(!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if(!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Failed to stop executor");
                }
            }
        } catch(InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class ReadTask implements Runnable, AutoCloseable {
        private final Scanner reader;
        private volatile boolean isStopped = false;

        private ReadTask(Scanner reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            String message;
            try {
                while(!isStopped && (message = reader.nextLine()) != null) {
                    System.out.println(message);
                }
            } catch (NoSuchElementException e) {
                // just exit
            }
        }

        @Override
        public void close() {
            isStopped = true;
        }
    }
}
