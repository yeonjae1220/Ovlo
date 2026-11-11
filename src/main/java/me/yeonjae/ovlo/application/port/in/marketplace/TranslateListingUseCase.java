package me.yeonjae.ovlo.application.port.in.marketplace;
import me.yeonjae.ovlo.application.dto.result.ListingTranslation;
public interface TranslateListingUseCase {
    ListingTranslation translate(long actor, long listingId, String targetLanguage);
}
