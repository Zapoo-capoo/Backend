package com.capoo.chat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    CONVERSATION_NOT_EXISTED(2001, "Conversation not existed", HttpStatus.NOT_FOUND),
    MESSAGE_NOT_FOUND(2002, "Message not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_ACTION(2003, "You are not allowed to perform this action", HttpStatus.FORBIDDEN),
    INVALID_CURSOR(2004, "Invalid cursor", HttpStatus.BAD_REQUEST),
    GROUP_NAME_REQUIRED(2005, "Group name is required", HttpStatus.BAD_REQUEST),
    GROUP_NAME_TOO_LONG(2006, "Group name must be at most 100 characters", HttpStatus.BAD_REQUEST),
    GROUP_MEMBERS_REQUIRED(2007, "A group needs at least 1 member besides you", HttpStatus.BAD_REQUEST),
    PARTICIPANT_NOT_FOUND(2008, "Participant not found", HttpStatus.NOT_FOUND),
    NOT_GROUP_CONVERSATION(2009, "This conversation is not a group", HttpStatus.BAD_REQUEST),
    TOO_MANY_ATTACHMENTS(2010, "A message can have at most 5 attachments", HttpStatus.BAD_REQUEST),
    MESSAGE_EMPTY(2011, "A message needs some text or at least one attachment", HttpStatus.BAD_REQUEST),
    ATTACHMENT_NOT_FOUND(2012, "Attachment file not found", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_ATTACHMENT_TYPE(2013, "Only images and videos can be attached", HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
