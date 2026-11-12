package me.yeonjae.ovlo.application.service.command;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import me.yeonjae.ovlo.application.dto.result.ListingResult;
import me.yeonjae.ovlo.application.dto.result.ListingTranslation;
import me.yeonjae.ovlo.application.port.in.marketplace.*;
import me.yeonjae.ovlo.application.port.out.marketplace.*;
import me.yeonjae.ovlo.domain.marketplace.model.ListingDetails;
import me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException.Kind.*;

/** Cross-instance single flight. Provider calls and follower waits never hold a DB transaction. */
@Service
public class ListingTranslationService implements TranslateListingUseCase {
    private final MarketplaceUseCase listings;
    private final MarketplaceStore store;
    private final ListingTranslationPort translator;
    private final TranslationLeasePort leases;
    private final TransactionTemplate transaction;
    private final Duration leaseLifetime;
    private final long waitNanos;
    private final ScheduledExecutorService renewals=Executors.newScheduledThreadPool(2,
            Thread.ofPlatform().daemon().name("market-translation-lease-",0).factory());

    public ListingTranslationService(MarketplaceUseCase listings, MarketplaceStore store,
            ListingTranslationPort translator, TranslationLeasePort leases, PlatformTransactionManager tx,
            @Value("${ovlo.marketplace.translation.lease-seconds:120}") long leaseSeconds,
            @Value("${ovlo.marketplace.translation.wait-seconds:120}") long waitSeconds) {
        if(leaseSeconds<3 || waitSeconds<1) throw new IllegalArgumentException("Invalid translation lease/wait duration");
        this.listings=listings; this.store=store; this.translator=translator; this.leases=leases;
        this.transaction=new TransactionTemplate(tx); this.leaseLifetime=Duration.ofSeconds(leaseSeconds);
        this.waitNanos=Duration.ofSeconds(waitSeconds).toNanos();
    }
    @Override public ListingTranslation translate(long actor,long id,String language) {
        if(!ListingDetails.LANGUAGES.contains(language)) throw new IllegalArgumentException("Unsupported translation language");
        var listing=listings.get(actor,id);
        var key=new TranslationLeasePort.Key(id,listing.version(),language);
        UUID owner=UUID.randomUUID(); long started=System.nanoTime();
        while(true) {
            // Each follower checks its own visibility, including after waiting for someone else's work.
            var current=readable(actor,key);
            if(current.details().sourceLanguage().equals(language))
                return new ListingTranslation(id,key.version(),language,current.details().title(),current.details().description(),false);
            var cached=store.translation(id,key.version(),language);
            if(cached.isPresent()) { readable(actor,key); return cached.get(); }
            if(leases.acquire(key,owner,leaseLifetime)) {
                try {
                    // Another owner may have published between our cache read and claim.
                    current=readable(actor,key);
                    cached=store.translation(id,key.version(),language);
                    if(cached.isPresent()) { readable(actor,key); return cached.get(); }
                    return generate(actor,key,owner,current);
                } finally { leases.release(key,owner); }
            }
            if(System.nanoTime()-started>=waitNanos) throw unavailable();
            try { Thread.sleep(250); }
            catch(InterruptedException e) { Thread.currentThread().interrupt(); throw unavailable(); }
        }
    }
    private ListingResult readable(long actor,TranslationLeasePort.Key key) {
        var current=listings.get(actor,key.listingId());
        if(current.hidden()) throw new MarketplaceException(NOT_FOUND,"Listing not found");
        if(current.version()!=key.version()) throw new MarketplaceException(CONFLICT,"Listing changed. Reload before translating.");
        return current;
    }
    private ListingTranslation generate(long actor,TranslationLeasePort.Key key,UUID owner,ListingResult listing) {
        var lost=new AtomicBoolean();
        long period=leaseLifetime.toSeconds()/3;
        var heartbeat=renewals.scheduleAtFixedRate(()->{
            try { if(!leases.renew(key,owner,leaseLifetime)) lost.set(true); }
            catch(RuntimeException e) { lost.set(true); }
        },period,period,TimeUnit.SECONDS);
        try {
            var d=listing.details(); ListingTranslationPort.Text text;
            try { text=translator.translateListing(d.sourceLanguage(),key.language(),d.title(),d.description()); }
            catch(Exception e) { throw unavailable(); }
            if(text==null || text.title()==null || text.title().isBlank() || text.title().length()>1000
                    || text.description()==null || text.description().isBlank() || text.description().length()>20000) throw unavailable();
            var result=new ListingTranslation(key.listingId(),key.version(),key.language(),text.title(),text.description(),true);
            return transaction.execute(tx->{
                store.find(key.listingId(),true).orElseThrow(()->new MarketplaceException(NOT_FOUND,"Listing not found"));
                readable(actor,key);
                // A replaced owner cannot cache a late response or release the new owner's claim.
                if(lost.get() || !leases.lockOwned(key,owner)) throw unavailable();
                store.cacheTranslation(result); return result;
            });
        } finally { heartbeat.cancel(false); }
    }
    private MarketplaceException unavailable() {
        return new MarketplaceException(UNAVAILABLE,"Translation is unavailable. You can still read the original.");
    }
    @PreDestroy public void close() { renewals.shutdownNow(); }
}
