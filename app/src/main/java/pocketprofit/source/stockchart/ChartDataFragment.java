package com.pocketprofit.source.stockchart;

/**
 * ChartDataFragment represents a single element in the ChartData.
 * It will contain the price and the text label for that element.
 */
public class ChartDataFragment {
    private double mPrice;
    private String mLabel;

    public ChartDataFragment(double price, String label) {
        this.mPrice = price;
        this.mLabel = label;
    }

    public double getPrice() {
        return mPrice;
    }

    public String getLabel() {
        return mLabel;
    }
}