package me.yeonjae.ovlo.application.port.out.office;

import java.util.Optional;

/** Short-lived exclusive ownership of one notice-version-language or message-language translation. */
public interface TranslationWorkPort {
    Optional<Lease> tryAcquire(String workKey);
    interface Lease extends AutoCloseable {
        boolean isHeld();
        @Override void close();
    }
}
