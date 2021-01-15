package com.pocketprofit.source.entries;

/**
 * StockEntry is a more derived version of SearchResultEntry, more specifically, it includes the
 * price of the stock as well as color to indicate if the stock is trading above or below it's
 * opening price in the most recent trading day.
 */
public class StockEntry extends SearchResultEntry {
    private double mPrice;
    private int mColor;

    public StockEntry (String symbol, String subheader, double price, int color) {
        super(symbol, subheader);
        mPrice = price;
        mColor = color;
    }

    public double getPrice() {
        return mPrice;
    }

    public int getColor() {
        return mColor;
    }
}