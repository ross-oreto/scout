package io.oreto.jpa.dsl;

public class BadQueryException extends RuntimeException {
    private int i = -1;

    public BadQueryException(String message) {
        super(message);
    }

    public BadQueryException(String message, int i) {
        super(message);
        this.i = i;
    }

    public int at() { return i; }
}
