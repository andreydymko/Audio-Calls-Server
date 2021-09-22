package com.andreydymko.Call;

import com.andreydymko.ArrayLocalUtils;
import com.andreydymko.Connection.ConnectedUserModel;
import com.andreydymko.User.UserModel;

import java.util.*;

public class CallModel {
    private final static int MIN_USERS_COUNT = 2;
    private final UUID UUID;
    private final SoundThread thread;
    private final List<UserModelWithStatus> invitedUsers;
    private final List<CallChangedCallback> callChangedCallbacks;
    private CallShouldBeEndedCallback callShouldBeEndedCallback;

    public CallModel() {
        this.UUID = java.util.UUID.randomUUID();
        this.invitedUsers = new ArrayList<>();
        this.thread = new SoundThread();
        this.callChangedCallbacks = new LinkedList<>();
    }

    public void setOnCallChangedListener(CallChangedCallback changedListener) {
        this.callChangedCallbacks.add(changedListener);
    }

    public void removeOnCallChangedListener(CallChangedCallback changedListener) {
        this.callChangedCallbacks.remove(changedListener);
    }

    public void setOnCallShouldBeEndedListener(CallShouldBeEndedCallback callShouldBeEndedListener) {
        this.callShouldBeEndedCallback = callShouldBeEndedListener;
    }

    public void removeOnCallShouldBeEndedListener() {
        this.callShouldBeEndedCallback = null;
    }

    public UUID getUUID() {
        return UUID;
    }

    public int getPort() {
        return this.thread.getUDPPort();
    }

    public void inviteUser(UserModel userModel) {
        if (!ArrayLocalUtils.addUnique(invitedUsers, new UserModelWithStatus(userModel))) {
            return;
        }
        for (CallChangedCallback callback : callChangedCallbacks) {
            callback.onUserEnteredCall(this, userModel);
        }
    }

    public void removeUser(UserModel userModel) {
        if (userModel == null) {
            return;
        }
        if (!invitedUsers.removeIf(userModelWithStatus -> userModel.equals(userModelWithStatus.getUserModel()))) {
            return;
        }
        disconnectUser(userModel);
        for (CallChangedCallback callback : callChangedCallbacks) {
            callback.onUserLeftCall(this, userModel);
        }
        callShouldBeEnded(invitedUsers.size() < MIN_USERS_COUNT);
    }

    public List<UserModelWithStatus> getInvitedUsers() {
        return invitedUsers;
    }

    public void startCall() {
        for (CallChangedCallback callback : callChangedCallbacks) {
            callback.onCallStarted(this);
        }
        thread.start();
    }

    public void endCall() {
        System.out.println("CallModel: ending call");
        List<ConnectedUserModel> users = thread.getUsersList();
        try {
            for (int i = 0, end = users.size(); i < end; i++) { // todo hangs thread up
                disconnectUser(users.get(i)); // todo not effective, remake?
            }
        } catch (IndexOutOfBoundsException ignore) {}
        while (!callChangedCallbacks.isEmpty()) { // todo check
            // this callback invokes function that removes entries of the list of callbacks
            // that's the problem
            callChangedCallbacks.get(0).onCallEnded(this);
        }
        thread.interrupt();
    }

    public boolean connectUser(ConnectedUserModel connectedUserModel) {
        UserModelWithStatus userModelWithStatus = findUserWithStatusByUserModel(connectedUserModel.getUserModel());
        if (userModelWithStatus == null) {
            return false;
        }

        userModelWithStatus.setConnectedThere(true);
        if (!thread.addUser(connectedUserModel)) {
            return false;
        }

        for (CallChangedCallback callback : callChangedCallbacks) {
            callback.onUserConnectedToCall(this, connectedUserModel.getUserModel());
        }
        return true;
    }

    public void disconnectUser(UserModel userModel) {
        if (userModel == null) {
            return;
        }
        if (!thread.removeUser(userModel)) {
            return;
        }

        finallyDisconnectUser(userModel);
    }

    public void disconnectUser(ConnectedUserModel connectedUserModel) {
        UserModelWithStatus userModelWithStatus = findUserWithStatusByUserModel(connectedUserModel.getUserModel());
        if (userModelWithStatus == null) {
            return;
        }
        if (!thread.removeUser(connectedUserModel)) {
            return;
        }
        userModelWithStatus.setConnectedThere(false);

        finallyDisconnectUser(userModelWithStatus.getUserModel());
    }

    private void finallyDisconnectUser(UserModel userModel) {
        for (CallChangedCallback callback : callChangedCallbacks) {
            callback.onUserDisconnectedFromCall(this, userModel);
        }
        callShouldBeEnded(getConnectedUsersCount() < MIN_USERS_COUNT);
    }

    private void callShouldBeEnded(boolean isShouldBeEnded) {
        if (callShouldBeEndedCallback != null && isShouldBeEnded) {
            callShouldBeEndedCallback.onCallShouldBeEnded(this);
        }
    }

    public int getConnectedUsersCount() {
        return thread.getUsersCount();
    }

    private UserModelWithStatus findUserWithStatusByUserModel(UserModel userModel) {
        return invitedUsers.stream().filter(userModelWithStatus -> userModel.equals(userModelWithStatus.getUserModel())).findFirst().orElse(null);
    }

    public class UserModelWithStatus {
        private UserModel userModel;
        private boolean isConnectedThere;

        UserModelWithStatus(UserModel userModel) {
            this.userModel = userModel;
        }

        public UserModel getUserModel() {
            return userModel;
        }

        public void setUserModel(UserModel userModel) {
            this.userModel = userModel;
        }

        public boolean isConnectedThere() {
            return isConnectedThere;
        }

        public void setConnectedThere(boolean connectedThere) {
            isConnectedThere = connectedThere;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserModelWithStatus that = (UserModelWithStatus) o;
            return Objects.equals(userModel, that.userModel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userModel);
        }
    }
}
