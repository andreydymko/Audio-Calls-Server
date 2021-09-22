package com.andreydymko;

import com.andreydymko.Call.CallManager;
import com.andreydymko.Connection.ConnectionThread;
import com.andreydymko.User.UserManager;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        String serverIp = "192.168.1.100";
        int serverPort = 8356;
        System.out.println("Server starting on: " + serverIp + ":" + serverPort);
        System.out.println("Starting...");
        ConnectionThread mainThread = null;
        try {
            mainThread = new ConnectionThread(InetAddress.getByName(serverIp), serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mainThread == null) {
            System.out.println("Could not start the server. Stopping.");
            return;
        }
        mainThread.start();
        System.out.println("Started, enter \"stop\" to stop");

        UserManager.getInstance().registerNewUser("andrey12345", "11111111");
        UserManager.getInstance().registerNewUser("andreydymko123@gmail.com", "11111111");
        UserManager.getInstance().registerNewUser("11111111", "11111111");

        Scanner scanner = new Scanner(System.in);
        String input;
        do {
            input = scanner.nextLine();
        } while (!(input.equals("stop")));

        System.out.println("Stopping...");
        CallManager.getInstance().endAllCalls();
        mainThread.interrupt();
        try {
            mainThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Server stopped");
    }
}
