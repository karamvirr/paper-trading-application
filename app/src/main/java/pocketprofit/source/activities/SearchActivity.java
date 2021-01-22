package com.pocketprofit.source.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.pocketprofit.R;
import com.pocketprofit.source.JSONArrayCallback;
import com.pocketprofit.source.Util;
import com.pocketprofit.source.adapters.SearchResultAdapter;
import com.pocketprofit.source.entries.SearchResultEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {

    // bridge between our data (List<SearchResultEntry> - search result for query) and our RecyclerView
    private SearchResultAdapter mAdapter;

    // caches all search results to increase performance on searches on previous queries (the first
    // call to query "APPL" will take time as it will send a request to the PocketProfit API, however,
    // after this the result is cached locally and all future calls to "APPL" in this activity instance
    // will display the results almost immediately). also this serves as a way to save money, since
    // search queries that were performed already don't have to request data from the API again.
    private Map<String, List<SearchResultEntry>> mResultCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        if (Util.DISPLAY_ADS) {
            AdView adView = (AdView) this.findViewById(R.id.ad_view);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else {
            this.findViewById(R.id.ad_container).setVisibility(View.GONE);
        }

        ImageView backButton = findViewById(R.id.back_icon);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        buildRecyclerView();

        mResultCache = new HashMap<String, List<SearchResultEntry>>();
        mResultCache.put("", new ArrayList<SearchResultEntry>());  // empty search box.
        setSearchBar();
    }

    /**
     * Sets up the search bar which the user will use to look up different companies/securities
     * available to trade.
     */
    public void setSearchBar() {
        ((EditText) this.findViewById(R.id.search_bar)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                toggleSearchProgressBarVisibility(true);
                executeQuery(getQuery());
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    /**
     * Returns the text that is currently in the search box in lowercase, this represents the search query the
     * user is making.
     *
     * @return  the text currently in the search box.
     */
    private String getQuery() {
        EditText searchBox = (EditText) this.findViewById(R.id.search_bar);
        return searchBox.getText().toString().toLowerCase();
    }

    /**
     * Switches from displaying either a search icon or a progress bar based on the parameter
     * given.
     *
     * @param isVisible boolean to determine if progress bar is visible.
     */
    private void toggleSearchProgressBarVisibility(boolean isVisible) {
        if (isVisible) {
            this.findViewById(R.id.search_progress).setVisibility(View.VISIBLE);
            this.findViewById(R.id.search_icon).setVisibility(View.GONE);
        } else {
            this.findViewById(R.id.search_progress).setVisibility(View.GONE);
            this.findViewById(R.id.search_icon).setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets up a container that will be used to store results gathered from user input.
     */
    public void buildRecyclerView() {
        RecyclerView mRecyclerView = this.findViewById(R.id.recycler_view);
        // responsible for aligning the single items in our list.
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new SearchResultAdapter(new ArrayList<SearchResultEntry>(), this);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new SearchResultAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                SearchResultEntry querySelected = mAdapter.get(position);
                String companyName = querySelected.getSubheader();
                String companySymbol = querySelected.getHeader();
                openStockInformationActivity(companyName, companySymbol);
            }
        });
    }

    /**
     * Opens up the Stock Information Activity for the security that has been selected.
     *
     * @param name the name of the security selected.
     * @param symbol the symbol of the security selected.
     */
    private void openStockInformationActivity(String name, String symbol) {
        Intent intent = new Intent(getBaseContext(), StockInformationActivity.class);
        intent.putExtra(Util.EXTRA_NAME, name);
        intent.putExtra(Util.EXTRA_SYMBOL, symbol);
        startActivity(intent);
    }

    /**
     * Fetches search result information from an external API using the provided user input.
     * This information will then be displayed on the screen for the user to see.
     *
     * @param query the user input
     */
    public void executeQuery(final String query) {
        if (mResultCache.containsKey(query)) {
            toggleSearchProgressBarVisibility(false);
            mAdapter.setList(mResultCache.get(query));
        } else {
            Util.fetchSearchResults(getBaseContext(), query, new JSONArrayCallback() {
                @Override
                public void onSuccess(JSONArray response) {
                    List<SearchResultEntry> searchResultList = parseSearchResult(response);
                    mResultCache.put(query, searchResultList);
                    // only update the search result view if the JSON response is for the query
                    // currently on the search box. 
                    if (query.equals(getQuery())) {
                        toggleSearchProgressBarVisibility(false);
                        mAdapter.setList(searchResultList);
                    }
                }
            });
        }
    }

    /**
     * Uses the JSON list provided as a parameter to return a parsed list. A parsed list is all
     * the ticker symbols in the JSON param that are supported by PocketProfit.
     * The returned list can be used by the adapter to display contents on the recycler view.
     *
     * @param jsonResponse  JSON response from a call to the PocketProfit API
     * @return              returns a parsed list of the JSON given as a parameter.
     */
    private List<SearchResultEntry> parseSearchResult(JSONArray jsonResponse) {
        List<SearchResultEntry> parsedList = new ArrayList<SearchResultEntry>();
        for (int i = 0; i < jsonResponse.length(); i++) {
            try {
                JSONObject result = jsonResponse.getJSONObject(i);
                String name = result.getString("securityName");
                String symbol = result.getString("symbol");
                String exchange = result.getString("exchange");
                String securityType = result.getString("securityType");

                if (!symbol.contains("-") &&
                        !Util.EXCHANGES_NOT_SUPPORTED.contains(exchange) && !name.equals("")
                        && !securityType.equals("MF_O") && !securityType.equals("PREF")) {
                    parsedList.add(new SearchResultEntry(symbol, name));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return parsedList;
    }
}