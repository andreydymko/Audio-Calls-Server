package com.andreydymko.Connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConnectionThread extends Thread {
    private final static int MAX_CONNECTIONS = 50000;
    private final static int MAX_TIME_TO_WAIT = 5000;
    private final static int MAX_THREADS_NUM = 40;

    private final ServerSocket serverSocket;
    private final ExecutorService executorService;

    public ConnectionThread(InetAddress addressToBindTo, int port) throws IOException {
        serverSocket = new ServerSocket(port, MAX_CONNECTIONS, addressToBindTo);
        serverSocket.setSoTimeout(MAX_TIME_TO_WAIT);

        executorService = Executors.newFixedThreadPool(MAX_THREADS_NUM);
    }

    public void run() {
        Socket socket = null;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                socket = serverSocket.accept();
            } catch (SocketTimeoutException ex) {
                continue;
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (socket == null) {
                continue;
            }
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            System.out.println("adding new socket with address - " + socket.getInetAddress().toString());
            executorService.submit(new ClientServingRunnable(socket));
        }

        threadStopProcedures();
    }

    private void threadStopProcedures() {
        shutdownAndAwaitTermination(executorService);
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
