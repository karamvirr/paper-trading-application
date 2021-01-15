package com.pocketprofit.source.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.pocketprofit.R;
import com.pocketprofit.source.JSONArrayCallback;
import com.pocketprofit.source.Util;

import org.json.JSONArray;
import org.json.JSONObject;

public class SectorListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sector_list);

        if (Util.DISPLAY_ADS) {
            AdView adView = (AdView) this.findViewById(R.id.ad_view);
            final AdRequest adRequest = new AdRequest.Builder().build();
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

        Util.fetchSectorList(this, new JSONArrayCallback() {
            @Override
            public void onSuccess(JSONArray result) {
                parseJSONResult(result);
            }
        });
    }

    /**
     * Parses the JSON information that is retrieved from the API call to the PocketProfit server,
     * and displays the relevant information on the user's screen.
     *
     * @param json  the JSON result from the API call.
     */
    private void parseJSONResult(JSONArray json) {
        ShimmerFrameLayout shimmerFrameLayout = this.findViewById(R.id.sector_list_shimmer);
        shimmerFrameLayout.stopShimmer();
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject sectorJSON = json.getJSONObject(i);
                String sector = sectorJSON.getString("name");
                if (!Util.SECTORS_NOT_SUPPORTED.contains(sector)) {
                    insertItem(sector);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        shimmerFrameLayout.setVisibility(View.GONE);
    }

    /**
     * Displays a text view on the user's screen containing the information passed in as a parameter.
     *
     * @param sector    name to display on the user on screen.
     */
    private void insertItem(final String sector) {
        LinearLayout shimmerLayout = this.findViewById(R.id.shimmer_layout);
        LinearLayout linearLayout = this.findViewById(R.id.sector_list);

        View inflatedLayout = getLayoutInflater().inflate(R.layout.sector_name_entry, linearLayout, false);
        TextView sectorNameHeader = inflatedLayout.findViewById(R.id.sector_header);
        sectorNameHeader.setText(sector);

        inflatedLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView selectedSectorView = view.findViewById(R.id.sector_header);
                Intent intent = new Intent (getBaseContext(), SectorSecurityInformation.class);
                intent.putExtra(Util.EXTRA_SECTOR, selectedSectorView.getText());
                startActivity(intent);
            }
        });

        if (shimmerLayout.getChildCount() > 0) {
            shimmerLayout.removeViewAt(0);
        }
        linearLayout.addView(inflatedLayout);
    }

}