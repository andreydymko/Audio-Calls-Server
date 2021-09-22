package com.andreydymko.Call;

import com.andreydymko.ArrayLocalUtils;
import com.andreydymko.Connection.ConnectedUserModel;
import com.andreydymko.User.UserModel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class SoundThread extends Thread {
    private final static int MAX_TIME_TO_WAIT = 200;
    private final static int BUFFER_SIZE = 8192; //3584; // pcm16bit, mono, 44,1 khz
    private final List<ConnectedUserModel> usersList;
    private final List<DatagramPacket> packets;
    private DatagramSocket datagramSocket;

    public SoundThread() {
        try {
            datagramSocket = new DatagramSocket();
            //datagramSocket.setSoTimeout(MAX_TIME_TO_WAIT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        usersList = new ArrayList<>();
        packets = new ArrayList<>();
    }

    public int getUDPPort() {
        return datagramSocket.getLocalPort();
    }

    public boolean removeUser(ConnectedUserModel userModel) {
        return usersList.remove(userModel);
    }

    public boolean removeUser(UserModel userModel) {
        return usersList.removeIf(connectedUserModel -> userModel.equals(connectedUserModel.getUserModel()));
    }

    public boolean addUser(ConnectedUserModel userModel) {
        return ArrayLocalUtils.addUnique(usersList, userModel);
    }

    public int getUsersCount() {
        return usersList.size();
    }

    public List<ConnectedUserModel> getUsersList() {
        return usersList;
    }

    public void run() {
        byte[] buffer1 = new byte[BUFFER_SIZE];
        DatagramPacket packet1 = new DatagramPacket(buffer1, buffer1.length);
        packet1.setPort(datagramSocket.getLocalPort());

        byte[] buffer2 = new byte[BUFFER_SIZE];
        DatagramPacket packet2 = new DatagramPacket(buffer2, buffer2.length);
        packet1.setPort(datagramSocket.getLocalPort());

        ConnectedUserModel userModel;
        int i, size;
        while (!Thread.interrupted()) {
            size = usersList.size();
            try {
                // TODO race condition?
                //  since we can receive a lot of packets from one person, while haven't received any from the other
                datagramSocket.receive(packet1);
                datagramSocket.receive(packet2);
            //} catch (SocketTimeoutException ignore){
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (i = 0; i < size; i++) {
                try {
                    userModel = usersList.get(i);
                } catch (IndexOutOfBoundsException ignore) {
                    continue;
                }
                if (!userModel.getInetAddress().equals(packet1.getAddress())) {
                    packet1.setAddress(userModel.getInetAddress());
                    try {
                        // TODO maybe we should mix sounds together? probably
                        datagramSocket.send(packet1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (!userModel.getInetAddress().equals(packet2.getAddress())) {
                    packet2.setAddress(userModel.getInetAddress());
                    try {
                        // TODO maybe we should mix sounds together? probably
                        datagramSocket.send(packet2);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        System.out.println("Call thread is finishing");
        datagramSocket.close();
    }
}
