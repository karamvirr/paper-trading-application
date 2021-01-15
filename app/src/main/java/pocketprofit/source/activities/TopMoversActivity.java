package com.pocketprofit.source.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.pocketprofit.R;
import com.pocketprofit.source.JSONArrayCallback;
import com.pocketprofit.source.Util;
import com.pocketprofit.source.adapters.EnhancedStockAdapter;
import com.pocketprofit.source.entries.EnhancedStockEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TopMoversActivity extends AppCompatActivity {
    private List<EnhancedStockEntry> mGainerList;
    private List<EnhancedStockEntry> mLoserList;

    private Button mGainers;
    private Button mLosers;
    private Button mSelected;

    private EnhancedStockAdapter mAdapter;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_movers);

        if (Util.DISPLAY_ADS) {
            AdView adView = (AdView) this.findViewById(R.id.ad_view);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else {
            this.findViewById(R.id.ad_container).setVisibility(View.GONE);
        }

        ImageView backButton = (ImageView) this.findViewById(R.id.back_icon);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mGainers = (Button) this.findViewById(R.id.gainers);
        mLosers = (Button) this.findViewById(R.id.losers);

        mSelected = mGainers;

        mSelected.getBackground().setColorFilter(getResources().getColor(R.color.profit), PorterDuff.Mode.SRC_ATOP);
        mSelected.setTextColor(Color.BLACK);

        mGainerList = new ArrayList<>();
        mLoserList = new ArrayList<>();

        buildRecyclerView();
        fetchTopMoversData();

        mGainers.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mSelected == view) {
                            view.getBackground().setColorFilter(ColorUtils.blendARGB(getResources().getColor(R.color.profit), Color.BLACK, 0.25f), PorterDuff.Mode.SRC_ATOP);
                            return true;
                        }
                    case MotionEvent.ACTION_UP:
                        view.getBackground().setColorFilter(getResources().getColor(R.color.profit), PorterDuff.Mode.SRC_ATOP);
                        onClick(mGainers);
                        return true;
                }
                return false;
            }
        });
        mLosers.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mSelected == view) {
                            view.getBackground().setColorFilter(ColorUtils.blendARGB(getResources().getColor(R.color.loss), Color.BLACK, 0.25f), PorterDuff.Mode.SRC_ATOP);
                            return true;
                        }
                    case MotionEvent.ACTION_UP:
                        view.getBackground().setColorFilter(getResources().getColor(R.color.loss), PorterDuff.Mode.SRC_ATOP);
                        onClick(mLosers);
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * Fetches information about today's largest price movements by making an external API call.
     */
    public void fetchTopMoversData() {
        final ShimmerFrameLayout shimmerFrameLayout = this.findViewById(R.id.top_mover_shimmer);
        final ScrollView scrollView = this.findViewById(R.id.placeholder_view);

        Util.fetchTopGainers(this, new JSONArrayCallback() {
            @Override
            public void onSuccess(JSONArray result) {
                parseData(result, mGainerList);

                Util.fetchTopLosers(getBaseContext(), new JSONArrayCallback() {
                    @Override
                    public void onSuccess(JSONArray result) {
                        parseData(result, mLoserList);

                        scrollView.setVisibility(View.GONE);
                        shimmerFrameLayout.stopShimmer();
                        if (mSelected.getText().toString().contains("Gainers")) {
                            setList(mGainerList);
                        } else {
                            setList(mLoserList);
                        }
                    }
                });
            }
        });
    }

    /**
     * Parses through the JSON response data and puts that information into the list given as a
     * parameter.
     *
     * @param result the JSON response from an external API call.
     * @param list the list that will be populated with the response information.
     */
    private void parseData(JSONArray result, List<EnhancedStockEntry> list) {
        for (int i = 0; i < result.length(); i++) {
            try {
                JSONObject json = result.getJSONObject(i);

                String name = json.getString("companyName");
                String symbol = json.getString("symbol");

                double currentPrice = json.getDouble("latestPrice");
                double totalChange = json.getDouble("change");
                double percentChange = json.getDouble("changePercent");

                int color;
                if (totalChange >= 0.0) {
                    color = this.getResources().getColor(R.color.profit);
                } else {
                    color = getResources().getColor(R.color.loss);
                }
                list.add(new EnhancedStockEntry(name, symbol, color, currentPrice, totalChange, percentChange));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Builds a recycler view that contains information of stocks that had the largest price
     * movement of the day.
     */
    public void buildRecyclerView() {
        RecyclerView mRecyclerView = (RecyclerView) this.findViewById(R.id.movers_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new EnhancedStockAdapter(mGainerList, this);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new EnhancedStockAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                EnhancedStockEntry itemSelected = mAdapter.get(position);
                String companySymbol = itemSelected.getHeader();
                String companyName = itemSelected.getSubheader();
                openStockInformationActivity(companyName, companySymbol);
            }
        });
    }

    /**
     * Displays the given top mover entry information on the user's screen.
     * If the given list is empty, then there will be text on the screen to indicate that to the
     * user.
     *
     * @param list  of contents to display on the screen.
     */
    private void setList(List<EnhancedStockEntry> list) {
        mAdapter.setList(list);
        ScrollView shimmerScrollView = this.findViewById(R.id.placeholder_view);
        // only toggle the text visibility if data request is still processing.
        if (shimmerScrollView.getVisibility() == View.GONE) {
            if (list.size() == 0) {
                this.findViewById(R.id.no_top_movers_data).setVisibility(View.VISIBLE);
            } else {
                this.findViewById(R.id.no_top_movers_data).setVisibility(View.GONE);
            }
        }
    }

    /**
     * Open up the StockInformationActivity of the given security name and ticker symbol.
     *
     * @param name the name of the security.
     * @param symbol the ticker symbol of the security.
     */
    public void openStockInformationActivity(String name, String symbol) {
        Intent intent = new Intent(getBaseContext(), StockInformationActivity.class);
        intent.putExtra(Util.EXTRA_NAME, name);
        intent.putExtra(Util.EXTRA_SYMBOL, symbol);
        startActivity(intent);
    }

    /**
     * Displays information of either the today's top gainers or losers based upon the view passed
     * in as a parameter. This view is either a top gainers or top losers option.
     *
     * @param view the view that has been selected
     */
    public void onClick(View view) {
        if (mSelected != view) {
            mSelected.getBackground().setColorFilter(getResources().getColor(R.color.background), PorterDuff.Mode.SRC_ATOP);
            mSelected.setTextColor((getResources().getColor(R.color.gray)));

            mSelected = (Button) view;
            if (view == mGainers) {
                mSelected.getBackground().setColorFilter(getResources().getColor(R.color.profit), PorterDuff.Mode.SRC_ATOP);
                setList(mGainerList);
            } else {
                mSelected.getBackground().setColorFilter(getResources().getColor(R.color.loss), PorterDuff.Mode.SRC_ATOP);
                setList(mLoserList);
            }
            mSelected.setTextColor(Color.BLACK);
        }
    }
}