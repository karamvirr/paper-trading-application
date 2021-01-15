package com.pocketprofit.source.entries;

/**
 * EnhancedStockEntry is is just like StockEntry but it includes more information about the stock
 * that it is describing, more specifically, it includes details of the stocks change in price
 * (both value and percentage) in the latest trading day.
 */
public class EnhancedStockEntry extends StockEntry {
    private double mTotalChange;
    private double mPercentChange;

    public EnhancedStockEntry(String companyName, String companySymbol, int color,
                              double currentPrice, double totalChange, double percentChange) {
        super(companySymbol, companyName, currentPrice, color);
        mTotalChange = totalChange;
        mPercentChange = percentChange;
    }

    public double getTotalChange() {
        return mTotalChange;
    }

    public double getPercentChange() {
        return mPercentChange;
    }
}