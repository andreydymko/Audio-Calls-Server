package com.andreydymko.Connection;

import java.net.Socket;

public class ClientServingRunnable implements Runnable {
    private final ConnectionModel activeConnection;

    public ClientServingRunnable(Socket socket) {
        activeConnection = new ConnectionModel(socket);
    }

    public void run() {
        System.out.println("Thread starting");
        while (!Thread.currentThread().isInterrupted() && !activeConnection.isConnectionShouldBeClosed()) {
            activeConnection.drainMsgsQueue();
            activeConnection.resolveRequest();
        }
        finishingProcedures();
        System.out.println("Thread closing");
    }

    private void finishingProcedures() {
        activeConnection.closeConnection();
    }
}
