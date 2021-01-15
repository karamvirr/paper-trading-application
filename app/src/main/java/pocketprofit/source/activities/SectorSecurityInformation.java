package com.pocketprofit.source.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.pocketprofit.R;
import com.pocketprofit.source.JSONArrayCallback;
import com.pocketprofit.source.Util;
import com.pocketprofit.source.adapters.EnhancedStockAdapter;
import com.pocketprofit.source.entries.EnhancedStockEntry;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SectorSecurityInformation extends AppCompatActivity {
    private List<EnhancedStockEntry> sectorDataList;
    private EnhancedStockAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sector_security_information);

        if (Util.DISPLAY_ADS) {
            AdView adView = (AdView) this.findViewById(R.id.ad_view);
            final AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else {
            this.findViewById(R.id.ad_container).setVisibility(View.INVISIBLE);
        }

        ImageView backButton = findViewById(R.id.back_icon);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        buildRecyclerView();
        setToolbar();
        setSortingMenu();
        Util.fetchSectorInformation(this, getIntent().getStringExtra(Util.EXTRA_SECTOR), new JSONArrayCallback() {
            @Override
            public void onSuccess(JSONArray result) {
                processSectorInformation(result);
            }
        });
    }

    /**
     * Parses the JSON response from the PocketProfit server and displays relevant information on
     * the user's screen.
     * The JSON data given in as a parameter will contain information (price, symbol, name, change,
     * etc) of stocks all in the same sector.
     *
     * @param jsonArray     the JSON data retrieved from the API call.
     */
    private void processSectorInformation(JSONArray jsonArray) {
        // Removing the shimmer layout.
        ShimmerFrameLayout shimmerFrameLayout = this.findViewById(R.id.sector_info_shimmer);
        shimmerFrameLayout.stopShimmer();
        ScrollView scrollView = this.findViewById(R.id.placeholder_view);
        scrollView.setVisibility(View.GONE);

        int counter = 0;
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject companyData = jsonArray.getJSONObject(i);
                String name = companyData.getString("companyName");
                String symbol = companyData.getString("symbol");

                double currentPrice = companyData.getDouble("latestPrice");
                double totalChange = companyData.getDouble("change");
                double percentChange = companyData.getDouble("changePercent");
                String exchange = companyData.getString("primaryExchange");

                int color;
                if (totalChange >= 0.0) {
                    color = this.getResources().getColor(R.color.profit);
                } else {
                    color = getResources().getColor(R.color.loss);
                }

                if (!symbol.contains("-") && !Util.EXCHANGES_NOT_SUPPORTED.contains(exchange)) {
                    counter++;
                    sectorDataList.add(new EnhancedStockEntry(name, symbol, color, currentPrice, totalChange, percentChange));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        TextView bottomHeader = (TextView) this.findViewById(R.id.bottom_header);
        bottomHeader.setText(Util.formatShareCountText(counter) + " items");
    }

    /**
     * Sets up a container that will be used to store stock information.
     */
    private void buildRecyclerView() {
        sectorDataList = new ArrayList<>();

        RecyclerView mRecyclerView = (RecyclerView) this.findViewById(R.id.sector_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new EnhancedStockAdapter(sectorDataList, this);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new EnhancedStockAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                EnhancedStockEntry itemSelected = mAdapter.get(position);
                String companySymbol = itemSelected.getHeader();
                String companyName = itemSelected.getSubheader();

                Intent intent = new Intent(getBaseContext(), StockInformationActivity.class);
                intent.putExtra(Util.EXTRA_NAME, companyName);
                intent.putExtra(Util.EXTRA_SYMBOL, companySymbol);
                startActivity(intent);
            }
        });
    }

    /**
     * Sets up the top toolbar of the activity. This toolbar will contain the name of the sector
     * as well as the number of items (stocks displayed in the recycler view) once that information
     * has been retrieved from the PocketProfit server ("-" placeholder text)
     */
    private void setToolbar() {
        String sector = getIntent().getStringExtra(Util.EXTRA_SECTOR);
        TextView topHeader = (TextView) this.findViewById(R.id.top_header);
        topHeader.setText(sector);
        topHeader.setTextSize(15);

        // placeholder text until the actual data is retrieved and processed from the
        // API request from PocketProfit.
        String placeholder = "-";
        TextView bottomHeader = (TextView) this.findViewById(R.id.bottom_header);
        bottomHeader.setText(placeholder);
    }

    /**
     * Sets up the sorting menu FAB. This will allow the user to sort the results based on symbol
     * or price in either ascending or descending order. Allowing them to readily parse through the
     * information being displayed.
     */
    private void setSortingMenu() {
        final FloatingActionsMenu menu = (FloatingActionsMenu) this.findViewById(R.id.sorting_options);
        final RecyclerView recyclerView = this.findViewById(R.id.sector_recycler_view);
        final ScrollView scrollViewPlaceholder = this.findViewById(R.id.placeholder_view);

        menu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                if (scrollViewPlaceholder.getVisibility() == View.VISIBLE) {
                    scrollViewPlaceholder.setAlpha(Util.OPACITY);
                } else {
                    recyclerView.setAlpha(Util.OPACITY);
                }
            }

            @Override
            public void onMenuCollapsed() {
                if (scrollViewPlaceholder.getVisibility() == View.VISIBLE) {
                    scrollViewPlaceholder.setAlpha(1f);
                } else {
                    recyclerView.setAlpha(1f);
                }
            }
        });
        menu.findViewById(R.id.symbol_ascending).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.collapse();
                Collections.sort(sectorDataList, new Comparator<EnhancedStockEntry>() {
                    @Override
                    public int compare(EnhancedStockEntry a, EnhancedStockEntry b) {
                        return a.getHeader().compareTo(b.getHeader());
                    }
                });
                mAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(0);
            }
        });
        menu.findViewById(R.id.symbol_descending).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.collapse();
                Collections.sort(sectorDataList, new Comparator<EnhancedStockEntry>() {
                    @Override
                    public int compare(EnhancedStockEntry a, EnhancedStockEntry b) {
                        return -a.getHeader().compareTo(b.getHeader());
                    }
                });
                mAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(0);
            }
        });
        menu.findViewById(R.id.price_ascending).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.collapse();
                Collections.sort(sectorDataList, new Comparator<EnhancedStockEntry>() {
                    @Override
                    public int compare(EnhancedStockEntry a, EnhancedStockEntry b) {
                        return -Double.compare(b.getPrice(), a.getPrice());
                    }
                });
                mAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(0);
            }
        });
        menu.findViewById(R.id.price_descending).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.collapse();
                Collections.sort(sectorDataList, new Comparator<EnhancedStockEntry>() {
                    @Override
                    public int compare(EnhancedStockEntry a, EnhancedStockEntry b) {
                        return Double.compare(b.getPrice(), a.getPrice());
                    }
                });
                mAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(0);
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
    }
}