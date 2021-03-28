package com.pocketprofit.source.stockchart;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import com.android.volley.VolleyError;
import com.pocketprofit.R;
import com.pocketprofit.source.JSONArrayCallback;
import com.pocketprofit.source.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StockChartView extends AbstractStockChartView {
    // width of the chart lines drawn on the canvas.
    public static final int STROKE_WIDTH = 3;

    // used to create the intraday chart, makes it so the line chart consists of this many segments.
    // total minutes (5min intervals) in a regular trading session (9:30am-4:00pm EST)
    public static final int MINUTES_IN_TRADING_HOURS = 78;

    /**
     * Cache Collection: Every time a user selects a different chart range that has not been
     * already selected, the information gathered from the API GET request is stored in the
     * corresponding objects. This increases performance as subsequent selections on this range
     * (ex. user clicking '6m' -> '1y' -> '6m') will not require a network request as its already
     * saved locally. Also, it saves money as unnecessary  API requests are not sent :).
     */
    private final Map<String, ChartData> mChartDataCache;

    // the data that is currently being used to draw the graph.
    private ChartData mCurrentRangeData;

    // the chart range that is currently in view.
    private String mRange;

    // ticker symbol of the company the charts describe.
    private String mSymbol;
    // the latest price and the previous close price for the company the charts describe.
    private float mLatestPrice;
    private float mPreviousClose;

    // stores a references to the context of the chart view's parent activity
    private final Context mContext;

    // helps handle the interaction between the user and the stockchartview.
    private final GestureDetector mGestureDetector;

    // helps activate haptic feedback at correct times when selecting through 5d data
    private String mCurrentFiveDayLabel;
    private Set<String> mHighlightedFiveDayLabels;

    /**
     * Canvas brushes.
     */
    private final Paint mChartBrush;
    private final Paint mDottedBrush;
    private final Paint mSelectionBrush;
    private final Paint mTextBrush;

    // used to make the previous close dotted line on chart when the range is 1d (intraday)
    // note: stored as a field to avoid 'new Path()' memory allocations in onDraw(...)
    private final Path mPreviousCloseLine;

    /**
     * The currently selected element of the chart.
     */
    private int mCurrentIndex;

    public StockChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context; 
        mState = State.OFF;
        mPreviousCloseLine = new Path();
        mCurrentIndex = -1;  // not set sentinel

        mChartDataCache = new HashMap<String, ChartData>();

        mChartBrush = new Paint(Paint.ANTI_ALIAS_FLAG);
        mChartBrush.setStrokeWidth(STROKE_WIDTH);
        mChartBrush.setDither(true);
        mChartBrush.setStyle(Paint.Style.STROKE);
        mChartBrush.setStrokeJoin(Paint.Join.ROUND);
        mChartBrush.setStrokeCap(Paint.Cap.ROUND);
        mChartBrush.setPathEffect(new CornerPathEffect(10) );
        mChartBrush.setAntiAlias(true);

        mSelectionBrush = new Paint();
        mSelectionBrush.setStyle(Paint.Style.STROKE);
        mSelectionBrush.setDither(true);
        mSelectionBrush.setStrokeCap(Paint.Cap.ROUND);
        mSelectionBrush.setStrokeWidth(STROKE_WIDTH + 2);
        mSelectionBrush.setColor(Color.GRAY);
        mSelectionBrush.setPathEffect(new CornerPathEffect(10) );
        mSelectionBrush.setAntiAlias(true);

        mDottedBrush = new Paint();
        mDottedBrush.setStyle(Paint.Style.STROKE);
        mDottedBrush.setStrokeWidth((int) (STROKE_WIDTH / 2));
        mDottedBrush.setStrokeCap(Paint.Cap.ROUND);
        mDottedBrush.setColor(Color.LTGRAY);
        mDottedBrush.setAlpha((int) (255 * 0.7));  // setting opacity of dotted line to ~70%
        mDottedBrush.setPathEffect(new DashPathEffect(new float[]{6f,10f}, 0));

        mTextBrush = new TextPaint();
        mTextBrush.setAntiAlias(true);
        mTextBrush.setColor(Color.GRAY);
        mTextBrush.setTextSize(14 * getResources().getDisplayMetrics().density);
        mTextBrush.setTypeface(ResourcesCompat.getFont(context, R.font.nunito));

        this.setHapticFeedbackEnabled(true);
        mCurrentFiveDayLabel = null;
        mHighlightedFiveDayLabels = new HashSet<String>();

        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                mState = State.ON;
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (distanceX <= 75 && distanceY <= 75 && mState == State.OFF) {
                    mState = State.ON;
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                }
                return super.onScroll(e1, e2, distanceX, distanceY);
            }
        });
    }

    /**
     * Sets the ticker symbol, the previous close, and the latest price of the company this
     * stock chart describes.
     *
     * @param symbol            ticker symbol of the security.
     * @param previousClose     the previous close price of the stock.
     * @param latestPrice       the latest price of the stock.
     */
    public void setValues(String symbol, double previousClose, double latestPrice) {
        mSymbol = symbol;
        mPreviousClose = (float) previousClose;
        mLatestPrice = (float) latestPrice;
        int color = getIntervalColor(mPreviousClose, mLatestPrice);
        mChartBrush.setColor(color);
    }

    /**
     * Updates the latest price of the stock the chart describes.
     *
     * @param updatedStockPrice the updated price of the stock.
     */
    public void updateLatestPrice(double updatedStockPrice) {
        mLatestPrice = (float) updatedStockPrice;
    }

    /**
     * Clears the vertical line with the label on top which represents a user selection.
     */
    public void clearUserSelection() {
        mState = State.OFF;
        mCurrentIndex = -1;
        mCurrentFiveDayLabel = null;
        mHighlightedFiveDayLabels.clear();
        invalidate();
    }

    /**
     * Returns the previous close of the stock modeled by the chart.
     *
     * @return  previous close price of stock.
     */
    public double getPreviousClose() {
        return mPreviousClose;
    }

    /**
     * Precondition: mCurrentRangeData != null and contains at least one valid non-null entry.
     * Finds and returns the first valid chart element of the chart currently on display.
     * First in this case is defined as the left most element.
     *
     * @return  ChartDataFragment of the first valid chart element.
     */
    public double getFirst() {
        return getFirst(mCurrentRangeData.getList());
    }

    /**
     * Precondition: mCurrentRangeData != null and contains at least one valid non-null entry.
     * Finds and returns the first valid chart element of the list given in as a parameter.
     * First in this case is defined as the left most element.
     *
     * @return  ChartDataFragment of the first valid chart element.
     */
    public double getFirst(List<ChartDataFragment> data) {
        int index = 0;
        while (index < data.size() && data.get(index) == null) {
            index++;
        }
        return data.get(index).getPrice();
    }

    /**
     * Returns the color of the current chart in view.
     *
     * @return  graph color as int.
     */
    public int getColor() {
        return mChartBrush.getColor();
    }

    /**
     * Updates the current chart currently displayed on the screen using the range given in as a
     * parameter.
     * If the chart range data is not currently cached, then it will make a call to the PocketProfit
     * server to retrieve that information.
     * It will also alert the listeners as to the current color of the chart (green if stock price
     * increases in range, red otherwise) as well as to alert listeners as to the total value change
     * and percentage change of the stock in the range provided.
     *
     * @param range the chart range selected by the user.
     */
    public void setChartRange(final String range) {
        clearUserSelection();
        mRange = range;
        if (!mChartDataCache.containsKey(mRange)) {
            switch (mRange) {
                // lot of redundant code here due to async function calls
                case "1D":
                    Util.fetchIntradayChartData(mContext, mSymbol, new JSONArrayCallback() {
                        @Override
                        public void onSuccess(JSONArray result) {
                            mCurrentRangeData = parseJSONResult(result);
                            mChartDataCache.put(mRange, mCurrentRangeData);
                            notifyChartChangeListeners();
                            invalidate();
                        }

                        @Override
                        public void onFailure(VolleyError error, String errorType) {
                            // TODO
                        }
                    });
                    break;
                case "5D":
                    Util.fetchFiveDayChartData(mContext, mSymbol, new JSONArrayCallback() {
                        @Override
                        public void onSuccess(JSONArray result) {
                            mChartDataCache.put(mRange, parseJSONResult(result));
                            mCurrentRangeData = mChartDataCache.get(mRange);
                            notifyChartChangeListeners();
                            invalidate();
                        }

                        @Override
                        public void onFailure(VolleyError error, String errorType) {
                            // TODO
                        }
                    });
                    break;
                case "1M":
                    Util.fetchOneMonthChartData(mContext, mSymbol, new JSONArrayCallback() {
                        @Override
                        public void onSuccess(JSONArray result) {
                            mChartDataCache.put(mRange, parseJSONResult(result));
                            mCurrentRangeData = mChartDataCache.get(mRange);
                            notifyChartChangeListeners();
                            invalidate();
                        }

                        @Override
                        public void onFailure(VolleyError error, String errorType) {
                            // TODO
                        }
                    });
                    break;
                case "6M":
                    Util.fetchSixMonthChartData(mContext, mSymbol, new JSONArrayCallback() {
                        @Override
                        public void onSuccess(JSONArray result) {
                            mChartDataCache.put(mRange, parseJSONResult(result));
                            mCurrentRangeData = mChartDataCache.get(mRange);
                            notifyChartChangeListeners();
                            invalidate();
                        }

                        @Override
                        public void onFailure(VolleyError error, String errorType) {
                            // TODO
                        }
                    });
                    break;
                case "1Y":
                    Util.fetchOneYearChartData(mContext, mSymbol, new JSONArrayCallback() {
                        @Override
                        public void onSuccess(JSONArray result) {
                            mChartDataCache.put(mRange, parseJSONResult(result));
                            mCurrentRangeData = mChartDataCache.get(mRange);
                            notifyChartChangeListeners();
                            invalidate();
                        }

                        @Override
                        public void onFailure(VolleyError error, String errorType) {
                            // TODO
                        }
                    });
                    break;
                case "5Y":
                    Util.fetchFiveYearChartData(mContext, mSymbol, new JSONArrayCallback() {
                        @Override
                        public void onSuccess(JSONArray result) {
                            mChartDataCache.put(mRange, parseJSONResult(result));
                            mCurrentRangeData = mChartDataCache.get(mRange);
                            notifyChartChangeListeners();
                            invalidate();
                        }

                        @Override
                        public void onFailure(VolleyError error, String errorType) {
                            // TODO
                        }
                    });
                    break;
            }
        } else {
            mCurrentRangeData = mChartDataCache.get(mRange);
            notifyChartChangeListeners();
            invalidate();
        }
    }

    /**
     * Creates and initializes a ChartData using the JSON information given in as a parameter.
     * This information is then returned.
     *
     * @param result    the JSON data sent back from a call to the PocketProfit server.
     * @return          the ChartData that describes the JSON parameter.
     */
    public ChartData parseJSONResult(JSONArray result) {
        List<ChartDataFragment> parsedResult = new ArrayList<>();
        // valid entries in a ChartData are entries whose "close" field is not null.
        // this means that they have a value and therefore able to be shown in a graph.
        int validEntries = 0;
        double min = mRange.equals("1D") ? mPreviousClose : Double.MAX_VALUE;
        double max = mRange.equals("1D") ? mPreviousClose : Double.MIN_VALUE;
        String column = mRange.equals("1D") ? "label" : "date";

        for (int i = 0; i < result.length(); i++) {
            try {
                JSONObject jsonObject = result.getJSONObject(i);
                if (jsonObject.isNull("close")) {
                    parsedResult.add(null);
                } else {
                    validEntries++;
                    double close = jsonObject.getDouble("close");
                    String label = jsonObject.getString(column);

                    if (close < min) {
                        min = close;
                    }
                    if (close > max) {
                        max = close;
                    }

                    if (!mRange.equals("1D")) {
                        // yyyy-mm-dd -> MONTH DAY+SUFFIX, YEAR
                        String[] splitDate = label.split("-");
                        String year = splitDate[0];
                        int monthIndex = Integer.parseInt(splitDate[1]) - 1;
                        String month = new DateFormatSymbols().getMonths()[monthIndex].substring(0, 3);
                        String day = splitDate[2];
                        if (day.charAt(0) == '0') {
                            day = day.substring(1);
                        }
                        label = month + " " + day + Util.getDayOfMonthSuffix(Integer.parseInt(day)) + ", " + year;
                    } else {
                        if (label.startsWith("0")) {
                            label = label.substring(1);
                        }
                        // ex. 'x AM/PM' -> 'x:00 AM/PM'
                        if (!label.contains(":")) {
                            label = label.substring(0, label.indexOf(' ')) + ":00 " + label.substring(label.indexOf(' ') + 1);
                        }
                    }
                    if (mRange.equals("5D")) {
                        String minute = jsonObject.getString("minute");
                        label += " at " + Util.convertMilitaryToStandard(minute);
                    }

                    parsedResult.add(new ChartDataFragment(close, label));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        int rangeColor;
        String chartDataText;
        if (validEntries == 0) {
            // result.length() == 0 || result contains > 0 close values all set to null
            rangeColor = mContext.getResources().getColor(R.color.lightGray);
            chartDataText = mContext.getString(R.string.percent_change_placeholder);
        } else {
            // validEntries != 0, result.length() > 0, result.length() may not equal validEntries
            double first = mRange.equals("1D") ? mPreviousClose : getFirst(parsedResult);
            double last = mLatestPrice;
            rangeColor = getIntervalColor(first, last);
            chartDataText = Util.getPercentChangeText(first, mLatestPrice, false, false);
        }
        return new ChartData(parsedResult, min, max, rangeColor, validEntries, chartDataText);
    }

    /**
     * Handles the user's touch input on the chart. Allows user to select the chart element closest
     * to where the user made a selection on the chart.
     *
     * @param event Event for touch.
     * @return      true if the listener has consumed the event, false otherwise.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCurrentRangeData == null || mCurrentRangeData.getValidEntriesRegistered() <= 1) {
            return false;
        }
        mGestureDetector.onTouchEvent(event);
        if (mState == State.ON) {
            setPositionIndex(event);
            double first = mRange.equals("1D") ? mPreviousClose : getFirst();
            double last = mLatestPrice;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                getParent().requestDisallowInterceptTouchEvent(false);
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                clearUserSelection();
            } else {
                if (mCurrentIndex != -1) {
                    last = mCurrentRangeData.get(mCurrentIndex).getPrice();
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                invalidate();
            }
            invokeUserSelectionListeners(getIntervalColor(first, last),
                    Util.getPercentChangeText(first, last, false, false), last);
        }
        return true;
    }

    /**
     * Finds and sets the index of the chart data element closest to the the point on the chart
     * that the user has selected. This is done by using the MotionEvent parameter which represents
     * a user pressing down or dragging finger on chart.
     * If there is no valid index, then the mCurrentIndex is set to -1.
     *
     * @param event Event for touch.
     */
    public void setPositionIndex(MotionEvent event) {
        if (mCurrentRangeData != null) {
            List<ChartDataFragment> chartElements = mCurrentRangeData.getList();
            float viewWidth = getWidth();
            float touchX = event.getX();
            float deltaX = viewWidth / (mRange.equals("1D") ? MINUTES_IN_TRADING_HOURS - 1 : chartElements.size() - 1);

            int index = mCurrentIndex;
            for (int i = 0; i < chartElements.size(); i++) {
                if (chartElements.get(i) != null) {
                    float elementXPosition = i * deltaX;
                    if (touchX <= elementXPosition + (deltaX / 2) && touchX >= elementXPosition - (deltaX / 2)) {
                        index = i;
                        break;
                    }
                }
            }
            mCurrentIndex = index;
        }
    }

    /**
     * Draw the Stock Chart on the Canvas
     *
     * @param canvas the canvas that is drawn upon
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // don't draw on canvas if no data has been fetched yet.
        if (mCurrentRangeData != null) {
            float viewWidth = getWidth();
            float viewHeight = getHeight();

            if (mCurrentRangeData.getValidEntriesRegistered() <= 1) {
                // not enough valid chart elements to chart.
                // this could either be cause the stock has no or low trading volume (ex. some days
                // of BRK.A trading) or the markets have just opened up. in this case we would just
                // want to show a chart of the previous close if intraday or notify the user via text.
                if (mRange.equals("1D")) {
                    mPreviousCloseLine.reset();
                    mPreviousCloseLine.moveTo(0, viewHeight * 0.5f);
                    mPreviousCloseLine.lineTo(viewWidth, viewHeight * 0.5f);
                    canvas.drawPath(mPreviousCloseLine, mDottedBrush);
                } else {
                    String message = "No Data Available";
                    mTextBrush.setTextSize(18 * getResources().getDisplayMetrics().density);
                    float offset = mTextBrush.measureText(message);
                    canvas.drawText(message, 0.5f * (viewWidth - offset), 0.5f * viewHeight, mTextBrush);
                    mTextBrush.setTextSize(14 * getResources().getDisplayMetrics().density);
                }
            } else {
                float textArea = viewHeight * 0.1f;
                List<ChartDataFragment> chartElements = mCurrentRangeData.getList();

                float deltaX = viewWidth / (mRange.equals("1D") ? MINUTES_IN_TRADING_HOURS - 1 : chartElements.size() - 1);

                if (mRange.equals("5D") && mCurrentIndex != -1) {
                    // only appending the date portion of the label
                    String selectedLabel = chartElements.get(mCurrentIndex).getLabel();
                    selectedLabel = selectedLabel.substring(0, selectedLabel.indexOf(" at "));
                    mHighlightedFiveDayLabels.add(selectedLabel);
                    // find the next non-null label
                    int i = mCurrentIndex + 1;
                    while (i < chartElements.size() && chartElements.get(i) == null) {
                        i++;
                    }
                    // next non-null label exists
                    if (i < chartElements.size()) {
                        String nextLabel = chartElements.get(i).getLabel();
                        mHighlightedFiveDayLabels.add(nextLabel.substring(0, nextLabel.indexOf(" at ")));
                    }
                    if (mCurrentFiveDayLabel != null && !mCurrentFiveDayLabel.equals(selectedLabel)) {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                    }
                    mCurrentFiveDayLabel = selectedLabel;
                }

                int i = 0;
                while (i < chartElements.size() && chartElements.get(i) == null) {
                    i++;
                }
                int j = i + 1;
                while (j < chartElements.size()) {
                    if (chartElements.get(j) != null) {
                        if (mCurrentIndex != -1) {
                            if (mRange.equals("5D")) {
                                String selectedLabel = chartElements.get(j).getLabel();
                                selectedLabel = selectedLabel.substring(0, selectedLabel.indexOf(" at "));
                                if (!mHighlightedFiveDayLabels.contains(selectedLabel)) {
                                    mChartBrush.setColor(ColorUtils.blendARGB(mChartBrush.getColor(), Color.BLACK, 0.6f));
                                }
                            } else {
                                if (mCurrentIndex <= i) {
                                    mChartBrush.setColor(ColorUtils.blendARGB(mChartBrush.getColor(), Color.BLACK, 0.6f));
                                }
                            }
                        }
                        canvas.drawLine(deltaX * i, getYPosition(chartElements.get(i).getPrice()),
                                deltaX * j, getYPosition(chartElements.get(j).getPrice()), mChartBrush);
                        mChartBrush.setColor(mCurrentRangeData.getChartRangeColor());
                        i = j;
                    }
                    j++;
                }

                if (mRange.equals("1D")) {  // draw the previous close line
                    mPreviousCloseLine.reset();
                    mPreviousCloseLine.moveTo(0, getYPosition(mPreviousClose));
                    mPreviousCloseLine.lineTo(viewWidth, getYPosition(mPreviousClose));
                    canvas.drawPath(mPreviousCloseLine, mDottedBrush);
                }

                if (mCurrentIndex != -1 && mCurrentIndex < chartElements.size()) {
                    String selectedLabel = chartElements.get(mCurrentIndex).getLabel();
                    canvas.drawLine(mCurrentIndex * deltaX, textArea, mCurrentIndex * deltaX, viewHeight - STROKE_WIDTH, mSelectionBrush);

                    float textWidth = mTextBrush.measureText(selectedLabel);
                    float textPosition = mCurrentIndex * deltaX - (textWidth / 2);
                    int buffer = (int) (Resources.getSystem().getDisplayMetrics().density * 10);
                    if (textPosition < buffer) {
                        textPosition = buffer;
                    } else if (textPosition + textWidth >= viewWidth - buffer) {
                        textPosition = viewWidth - textWidth - buffer;
                    }
                    // FontMetrics used to help ensure that the text is drawn centered vertically of the textArea
                    Paint.FontMetrics fm = mTextBrush.getFontMetrics();
                    float lineHeight = fm.descent - fm.ascent + fm.ascent;
                    canvas.drawText(selectedLabel, textPosition, textArea * 0.5f + lineHeight, mTextBrush);
                }

                mHighlightedFiveDayLabels.clear();
            }
        }
    }

    /**
     * Returns the color for the endpoints given in as a parameter.
     * This color signals if the interval is a net gain or loss.
     *
     * @param a the starting value.
     * @param b the ending value.
     * @return  returns green if b >= a, red otherwise.
     */
    private int getIntervalColor(double a, double b) {
        return ((b >= a) ? getResources().getColor(R.color.profit)
                : getResources().getColor(R.color.loss));
    }

    /**
     * Takes in an stock price and returns its corresponding vertical position on the chart using a linear
     * scale function
     *
     * @param price the price of the function we want to scale.
     * @return      the y position on the chart view that the price maps to.
     */
    private float getYPosition(float price) {
        // accepts input between chart min & max (the domain) and maps to output between
        // rangeMin & rangeMax
        float viewHeight = getHeight();

        float domainMin = mCurrentRangeData.getMin();
        float domainMax = mCurrentRangeData.getMax();

        float rangeMin = viewHeight * 0.95f;
        float rangeMax = viewHeight * 0.15f;

        return (((rangeMax - rangeMin) * (price - domainMin)) / (domainMax - domainMin)) + rangeMin;
    }

    /**
     * Invokes all current listeners of this view of the changes to the chart range.
     */
    private void notifyChartChangeListeners() {
        if (mCurrentRangeData != null) {
            mChartBrush.setColor(mCurrentRangeData.getChartRangeColor());
            invokeChartChangeListeners(mCurrentRangeData.getChartRangeColor(), mCurrentRangeData.getChartRangeText());
        }
    }
}