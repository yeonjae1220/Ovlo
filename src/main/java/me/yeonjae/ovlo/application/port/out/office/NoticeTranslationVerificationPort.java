package me.yeonjae.ovlo.application.port.out.office;

/** Independent semantic check; unavailable or malformed verification must never publish a translation. */
public interface NoticeTranslationVerificationPort {
    boolean verifyTranslation(TranslationPort.Request source, TranslationPort.Result translation);
}
