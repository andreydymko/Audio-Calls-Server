package com.andreydymko.User;

import com.andreydymko.Call.CallChangedCallback;
import com.andreydymko.Call.CallModel;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class UserModel implements CallChangedCallback {
    private final UUID UUID;

    private final String userLogin;
    private final String password;
    private final List<CallModel> userCalls;
    private boolean isOnline;
    private UserCallsChanged dataChangedCallback;

    UserModel(String userLogin, String password) {
        this.UUID = java.util.UUID.randomUUID();
        this.password = password;
        this.userLogin = userLogin;
        this.userCalls = new LinkedList<>();
        this.isOnline = false;
    }

    public void setOnDataChangedListener(UserCallsChanged dataChangedListener) {
        this.dataChangedCallback = dataChangedListener;
    }

    public void removeOnDataChangedListener() {
        this.dataChangedCallback = null;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setStatus(boolean isOnline) {
        this.isOnline = isOnline;
        if (!isOnline) {
            disconnectFromAllCalls();
        }
    }

    private void disconnectFromAllCalls() {
        for (CallModel userCall : userCalls) {
            userCall.disconnectUser(this);
        }
    }

    public void inviteToCall(CallModel callModel) {
        if (dataChangedCallback != null) {
            dataChangedCallback.onToCallInvited(callModel);
        }
    }

    public void addCall(CallModel callModel) {
        if (callModel == null) {
            return;
        }
        callModel.setOnCallChangedListener(this);
        userCalls.add(callModel);
        if (dataChangedCallback != null) {
            dataChangedCallback.onCallAdded(callModel);
        }
    }

    public void removeCall(CallModel callModel) {
        if (callModel == null) {
            return;
        }
        callModel.removeOnCallChangedListener(this);
        userCalls.remove(callModel);
        if (dataChangedCallback != null) {
            dataChangedCallback.onCallDeleted(callModel);
        }
    }

    public List<CallModel> getUserCalls() {
        return userCalls;
    }

    public UUID getUUID() {
        return UUID;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserModel userModel = (UserModel) o;
        return Objects.equals(UUID, userModel.UUID) &&
                Objects.equals(userLogin, userModel.userLogin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(UUID, userLogin);
    }

    @Override
    public void onCallStarted(CallModel callModel) {
        // todo is it right???
        // no, it isn't, do we even need this callback?
        // addCall(callModel);
    }

    @Override
    public void onCallEnded(CallModel callModel) {
        removeCall(callModel);
    }

    @Override
    public void onUserEnteredCall(CallModel callModel, UserModel userModel) {
        if (dataChangedCallback != null) {
            dataChangedCallback.onSomeoneEnteredCall(callModel, userModel);
        }
    }

    @Override
    public void onUserLeftCall(CallModel callModel, UserModel userModel) {
        if (dataChangedCallback != null) {
            dataChangedCallback.onSomeoneLeftCall(callModel, userModel);
        }
    }

    @Override
    public void onUserConnectedToCall(CallModel callModel, UserModel userModel) {
        if (dataChangedCallback != null) {
            dataChangedCallback.onSomeoneConnectedToCall(callModel, userModel);
        }
    }

    @Override
    public void onUserDisconnectedFromCall(CallModel callModel, UserModel userModel) {
        if (dataChangedCallback != null) {
            dataChangedCallback.onSomeoneDisconnectedFromCall(callModel, userModel);
        }
    }
}
