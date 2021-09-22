package com.andreydymko.Call;

import com.andreydymko.User.UserModel;

public interface CallChangedCallback {
    void onCallStarted(CallModel callModel); // todo remove???
    void onCallEnded(CallModel callModel);
    void onUserEnteredCall(CallModel callModel, UserModel userModel);
    void onUserLeftCall(CallModel callModel, UserModel userModel);
    void onUserConnectedToCall(CallModel callModel, UserModel userModel);
    void onUserDisconnectedFromCall(CallModel callModel, UserModel userModel);
}
