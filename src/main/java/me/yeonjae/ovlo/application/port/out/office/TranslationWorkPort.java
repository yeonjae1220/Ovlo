package me.yeonjae.ovlo.application.port.out.office;

import java.util.Optional;

/** Short-lived exclusive ownership of one notice-version-language or message-language translation. */
public interface TranslationWorkPort {
    Optional<Lease> tryAcquire(String workKey);
    interface Lease extends AutoCloseable {
        boolean isHeld();
        /** Reserve a bounded message-translation attempt before calling the provider. */
        boolean tryBeginAttempt();
        void translationFailed();
        void translationSucceeded();
        @Override void close();
    }
}
