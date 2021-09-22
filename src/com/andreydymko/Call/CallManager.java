package com.andreydymko.Call;

import com.andreydymko.Connection.ConnectedUserModel;
import com.andreydymko.User.UserModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CallManager implements CallShouldBeEndedCallback {
    private CallManager(){}
    private static CallManager instance;

    public static CallManager getInstance() {
        if (instance == null) {
            synchronized (CallManager.class) {
                if (instance == null) {
                    instance = new CallManager();
                }
            }
        }
        return instance;
    }

    private final List<CallModel> activeCalls = new ArrayList<>();

    public CallModel createCall(UserModel creator, List<UserModel> invitedUsers) {
        invitedUsers.add(creator);
        invitedUsers.removeIf(userModel -> !userModel.isOnline());
        if (invitedUsers.size() < 2) {
            return null;
        }

        CallModel callModel = new CallModel();
        inviteUsersToCall(callModel, creator, invitedUsers);
        callModel.startCall();
        callModel.setOnCallShouldBeEndedListener(this);
        activeCalls.add(callModel);
        return callModel;
    }

    public CallModel getCallFromUUID(UUID callUuid) {
        return activeCalls.stream().filter(callModel -> callUuid.equals(callModel.getUUID())).findFirst().orElse(null);
    }

    public CallModel connectUserToCall(ConnectedUserModel connectedUserModel, UUID callUuid) {
        CallModel callModel = getCallFromUUID(callUuid);
        if (callModel == null || !callModel.connectUser(connectedUserModel)) {
            return null;
        }
        return callModel;
    }

    public void disconnectUserFromCall(ConnectedUserModel connectedUserModel, UUID callUuid) {
        CallModel callModel = getCallFromUUID(callUuid);
        if (callModel == null) {
            return;
        }
        callModel.disconnectUser(connectedUserModel);
    }

    public CallModel inviteUsersToCall(UUID callId, UserModel initiator, List<UserModel> users) {
        return inviteUsersToCall(getCallFromUUID(callId), initiator, users);
    }

    private CallModel inviteUsersToCall(CallModel callModel, UserModel initiator, List<UserModel> invitedUsers) {
        if (callModel == null) {
            return null;
        }
        for (UserModel user : invitedUsers) {
            if (!user.equals(initiator)) {
                user.inviteToCall(callModel);
            }
            callModel.inviteUser(user);
        }
        return callModel;
    }

    public CallModel inviteUserToCall(UUID callId, UserModel userModel) {
        return inviteUserToCall(getCallFromUUID(callId), userModel);
    }

    private CallModel inviteUserToCall(CallModel callModel, UserModel userModel) {
        if (callModel == null || userModel == null) {
            return null;
        }
        userModel.inviteToCall(callModel);
        callModel.inviteUser(userModel);
        return callModel;
    }

    public CallModel removeUserFromCall(UUID callUuid, UserModel userModel) {
        return removeUserFromCall(getCallFromUUID(callUuid), userModel);
    }

    private CallModel removeUserFromCall(CallModel callModel, UserModel userModel) {
        if (callModel == null || userModel == null) {
            return null;
        }
        callModel.removeUser(userModel);
        return callModel;
    }

    @Override
    public void onCallShouldBeEnded(CallModel callModel) {
        endCall(callModel);
    }

    private void endCall(CallModel callModel) {
        // todo remake
        List<CallModel.UserModelWithStatus> userModels = callModel.getInvitedUsers();
        // notifying every invited user that call ended todo remove?
//        for (CallModel.UserModelWithStatus userModel : userModels) {
//            userModel.getUserModel().removeCall(callModel);
//        }
        callModel.removeOnCallShouldBeEndedListener();
        callModel.endCall();
        activeCalls.remove(callModel);
    }

    public void endAllCalls() {
        for (CallModel activeCall : activeCalls) {
            endCall(activeCall);
        }
    }
}
