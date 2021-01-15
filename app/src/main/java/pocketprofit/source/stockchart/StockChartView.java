package com.pocketprofit.source.stockchart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import com.pocketprofit.R;
import com.pocketprofit.source.JSONArrayCallback;
import com.pocketprofit.source.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;

public class StockChartView extends AbstractStockChartView {
    // width of the chart lines drawn on the canvas.
    public static final int STROKE_WIDTH = 3;

    // used to create the intraday chart, partitions the chart to this many segments.
    // total minutes in a regular trading session (9:30am-4:00pm)
    public static final int MINUTES_IN_TRADING_HOURS = 390;

    // used when parsing through chart elemente data sent back from a PocketProfit server.
    // elements whose price is 'null' are flagged with this value.
    public static final int INVALID_ENTRY_FLAG = 0;

    /**
     * Cache Collection: Every time a user selects a different chart range that has not been
     * already selected, the information gathered from the API GET request is stored in the
     * corresponding objects. This increases performance as subsequent selections on this range
     * (ex. user clicking '6m' -> '1y' -> '6m') will not require a network request as its already
     * saved locally. Also, it saves money as unnecessary  API requests are not sent :).
     */
    private ChartData mOneDay;
    private ChartData mFiveDay;
    private ChartData mOneMonth;
    private ChartData mSixMonth;
    private ChartData mOneYear;
    private ChartData mFiveYear;

    // the data that is currently being used to draw the graph.
    private ChartData mCurrentUtilizedData;

    // ticker symbol of the company the charts describe.
    private String mSymbol;
    // the latest price and the previous close price for the company the charts describe.
    private double mLatestPrice;
    private double mPreviousClose;
    // the chart range that is currently in view.
    private String mRange;

    // stores a references to the context of the chart view's parent activity
    private Context mContext;
 
    /**
     * Canvas brushes.
     */
    private Paint mChartBrush;
    private Paint mDottedBrush;
    private Paint mSelectionBrush;
    private Paint mTextBrush;

    // used to make the previous close dotted line on chart when the range is 1d (intraday)
    // note: stored as a field to avoid 'new Path()' memory allocations in onDraw(...)
    private Path mPreviousCloseLine;

    /**
     * The currently selected element of the chart.
     */
    private int mCurrentIndex = -1;

    public StockChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context; 
        mState = State.OFF;
        mPreviousCloseLine = new Path();

        mChartBrush = new Paint(Paint.ANTI_ALIAS_FLAG);
        mChartBrush.setStyle(Paint.Style.FILL);
        mChartBrush.setStrokeWidth(STROKE_WIDTH);
        mChartBrush.setStrokeCap(Paint.Cap.ROUND);
        mChartBrush.setStrokeJoin(Paint.Join.ROUND);
        mChartBrush.setAntiAlias(true);

        mSelectionBrush = new Paint();
        mSelectionBrush.setStyle(Paint.Style.FILL);
        mSelectionBrush.setStrokeCap(Paint.Cap.ROUND);
        mSelectionBrush.setStrokeWidth(STROKE_WIDTH + 1);
        mSelectionBrush.setColor(Color.GRAY);

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
        mPreviousClose = previousClose;
        mLatestPrice = latestPrice;
    }

    /**
     * Updates the latest price of the stock the chart describes.
     *
     * @param updatedStockPrice the updated price of the stock.
     */
    public void updateLatestPrice(double updatedStockPrice) {
        mLatestPrice = updatedStockPrice;
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
     * Returns the latest price of the stock modeled by the chart.
     *
     * @return latest price of stock.
     */
    public double getLatestPrice() {
        return mLatestPrice;
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
        mRange = range;
        switch (mRange) {
            case "1D":
                if (mOneDay == null) {
                    Util.fetchIntradayChartData(mContext, mSymbol, new JSONArrayCallback() {
                        @Override
                        public void onSuccess(JSONArray result) {
                            mOneDay = parseJSONResult(result);
                            mCurrentUtilizedData = mOneDay;
                            notifyChartChangeListeners();
                            invalidate();
                        }
                    });
                } else {
                    mCurrentUtilizedData = mOneDay;
                    notifyChartChangeListeners();
                    invalidate();
                }
                break;
            case "5D":
                if (mFiveDay == null) {
                    Util.fetchFiveDayChartData(mContext, mSymbol, new JSONArrayCallback() {
                        @Override
                        public void onSuccess(JSONArray result) {
                            mFiveDay = parseJSONResult(result);
                            mCurrentUtilizedData = mFiveDay;
                            notifyChartChangeListeners();
                            invalidate();
                        }
                    });
                } else {
                    mCurrentUtilizedData = mFiveDay;
                    notifyChartChangeListeners();
                    invalidate();
                }
                break;
            case "1M":
                if (mOneMonth == null) {
                    Util.fetchOneMonthChartData(mContext, mSymbol, new JSONArrayCallback() {
                        @Override
                        public void onSuccess(JSONArray result) {
                            mOneMonth = parseJSONResult(result);
                            mCurrentUtilizedData = mOneMonth;
                            notifyChartChangeListeners();
                            invalidate();
                        }
                    });
                } else {
                    mCurrentUtilizedData = mOneMonth;
                    notifyChartChangeListeners();
                    invalidate();
                }
                break;
            case "6M":
                if (mSixMonth == null) {
                    Util.fetchSixMonthChartData(mContext, mSymbol, new JSONArrayCallback() {
                        @Override
                        public void onSuccess(JSONArray result) {
                            mSixMonth = parseJSONResult(result);
                            mCurrentUtilizedData = mSixMonth;
                            notifyChartChangeListeners();
                            invalidate();
                        }
                    });
                } else {
                    mCurrentUtilizedData = mSixMonth;
                    notifyChartChangeListeners();
                    invalidate();
                }
                break;
            case "1Y":
                if (mOneYear == null) {
                    Util.fetchOneYearChartData(mContext, mSymbol, new JSONArrayCallback() {
                        @Override
                        public void onSuccess(JSONArray result) {
                            mOneYear = parseJSONResult(result);
                            mCurrentUtilizedData = mOneYear;
                            notifyChartChangeListeners();
                            invalidate();
                        }
                    });
                } else {
                    mCurrentUtilizedData = mOneYear;
                    notifyChartChangeListeners();
                    invalidate();
                }
                break;
            case "5Y":
                if (mFiveYear == null) {
                    Util.fetchFiveYearChartData(mContext, mSymbol, new JSONArrayCallback() {
                        @Override
                        public void onSuccess(JSONArray result) {
                            mFiveYear = parseJSONResult(result);
                            mCurrentUtilizedData = mFiveYear;
                            notifyChartChangeListeners();
                            invalidate();
                        }
                    });
                } else {
                    mCurrentUtilizedData = mFiveYear;
                    notifyChartChangeListeners();
                    invalidate();
                }
                break;
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
        double min = mRange.equals("1D") ? Math.min(mPreviousClose, mLatestPrice) : Double.MAX_VALUE;
        double max = mRange.equals("1D") ? Math.max(mPreviousClose, mLatestPrice) : Double.MIN_VALUE;
        String column = mRange.equals("1D") ? "label" : "date";

        for (int i = 0; i < result.length(); i++) {
            try {
                JSONObject jsonObject = result.getJSONObject(i);

                String label = jsonObject.getString(column);

                double close;
                if (jsonObject.isNull("close")) {
                    close = INVALID_ENTRY_FLAG;
                } else {
                    close = jsonObject.getDouble("close");
                    validEntries++;
                }

                if (close < min && close != INVALID_ENTRY_FLAG) {
                    min = close;
                }
                if (close > max && close != INVALID_ENTRY_FLAG) {
                    max = close;
                }

                // ex. x AM/PM --> 1:00 AM/PM;
                if (mRange.equals("1D") && !label.equals("null") && !label.contains(":")) {
                    label = label.substring(0, label.indexOf(' ')) + ":00 " + label.substring(label.indexOf(' ') + 1);
                }

                if (!mRange.equals("1D")) {
                    // yyyy-mm-dd -> m dd, yy
                    String[] splitDate = label.split("-");
                    String year = splitDate[0];
                    String day = splitDate[2];
                    if (day.charAt(0) == '0') {
                        day = day.substring(1);
                    }
                    int monthIndex = Integer.parseInt(splitDate[1]) - 1;
                    String month = new DateFormatSymbols().getMonths()[monthIndex].substring(0, 3);

                    label = month + " " + day + Util.getDayOfMonthSuffix(Integer.parseInt(day)) + ", " + year;
                }
                if (mRange.equals("5D")) {
                    String minute = jsonObject.getString("minute");
                    label += " at " + Util.convertMilitaryToStandard(minute);
                }

                parsedResult.add(new ChartDataFragment(close, label));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        int chartRangeColor;
        if (validEntries >= 2) {
            // initialized at zero, but will always be set in the if/else
            double first = 0;
            double last = 0;
            if (mRange.equals("1D")) {
                first = mPreviousClose;
                last = mLatestPrice;
            } else {
                int i = 0;
                boolean done = false;
                while (i < parsedResult.size() && !done) {
                    if (parsedResult.get(i).getPrice() != INVALID_ENTRY_FLAG) {
                        done = true;
                        first = parsedResult.get(i).getPrice();
                    }
                    i++;
                }

                i = parsedResult.size() - 1;
                done = false;
                while (i >= 0 && !done) {
                    if (parsedResult.get(i).getPrice() != INVALID_ENTRY_FLAG) {
                        done = true;
                        last = parsedResult.get(i).getPrice();
                    }
                    i--;
                }
            }
            chartRangeColor = last >= first ?  getResources().getColor(R.color.profit) : getResources().getColor(R.color.loss);
        } else {
            chartRangeColor = Color.GRAY;
        }
        return new ChartData(parsedResult, min, max, chartRangeColor, validEntries);
    }

    /**
     * Invokes all current listeners of this view of the changes to the chart range.
     */
    private void notifyChartChangeListeners() {
        if (mCurrentUtilizedData != null) {
            List<ChartDataFragment> chartData = mCurrentUtilizedData.getList();
            if (chartData != null && mCurrentUtilizedData.getValidEntriesRegistered() >= 1) {
                double start = (mRange.equals("1D") ? mPreviousClose : getFirst().getPrice());
                double end = mLatestPrice;

                int color = (end >= start) ? getResources().getColor(R.color.profit) : getResources().getColor(R.color.loss);
                mChartBrush.setColor(color);
                invokeChartChangeListeners(color, Util.getPercentChangeText(start, end, false));
            } else {
                int color;
                if (mRange.equals("1D")) {
                    color = (mLatestPrice >= mPreviousClose) ? getResources().getColor(R.color.profit) : getResources().getColor(R.color.loss);
                } else {
                    color = Color.GRAY;
                }
                invokeChartChangeListeners(color, Util.getPercentChangeText(mPreviousClose, mLatestPrice, false));
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
     * Handles the user's touch input on the chart. Allows user to select the chart element closest
     * to where the user made a selection on the chart.
     *
     * @param event Event for touch.
     * @return      True if the listener has consumed the event, false otherwise.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCurrentIndex == -1 && (mCurrentUtilizedData == null || mCurrentUtilizedData.size() <= 1)) {
            return false;
        }

        List<ChartDataFragment> chartData = mCurrentUtilizedData.getList();
        switch (mState) {
            case OFF:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mCurrentIndex = getPositionIndex(event);

                    getParent().requestDisallowInterceptTouchEvent(true);

                    if (mCurrentIndex != -1) {
                        this.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                        mState = State.ON;
                        double first = (mRange.equals("1D") ? mPreviousClose : getFirst().getPrice());
                        double last = chartData.get(mCurrentIndex).getPrice();

                        invokeUserSelectionListeners(getIntervalColor(first, last), Util.getPercentChangeText(first, last, false), last);
                        invalidate();
                        return true;
                    }
                }
            case ON:
                getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    int onDragSelectionIndex = getPositionIndex(event);
                    if (onDragSelectionIndex != -1) {
                        this.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                        mCurrentIndex = onDragSelectionIndex;
                    }

                    double first = (mRange.equals("1D") ? mPreviousClose : getFirst().getPrice());
                    double last = chartData.get(mCurrentIndex).getPrice();

                    invokeUserSelectionListeners(getIntervalColor(first, last), Util.getPercentChangeText(first, last, false), last);
                    invalidate();
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    mState = State.OFF;
                    mCurrentIndex = -1;

                    double first = (mRange.equals("1D") ? mPreviousClose : getFirst().getPrice());
                    double last = mLatestPrice;
                    invokeUserSelectionListeners(getIntervalColor(first, last), Util.getPercentChangeText(first, last, false), last);
                    invalidate();
                }
        }
        getParent().requestDisallowInterceptTouchEvent(false);
        return false;
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
     * Clears the vertical line with the label on top which represents a user selection.
     */
    public void clearUserSelection() {
        //getParent().requestDisallowInterceptTouchEvent(false);
        mCurrentIndex = -1;
        mState = State.OFF;
        invalidate();
    }

    /**
     * Finds and returns the index of the chart data element closest to the the point on the chart
     * that the user has selected. This is done by using the MotionEvent parameter which represents
     * a user pressing down or dragging finger on chart).
     * If there is no valid index, then returns -1.
     *
     * @param event Event for touch.
     * @return      the index of the valid chart data element selected, -1 otherwise.
     */
    public int getPositionIndex(MotionEvent event) {
        if (mCurrentUtilizedData != null) {
            List<ChartDataFragment> chartData = mCurrentUtilizedData.getList();
            int size = chartData.size();
            if (mRange.equals("1D")) {
                size = MINUTES_IN_TRADING_HOURS;
            }
            float width = getWidth();
            float increment = (width / size);

            float pointX = event.getX();

            int i = 0;
            boolean done = false;
            double positionA = 0.0;
            int indexA = 0;
            ChartDataFragment fragment;
            while (i < chartData.size() && !done) {
                fragment = chartData.get(i);
                if (fragment.getPrice() != INVALID_ENTRY_FLAG) {
                    positionA = increment * i;
                    indexA = i;
                    done = true;
                }
                i++;
            }

            done = false;
            while (i < chartData.size()) {
                fragment = chartData.get(i);
                if (fragment.getPrice() != INVALID_ENTRY_FLAG) {
                    double positionB = increment * i;
                    double distance = positionB - positionA;
                    if (pointX <= increment * (indexA) + (distance / 2)) {
                        return indexA;
                    } else if (pointX <= increment * (indexA) + (distance)) {
                        return i;
                    }
                    positionA = positionB;
                    indexA = i;
                }
                i++;
            }

            i = chartData.size() - 1;
            while (i >= 0) {
                fragment = chartData.get(i);
                if (fragment.getPrice() != INVALID_ENTRY_FLAG) {
                    return i;
                }
                i--;
            }
        }
        return -1;
    }

    /**
     * Finds and returns the first valid chart element of the chart currently on display.
     * If no valid entry is found, returns null.
     * First in this case is defined as the left most element.
     *
     * @return  ChartDataFragment of the first valid chart element.
     */
    public ChartDataFragment getFirst() {
        if (mCurrentUtilizedData != null) {
            List<ChartDataFragment> data = mCurrentUtilizedData.getList();
            if (data != null) {
                int i = 0;
                while (i < data.size()) {
                    ChartDataFragment fragment = data.get(i);
                    if (fragment.getPrice() != INVALID_ENTRY_FLAG) {
                        return fragment;
                    }
                    i++;
                }
            }
        }
        return null;
    }

    /**
     * Finds and returns the last valid chart element of the chart currently on display.
     * If no valid entry is found, returns null.
     * Last in this case is defined as the right most.
     *
     * @return  ChartDataFragment of the last valid chart element.
     */
    public ChartDataFragment getLast() {
        List<ChartDataFragment> data = mCurrentUtilizedData.getList();
        if (data != null) {
            int i = data.size() - 1;
            while (i >= 0) {
                ChartDataFragment fragment = data.get(i);
                if (fragment.getPrice() != INVALID_ENTRY_FLAG) {
                    return fragment;
                }
                i--;
            }
        }
        return null;
    }

    /**
     * Draw the Stock Chart on the Canvas
     * @param canvas the canvas that is drawn upon
     */
    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();

        if (mCurrentUtilizedData != null) {
            if (mCurrentUtilizedData.size() == 0) {
                // no chart data...
                String message = "No Chart Data Available.";
                mTextBrush.setTextSize(18 * getResources().getDisplayMetrics().density);
                float offset = mTextBrush.measureText(message);
                canvas.drawText(message, 0.5f * (width - offset), 0.5f * height, mTextBrush);
                mTextBrush.setTextSize(14 * getResources().getDisplayMetrics().density);
            } else if (mCurrentUtilizedData.getValidEntriesRegistered() >= 2) {
                List<ChartDataFragment> chartElements = mCurrentUtilizedData.getList();
                int size = chartElements.size();

                double min = mCurrentUtilizedData.getMin();
                double max = mCurrentUtilizedData.getMax();

                float delta = (float) (max - min);
                float chartHeight = height - height * 0.1f;

                float incrementY = chartHeight / delta;

                if (mRange.equals("1D")) {
                    // stroke width is slightly smaller to adjust for the increased number of charted elements.
                    mChartBrush.setStrokeWidth(2);
                    size = MINUTES_IN_TRADING_HOURS;

                    // drawing the dotted line that represents the previous close for intraday chart.
                    if (mPreviousClose < min) {
                        min = (float) mPreviousClose;
                        delta = (float) (max - min);
                    } else if (mPreviousClose > max) {
                        max = (float) mPreviousClose;
                        delta = (float) (max - min);
                    }
                    mPreviousCloseLine.reset();
                    incrementY = chartHeight / delta;
                    float v = (float) (height - (mPreviousClose - min) * incrementY) - 5;
                    mPreviousCloseLine.moveTo(0, v);
                    mPreviousCloseLine.lineTo(width, v);
                    canvas.drawPath(mPreviousCloseLine, mDottedBrush);
                } else {
                    mChartBrush.setStrokeWidth(STROKE_WIDTH);
                }
                float incrementX = width / (size - 1);

                mChartBrush.setColor(mCurrentUtilizedData.getChartRangeColor());

                int j = 0;
                boolean foundNonNull = false;
                ChartDataFragment fragment;
                float startX = 0, startY = 0;
                while (j < chartElements.size() && !foundNonNull) {
                    fragment = chartElements.get(j);
                    if (fragment.getPrice() != INVALID_ENTRY_FLAG) {
                        startX = incrementX * j;
                        startY = (float) (height - ((fragment.getPrice() - min) * incrementY)) - 5;
                        foundNonNull = true;
                    }
                    j++;
                }

                for (int i = 1; i < chartElements.size(); i++) {
                    fragment = chartElements.get(i);
                    if (fragment.getPrice() != INVALID_ENTRY_FLAG) {
                        float endX = incrementX * i;
                        float endY = (float) (height - ((fragment.getPrice() - min) * incrementY)) - 5;

                        if (mCurrentIndex != -1) {
                            if (i <= mCurrentIndex) {
                                canvas.drawLine(startX, startY, endX, endY, mChartBrush);
                            } else {
                                mChartBrush.setColor(ColorUtils.blendARGB(mChartBrush.getColor(), Color.BLACK, 0.7f));
                                canvas.drawLine(startX, startY, endX, endY, mChartBrush);
                                mChartBrush.setColor(mCurrentUtilizedData.getChartRangeColor());
                            }
                        } else {
                            canvas.drawLine(startX, startY, endX, endY, mChartBrush);
                        }

                        startX = endX;
                        startY = endY;
                    }
                }

                // handling the case if we are drawing the selection line & label fr
                // handling the case where we are also drawing a selection the user has made.
                // the selection is drawn by drawing a vertical line at the selected element (if
                // there is one) and a label on top describing the date of this recorded chart
                // element.
                if (mCurrentIndex != -1) {
                    float x = incrementX * mCurrentIndex;
                    if (x >= width) {
                        x = width - 1;
                    }
                    float textWidth = mTextBrush.measureText(chartElements.get(mCurrentIndex).getLabel());
                    textWidth = x - (textWidth / 2);
                    if (textWidth < 0) {
                        textWidth = 0;
                    }
                    if (x + (mTextBrush.measureText(chartElements.get(mCurrentIndex).getLabel()) / 2) >= width) {
                        textWidth = width - mTextBrush.measureText(chartElements.get(mCurrentIndex).getLabel());
                    }

                    double z = chartElements.get(mCurrentIndex).getPrice();
                    canvas.drawLine(x, height, x, (float) (height - ((max - min) * incrementY)), mSelectionBrush);
                    canvas.drawText(chartElements.get(mCurrentIndex).getLabel(), textWidth, (height * 0.1f) * 0.5f, mTextBrush);
                }
            } else {
                // not enough valid chart elements to chart.
                // this could either be cause the stock has very low trading volume (ex. some days
                // of BRK.A trading) or the markets have just opened up. in this case we would just
                // want to show a chart of the previous close.
                if (mRange.equals("1D")) {
                    float chartHeight = height - height * 0.1f;

                    mPreviousCloseLine.reset();
                    mPreviousCloseLine.moveTo(0, chartHeight * 0.6f);
                    mPreviousCloseLine.lineTo(width, chartHeight * 0.6f);
                    canvas.drawPath(mPreviousCloseLine, mDottedBrush);
                }
            }
        }
    }
}