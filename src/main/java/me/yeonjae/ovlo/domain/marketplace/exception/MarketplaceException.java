package me.yeonjae.ovlo.domain.marketplace.exception;

public class MarketplaceException extends RuntimeException {
    public enum Kind { NOT_FOUND, FORBIDDEN, CONFLICT, UNAVAILABLE }
    private final Kind kind;
    public MarketplaceException(Kind kind, String message) { super(message); this.kind = kind; }
    public Kind kind() { return kind; }
}
