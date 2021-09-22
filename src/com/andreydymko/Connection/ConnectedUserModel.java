package com.andreydymko.Connection;

import com.andreydymko.User.UserCallsChanged;
import com.andreydymko.User.UserModel;

import java.net.InetAddress;

public class ConnectedUserModel {
    private InetAddress inetAddress;
    private UserModel userModel;

    ConnectedUserModel(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public UserModel getUserModel() {
        return userModel;
    }

    public void setUserModel(UserModel userModel) {
        this.userModel = userModel;
    }

    public void setOnUserDataChangeListener(UserCallsChanged changeListener) {
        if (userModel != null) {
            userModel.setOnDataChangedListener(changeListener);
        }
    }

    public void removeOnUserDataChangeListener() {
        if (userModel != null) {
            userModel.removeOnDataChangedListener();
        }
    }
}
