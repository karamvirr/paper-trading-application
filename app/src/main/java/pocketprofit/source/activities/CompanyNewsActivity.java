package com.pocketprofit.source.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.pocketprofit.R;
import com.pocketprofit.source.JSONArrayCallback;
import com.pocketprofit.source.Util;
import com.pocketprofit.source.adapters.NewsAdapter;
import com.pocketprofit.source.adapters.SearchResultAdapter;
import com.pocketprofit.source.entries.NewsEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CompanyNewsActivity extends AppCompatActivity {

    // bridge between our data (mNewsList) and our RecyclerView
    private NewsAdapter mAdapter;
    private List<NewsEntry> mNewsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_news);

        if (Util.DISPLAY_ADS) {
            AdView adView = (AdView) this.findViewById(R.id.ad_view);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else {
            this.findViewById(R.id.ad_container).setVisibility(View.GONE);
        }

        TextView headerText = (TextView) this.findViewById(R.id.header);
        String header = getIntent().getStringExtra(Util.EXTRA_SYMBOL) + " News";
        headerText.setText(header);

        ImageView backButton = findViewById(R.id.back_icon);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mNewsList = new ArrayList<>();
        buildRecyclerView();

        getNewsInformation();
    }

    /**
     * Makes an external API call for recent news articles regarding a stock.
     */
    public void getNewsInformation() {
        String symbol = getIntent().getStringExtra(Util.EXTRA_SYMBOL);
        Util.fetchCompanyNews(getBaseContext(), symbol, new JSONArrayCallback() {
            @Override
            public void onSuccess(JSONArray response) {
                if (response.length() == 0) {
                    CompanyNewsActivity.this.findViewById(R.id.no_news).setVisibility(View.VISIBLE);
                } else {
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject result = response.getJSONObject(i);

                            String headline = result.getString("headline");
                            String source = result.getString("source");
                            String siteURL = result.getString("url");
                            String summary = result.getString("summary");
                            String imageURL = result.getString("image");

                            String language = result.getString("lang");
                            boolean hasPaywall = result.getBoolean("hasPaywall");
                            if (language.equals("en") && !hasPaywall) {
                                insertEntry(i, source, headline, summary, imageURL, siteURL);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    /**
     * Creates and initializes a news article entry using the given information.
     * This article is then visible on the screen for the user to see, with its position relative
     * to other articles being determined by the parameter 'position'.
     *
     * @param position  the position in the list to place the news entry.
     * @param source    source of the article
     * @param headline  headline of the article
     * @param summary   summary of the article
     * @param imageURL  image url of the article
     * @param siteURL   website url of the article
     */
    public void insertEntry(int position, String source, String headline, String summary,
                            String imageURL, String siteURL) {
        mNewsList.add(new NewsEntry(source, headline, summary, imageURL, siteURL));
        mAdapter.notifyItemInserted(position);
    }

    /**
     * Builds the recycler view which will contain all of the new article elements.
     */
    public void buildRecyclerView() {
        RecyclerView mRecyclerView = this.findViewById(R.id.recycler_view);
        // responsible for aligning the single items in our list.
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new NewsAdapter(mNewsList, this);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new SearchResultAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                NewsEntry newEntrySelected = mNewsList.get(position);
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(newEntrySelected.getSiteURL())));
            }
        });
    }
}