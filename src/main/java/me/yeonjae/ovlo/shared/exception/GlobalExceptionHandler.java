package me.yeonjae.ovlo.shared.exception;

import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import me.yeonjae.ovlo.domain.board.exception.BoardException;
import me.yeonjae.ovlo.domain.chat.exception.ChatException;
import me.yeonjae.ovlo.domain.follow.exception.FollowException;
import me.yeonjae.ovlo.domain.media.exception.MediaException;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.post.exception.PostException;
import me.yeonjae.ovlo.domain.university.exception.UniversityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ErrorResponse> handleMemberException(MemberException ex) {
        return ResponseEntity.status(resolveStatus(ex.getErrorType()))
                .body(ErrorResponse.of(resolveCode(MemberException.class, ex.getErrorType()), ex.getMessage()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
        return ResponseEntity.status(resolveStatus(ex.getErrorType()))
                .body(ErrorResponse.of(resolveCode(AuthException.class, ex.getErrorType()), ex.getMessage()));
    }

    @ExceptionHandler(UniversityException.class)
    public ResponseEntity<ErrorResponse> handleUniversityException(UniversityException ex) {
        return ResponseEntity.status(resolveStatus(ex.getErrorType()))
                .body(ErrorResponse.of(resolveCode(UniversityException.class, ex.getErrorType()), ex.getMessage()));
    }

    @ExceptionHandler(BoardException.class)
    public ResponseEntity<ErrorResponse> handleBoardException(BoardException ex) {
        return ResponseEntity.status(resolveStatus(ex.getErrorType()))
                .body(ErrorResponse.of(resolveCode(BoardException.class, ex.getErrorType()), ex.getMessage()));
    }

    @ExceptionHandler(PostException.class)
    public ResponseEntity<ErrorResponse> handlePostException(PostException ex) {
        return ResponseEntity.status(resolveStatus(ex.getErrorType()))
                .body(ErrorResponse.of(resolveCode(PostException.class, ex.getErrorType()), ex.getMessage()));
    }

    @ExceptionHandler(FollowException.class)
    public ResponseEntity<ErrorResponse> handleFollowException(FollowException ex) {
        return ResponseEntity.status(resolveStatus(ex.getErrorType()))
                .body(ErrorResponse.of(resolveCode(FollowException.class, ex.getErrorType()), ex.getMessage()));
    }

    @ExceptionHandler(MediaException.class)
    public ResponseEntity<ErrorResponse> handleMediaException(MediaException ex) {
        return ResponseEntity.status(resolveStatus(ex.getErrorType()))
                .body(ErrorResponse.of(resolveCode(MediaException.class, ex.getErrorType()), ex.getMessage()));
    }

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ErrorResponse> handleChatException(ChatException ex) {
        return ResponseEntity.status(resolveStatus(ex.getErrorType()))
                .body(ErrorResponse.of(resolveCode(ChatException.class, ex.getErrorType()), ex.getMessage()));
    }

    /**
     * 도메인 예외의 ErrorType enum name을 HTTP 상태로 변환.
     * 각 도메인이 독립 enum을 갖지만 이름이 HTTP 시맨틱을 따르므로 name() 기반 매핑 사용.
     */
    private static HttpStatus resolveStatus(Enum<?> errorType) {
        return switch (errorType.name()) {
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "CONFLICT" -> HttpStatus.CONFLICT;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "BAD_REQUEST" -> HttpStatus.BAD_REQUEST;
            case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * 도메인 예외 클래스명 + ErrorType name 으로 machine-readable 코드 생성.
     * 예: UniversityException + NOT_FOUND → "UNIVERSITY_NOT_FOUND"
     */
    private static String resolveCode(Class<?> exClass, Enum<?> errorType) {
        String domain = exClass.getSimpleName().replace("Exception", "").toUpperCase();
        return domain + "_" + errorType.name();
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorResponse.of("TOO_MANY_REQUESTS", ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ErrorResponse.of("REQUEST_ERROR", ex.getReason()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("OPTIMISTIC_LOCK_CONFLICT", "동시 수정이 발생했습니다. 다시 시도해주세요"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();
        // JVM enum 오류 "No enum constant com.example.Foo.BAR" → 내부 클래스 경로 노출 방지
        if (message != null && message.startsWith("No enum constant")) {
            String value = message.substring(message.lastIndexOf('.') + 1);
            message = "유효하지 않은 값입니다: " + value;
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_ARGUMENT", message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.ofFields("VALIDATION_ERROR", errors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getValueResults().forEach(result ->
                result.getResolvableErrors().forEach(err ->
                        errors.put(result.getMethodParameter().getParameterName(),
                                err.getDefaultMessage())));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errors.isEmpty()
                        ? ErrorResponse.of("VALIDATION_ERROR", ex.getMessage())
                        : ErrorResponse.ofFields("VALIDATION_ERROR", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception [{}]: {}", ex.getClass().getName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Internal server error"));
    }
}
