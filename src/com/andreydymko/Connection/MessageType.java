package com.andreydymko.Connection;

/**
 * List of message types to send to clients,
 * what they are followed by
 * and what client should send in return
 */
class OutgoingMessageType {
    /** followed by one incoming message type byte (describing where error has occurred)
     * Answer from client: nothing */
    public static final byte ANSWER_ERROR = 0;

    /** followed by UUID of client (already created or newly allocated).
     * Answer from client: nothing */
    public static final byte ANSWER_LOGIN_SUCCESS = -1;

    /** followed by nothing.
     * Answer from client: ANSWER_PING */
    public static final byte REQUEST_PING = -2;

    /** followed by call UUID, then num of users, then list of users UUIDs.
     * Answer from client: ANSWER_ACCEPT_INCOMING_CALL or ANSWER_REJECT_INCOMING_CALL */
    public static final byte REQUEST_CALL_INVITATION = -3;

    /** followed by UUID of the new call, then newly allocated UDP port (0 if error)
     * Answer from client: nothing */
    public static final byte ANSWER_CALL_CREATED = -4;

    /** followed by call UUID.
     * Answer from client: nothing */
    public static final byte ANSWER_NEW_CALL_STARTED = -5;

    /** followed by call UUID.
     * Answer from client: nothing */
    public static final byte ANSWER_CALL_ENDED = -6;

    /** followed by user UUID.
     * Answer from client: nothing */
    public static final byte ANSWER_NEW_USER_ONLINE = -7;

    /** followed by user UUID.
     * Answer from client: nothing */
    public static final byte ANSWER_NEW_USER_OFFLINE = -8;

    /** followed by num of users, then list of structs (user UUID, user status).
     * Answer from client: nothing */
    public static final byte ANSWER_UPDATE_USERS_LIST = -9;

    /** followed by call UUID, then user UUID.
     * Answer from client: nothing */
    public static final byte ANSWER_NEW_USER_CONNECTED_TO_CALL = -10;

    /** followed by call UUID, then user UUID.
     * Answer from client: nothing */
    public static final byte ANSWER_NEW_USER_DISCONNECTED_FROM_CALL = -11;

    /** followed by call UUID, then user UUID.
     * Answer from client: nothing */
    public static final byte ANSWER_NEW_USER_ENTERED_CALL = -12;

    /** followed by call UUID, then user UUID.
     * Answer from client: nothing */
    public static final byte ANSWER_NEW_USER_LEFT_CALL = -13;

    /** followed by num of calls, then list of calls UUIDs.
     * Answer from client: nothing */
    public static final byte ANSWER_UPDATE_USER_CALLS_LIST = -14;

    /** followed by call UUID, then num of users, then list of structs (user UUID, user status, is user connected to call status).
     * Answer from client: nothing */
    public static final byte ANSWER_UPDATE_CALL_USERS_LIST = -15;
}

/**
 * List of message types to receive from clients,
 * what they are followed by
 */
class IncomingMessageType {
    /** followed by length of string bytes, then string in UTF-16LE (as login),
     * then length of pass in bytes, then sting in UTF-16LE (as password)
     * Answer to client: ANSWER_LOGIN_SUCCESS */
    public static final byte REQUEST_REGISTER = 1;

    /** followed by length of string bytes, then string in UTF-16LE (as login),
     * then length of pass in bytes, then sting in UTF-16LE (as password)
     * Answer to client: ANSWER_LOGIN_SUCCESS */
    public static final byte REQUEST_LOGIN = 2;
    /** followed by nothing
     * Answer to client: nothing */
    public static final byte REQUEST_LOGOUT = 3;

    /** followed by num of users, then list of users UUIDs.
     * Answer to client: ANSWER_CALL_CREATED or ANSWER_ERROR */
    public static final byte REQUEST_CREATE_CALL = 4;

    /** followed by UUID of the call.
     * Answer to client: ANSWER_CALL_CREATED or ANSWER_ERROR */
    public static final byte REQUEST_ENTER_CALL = 5;

    /** followed by UUID of the call.
     * Answer to client: nothing */
    public static final byte REQUEST_QUIT_CALL = 6;

    /** followed by UUID of the call.
     * Answer to client: nothing */
    public static final byte ANSWER_ACCEPT_INCOMING_CALL = 7;

    /** followed by UUID of the call.
     * Answer to client: nothing */
    public static final byte ANSWER_REJECT_INCOMING_CALL = 8;

    /** followed by call UUID, then num of users, then list of users UUIDs.
     * Answer to client: nothing */
    public static final byte REQUEST_INVITE_USERS = 9;

    /** followed by nothing
     * Answer to client: ANSWER_GET_USERS_LIST */
    public static final byte REQUEST_GET_USERS_LIST = 10;

    /** followed by nothing
     * Answer to client: ANSWER_UPDATE_USER_CALLS_LIST */
    public static final byte REQUEST_GET_USER_CALLS_LIST = 11;

    /** followed by UUID of the call
     * Answer to client: ANSWER_UPDATE_CALL_USERS_LIST */
    public static final byte REQUEST_GET_CALL_USERS_LIST = 12;

    /** followed by nothing.
     * Answer to client: nothing  */
    public static final byte ANSWER_PING = 13;
}
