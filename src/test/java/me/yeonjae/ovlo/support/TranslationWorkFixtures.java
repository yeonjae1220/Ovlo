package me.yeonjae.ovlo.support;
import me.yeonjae.ovlo.application.port.out.office.TranslationWorkPort;
import java.util.Optional;
public final class TranslationWorkFixtures {
    private TranslationWorkFixtures() {}
    public static TranslationWorkPort available() {
        return key -> Optional.of(new TranslationWorkPort.Lease() {
            public boolean isHeld() { return true; }
            public boolean isRetryAllowed() { return true; }
            public boolean tryBeginAttempt() { return true; }
            public void translationFailed() {}
            public void translationSucceeded() {}
            public void close() {}
        });
    }
}
