package com.pocketprofit.source.stockchart;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractStockChartView extends View {
    /**
     * The default range that is selected when the StockChartView first launches.
     */
    public static final String DEFAULT_RANGE = "oneDay";

    /**
     * Used the state to keep track of the PPS state for StockChartView.
     */
    protected enum State { OFF, ON }
    protected State mState;

    /*
     * Whenever user selects a chart option: (ChartChangeListener)
     *      - Change the change text and color.
     *      - Change the news portal button color
     *      - Change the chart option button color.
     *
     * Whenever user makes selection on chart: (UserSelectionListener)
     *      - Change the price text.
     *      - Change the change text and color.
     */

    /* ****************************************************************************************** *
     *                               Chart Color Listener
     * ****************************************************************************************** */


    /**
     * A list of registered ChartChangeListeners
     */
    private List<ChartChangeListener> mChartChangeListeners;

    /**
     * Class which defines a listener to be called when the chart has successfully loaded onto the
     * StockChartView. This is useful as some Views derive their color dynamically based on the
     * color of the current chart range.
     */
    public interface ChartChangeListener {
        void onChartChange(int newColor, String chartChange);
    }

    /**
     * Registers the provided listener.
     *
     * @param chartChangeListener the listener to be added to the collection.
     * @throws IllegalArgumentException if the given chartChangeListener is null
     * @return true if the provide listener is successfully added, false otherwise.
     */
    public final boolean addChartChangeListener(ChartChangeListener chartChangeListener) {
        if (chartChangeListener == null) {
            throw new IllegalArgumentException("chartChangeListener should never be null!");
        }
        return mChartChangeListeners.add(chartChangeListener);
    }

    /**
     * Removes a ChartChangeListener, if it currently is in the collection of listeners.
     *
     * @param chartChangeListener the listener to be removed from the collection.
     * @return true if the provided listener is successfully removed, false otherwise.
     */
    public final boolean removeChartChangeListener(ChartChangeListener chartChangeListener) {
        return mChartChangeListeners.remove(chartChangeListener);
    }

    /**
     * Method that will notify all of the registered listeners that the new chart has been
     * successfully loaded onto the StockChartView and a new color has been changed in accordance.
     *
     * @param newColor      the color to set the param 'chartChange' to.
     * @param chartChange   the string containing text information about the range selected.
     */
    protected void invokeChartChangeListeners(int newColor, String chartChange) {
        for (int i = 0; i < mChartChangeListeners.size(); i++) {
            ChartChangeListener l = mChartChangeListeners.get(i);
            l.onChartChange(newColor, chartChange);
        }
    }


    /* ****************************************************************************************** *
     *                               User Selection Listener
     * ****************************************************************************************** */


    /**
     * A list of registered UserSelectionListeners
     */
    private List<UserSelectionListener> mUserSelectionListeners;

    /**
     * Class which defines a listener to be called when a selection is made on the StockChartView.
     * This is useful as some TextViews derive their text content & color dynamically based upon
     * the information currently selected by the user.
     */
    public interface UserSelectionListener {
        void onUserSelection(int color, String chartChange, double price);
    }

    /**
     * Registers the provided listener.
     *
     * @param userSelectionListener the listener to be added to the collection.
     * @throws IllegalArgumentException if the given userSelectionListener is null.
     * @return true if the provided listener is successfully added, false otherwise.
     */
    public final boolean addUserSelectionListener(UserSelectionListener userSelectionListener) {
        if (userSelectionListener == null) {
            throw new IllegalArgumentException("userSelectionListener should never be null!");
        }
        return mUserSelectionListeners.add(userSelectionListener);
    }

    /**
     * Removes a UserSelectionListener if it currently is in the collection of listeners.
     *
     * @param userSelectionListener the listener to be removed from the collection.
     * @return true if the provided listener is successfully removed, false otherwise.
     */
    public final boolean removeUserSelectionListener(UserSelectionListener userSelectionListener) {
        return mUserSelectionListeners.remove(userSelectionListener);
    }

    /**
     * Method that will notify all the registered listeners that the user has made a selection on
     * the StockChartView.
     *
     * @param color         the color to set the 'chartChange' param.
     * @param chartChange   the string containing information about the user selection.
     * @param price         the price of the stock
     */
    protected void invokeUserSelectionListeners(int color, String chartChange, double price) {
        for (int i = 0; i < mUserSelectionListeners.size(); i++) {
            UserSelectionListener l = mUserSelectionListeners.get(i);
            l.onUserSelection(color, chartChange, price);
        }
    }

    public AbstractStockChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mChartChangeListeners = new ArrayList<>();
        mUserSelectionListeners = new ArrayList<>();
    }
}