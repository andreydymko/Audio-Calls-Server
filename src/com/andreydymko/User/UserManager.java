package com.andreydymko.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserManager {
    private UserManager(){}
    private static UserManager instance;

    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager();
                }
            }
        }
        return instance;
    }

    private final static int MIN_LOGIN_LENGTH = 8;
    private final static int MIN_PASS_LENGTH = 8;
    private final static int MAX_LOGIN_LENGTH = 100;
    private final static int MAX_PASS_LENGTH = 100;

    private final List<UserModel> userPool = new ArrayList<>();

    public UserModel getUser(String login, String password) {
        if (!checkLogin(login) || !checkPassword(password)) {
            return null;
        }
        UserModel user = getUserFromLogin(login);
        if (user == null || !user.getPassword().equals(password)) {
            return null;
        }

        return user;
    }

    public UserModel registerNewUser(String login, String password) {
        if (!checkLogin(login) || !checkPassword(password)) {
            return null;
        }
        if (getUserFromLogin(login) != null) {
            // user with that login already registered
            return null;
        }
        UserModel userModel = new UserModel(login, password);
        userPool.add(userModel);
        return userModel;
    }



    private UserModel getUserFromLogin(String login) {
        if (!checkLogin(login)) {
            return null;
        }
        return userPool.stream().filter(userModel -> login.equals(userModel.getUserLogin())).findFirst().orElse(null);
    }

    public UserModel getUserFromUIID(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return userPool.stream().filter(userModel -> uuid.equals(userModel.getUUID())).findFirst().orElse(null);
    }

    public List<UserModel> getUsers() {
        return userPool;
    }


    public static boolean checkLogin(String login) {
        if (login == null) return false;
        String trimmedLogin = login.trim();
        return !trimmedLogin.isEmpty() && trimmedLogin.length() >= MIN_LOGIN_LENGTH && trimmedLogin.length() < MAX_LOGIN_LENGTH;
    }

    public static boolean checkPassword(String password) {
        if (password == null) {
            return false;
        }
        String trimmedPass = password.trim();
        return !trimmedPass.isEmpty() && trimmedPass.length() >= MIN_PASS_LENGTH && trimmedPass.length() < MAX_PASS_LENGTH;
    }


    public void removeUser(UserModel userModel) {
        if (userModel == null) {
            return;
        }
        userPool.remove(userModel);
    }

    public void removeUser(UUID uuid) {
        if (uuid == null) {
            return;
        }
        userPool.removeIf(userModel -> uuid.equals(userModel.getUUID()));
    }

    public void removeUser(String login) {
        if (checkLogin(login)) {
            return;
        }
        userPool.removeIf(userModel -> login.equals(userModel.getUserLogin()));
    }
}
