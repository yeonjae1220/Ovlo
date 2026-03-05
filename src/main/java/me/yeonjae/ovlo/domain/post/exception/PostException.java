package me.yeonjae.ovlo.domain.post.exception;

public class PostException extends RuntimeException {

    public PostException(String message) {
        super(message);
    }

    public PostException(String message, Throwable cause) {
        super(message, cause);
    }
}
