package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Persist the identity of the actual translated evidence, independent of unrelated row revisions. */
public final class NoticeTranslationEvidence {
    private NoticeTranslationEvidence() {}
    public static String fingerprint(Notice notice, NoticeLang lang) {
        if (lang==notice.getSourceLang()) return null;
        var translation=notice.getTranslation(lang).filter(t -> !t.isStaleFor(notice.getVersion())).orElse(null);
        if (translation==null) return null;
        String title=translation.getTitle(), body=translation.getBody();
        String input="v1:"+lang.code()+":"+notice.getVersion()+":"+title.length()+":"+title+body.length()+":"+body;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public static boolean matches(Notice notice, NoticeLang lang, String recorded) {
        return lang==notice.getSourceLang() || recorded!=null && recorded.equals(fingerprint(notice,lang));
    }
}
