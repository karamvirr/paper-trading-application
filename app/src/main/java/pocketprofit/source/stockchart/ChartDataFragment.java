package com.pocketprofit.source.stockchart;

/**
 * ChartDataFragment represents a single element in the ChartData.
 * It will contain the price and the text label for that element.
 */
public class ChartDataFragment {
    private final float mPrice;
    private final String mLabel;

    public ChartDataFragment(double price, String label) {
        this.mPrice = (float) price;
        this.mLabel = label;
    }

    public float getPrice() {
        return mPrice;
    }

    public String getLabel() {
        return mLabel;
    }
}