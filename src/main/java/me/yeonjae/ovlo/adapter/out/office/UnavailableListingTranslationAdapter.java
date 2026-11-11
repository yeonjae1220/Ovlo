package me.yeonjae.ovlo.adapter.out.office;
import me.yeonjae.ovlo.application.port.out.marketplace.ListingTranslationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
/** Never present the development fake translator's echoed input as a real translation. */
@Component
@ConditionalOnProperty(name="ovlo.llm.provider",havingValue="fake",matchIfMissing=true)
public class UnavailableListingTranslationAdapter implements ListingTranslationPort {
    @Override public Text translateListing(String from,String to,String title,String body) { throw new IllegalStateException("Real translation provider not configured"); }
}
