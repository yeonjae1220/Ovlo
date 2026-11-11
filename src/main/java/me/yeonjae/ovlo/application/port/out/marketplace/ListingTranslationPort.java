package me.yeonjae.ovlo.application.port.out.marketplace;
public interface ListingTranslationPort {
    record Text(String title, String description) {}
    Text translateListing(String sourceLanguage, String targetLanguage, String title, String description);
}
