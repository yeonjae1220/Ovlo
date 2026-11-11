package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.result.ListingTranslation;
import me.yeonjae.ovlo.application.port.in.marketplace.*;
import me.yeonjae.ovlo.application.port.out.marketplace.*;
import me.yeonjae.ovlo.domain.marketplace.model.ListingDetails;
import me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException.Kind.*;

/** External translation runs outside a DB transaction. Visibility/version is checked again before caching. */
@Service
public class ListingTranslationService implements TranslateListingUseCase {
    private final MarketplaceUseCase listings;
    private final MarketplaceStore store;
    private final ListingTranslationPort translator;
    private final TransactionTemplate transaction;
    public ListingTranslationService(MarketplaceUseCase listings,MarketplaceStore store,ListingTranslationPort translator,PlatformTransactionManager tx) {
        this.listings=listings; this.store=store; this.translator=translator; this.transaction=new TransactionTemplate(tx);
    }
    @Override public ListingTranslation translate(long actor,long id,String language) {
        if(!ListingDetails.LANGUAGES.contains(language)) throw new IllegalArgumentException("Unsupported translation language");
        var listing=listings.get(actor,id); var d=listing.details();
        if(listing.hidden()) throw new MarketplaceException(NOT_FOUND,"Listing not found");
        if(d.sourceLanguage().equals(language)) return new ListingTranslation(id,listing.version(),language,d.title(),d.description(),false);
        var cached=store.translation(id,listing.version(),language); if(cached.isPresent()) return cached.get();
        ListingTranslationPort.Text text;
        try { text=translator.translateListing(d.sourceLanguage(),language,d.title(),d.description()); }
        catch(Exception e) { throw new MarketplaceException(UNAVAILABLE,"Translation is unavailable. You can still read the original."); }
        if(text==null || text.title()==null || text.title().isBlank() || text.title().length()>1000 || text.description()==null || text.description().isBlank() || text.description().length()>20000)
            throw new MarketplaceException(UNAVAILABLE,"Translation response was invalid. Please try again.");
        var result=new ListingTranslation(id,listing.version(),language,text.title(),text.description(),true);
        return transaction.execute(tx->{
            // Lock prevents edit/delete/moderation racing the final check and cache write.
            store.find(id,true).orElseThrow(()->new MarketplaceException(NOT_FOUND,"Listing not found"));
            var current=listings.get(actor,id);
            if(current.hidden() || current.version()!=listing.version()) throw new MarketplaceException(CONFLICT,"Listing changed. Reload before translating.");
            store.cacheTranslation(result); return result;
        });
    }
}
