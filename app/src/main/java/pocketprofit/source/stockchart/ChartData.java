package com.pocketprofit.source.stockchart;

import java.util.List;

/**
 * A ChartData is a collection of various ChartDataFragments.
 * This object will also keep track of the number of valid entries, the min and max value of the
 * chart, as well as the chart range color.
 * The chart range color is red if the latest price is lower than the first price, and green
 * otherwise.
 */
public class ChartData {
    private List<ChartDataFragment> mList;
    /**
     * The lowest stock price and highest stock price in the current chart range.
     * These values are derived in parseJSONArray() to be utilized in onDraw() to ensure the graph
     * is scaled proportionally to the size of the StockChartView.
     */
    private double mMin;
    private double mMax;
    private int mChartRangeColor;
    private int mValidEntriesRegistered;

    public ChartData(List<ChartDataFragment> list, double min, double max, int chartRangeColor, int validEntriesRegistered) {
        this.mList = list;
        this.mMin = min;
        this.mMax = max;
        mChartRangeColor = chartRangeColor;
        mValidEntriesRegistered = validEntriesRegistered;
    }

    public int getValidEntriesRegistered() {
        return mValidEntriesRegistered;
    }

    public List<ChartDataFragment> getList() {
        return mList;
    }

    public double getMin() {
        return mMin;
    }

    public double getMax() {
        return mMax;
    }

    public int getChartRangeColor() {
        return mChartRangeColor;
    }

    public int size() {
        return (mList == null) ? 0 : mList.size();
    }

}