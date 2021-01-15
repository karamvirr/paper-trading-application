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
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    // bridge between our data (mQueryResultList) and our RecyclerView
    private SearchResultAdapter mAdapter;
    private List<SearchResultEntry> mQueryResultList;

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

        mQueryResultList = new ArrayList<>();
        buildRecyclerView();
        setSearchBar();
    }

    /**
     * Sets up the search bar which the user will use to look up different companies/securities
     * available to trade.
     */
    public void setSearchBar() {
        final EditText searchBox = (EditText) this.findViewById(R.id.search_bar);

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (searchBox.getText().toString().length() == 0) {
                    clearQueryResults();
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (searchBox.getText().toString().length() == 0) {
                    clearQueryResults();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                clearQueryResults();
                if (searchBox.getText().toString().length() > 0) {
                    toggleSearchProgressBarVisibility(true);
                    executeQuery(searchBox.getText().toString());
                }
            }
        });
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
     * Removes all of the search results currently displayed on the screen.
     */
    public void clearQueryResults() {
        int size = mQueryResultList.size();
        for (int i = 0; i < size; i++) {
            removeEntry(0);
        }
    }

    /**
     * Sets up a container that will be used to store results gathered from user input.
     */
    public void buildRecyclerView() {
        RecyclerView mRecyclerView = this.findViewById(R.id.recycler_view);
        // responsible for aligning the single items in our list.
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new SearchResultAdapter(mQueryResultList, this);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new SearchResultAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                SearchResultEntry querySelected = mQueryResultList.get(position);
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
    public void executeQuery(String query) {
        Util.fetchSearchResults(getBaseContext(), query, new JSONArrayCallback() {
            @Override
            public void onSuccess(JSONArray response) {
                toggleSearchProgressBarVisibility(false);
                clearQueryResults();

                int position = 0;
                for (int i = 0; i < response.length(); i++) {
                    try {
                        JSONObject result = response.getJSONObject(i);
                        String name = result.getString("securityName");
                        String symbol = result.getString("symbol");
                        String exchange = result.getString("exchange");
                        String securityType = result.getString("securityType");

                        if (!symbol.contains("-") &&
                                !Util.EXCHANGES_NOT_SUPPORTED.contains(exchange) && !name.equals("")
                                && !securityType.equals("MF_O") && !securityType.equals("PREF")) {
                            insertEntry(position++, name, symbol);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Places a search result element onto the screen for the user to see. This search result will
     * contain information such as the name & symbol of the company/security, if
     * one is available.
     * The user can also click on this entry to open up its stock information chart.
     *
     * @param position  the position to place this entry on the screen.
     * @param name      the company/security name of the search result entry.
     * @param symbol    the ticker symbol of the company/security name.
     */
    public void insertEntry(int position, String name, String symbol) {
        mQueryResultList.add(position, new SearchResultEntry(symbol, name));
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Removes a search result element at the position defined in the parameter from the screen.
     *
     * @param position the position of the element to remove in the search result list.
     */
    public void removeEntry(int position) {
        mQueryResultList.remove(position);
        mAdapter.notifyDataSetChanged();
    }
}