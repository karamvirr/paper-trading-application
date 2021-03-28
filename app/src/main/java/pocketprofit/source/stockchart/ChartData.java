package com.pocketprofit.source.stockchart;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A ChartData is a collection of various ChartDataFragments.
 * This object will also keep track of the number of valid entries, the min and max value of the
 * chart, range text, as well as the chart range color.
 * The chart range color is red if the latest price is lower than the first price, and green
 * otherwise.
 */
public class ChartData {
    private final List<ChartDataFragment> mList;
    /**
     * The lowest stock price and highest stock price in the current chart range.
     * These values are derived in parseJSONArray() to be utilized in onDraw() to ensure the graph
     * is scaled proportionally to the size of the StockChartView.
     */
    private final float mMin;
    private final float mMax;
    private final int mChartRangeColor;
    private final int mValidEntriesRegistered;
    private String mChartRangeText;

    public ChartData(List<ChartDataFragment> list, double min, double max, int chartRangeColor, int validEntriesRegistered, String chartRangeText) {
        this.mList = list;
        this.mMin = (float) min;
        this.mMax = (float) max;
        this.mChartRangeColor = chartRangeColor;
        this.mValidEntriesRegistered = validEntriesRegistered;
        this.mChartRangeText = chartRangeText;
    }

    public void updateChartRangeText(String updatedText) {
        this.mChartRangeText = updatedText;
    }

    public String getChartRangeText() {
        return mChartRangeText;
    }

    public int getValidEntriesRegistered() {
        return mValidEntriesRegistered;
    }

    public List<ChartDataFragment> getList() {
        return mList;
    }

    public float getMin() {
        return mMin;
    }

    public float getMax() {
        return mMax;
    }

    public int getChartRangeColor() {
        return mChartRangeColor;
    }

    public int size() {
        return (mList == null) ? 0 : mList.size();
    }

    public ChartDataFragment get(int i) {
        if (i >= 0 && i < mList.size()) {
            return mList.get(i);
        }
        return null;
    }

    @NotNull
    public String toString() {
        StringBuilder str = new StringBuilder("[");
        if (mList.size() > 0) {
            int i = 0;
            while (i < mList.size() && mList.get(i) == null) {
                i++;
            }
            str.append(mList.get(i).getLabel()).append(" - ").append(mList.get(i++).getPrice());
            while (i < mList.size()) {
                if (mList.get(i) != null) {
                    str.append(", ").append(mList.get(i).getLabel()).append(" - ").append(mList.get(i).getPrice());
                }
                i++;
            }
        }
        return str + "]";
    }

}