package com.pocketprofit.source.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.NestedScrollView;
import androidx.gridlayout.widget.GridLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.pocketprofit.R;
import com.pocketprofit.source.JSONObjectCallback;
import com.pocketprofit.source.Util;
import com.pocketprofit.source.database.DatabaseHelper;
import com.pocketprofit.source.stockchart.AbstractStockChartView;
import com.pocketprofit.source.stockchart.StockChartView;
import com.robinhood.ticker.TickerUtils;
import com.robinhood.ticker.TickerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class StockInformationActivity extends AppCompatActivity implements View.OnClickListener, AbstractStockChartView.UserSelectionListener, AbstractStockChartView.ChartChangeListener {
    // the chart containing intraday-historical prices.
    private StockChartView mStockChartView;

    // the chart range currently selected.
    public TextView optionSelected;

    private DatabaseHelper mDatabase;

    private ViewFlipper mWatchListFlipper;

    private String mSymbol;

    private boolean mUSMarketOpen;
    private double mLatestPrice;
    private String mLatestTime;

    private InterstitialAd mInterstitialAd;

    private ShimmerFrameLayout mContainer;

    private Rect rect;  // rect to hold the bounds of the view.
    // placeholder view while the client fetches information from PocketProfit server.
    private ShimmerFrameLayout mChartShimmerLayout;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_information);

        // loading Ad.
        if (Util.DISPLAY_ADS) {
            AdView adView = (AdView) this.findViewById(R.id.ad_view);
            final AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);

            mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId(getResources().getString(R.string.stockchart_interstitial_ad));
            mInterstitialAd.loadAd(adRequest);
            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    mInterstitialAd.loadAd(adRequest);
                }
            });
        } else {
            this.findViewById(R.id.ad_container).setVisibility(View.GONE);
        }

        setupStockPriceText();

        final NestedScrollView scrollView = (NestedScrollView) this.findViewById(R.id.stock_info_scrollview);

        mSymbol = getIntent().getStringExtra(Util.EXTRA_SYMBOL);
        mDatabase = DatabaseHelper.getInstance(this);

        // toolbar header
        TextView symbolText = this.findViewById(R.id.symbol);
        symbolText.setText(mSymbol);

        // toolbar back button
        ImageView back = this.findViewById(R.id.back_icon);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mWatchListFlipper = this.findViewById(R.id.watchlist_flipper);
        mWatchListFlipper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleWatchlist();
            }
        });

        mContainer = (ShimmerFrameLayout) this.findViewById(R.id.stock_info_shimmer);

        mStockChartView = this.findViewById(R.id.stockChart);
        mStockChartView.addUserSelectionListener(this);
        mStockChartView.addChartChangeListener(this);
        mChartShimmerLayout = (ShimmerFrameLayout) this.findViewById(R.id.chart_shimmer);

        Util.fetchStockQuote(this, mSymbol, new JSONObjectCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    mUSMarketOpen = result.getBoolean("isUSMarketOpen");
                    mLatestTime = Util.normalizeDate(result.getString("latestTime"));

                    String companyName = result.getString("companyName");
                    setCompanyName(companyName);

                    mLatestPrice = Math.round(result.getDouble("latestPrice") * 100.0) / 100.0;
                    double previousClose = result.getDouble("previousClose");

                    // if the user currently owns the stock, its current price in the internal
                    // database is updated and position information is displayed on screen.
                    if (mDatabase.userOwns(mSymbol)) {
                        mWatchListFlipper.setVisibility(View.INVISIBLE);
                        mDatabase.updateCurrentPrice(mSymbol, mLatestPrice);
                        setCurrentStockPositionInfo();
                    }

                    updateStockPriceText(mLatestPrice);

                    mStockChartView.setValues(mSymbol, previousClose, mLatestPrice);
                    setUpChartRangeOptions();
                    setStartingRange();

                    int color = (mLatestPrice < previousClose) ? getResources().getColor(R.color.loss) : getResources().getColor(R.color.profit);

                    TextView changeText = (TextView) findViewById(R.id.change);
                    changeText.setTextColor(color);
                    changeText.setText(Util.getPercentChangeText(previousClose, result.getDouble("latestPrice"), false));

                    setCompanyStats(result);

                    togglePriceFluctuationArrow(mLatestPrice >= previousClose);

                    mContainer.stopShimmer();
                    mContainer.setVisibility(View.GONE);
                    findViewById(R.id.stock_price_container).setVisibility(View.VISIBLE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) this.findViewById(R.id.stock_refresh_view);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mContainer.setVisibility(View.VISIBLE);
                findViewById(R.id.stock_info_shimmer).setVisibility(View.GONE);
                mContainer.startShimmer();
                Util.fetchStockQuote(StockInformationActivity.this, mSymbol, new JSONObjectCallback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        try {
                            mUSMarketOpen = result.getBoolean("isUSMarketOpen");
                            mLatestPrice = Math.round(result.getDouble("latestPrice") * 100.0) / 100.0;

                            mLatestTime = Util.normalizeDate(result.getString("latestTime"));

                            double priceA = mLatestPrice;
                            try {
                                if (optionSelected != null) {
                                    if (optionSelected.getText().toString().equals("1D")) {
                                        priceA = result.getDouble("previousClose");
                                    } else {
                                        priceA = mStockChartView.getFirst().getPrice();
                                    }
                                }
                            } catch (Exception e) {
                                priceA = result.getDouble("previousClose");
                            }

                            updateStockPriceText(mLatestPrice);

                            mStockChartView.updateLatestPrice(mLatestPrice);

                            int color = (mLatestPrice < priceA) ? getResources().getColor(R.color.loss) : getResources().getColor(R.color.profit);

                            TextView changeText = (TextView) findViewById(R.id.change);
                            changeText.setTextColor(color);
                            changeText.setText(Util.getPercentChangeText(priceA, result.getDouble("latestPrice"), false));

                            if (mDatabase.userOwns(mSymbol)) {
                                mDatabase.updateCurrentPrice(mSymbol, mLatestPrice);
                                setCurrentStockPositionInfo();
                            }

                            togglePriceFluctuationArrow(mLatestPrice >= priceA);

                            findViewById(R.id.stock_info_shimmer).setVisibility(View.VISIBLE);
                            mContainer.stopShimmer();
                            mContainer.setVisibility(View.GONE);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                if (Util.DISPLAY_ADS) {
                    int randomNumber = new Random().nextInt(100) + 1;
                    if (randomNumber <= Util.AD_FREQUENCY) {
                        if (mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                        }
                    }
                }

                mStockChartView.clearUserSelection();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // information about company
        Util.fetchInfoAboutCompany(getBaseContext(), mSymbol, new JSONObjectCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                setInfoAboutCompany(result);
            }
        });

        // news tab
        setNewsPortalButton();
        findViewById(R.id.news_button).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int color = mStockChartView.getColor();
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                        view.getBackground().setColorFilter(ColorUtils.blendARGB(color, Color.BLACK, 0.25f), PorterDuff.Mode.SRC_ATOP);
                        scrollView.requestDisallowInterceptTouchEvent(true);
                        return true;
                    case MotionEvent.ACTION_UP:
                        view.playSoundEffect(SoundEffectConstants.CLICK);
                        view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                        scrollView.requestDisallowInterceptTouchEvent(false);
                        if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {
                            openCompanyNewsActivity();
                            return true;
                        }
                }
                return false;
            }
        });

        // transaction button setup
        findViewById(R.id.buy).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                        view.getBackground().setColorFilter(ColorUtils.blendARGB(getResources().getColor(R.color.profit), Color.BLACK, 0.25f), PorterDuff.Mode.SRC_ATOP);
                        scrollView.requestDisallowInterceptTouchEvent(true);
                        return true;
                    case MotionEvent.ACTION_UP:
                        view.getBackground().setColorFilter(getResources().getColor(R.color.profit), PorterDuff.Mode.SRC_ATOP);
                        scrollView.requestDisallowInterceptTouchEvent(false);
                        if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {
                            view.playSoundEffect(SoundEffectConstants.CLICK);
                            openStockTransactionActivity(true);
                            return true;
                        }
                }
                return false;
            }
        });
        findViewById(R.id.sell).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                        view.getBackground().setColorFilter(ColorUtils.blendARGB(getResources().getColor(R.color.loss), Color.BLACK, 0.25f), PorterDuff.Mode.SRC_ATOP);
                        scrollView.requestDisallowInterceptTouchEvent(true);
                        return true;
                    case MotionEvent.ACTION_UP:
                        view.getBackground().setColorFilter(getResources().getColor(R.color.loss), PorterDuff.Mode.SRC_ATOP);
                        scrollView.requestDisallowInterceptTouchEvent(false);
                        if (rect.contains(view.getLeft() + (int) motionEvent.getX(), view.getTop() + (int) motionEvent.getY())) {
                            view.playSoundEffect(SoundEffectConstants.CLICK);
                            openStockTransactionActivity(false);
                            return true;
                        }
                }
                return false;
            }
        });

    }

    /**
     * Sets and displays statistics about the security displayed in this activity.
     *
     * @param result the JSON response from an API call.
     */
    public void setCompanyStats(JSONObject result) {
        try {
            String yearHigh = result.isNull("week52High") ? "-" : Util.formatPriceText(result.getDouble("week52High"), false, true);
            String yearLow = result.isNull("week52Low") ? "-" : Util.formatPriceText( result.getDouble("week52Low"), false, true);
            String open = result.isNull("open") ? "-" : Util.formatPriceText(result.getDouble("open"), false, true);
            String high = result.isNull("high") ? "-" : Util.formatPriceText(result.getDouble("high"), false, true);
            String low = result.isNull("low") ? "-" : Util.formatPriceText(result.getDouble("low"), false, true);
            String volume = result.isNull("volume") ? "-" : Util.formatVolume((long) (result.getDouble("volume")));
            String averageVolume = result.isNull("avgTotalVolume") ? "-" : Util.formatVolume((long) (result.getDouble("avgTotalVolume")));
            String marketCap = result.isNull("marketCap") ? "-" : Util.formatMarketCap(result.getDouble("marketCap"));
            String ytdChange = result.isNull("ytdChange") ? "-" : Util.formatPercentageText(result.getDouble("ytdChange"));
            String peRatio = result.isNull("peRatio") ? "-" :
                    Util.formatPriceText(result.getDouble("peRatio"), false, true);

            GridLayout statsLayout = (GridLayout) findViewById(R.id.stats_grid);

            inflateText("Open:", open, statsLayout, R.layout.text_entry_grid, true);
            inflateText("Volume:", volume, statsLayout, R.layout.text_entry_grid, false);
            inflateText("Low:", low, statsLayout, R.layout.text_entry_grid, true);
            inflateText("Avg. Volume:", averageVolume, statsLayout, R.layout.text_entry_grid, false);
            inflateText("High:", high, statsLayout, R.layout.text_entry_grid, true);
            inflateText("P/E Ratio:", peRatio, statsLayout, R.layout.text_entry_grid, false);
            inflateText("52-wk Low:", yearLow, statsLayout, R.layout.text_entry_grid, true);
            inflateText("Market Cap:", marketCap, statsLayout, R.layout.text_entry_grid, false);
            inflateText("52-wk High:", yearHigh, statsLayout, R.layout.text_entry_grid, true);
            inflateText("YTD Change:", ytdChange, statsLayout, R.layout.text_entry_grid, false);

            findViewById(R.id.stats_header).setVisibility(View.VISIBLE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays information about the users position regarding the current security displayed on the
     * StockInformationActivity.
     * This method should only be called if the user has at least one share of the security.
     */
    public void setCurrentStockPositionInfo() {
        GridLayout gridLayout = this.findViewById(R.id.position_grid);
        LinearLayout layout = this.findViewById(R.id.position_container);
        if (gridLayout.getChildCount() > 0) {
            gridLayout.removeAllViews();
        }
        if (layout.getChildCount() > 0) {
            layout.removeAllViews();
        }
        int totalShares = mDatabase.getShareCount(mSymbol);
        double averageCost = mDatabase.getAverageCost(mSymbol);

        double totalCost = mDatabase.getStockCost(mSymbol);
        double totalEquity = mDatabase.getStockEquity(mSymbol);

        inflateText("Shares Owned:", Util.formatShareCountText(totalShares), gridLayout, R.layout.position_text_entry_grid, true);
        inflateText("Average Cost:", Util.formatPriceText(averageCost, true, true), gridLayout, R.layout.position_text_entry_grid, false);
        inflateText("Equity:", Util.formatPriceText(totalEquity, true, true), gridLayout, R.layout.position_text_entry_grid, true);
        inflateText("Portfolio Diversity:", Util.formatPercentageText(100.0 * (totalEquity / Util.getPortfolioValue(this))), gridLayout, R.layout.position_text_entry_grid, false);

        inflateText("Total Return:", Util.getPercentChangeText(totalCost, totalEquity, true), layout);

        findViewById(R.id.position_header).setVisibility(View.VISIBLE);
    }

    /**
     * Displays a popup notifying the user that the market is closed and that no trades can
     * currently be executed. It will also give the user the option to view the next trading hours
     * by visiting a website.
     * This popup should be launched whenever the user selects either the buy or sell button
     * while the market is closed.
     */
    public void marketClosedPopup() {
        final Dialog marketClosed = new Dialog(this);
        marketClosed.setContentView(R.layout.market_closed_popup);
        marketClosed.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Button marketHoursButton = (Button) marketClosed.findViewById(R.id.market_hours);
        marketHoursButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Util.MARKET_HOURS_URL)));
            }
        });

        ImageView cancelButton = (ImageView) marketClosed.findViewById(R.id.close_popup);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                marketClosed.dismiss();
            }
        });

        marketClosed.show();
    }

    /**
     * Opens the stock transaction page if it is currently trading hours and if the price of the
     * security has loaded (!= 0.0).
     *
     * @param isBuy type of market order selected, true if its going to be a buy order, false if
     *              its going to be a sell order.
     */
    private void openStockTransactionActivity(boolean isBuy) {
        if (!mUSMarketOpen) {
            marketClosedPopup();
        } else if (mLatestPrice != 0.00) {
            Intent intent = new Intent(this, StockTransactionActivity.class);
            TextView companyName = (TextView) this.findViewById(R.id.name);

            intent.putExtra(Util.EXTRA_SYMBOL, mSymbol);
            intent.putExtra(Util.EXTRA_NAME, companyName.getText().toString());
            intent.putExtra(Util.EXTRA_STOCK_PRICE, mLatestPrice);
            intent.putExtra(Util.EXTRA_IS_BUY_TRANSACTION, isBuy);
            intent.putExtra(Util.EXTRA_DATE, mLatestTime);
            intent.putExtra(Util.EXTRA_PREVIOUS_CLOSE, mStockChartView.getPreviousClose());
            startActivity(intent);
        }
    }

    /**
     * Launches the Company News Portal page, which will contain recent articles regarding the
     * name of the security in the StockInformationActivty.
     */
    private void openCompanyNewsActivity() {
        Intent intent = new Intent(this, CompanyNewsActivity.class);
        intent.putExtra(Util.EXTRA_SYMBOL, mSymbol);
        startActivity(intent);
    }

    /**
     * Launches the TransactionHistoryActivity which will display all the transactions the
     * user has made regarding the stock currently being viewed.
     */
    private void openTransactionHistoryActivity() {
        Intent intent = new Intent(this, TransactionHistoryActivity.class);
        intent.putExtra(Util.EXTRA_SYMBOL, mSymbol);
        startActivity(intent);
    }

    /**
     * Displays either an up or down arrow next to the price stock price fluctuation text based
     * on the boolean value provided as a terminal.
     *
     * @param arrowUp   boolean to determine if the up arrow will be displayed.
     */
    private void togglePriceFluctuationArrow(boolean arrowUp) {
        ImageView arrow = this.findViewById(R.id.arrow);
        if (arrowUp) {
            arrow.setImageResource(R.drawable.ic_up_arrow);
        } else {
            arrow.setImageResource(R.drawable.ic_down_arrow);
        }
    }

    /**
     * Sets the text of the news portal button based upon the name of the security in the
     * StockInformationActivity.
     */
    public void setNewsPortalButton() {
        Button news = (Button) this.findViewById(R.id.news_button);
        news.setText(mSymbol + " News Portal");
    }

    /**
     * Sets and displays the name of the company using the security name passing in as a parameter.
     *
     * @param name the name of the company.
     */
    public void setCompanyName(String name) {
        TextView companyName = (TextView) this.findViewById(R.id.name);
        companyName.setText(name);
    }

    /**
     * Sets and displays a paragraph discribing the company using the description passed in as a
     * parameter.
     *
     * @param description a paragraph describing the company.
     */
    public void setCompanyDescription(String description) {
        TextView companyDescription = (TextView) this.findViewById(R.id.company_description);
        companyDescription.setText(description);
    }

    /**
     * Sets and displays information about the company using the JSON response object passed in as
     * a parameter.
     *
     * @param result the json response from an API call.
     */
    public void setInfoAboutCompany(JSONObject result) {
        try {
            String exchange = result.isNull("exchange") ? "-" : result.getString("exchange");
            String industry = result.getString("industry").equals("")? "-" : result.getString("industry");
            if (!result.isNull("description")
                    && !result.getString("description").equals("")
                    && !result.getString("description").equals("0")) {
                setCompanyDescription(result.getString("description"));
                findViewById(R.id.company_description_header).setVisibility(View.VISIBLE);
            }

            String ceo = result.getString("CEO");
            if (ceo.equals("")) {
                ceo = "-";
            }
            String sector = result.getString("sector").equals("") ? "-" : result.getString("sector");
            String employees = result.isNull("employees") ? "-" :
                    Util.formatShareCountText(result.getInt("employees"));

            String city = result.isNull("city") ? "-" : result.getString("city");
            String headquarters = city;

            String state = result.isNull("state") ? "-" : result.getString("state");
            if (!state.equals("-")) {
                headquarters += ", " + state;
            }
            String country = result.isNull("country") ? "-" : result.getString("country");
            if (!country.equals("-")) {
                headquarters += " (" + country + ")";
            }

            LinearLayout aboutCompanyGrid = (LinearLayout) findViewById(R.id.about_grid);

            inflateText("CEO:", ceo, aboutCompanyGrid);
            inflateText("Exchange:", exchange, aboutCompanyGrid);
            inflateText("Sector:", sector, aboutCompanyGrid);
            inflateText("Industry:", industry, aboutCompanyGrid);
            inflateText("Employees:", employees, aboutCompanyGrid);
            inflateText("Headquarters:", headquarters, aboutCompanyGrid);
            findViewById(R.id.about_header).setVisibility(View.VISIBLE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inflates and sets the text of a 'text_entry_grid' layout based upon the information provided
     * as a parameter into the given LinearLayout.
     * @param label the label of the text to be inflated.
     * @param value the content of the text to be inflated.
     * @param layout the layout to add the inflated view.
     */
    private void inflateText(String label, String value, LinearLayout layout) {
        View inflatedLayout = getLayoutInflater().inflate(R.layout.text_entry, layout, false);
        TextView textLabel = (TextView) inflatedLayout.findViewById(R.id.text_type);
        TextView textValue = (TextView) inflatedLayout.findViewById(R.id.text);

        textLabel.setText(label);
        textValue.setText(value);
        layout.addView(inflatedLayout);
    }

    /**
     * Inflates and sets the text of a 'text_entry_grid' layout based upon the information provided
     * as a parameter into the given GridLayout.
     *
     * @param label         the label of the text to be inflated
     * @param value         the content of the text to be inflated.
     * @param layout        the layout to add the inflated view.
     * @param leftHandSide  boolean indicating if inflated text layout will be on left hand side,
     *                      this is used to ensure correct margins are applied.
     */
    private void inflateText(String label, String value, GridLayout layout, int textLayoutID, boolean leftHandSide) {
        View inflatedLayout = getLayoutInflater().inflate(textLayoutID, layout, false);
        TextView textLabel = (TextView) inflatedLayout.findViewById(R.id.text_type);
        TextView textValue = (TextView) inflatedLayout.findViewById(R.id.text);

        textLabel.setText(label);
        textValue.setText(value);

        GridLayout.LayoutParams layoutParams = (GridLayout.LayoutParams) inflatedLayout.getLayoutParams();
        int marginSide = (int) getResources().getDimension(R.dimen.vMargin);
        int marginBottom = (int) getResources().getDimension(R.dimen.margin_bottom);
        if (leftHandSide) {
            layoutParams.setMargins(marginSide, 0, marginSide / 2, marginBottom);
        } else {
            layoutParams.setMargins(marginSide / 2, 0, marginSide, marginBottom);
        }
        inflatedLayout.setLayoutParams(layoutParams);

        if (label.equals("Portfolio Diversity:")) {
            ProgressBar progressBar = inflatedLayout.findViewById(R.id.portfolio_diversity_bar);
            progressBar.setVisibility(View.VISIBLE);
            int percentage = (int) Math.round(100.0 * (mDatabase.getStockEquity(mSymbol) / Util.getPortfolioValue(this)));
            progressBar.setProgress(percentage);

            textValue.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            LinearLayout.LayoutParams progressBarLayoutParams = new LinearLayout.LayoutParams(textValue.getMeasuredHeight(), textValue.getMeasuredHeight());
            progressBar.setLayoutParams(progressBarLayoutParams);
        }
        layout.addView(inflatedLayout);
    }

    /**
     * Initializes the chart range options to be responsive to user selection.
     */
    public void setUpChartRangeOptions() {
        TextView oneDayOption = this.findViewById(R.id.oneDay);
        TextView fiveDayOption = this.findViewById(R.id.fiveDay);
        TextView oneMonthOption = this.findViewById(R.id.oneMonth);
        TextView sixMonthOption = this.findViewById(R.id.sixMonth);
        TextView oneYearOption = this.findViewById(R.id.oneYear);
        TextView fiveYearOption = this.findViewById(R.id.fiveYear);

        oneDayOption.setOnClickListener(this);
        fiveDayOption.setOnClickListener(this);
        oneMonthOption.setOnClickListener(this);
        sixMonthOption.setOnClickListener(this);
        oneYearOption.setOnClickListener(this);
        fiveYearOption.setOnClickListener(this);
    }

    /**
     * This method is called whenever the user taps on the watchlist icon, to add/remove a stock
     * from the watchlist.
     * The watchlist icon adjusts accordingly and a toast message is displayed to the user to
     * notify them that a stock has been successfully added/removed from watchlist.
     */
    public void toggleWatchlist() {
        if (!Util.currentlyOnWatchlist(this, mSymbol)) {
            // add from watchlist
            mWatchListFlipper.setInAnimation(this, R.anim.slide_in_right);
            mWatchListFlipper.setOutAnimation(this, R.anim.slide_out_left);
            mWatchListFlipper.setDisplayedChild(1);
        } else {
            // remove from watchlist
            mWatchListFlipper.setInAnimation(this, android.R.anim.slide_in_left);
            mWatchListFlipper.setOutAnimation(this, android.R.anim.slide_out_right);
            mWatchListFlipper.setDisplayedChild(0);
        }
        Util.updateWatchList(this, mSymbol);
    }

    /**
     * Sets the watchlist icon according to if the user has this security on his/her watchlist
     * or not.
     */
    public void setWatchlistIcon() {
        if (!mDatabase.userOwns(mSymbol)) {
            mWatchListFlipper.setVisibility(View.VISIBLE);
            if (Util.currentlyOnWatchlist(this, mSymbol)) {
                mWatchListFlipper.setDisplayedChild(1);
            } else {
                mWatchListFlipper.setDisplayedChild(0);
            }
        } else {
            mWatchListFlipper.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Sets up the order history link that allows the user to view their order history of the stock
     * currently being viewed if the stock symbol is in the user's transaction history.
     */
    public void setStockTransactionHistory() {
        TextView orderHistory = this.findViewById(R.id.order_history_link);
        if (mDatabase.inStockTransactionHistory(mSymbol) && (orderHistory.getVisibility() == View.GONE)) {
            orderHistory.setVisibility(View.VISIBLE);
            orderHistory.setText("View " + mSymbol + " Order History");
            orderHistory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openTransactionHistoryActivity();
                }
            });
        }
    }

    /**
     * Sets the initial range of the chart and fetches relevant information to display on the
     * chart view.
     */
    public void setStartingRange() {
        int id = this.getResources().getIdentifier(AbstractStockChartView.DEFAULT_RANGE,
                "id", this.getBaseContext().getPackageName());
        optionSelected = this.findViewById(id);
        onRangeOptionSelected();
    }

    /**
     * Notifies the stock chart that a different chart range has been selected, allowing it to then
     * fetch relevant information and update the chart.
     */
    public void onRangeOptionSelected() {
        optionSelected.setTextColor(Color.BLACK);
        mStockChartView.setChartRange(optionSelected.getText().toString());
    }

    @Override
    public void onClick(View v) {
        if (v != optionSelected) {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            optionSelected.getBackground().setColorFilter(getResources().getColor(R.color.background), PorterDuff.Mode.SRC_ATOP);
            optionSelected.setTextColor(getResources().getColor(R.color.gray));
            optionSelected = (TextView) v;
            onRangeOptionSelected();
        }
    }

    @Override
    public void onUserSelection(int color, String chartChange, double price) {
        updateStockPriceText(price);
        togglePriceFluctuationArrow(chartChange.startsWith("+"));

        TextView changeText = (TextView) this.findViewById(R.id.change);
        changeText.setTextColor(color);
        changeText.setText(chartChange);
    }

    /**
     * Sets up the stock price animations.
     */
    public void setupStockPriceText() {
        // specifying the animation style of the stock price text.
        TickerView tickerView = findViewById(R.id.price);
        tickerView.setCharacterLists(TickerUtils.provideNumberList());
        Typeface typeface = ResourcesCompat.getFont(this, R.font.nunito);
        tickerView.setTypeface(typeface);
        //tickerView.setAnimationDuration(250);
        tickerView.setAnimationInterpolator(new OvershootInterpolator());
        tickerView.setPreferredScrollingDirection(TickerView.ScrollingDirection.ANY);
    }

    /**
     * Sets the stock price to the value provided as a paramter.
     *
     * @param price the new price of the stock.
     */
    private void updateStockPriceText(double price) {
        TickerView tickerView = findViewById(R.id.price);
        tickerView.setText(Util.formatPriceText(price, true, true), true);
    }

    @Override
    public void onChartChange(int newColor, String chartChange) {
        if (mChartShimmerLayout.isShimmerStarted()) {
            mChartShimmerLayout.setVisibility(View.GONE);
            this.findViewById(R.id.stockChart).setVisibility(View.VISIBLE);
        }

        togglePriceFluctuationArrow(chartChange.startsWith("+"));

        optionSelected.setTextColor(Color.BLACK);
        optionSelected.getBackground().setColorFilter(newColor, PorterDuff.Mode.SRC_ATOP);

        this.findViewById(R.id.news_button).getBackground().setColorFilter(newColor, PorterDuff.Mode.SRC_ATOP);

        TextView changeText = (TextView) this.findViewById(R.id.change);
        changeText.setTextColor(newColor);
        changeText.setText(chartChange);
    }

    @Override
    public void onResume() {
        super.onResume();
        setWatchlistIcon();
        setStockTransactionHistory();
        if (mDatabase.userOwns(mSymbol)) {
            setCurrentStockPositionInfo();
        } else {
            LinearLayout layout = findViewById(R.id.position_container);
            layout.removeAllViews();
            GridLayout gridLayout = findViewById(R.id.position_grid);
            gridLayout.removeAllViews();
            findViewById(R.id.position_header).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // unregister the listeners we created.
        mStockChartView.removeUserSelectionListener(this);
        mStockChartView.removeChartChangeListener(this);
        mDatabase.close();
    }

}