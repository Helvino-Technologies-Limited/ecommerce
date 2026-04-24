package com.helvino.ecommerce.enums;

public enum Currency {
    KES("KSh", "Kenyan Shilling"),
    USD("$", "US Dollar"),
    EUR("€", "Euro"),
    GBP("£", "British Pound"),
    UGX("UGX", "Ugandan Shilling"),
    TZS("TSh", "Tanzanian Shilling"),
    ETB("Br", "Ethiopian Birr"),
    NGN("₦", "Nigerian Naira"),
    GHS("GH₵", "Ghanaian Cedi"),
    ZAR("R", "South African Rand");

    private final String symbol;
    private final String displayName;

    Currency(String symbol, String displayName) {
        this.symbol = symbol;
        this.displayName = displayName;
    }

    public String getSymbol() { return symbol; }
    public String getDisplayName() { return displayName; }
}
