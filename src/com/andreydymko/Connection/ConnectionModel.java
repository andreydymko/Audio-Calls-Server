package com.andreydymko.Connection;

import com.andreydymko.ByteUtils;
import com.andreydymko.Call.CallManager;
import com.andreydymko.Call.CallModel;
import com.andreydymko.User.UserCallsChanged;
import com.andreydymko.User.UserManager;
import com.andreydymko.User.UserModel;
import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ConnectionModel implements UserCallsChanged {
    private final static int MAX_PINGS_TO_FAIL = 4;
    private final static int MAX_TIME_TO_ANSWER = 2000;
    private final static int MAX_TIME_TO_ADD_QUEUE = 300;
    private final static int MAX_TIME_TO_DRAIN_QUEUE = 100;

    private ConnectedUserModel connectedUser;
    private final Socket socket;
    private int failedPings = 0;

    private OutputStream socketOutputStream;
    private InputStream socketInputStream;

    private final BlockingQueue<byte[]> msgsToSend;

    public ConnectionModel(Socket socket) {
        this.socket = socket;

        try {
            this.socket.setSoTimeout(MAX_TIME_TO_ANSWER);
            this.socketOutputStream = socket.getOutputStream();
            this.socketInputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.connectedUser = new ConnectedUserModel(socket.getInetAddress());
        this.msgsToSend = new LinkedBlockingQueue<>();
    }

    public void resolveRequest() {
        try {
            byte requestCode = readByte();
            switch (requestCode) {
                case IncomingMessageType.REQUEST_REGISTER:
                    System.out.println("registering");
                    sendRegisterSuccess(requestCode);
                    break;
                case IncomingMessageType.REQUEST_LOGIN:
                    System.out.println("logging in");
                    sendLoginSuccess(requestCode);
                    break;
                case IncomingMessageType.REQUEST_LOGOUT:
                    System.out.println("logging out");
                    goOffline();
                    break;
                case IncomingMessageType.REQUEST_CREATE_CALL:
                    System.out.println("creating call");
                    createCall(requestCode);
                    break;
                case IncomingMessageType.REQUEST_ENTER_CALL:
                    System.out.println("entering call");
                    enterCall(requestCode);
                    break;
                case IncomingMessageType.REQUEST_QUIT_CALL:
                    System.out.println("quitting call");
                    quitCall();
                    break;
                case IncomingMessageType.ANSWER_ACCEPT_INCOMING_CALL:
                    System.out.println("accepting call");
                    acceptCall(requestCode);
                    break;
                case IncomingMessageType.ANSWER_REJECT_INCOMING_CALL:
                    System.out.println("rejecting call");
                    rejectCall();
                    break;
                case IncomingMessageType.REQUEST_INVITE_USERS:
                    System.out.println("inviting users to call");
                    inviteUsers(requestCode);
                    break;
                case IncomingMessageType.REQUEST_GET_USERS_LIST:
                    System.out.println("getting users list");
                    sendUsersListWithStatus(requestCode);
                    break;
                case IncomingMessageType.REQUEST_GET_USER_CALLS_LIST:
                    System.out.println("getting calls list");
                    sendUserCallsList(requestCode);
                    break;
                case IncomingMessageType.REQUEST_GET_CALL_USERS_LIST:
                    System.out.println("getting users inside call");
                    sendCallUsersList(requestCode);
                    break;
                case IncomingMessageType.ANSWER_PING:
                    break;
                default:
                    sendError(OutgoingMessageType.ANSWER_ERROR);
                    return;
            }
            pingSuccess();
        } catch (SocketTimeoutException e) {
            pingFailed();
            sendReceivePing();
        } catch (IOException | BufferUnderflowException e) {
            pingFailed();
            // todo remove print stack trace ???
            e.printStackTrace();
        }
    }

    private void sendPreDeterminedAnswer(byte answerCode, final byte[]... additionalData) {
        try {
            socketOutputStream.write(answerCode);
            for (byte[] additionalDatum : additionalData) {
                socketOutputStream.write(additionalDatum);
            }
        } catch (IOException e) {
            pingFailed();
            e.printStackTrace();
        }
    }

    private void sendPreDeterminedAnswer(byte answerCodes, byte additionalCode) {
        try {
            socketOutputStream.write(answerCodes);
            socketOutputStream.write(additionalCode);
        } catch (IOException e) {
            pingFailed();
            // todo e.printStackTrace();
        }
    }

    private boolean addMsgToQueue(byte[] msg) {
        try {
            msgsToSend.offer(msg, MAX_TIME_TO_ADD_QUEUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void drainMsgsQueue() {
        try {
            byte[] bytes;
            while ((bytes = msgsToSend.poll(MAX_TIME_TO_DRAIN_QUEUE, TimeUnit.MILLISECONDS)) != null) {
                socketOutputStream.write(bytes);
            }
        } catch (IOException | InterruptedException e) {
            pingFailed();
            e.printStackTrace();
        }
    }

    private void pingFailed() {
        failedPings++;
    }

    private void pingSuccess() {
        failedPings = 0;
    }

    public boolean isConnectionShouldBeClosed() {
        return failedPings >= MAX_PINGS_TO_FAIL
                || socket == null
                || socket.isClosed()
                || !socket.isConnected()
                || socket.isInputShutdown()
                || socket.isOutputShutdown();  // todo || this.connectedUser == null || !this.connectedUser.getUserModel().isOnline();
    }

    public void closeConnection() {
        if (connectedUser.getUserModel() != null) {
            connectedUser.removeOnUserDataChangeListener();
            connectedUser.getUserModel().setStatus(false);
            connectedUser.setUserModel(null);
            connectedUser = null;
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendReceivePing() {
        sendPreDeterminedAnswer(OutgoingMessageType.REQUEST_PING);
        resolveRequest();
    }

    private void sendRegisterSuccess(byte requestCode) throws IOException {
        String userLogin = readString();
        if (!UserManager.checkLogin(userLogin)) {
            sendError(requestCode);
            return;
        }

        String userPass = readString();
        if (!UserManager.checkPassword(userPass)) {
            sendError(requestCode);
            return;
        }

        UserModel userModel = UserManager.getInstance().registerNewUser(userLogin, userPass);
        if (userModel == null) {
            sendError(requestCode);
            return;
        }

        connectedUser.setUserModel(userModel);
        connectedUser.setOnUserDataChangeListener(this);
        connectedUser.getUserModel().setStatus(true);
        sendPreDeterminedAnswer(OutgoingMessageType.ANSWER_LOGIN_SUCCESS,
                ByteUtils.getUUIDBytes(connectedUser.getUserModel().getUUID()));
    }

    private void sendLoginSuccess(byte requestCode) throws IOException {
        String userLogin = readString();
        if (!UserManager.checkLogin(userLogin)) {
            sendError(requestCode);
            return;
        }

        String userPass = readString();
        if (!UserManager.checkPassword(userPass)) {
            sendError(requestCode);
            return;
        }

        UserModel userModel = UserManager.getInstance().getUser(userLogin, userPass);
        if (userModel == null) {
            sendError(requestCode);
            return;
        }

        connectedUser.setUserModel(userModel);
        connectedUser.setOnUserDataChangeListener(this);
        connectedUser.getUserModel().setStatus(true);
        sendPreDeterminedAnswer(OutgoingMessageType.ANSWER_LOGIN_SUCCESS,
                ByteUtils.getUUIDBytes(connectedUser.getUserModel().getUUID()));
    }

    private void goOffline() {
        if (!checkUser()) return;
        closeConnection();
    }

    private void createCall(byte requestCode) throws IOException {
        if (!checkUser()) return;

        int numOfUsers = readInt();
        if (numOfUsers <= 0) {
            sendError(requestCode);
            return;
        }
        ArrayList<UserModel> listOfUsers = new ArrayList<>(numOfUsers);

        UUID userUuid;
        UserManager userManager = UserManager.getInstance();
        for (int i = 0; i < numOfUsers; i++) {
            userUuid = readUUID();
            UserModel userModel = userManager.getUserFromUIID(userUuid);
            if (userModel == null) {
                sendError(requestCode);
                return;
            }
            listOfUsers.add(userModel);
        }
        CallModel callModel = CallManager.getInstance().createCall(this.connectedUser.getUserModel(), listOfUsers);
        if (callModel == null) {
            sendError(requestCode);
            return;
        }
        acceptCall(callModel);
        callModel.connectUser(this.connectedUser);
        sendCallInfo(callModel);
    }

    private void enterCall(byte requestCode) throws IOException {
        if (!checkUser()) return;
        UUID callUuid = readUUID();

        CallModel callModel = CallManager.getInstance().connectUserToCall(connectedUser, callUuid);
        if (callModel == null || callModel.getPort() == 0) {
            sendError(requestCode);
            return;
        }
        sendCallInfo(callModel);
    }

    private void sendCallInfo(@NotNull CallModel callModel) {
        sendPreDeterminedAnswer(OutgoingMessageType.ANSWER_CALL_CREATED,
                ByteUtils.getUUIDBytes(callModel.getUUID()),
                ByteUtils.getIntBytes(callModel.getPort()));
    }

    private void quitCall() throws IOException {
        if (!checkUser()) return;
        UUID callUuid = readUUID();
        CallManager.getInstance().disconnectUserFromCall(connectedUser, callUuid);
    }

    private void acceptCall(byte requestCode) throws IOException {
        if (!checkUser()) return;
        UUID callUuid = readUUID();
        CallModel callModel = CallManager.getInstance().getCallFromUUID(callUuid);
        if (callModel == null) {
            sendError(requestCode);
            return;
        }
        acceptCall(callModel);
    }

    private void acceptCall(CallModel callModel) {
        connectedUser.getUserModel().addCall(callModel);
    }

    private void rejectCall() throws IOException {
        if (!checkUser()) return;
        UUID callUuid = readUUID();
        CallManager.getInstance().removeUserFromCall(callUuid, connectedUser.getUserModel());
    }

    private void inviteUsers(byte requestCode) throws IOException {
        if (!checkUser()) return;
        UUID callUuid = readUUID();

        int numOfUsers = readInt();
        if (numOfUsers <= 0) {
            sendError(requestCode);
            return;
        }

        ArrayList<UserModel> listOfUsers = new ArrayList<>(numOfUsers);
        UUID userUuid;
        UserManager userManager = UserManager.getInstance();
        for (int i = 0; i < numOfUsers; i++) {
            userUuid = readUUID();
            UserModel userModel = userManager.getUserFromUIID(userUuid);
            if (userModel == null) {
                sendError(requestCode);
                return;
            }
            listOfUsers.add(userModel);
        }

        CallManager.getInstance().inviteUsersToCall(callUuid, this.connectedUser.getUserModel(), listOfUsers);
    }

    private void sendUsersListWithStatus(byte requestCode) throws IOException {
        if (!checkUser()) return;
        List<UserModel> users = UserManager.getInstance().getUsers();
        sendPreDeterminedAnswer(OutgoingMessageType.ANSWER_UPDATE_USERS_LIST, ByteUtils.getIntBytes(users.size()));
        for (UserModel user : users) {
            socketOutputStream.write(ByteUtils.getUUIDBytes(user.getUUID()));
            socketOutputStream.write(ByteUtils.getBoolBytes(user.isOnline()));
        }
    }

    private void sendUserCallsList(byte requestCode) throws IOException {
        if (!checkUser()) return;
        if (connectedUser.getUserModel() == null) {
            sendError(requestCode);
            return;
        }
        List<CallModel> calls = connectedUser.getUserModel().getUserCalls();
        sendPreDeterminedAnswer(OutgoingMessageType.ANSWER_UPDATE_USER_CALLS_LIST, ByteUtils.getIntBytes(calls.size()));
        for (CallModel call : calls) {
            socketOutputStream.write(ByteUtils.getUUIDBytes(call.getUUID()));
        }
    }

    private void sendCallUsersList(byte requestCode) throws IOException {
        if (!checkUser()) return;
        UUID callUuid = readUUID();
        CallModel callModel = CallManager.getInstance().getCallFromUUID(callUuid);
        if (callModel == null) {
            sendError(requestCode);
            return;
        }
        List<CallModel.UserModelWithStatus> invitedUsers = callModel.getInvitedUsers();
        sendPreDeterminedAnswer(OutgoingMessageType.ANSWER_UPDATE_CALL_USERS_LIST,
                ByteUtils.getUUIDBytes(callUuid),
                ByteUtils.getIntBytes(invitedUsers.size()));
        for (CallModel.UserModelWithStatus invitedUser : invitedUsers) {
            socketOutputStream.write(ByteUtils.getUUIDBytes(invitedUser.getUserModel().getUUID()));
            socketOutputStream.write(ByteUtils.getBoolBytes(invitedUser.getUserModel().isOnline()));
            socketOutputStream.write(ByteUtils.getBoolBytes(invitedUser.isConnectedThere()));
        }
    }

    private void sendError(byte errorCode) {
        sendPreDeterminedAnswer(OutgoingMessageType.ANSWER_ERROR, errorCode);
    }

    private boolean checkUser() {
        if (connectedUser.getUserModel() == null) {
            sendError(IncomingMessageType.REQUEST_LOGIN);
            return false;
        }
        return true;
    }

    private UUID readUUID() throws IOException {
        return NetUtils.readUUID(socketInputStream);
    }

    private int readInt() throws IOException {
        return NetUtils.readInt(socketInputStream);
    }

    private byte readByte() throws IOException {
        return NetUtils.readByte(socketInputStream);
    }

    private String readString() throws IOException {
        return NetUtils.readString(socketInputStream, NetUtils.readInt(socketInputStream), StandardCharsets.UTF_16LE);
    }

    private byte[] usersUuidsToBytes(List<CallModel.UserModelWithStatus> userModels) {
        byte[] res = new byte[userModels.size() * ByteUtils.UUID_BYTES];
        Iterator<CallModel.UserModelWithStatus> usersIterator = userModels.iterator();
        for (int i = 0; i < res.length && usersIterator.hasNext(); i += ByteUtils.UUID_BYTES) {
            System.arraycopy(ByteUtils.getUUIDBytes(usersIterator.next().getUserModel().getUUID()), 0, res, i, ByteUtils.UUID_BYTES);
        }
        return res;
    }

    @Override
    public void onToCallInvited(CallModel callModel) {
        System.out.println("got call invitation");
        List<CallModel.UserModelWithStatus> invitedUsers = callModel.getInvitedUsers();

        addMsgToQueue(ByteUtils.concatAll(OutgoingMessageType.REQUEST_CALL_INVITATION,
                ByteUtils.getUUIDBytes(callModel.getUUID()),
                ByteUtils.getIntBytes(invitedUsers.size()),
                usersUuidsToBytes(invitedUsers)));
    }

    @Override
    public void onCallAdded(CallModel callModel) {
        System.out.println("got new call to add");
        addMsgToQueue(ByteUtils.concatAll(OutgoingMessageType.ANSWER_NEW_CALL_STARTED,
                ByteUtils.getUUIDBytes(callModel.getUUID())));
    }

    @Override
    public void onCallDeleted(CallModel callModel) {
        System.out.println("got new call to delete");
        addMsgToQueue(ByteUtils.concatAll(OutgoingMessageType.ANSWER_CALL_ENDED,
                ByteUtils.getUUIDBytes(callModel.getUUID())));
    }

    @Override
    public void onSomeoneEnteredCall(CallModel callModel, UserModel userModel) {
        System.out.println("someone entered call");
        addMsgToQueue(ByteUtils.concatAll(OutgoingMessageType.ANSWER_NEW_USER_ENTERED_CALL,
                ByteUtils.getUUIDBytes(callModel.getUUID()),
                ByteUtils.getUUIDBytes(userModel.getUUID())));
    }

    @Override
    public void onSomeoneLeftCall(CallModel callModel, UserModel userModel) {
        System.out.println("someone left call");
        addMsgToQueue(ByteUtils.concatAll(OutgoingMessageType.ANSWER_NEW_USER_LEFT_CALL,
                ByteUtils.getUUIDBytes(callModel.getUUID()),
                ByteUtils.getUUIDBytes(userModel.getUUID())));
    }

    @Override
    public void onSomeoneConnectedToCall(CallModel callModel, UserModel userModel) {
        System.out.println("someone connected to call");
        addMsgToQueue(ByteUtils.concatAll(OutgoingMessageType.ANSWER_NEW_USER_CONNECTED_TO_CALL,
                ByteUtils.getUUIDBytes(callModel.getUUID()),
                ByteUtils.getUUIDBytes(userModel.getUUID())));
    }

    @Override
    public void onSomeoneDisconnectedFromCall(CallModel callModel, UserModel userModel) {
        System.out.println("someone disconnected from call");
        addMsgToQueue(ByteUtils.concatAll(OutgoingMessageType.ANSWER_NEW_USER_DISCONNECTED_FROM_CALL,
                ByteUtils.getUUIDBytes(callModel.getUUID()),
                ByteUtils.getUUIDBytes(userModel.getUUID())));
    }
}
