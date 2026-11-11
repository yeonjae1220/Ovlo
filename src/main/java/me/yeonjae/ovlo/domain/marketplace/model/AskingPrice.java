package me.yeonjae.ovlo.domain.marketplace.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

/** Seller's requested settlement currency; independent of pickup country. No implicit FX conversion. */
public record AskingPrice(BigDecimal amount, String currency) {
    public AskingPrice {
        if (amount == null || currency == null) throw new IllegalArgumentException("Price and currency are required");
        currency = currency.toUpperCase(Locale.ROOT);
        int digits = Currency.getInstance(currency).getDefaultFractionDigits();
        if (digits < 0 || digits > 8 || amount.signum() < 0 || amount.compareTo(new BigDecimal("999999999999")) > 0
                || amount.stripTrailingZeros().scale() > digits) throw new IllegalArgumentException("Invalid price or currency precision");
        amount = amount.setScale(digits);
    }
}
