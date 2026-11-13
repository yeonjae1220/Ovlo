package me.yeonjae.ovlo.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import me.yeonjae.ovlo.application.dto.command.MarketplaceSearch;
import me.yeonjae.ovlo.application.dto.result.*;
import me.yeonjae.ovlo.application.port.in.marketplace.*;
import me.yeonjae.ovlo.domain.marketplace.model.*;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/v1/marketplace")
public class MarketplaceApiController {
    private final MarketplaceUseCase market;
    private final TranslateListingUseCase translations;
    private final RateLimiterService limiter;
    public MarketplaceApiController(MarketplaceUseCase market,TranslateListingUseCase translations,RateLimiterService limiter) {this.market=market;this.translations=translations;this.limiter=limiter;}
    public record ListingRequest(@NotBlank @Size(max=120) String title,@NotBlank @Size(max=5000) String description,
            @NotBlank String sourceLanguage,@NotBlank String category,@NotBlank String condition,
            boolean free,boolean bundle,boolean partialPickup,@NotNull AskingPrice price,@NotNull PickupLocation location,
            LocalDate pickupFrom,LocalDate pickupUntil,@Size(max=3) Set<ListingTag> tags,@NotEmpty @Size(max=8) List<@NotNull UUID> photoIds) {
        ListingDetails details() {return new ListingDetails(title,description,sourceLanguage,category,condition,free,bundle,partialPickup,price,location,pickupFrom,pickupUntil,tags,photoIds);}
    }
    public record StatusRequest(@PositiveOrZero long version,@NotNull ListingStatus status) {}
    public record ReportRequest(@NotBlank @Size(max=1000) String reason) {}
    @GetMapping public MarketplaceUseCase.ListingPage search(@AuthenticationPrincipal Long actor,
            @RequestParam(required=false) @Size(max=2) String country,@RequestParam(required=false) @Size(max=100) String city,
            @RequestParam(required=false) Long universityId,@RequestParam(required=false) @Size(max=100) String keyword,
            @RequestParam(required=false) @Size(max=20) String category,@RequestParam(required=false) @Size(max=3) String currency,
            @RequestParam(required=false) ListingTag tag,@RequestParam(required=false) Boolean free,@RequestParam(required=false) Boolean bundle,
            @RequestParam(defaultValue="true") boolean availableOnly,@RequestParam(defaultValue="false") boolean mine,
            @RequestParam(defaultValue="0") @Min(0) @Max(1000) int page) {
        return market.search(actor,new MarketplaceSearch(country,city,universityId,keyword,category,currency,tag,free,bundle,availableOnly,mine,page));
    }
    @GetMapping("/options") public Map<String,Object> options() {
        return Map.of("currencies",Currency.getAvailableCurrencies().stream().filter(c->c.getDefaultFractionDigits()>=0).sorted(Comparator.comparing(Currency::getCurrencyCode))
                .map(c->Map.of("code",c.getCurrencyCode(),"digits",c.getDefaultFractionDigits())).toList(),"countries",Arrays.asList(Locale.getISOCountries()));
    }
    @GetMapping("/{id}") public ListingResult get(@AuthenticationPrincipal Long actor,@PathVariable long id) {return market.get(actor,id);}
    @PostMapping public ResponseEntity<ListingResult> create(@AuthenticationPrincipal Long actor,@Valid @RequestBody ListingRequest r) {
        limiter.checkMarketplaceWriteRate(actor); return ResponseEntity.status(201).body(market.create(actor,r.details()));
    }
    @PutMapping("/{id}") public ListingResult edit(@AuthenticationPrincipal Long actor,@PathVariable long id,@RequestParam @Min(0) long version,@Valid @RequestBody ListingRequest r) {
        limiter.checkMarketplaceWriteRate(actor); return market.edit(actor,id,version,r.details());
    }
    @PatchMapping("/{id}/status") public ListingResult status(@AuthenticationPrincipal Long actor,@PathVariable long id,@Valid @RequestBody StatusRequest r) {return market.status(actor,id,r.version(),r.status());}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal Long actor,@PathVariable long id,@RequestParam @Min(0) long version) {market.delete(actor,id,version);}
    @PostMapping(value="/photos",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<Map<String,UUID>> upload(@AuthenticationPrincipal Long actor,@RequestParam MultipartFile file) throws IOException {
        limiter.checkMarketplaceUploadRate(actor);
        if(file.isEmpty() || file.getSize()>8*1024*1024) throw new IllegalArgumentException("Photo must be under 8 MB");
        return ResponseEntity.status(201).body(Map.of("id",market.upload(actor,file.getBytes())));
    }
    @GetMapping("/photos/{id}") public ResponseEntity<byte[]> photo(@AuthenticationPrincipal Long actor,@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(market.photo(actor,id));
    }
    @PostMapping("/{id}/contact") public Map<String,Long> contact(@AuthenticationPrincipal Long actor,@PathVariable long id) {
        limiter.checkMarketplaceWriteRate(actor); return Map.of("roomId",market.contact(actor,id));
    }
    @PostMapping("/{id}/translations/{language}") public ListingTranslation translate(@AuthenticationPrincipal Long actor,@PathVariable long id,@PathVariable String language) {
        limiter.checkMarketplaceTranslationRate(actor); return translations.translate(actor,id,language);
    }
    @PostMapping("/{id}/reports") @ResponseStatus(HttpStatus.NO_CONTENT) public void report(@AuthenticationPrincipal Long actor,@PathVariable long id,@Valid @RequestBody ReportRequest r) {
        limiter.checkMarketplaceWriteRate(actor); market.report(actor,id,r.reason());
    }
    @GetMapping("/blocks") public MarketplaceUseCase.BlockedMemberPage blocks(@AuthenticationPrincipal Long actor,
            @RequestParam(required=false) @Positive Long before) {return market.blockedMembers(actor,before);}
    @PutMapping("/blocks/{target}") @ResponseStatus(HttpStatus.NO_CONTENT) public void block(@AuthenticationPrincipal Long actor,@PathVariable long target) {market.block(actor,target,true);}
    @DeleteMapping("/blocks/{target}") @ResponseStatus(HttpStatus.NO_CONTENT) public void unblock(@AuthenticationPrincipal Long actor,@PathVariable long target) {market.block(actor,target,false);}
}
