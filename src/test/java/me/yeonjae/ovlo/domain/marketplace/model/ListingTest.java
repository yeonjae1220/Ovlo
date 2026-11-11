package me.yeonjae.ovlo.domain.marketplace.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ListingTest {
    static ListingDetails details(boolean free, String amount, String currency, boolean bundle, boolean partial) {
        return new ListingDetails("Kitchen bundle", "Everything except the microwave", "en", "HOUSEHOLD", "GOOD",
                free, bundle, partial, new AskingPrice(new BigDecimal(amount), currency),
                new PickupLocation("FR", "Paris", 1L, "Campus gate"), null, LocalDate.of(2027, 1, 1),
                Set.of(ListingTag.RETURN_HOME), List.of(UUID.randomUUID()));
    }
    @Test void acceptsHomeCurrencyRegardlessOfPickupCountry() {
        var listing = Listing.create(7, details(false, "25000", "KRW", true, false));
        assertThat(listing.details().price().currency()).isEqualTo("KRW");
        assertThat(listing.status()).isEqualTo(ListingStatus.AVAILABLE);
    }
    @Test void validatesCurrencyPrecisionAndAmount() {
        assertThatThrownBy(() -> new AskingPrice(new BigDecimal("1.1"), "KRW")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AskingPrice(new BigDecimal("-1"), "EUR")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AskingPrice(BigDecimal.ONE, "XYZ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new AskingPrice(new BigDecimal("1.2300"), "EUR").amount()).isEqualByComparingTo("1.23");
        assertThat(new AskingPrice(new BigDecimal("1.234"), "KWD").amount()).isEqualByComparingTo("1.234");
        assertThat(new AskingPrice(new BigDecimal("1.2345"), "CLF").amount()).isEqualByComparingTo("1.2345");
    }
    @Test void freeAndPartialPickupHaveExplicitInvariants() {
        assertThatThrownBy(() -> details(true, "1", "EUR", true, true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> details(false, "0", "EUR", false, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> details(true, "0", "EUR", false, true)).isInstanceOf(IllegalArgumentException.class);
        assertThat(details(true, "0", "EUR", true, true).free()).isTrue();
    }
    @Test void onlyOwnerCanEditAndCompletedOrHiddenListingsCannotBeChanged() {
        var listing = Listing.create(7, details(true, "0", "EUR", true, true));
        assertThatThrownBy(() -> listing.changeStatus(8, ListingStatus.RESERVED)).hasMessageContaining("owner");
        listing.changeStatus(7, ListingStatus.PARTIALLY_TAKEN);
        listing.changeStatus(7, ListingStatus.RESERVED);
        listing.changeStatus(7, ListingStatus.AVAILABLE);
        listing.changeStatus(7, ListingStatus.COMPLETED);
        assertThatThrownBy(() -> listing.changeStatus(7, ListingStatus.AVAILABLE)).hasMessageContaining("completed");
        listing.moderate(true);
        assertThatThrownBy(() -> listing.edit(7, listing.details())).hasMessageContaining("hidden");
    }
}
