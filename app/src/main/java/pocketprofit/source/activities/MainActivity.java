package com.pocketprofit.source.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.pocketprofit.R;
import com.pocketprofit.source.JSONObjectCallback;
import com.pocketprofit.source.Util;
import com.pocketprofit.source.adapters.StockAdapter;
import com.pocketprofit.source.database.DatabaseHelper;
import com.pocketprofit.source.entries.StockEntry;
import com.robinhood.ticker.TickerUtils;
import com.robinhood.ticker.TickerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {
    private StockAdapter mStocksOwnedAdapter;
    private StockAdapter mWatchlistAdapter;

    private List<StockEntry> mStocksOwned;
    private List<StockEntry> mWatchlistStocks;

    private DatabaseHelper mDatabase;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Util.DISPLAY_ADS) {
            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                }
            });
            AdView adView = (AdView) this.findViewById(R.id.ad_view);
            final AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);

            mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId(getResources().getString(R.string.homepage_interstitial_ad));
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

        setupPortfolioValueText();
        mDatabase = DatabaseHelper.getInstance(this);
        Util.setDateJoined(this);
        setPortfolioText();
        buildRecyclerViews();
        setUpSwipeRefresh();
        setUpButtons();
    }

    /**
     * Sets up the swipe refresh layout that will allow the user to swipe down on the screen to
     * update the latest price of shares for both the stocks that the user currently owns and the
     * stocks that are currently on the watchlist.
     * For every swipe refresh, there will be a chance that an interstitial ad event will execute.
     */
    public void setUpSwipeRefresh() {
        mSwipeRefreshLayout = (SwipeRefreshLayout) this.findViewById(R.id.refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                recalibrateStockData();
                if (Util.DISPLAY_ADS) {
                    int randomNumber = new Random().nextInt(100) + 1;
                    if (randomNumber <= Util.AD_FREQUENCY) {
                        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                        }
                    }
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    /**
     * Sets up the recycler views that will be used to hold information about both the stocks that
     * the user currently owns and the stocks currently on the watchlist.
     */
    public void buildRecyclerViews() {
        RecyclerView stockRecyclerView = this.findViewById(R.id.stock_recycler_view);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mStocksOwned = new ArrayList<>();
        mStocksOwnedAdapter = new StockAdapter(this, mStocksOwned);
        stockRecyclerView.setAdapter(mStocksOwnedAdapter);
        stockRecyclerView.setNestedScrollingEnabled(false);

        mStocksOwnedAdapter.setOnItemClickListener(new StockAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(getApplicationContext(), StockInformationActivity.class);
                intent.putExtra(Util.EXTRA_SYMBOL, mStocksOwnedAdapter.getSymbol(position));
                startActivity(intent);
            }
        });

        final RecyclerView watchlistRecyclerView = this.findViewById(R.id.watchlist_recycler_view);
        watchlistRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mWatchlistStocks = new ArrayList<>();
        mWatchlistAdapter = new StockAdapter(this, mWatchlistStocks);
        watchlistRecyclerView.setAdapter(mWatchlistAdapter);
        watchlistRecyclerView.setNestedScrollingEnabled(false);
        mWatchlistAdapter.setOnItemClickListener(new StockAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent (getApplicationContext(), StockInformationActivity.class);
                intent.putExtra(Util.EXTRA_SYMBOL, mWatchlistAdapter.getSymbol(position));
                startActivity(intent);
            }
        });
    }

    /**
     * Sets up both the menu floating action button and the search floating action button.
     */
    public void setUpButtons() {
        final FloatingActionButton search = this.findViewById(R.id.search_button);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSearchActivity();
            }
        });

        final FloatingActionsMenu menu = (FloatingActionsMenu) this.findViewById(R.id.menu);
        menu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                mSwipeRefreshLayout.setAlpha(Util.OPACITY);
                search.setEnabled(false);
            }

            @Override
            public void onMenuCollapsed() {
                mSwipeRefreshLayout.setAlpha(1f);
                search.setEnabled(true);
            }
        });

        this.findViewById(R.id.menu_background).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (menu.isExpanded()) {
                    menu.collapse();
                    return true;
                }
                return false;
            }
        });
        this.findViewById(R.id.account_statement).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.collapse();
                Intent intent = new Intent(getBaseContext(), AccountStatementActivity.class);
                startActivity(intent);
            }
        });
        this.findViewById(R.id.transaction_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.collapse();
                Intent intent = new Intent(getBaseContext(), TransactionHistoryActivity.class);
                startActivity(intent);
            }
        });
        this.findViewById(R.id.top_movers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.collapse();
                Intent intent = new Intent(getBaseContext(), TopMoversActivity.class);
                startActivity(intent);
            }
        });
        this.findViewById(R.id.sectors).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.collapse();
                Intent intent = new Intent(getBaseContext(), SectorListActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Uses the parameter 'count' to make parameter 'v' either visible or invisible on the screen.
     *
     * @param count the number that is used to determine the visibility of view 'v'.
     * @param v     the view whose visibility will be set.
     */
    private void toggleViewVisibility(int count, View v) {
        if (count == 0) {
            v.setVisibility(View.GONE);
        } else {
            v.setVisibility(View.VISIBLE);
        }
    }

    /**
     * This method is called whenever the user swipes to refresh the screen or returns to the main
     * activity from a different activity.
     * Resets information regarding stocks currently in possession and on the watchlist with the
     * most recent price data.
     */
    private void recalibrateStockData() {
        LinearLayout stockShimmer = this.findViewById(R.id.shimmer_stock);
        LinearLayout watchlistShimmer = (LinearLayout) this.findViewById(R.id.shimmer_watchlist);
        // don't refresh the stock data is a previous refresh is still in progress.
        if ((stockShimmer.getChildCount() == 0) && (watchlistShimmer.getChildCount() == 0)) {
            loadPortfolioStockData();
            loadWatchlistStockData();
        }
    }

    /**
     * Opens up the Search Activity.
     */
    public void openSearchActivity() {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
    }

    /**
     * Sets the text describing the current portfolio value as well as the text describing how much
     * the portfolios value has changed today (most recent trading day).
     */
    public void setPortfolioText() {
        double currentPortfolioValue = Util.getPortfolioValue(this);
        setPortfolioValue(currentPortfolioValue);

        TextView portfolioChangeText = (TextView) this.findViewById(R.id.todays_change);
        int color;
        double startingValue;
        if (currentPortfolioValue == Util.STARTING_VALUE) {
            color = this.getResources().getColor(R.color.gray);
            startingValue = currentPortfolioValue;
        } else {
            double delta = mDatabase.getTodaysStockSaleProfit(Util.getTodaysDate());
            startingValue = currentPortfolioValue - delta;
            color = (delta >= 0) ? getResources().getColor(R.color.profit) : getResources().getColor(R.color.loss);
        }
        portfolioChangeText.setTextColor(color);
        portfolioChangeText.setText(Util.getPercentChangeText(startingValue, currentPortfolioValue, false));
    }

    /**
     * Uses the parameter which represents how much the users total stock equity value has
     * fluctuated in the most recent trading day (including today) to update the portfolio text
     * and the daily portfolio change text.
     *
     * @param dailyChangeValue represents how much the users portfolio value has changed in the most
     *                         recent trading session.
     */
    public void dailyPortfolioChangeUpdate(double dailyChangeValue) {
        double portfolioValue = Util.getPortfolioValue(this);
        setPortfolioValue(portfolioValue);

        dailyChangeValue += mDatabase.getTodaysStockSaleProfit();

        TextView todaysChange = (TextView) this.findViewById(R.id.todays_change);
        int color = getResources().getColor(R.color.gray);
        if (dailyChangeValue > 0) {
            color = this.getResources().getColor(R.color.profit);
        } else if (dailyChangeValue < 0) {
            color = this.getResources().getColor(R.color.loss);
        }
        todaysChange.setText(Util.getPercentChangeText(portfolioValue - dailyChangeValue, portfolioValue, false));
        todaysChange.setTextColor(color);
    }

    /**
     * Fetches stock information from an external API for each stock that is currently owned by the
     * user. Once this stock data is retrieved, it is processed and displayed on the user's screen.
     */
    private void loadWatchlistStockData() {
        if (mWatchlistStocks.size() > 0) {
            mWatchlistStocks.clear();
            mWatchlistAdapter.notifyDataSetChanged();
        }
        final List<String> symbols = new ArrayList<>(Util.getWatchlist(this));
        toggleViewVisibility(symbols.size(), findViewById(R.id.watchlist_header));

        final ShimmerFrameLayout watchlistShimmerLayout = this.findViewById(R.id.watchlist_placeholder);
        final LinearLayout layout = (LinearLayout) this.findViewById(R.id.shimmer_watchlist);
        watchlistShimmerLayout.setVisibility(View.VISIBLE);
        watchlistShimmerLayout.startShimmer();
        for (int i = layout.getChildCount(); i < symbols.size(); i++) {
            insertPlaceholder(layout);
        }

        stockDataRequest(0, symbols, layout, watchlistShimmerLayout, false, 0.0);
    }

    /**
     * Fetches stock information from an external API for each stock that is currently on the user's
     * watchlist. Once this stock data is retrieved, it is processed and displayed on the
     * user's screen.
     */
    private void loadPortfolioStockData() {
        if (mStocksOwned.size() > 0) {
            mStocksOwned.clear();
            mStocksOwnedAdapter.notifyDataSetChanged();
        }
        final List<String> symbols = mDatabase.getAllStockSymbols();
        if (symbols.size() == 0) {
            setPortfolioText();
        }
        toggleViewVisibility(symbols.size(), findViewById(R.id.stock_header));


        final ShimmerFrameLayout stocksShimmerLayout = this.findViewById(R.id.stocks_owned_placeholder);
        final LinearLayout layout = (LinearLayout) this.findViewById(R.id.shimmer_stock);
        stocksShimmerLayout.setVisibility(View.VISIBLE);
        stocksShimmerLayout.startShimmer();
        for (int i = layout.getChildCount(); i < symbols.size(); i++) {
            insertPlaceholder(layout);
        }

        stockDataRequest(0, symbols, layout, stocksShimmerLayout, true, 0.0);
    }

    /**
     * Fetches stock data of the i'th element of the given list (0 based index) and adds a view
     * onto the users screen as well as removing a placeholder view.
     * The boolean isPortfolioStock tells us if the stock we are going to process is in the user's
     * portfolio or watchlist. It uses this boolean to determine which block of code to execute
     * and how to format the view that will be added to the user's screen.
     * The parameter 'totalChange' is used if the stock processed is in the user's portfolio, it
     * will store how much the stock has gained/lost in value in the most recent trading day.
     *
     * @param position              the i'th index element of the list given for which we will fetch
     *                              the stock data.
     * @param list                  the list of stock symbols to get data for.
     * @param placeholderContainer  the layout that holds the placeholder views.
     * @param shimmerLayout         the shimmer layout that will serve as a placeholder until all
     *                              of the symbols in the list are processed.
     * @param isPortfolioStock      boolean value which tells us if the stock processed is a stock
     *                              in the users portfolio.
     * @param totalChange           parameter that will be used to store the daily price fluctuation
     *                              of all the stocks in the user's portfolio as they are
     *                              processed recursively.
     */
    private void stockDataRequest(final int position, final List<String> list,
                                      final LinearLayout placeholderContainer,
                                      final ShimmerFrameLayout shimmerLayout,
                                      final boolean isPortfolioStock, final double totalChange) {
        if (position < list.size()) {
            final String symbol = list.get(position);
            Util.fetchStockQuote(this, symbol, new JSONObjectCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        double latestPrice = Math.round(result.getDouble("latestPrice") * 100.0) / 100.0;
                        double change = result.getDouble("change");
                        int color = (change >= 0) ? MainActivity.this.getResources().getColor(R.color.profit) :
                                MainActivity.this.getResources().getColor(R.color.loss);

                        // todaysStockProfit refers to the total net gain/loss of the stocks that
                        // the user currently owns in his/her portfolio. this value is only changed
                        // if the parameter 'isStockOwned' is true.
                        double todaysStockProfit = totalChange;
                        if (isPortfolioStock) {
                            int sharesOwned = mDatabase.getShareCount(symbol);
                            String formattedShares = Util.formatShareCountText(sharesOwned);

                            double previousClose = result.getDouble("previousClose");

                            mDatabase.updateCurrentPrice(symbol, latestPrice);

                            Stack<Double> stocksPurchasedToday = mDatabase.getSharesBoughtToday(symbol, Util.getTodaysDate());
                            for (double purchasePrice : stocksPurchasedToday) {
                                todaysStockProfit += (latestPrice - purchasePrice);
                            }
                            int sharesOwnedPriorToToday = mDatabase.getShareCount(symbol) - stocksPurchasedToday.size();
                            todaysStockProfit += (sharesOwnedPriorToToday * (latestPrice - previousClose));
                            dailyPortfolioChangeUpdate(todaysStockProfit);

                            mStocksOwned.add(
                                    new StockEntry(symbol,
                                            formattedShares + " " +
                                                    (sharesOwned > 1 ? "Shares" : "Share"),
                                            latestPrice, color));
                            mStocksOwnedAdapter.notifyDataSetChanged();
                        } else {
                            String companyName = result.getString("companyName");

                            mWatchlistStocks.add(new StockEntry(symbol, companyName,
                                    latestPrice, color));
                            mWatchlistAdapter.notifyDataSetChanged();
                        }

                        // removing the placeholder shimmer view.
                        // if there are no more placeholder views are left (meaning all stocks have
                        // been processed) then the shimmer animation is stopped and the view is
                        // hidden.
                        placeholderContainer.removeView(placeholderContainer.getChildAt(0));
                        if (position == list.size() - 1) {
                            shimmerLayout.setVisibility(View.GONE);
                            shimmerLayout.stopShimmer();
                        }

                        // recursive call..
                        stockDataRequest(position + 1, list, placeholderContainer,
                                shimmerLayout, isPortfolioStock, todaysStockProfit);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * Sets the portfolio value text animations.
     */
    private void setupPortfolioValueText() {
        // specifying the animation style of the portfolio value text.
        TickerView tickerView = findViewById(R.id.portfolio_value_heading);
        Typeface typeface = ResourcesCompat.getFont(this, R.font.nunito);
        tickerView.setTypeface(typeface);
        tickerView.setAnimationDuration(250);
        tickerView.setCharacterLists(TickerUtils.provideNumberList());
        tickerView.setAnimationInterpolator(new OvershootInterpolator());
        tickerView.setPreferredScrollingDirection(TickerView.ScrollingDirection.ANY);
    }

    /**
     * Sets the text on screen representing the user's portfolio value to the value provided as
     * a parameter.
     *
     * @param value the value of the portfolio.
     */
    private void setPortfolioValue(double value) {
        TickerView portfolioValue = (TickerView) this.findViewById(R.id.portfolio_value_heading);
        portfolioValue.setText(Util.formatPriceText(value, true, true));
    }

    /**
     * Inflates a stock placeholder view into the layout provided. This layout is useful to show
     * on the screen while the stock information is being fetched from the API.
     *
     * @param layout the layout that will be the parent of the newly inserted placeholder.
     */
    private void insertPlaceholder(LinearLayout layout) {
        View placeholder = getLayoutInflater().inflate(R.layout.stock_placeholder, layout, false);
        layout.addView(placeholder);
    }

    @Override
    protected void onResume() {
        super.onResume();
        recalibrateStockData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }
}