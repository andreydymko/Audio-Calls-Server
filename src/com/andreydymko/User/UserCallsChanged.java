package com.andreydymko.User;

import com.andreydymko.Call.CallModel;


public interface UserCallsChanged {
    void onToCallInvited(CallModel callModel);
    void onCallAdded(CallModel callModel); // todo what should we do with it?
    void onCallDeleted(CallModel callModel);
    void onSomeoneEnteredCall(CallModel callModel, UserModel userModel);
    void onSomeoneLeftCall(CallModel callModel, UserModel userModel);
    void onSomeoneConnectedToCall(CallModel callModel, UserModel userModel);
    void onSomeoneDisconnectedFromCall(CallModel callModel, UserModel userModel);
}
