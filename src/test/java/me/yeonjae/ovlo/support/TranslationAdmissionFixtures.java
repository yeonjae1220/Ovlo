package me.yeonjae.ovlo.support;
import me.yeonjae.ovlo.application.port.out.office.TranslationAdmissionPort;
public final class TranslationAdmissionFixtures {
    private TranslationAdmissionFixtures() {}
    public static TranslationAdmissionPort available() { return (office, member) -> () -> {}; }
}
