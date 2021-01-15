package com.pocketprofit.source.activities;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.pocketprofit.R;
import com.pocketprofit.source.Util;
import com.pocketprofit.source.database.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccountStatementActivity extends AppCompatActivity {
    private DatabaseHelper mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_statement);

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

        mDatabase = DatabaseHelper.getInstance(this);       // close
        Map<String, Double> stocks = mDatabase.getStocksOwned();
        if (stocks.isEmpty()) {
            this.findViewById(R.id.stock_info).setVisibility(View.GONE);
        } else {
            setUpStockPieChart(stocks);
        }
        setPortfolioInformation();

        TextView resetButton = (TextView) this.findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openConfirmationPopup();
            }
        });
    }

    /**
     * Opens a popup that asks the user to confirm his/her decision to reset the portfolio.
     */
    public void openConfirmationPopup() {
        final Dialog confirmationPopup = new Dialog(this);
        confirmationPopup.setContentView(R.layout.confirmation_popup);

        confirmationPopup.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ImageView closePopup = (ImageView) confirmationPopup.findViewById(R.id.close_popup);
        closePopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmationPopup.dismiss();
            }
        });

        Button cancelButton = (Button) confirmationPopup.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmationPopup.dismiss();
            }
        });

        Button resetButton = (Button) confirmationPopup.findViewById(R.id.reset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetPortfolio(confirmationPopup);
            }
        });
        confirmationPopup.show();
    }

    /**
     * Resets the portfolio.
     * All stocks owned and stocks under watch are cleared.
     *
     * @param popup asking the user to confirm the decision to reset the portfolio.
     */
    public void resetPortfolio(Dialog popup) {
        Util.displayToast(this, "Portfolio Successfully Reset.");
        Util.clearWatchlist(this);
        Util.resetCashAvailable(this);
        mDatabase.clearTables();
        popup.dismiss();
        finish();
    }

    /**
     * Sets and displays key portfolio statistics to the user such as total portfolio return and
     * portfolio distribution of cash vs stocks.
     */
    public void setPortfolioInformation() {
        LinearLayout portfolioLayout = this.findViewById(R.id.porfolio_summary);

        double totalCash = Util.getCashAvailable(this);
        double totalEquity = mDatabase.getTotalEquity();
        double portfolioValue = totalCash + totalEquity;

        if (totalEquity == 0) {
            this.findViewById(R.id.portfolio_pie_chart).setVisibility(View.GONE);
        } else {
            setUpPortfolioPieChart();
        }

        inflateTextView("Portfolio Value:", Util.formatPriceText(portfolioValue,
                true, true), null, portfolioLayout);

        String totalEquityText = Util.formatPriceText(totalEquity,
                true, true);
        String totalCashText = Util.formatPriceText(totalCash,
                true, true);

        inflateTextView("Stocks:", totalEquityText,
                Util.formatPercentageText(totalEquity / portfolioValue * 100.00), portfolioLayout);
        inflateTextView("Cash:", totalCashText,
                Util.formatPercentageText(totalCash / portfolioValue * 100.00), portfolioLayout);
        inflateTextView("Total Return:",
                Util.getPercentChangeText(Util.STARTING_VALUE, portfolioValue, true),
                null, portfolioLayout);
        inflateTextView("Date Joined:",
                Util.getDateJoined(this), null, portfolioLayout);
    }

    /**
     * Inflates an 'account_summary_text_entry' layout given the information provided as parameters.
     *
     * @param header            information to store in the 'text_header' of the layout
     * @param text              information to store in the 'text' of the layout
     * @param supplementaryText optional text to store in the 'supplementary_text' of layout
     * @param layout            the layout which to add this inflated view to.
     */
    private void inflateTextView(String header, String text, String supplementaryText,
                                 LinearLayout layout) {
        View inflatedLayout = getLayoutInflater()
                .inflate(R.layout.account_summary_text_entry, layout, false);
        TextView headerText = (TextView) inflatedLayout.findViewById(R.id.text_header);
        TextView textValue = (TextView) inflatedLayout.findViewById(R.id.text);

        headerText.setText(header);
        textValue.setText(text);

        if (supplementaryText != null) {
            TextView additionalText =
                    (TextView) inflatedLayout.findViewById(R.id.supplementary_text);
            additionalText.setText(supplementaryText);
        }
        layout.addView(inflatedLayout);
        inflatedLayout.setAlpha(0f);
        inflatedLayout.animate().alpha(1f).setDuration(250);
    }

    /**
     * Initializes and displays a pie chart describing the current portfolio distribution of cash
     * vs stocks.
     * Elements of this pie chart can be selected to highlight the distribution information.
     */
    public void setUpPortfolioPieChart() {
        PieChart pieChart = this.findViewById(R.id.portfolio_pie_chart);

        double totalStockEquity = mDatabase.getTotalEquity();
        double totalCashValue = Util.getCashAvailable(this);

        List<PieEntry> entries = new ArrayList<>();
        if (totalStockEquity > 0) {
            entries.add(new PieEntry((float) totalStockEquity, "Stocks"));
        }
        entries.add(new PieEntry((float) totalCashValue, "Cash"));

        PieDataSet pieDataSet = new PieDataSet(entries, "portfolio");
        pieDataSet.setColors(ColorTemplate.PASTEL_COLORS);
        pieDataSet.setValueTextSize(16f);
        pieDataSet.setSliceSpace(5f);
        pieDataSet.setDrawValues(false);

        PieData pieData = new PieData(pieDataSet);

        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDragDecelerationFrictionCoef(0.65f);
        pieChart.setEntryLabelTypeface(ResourcesCompat.getFont(this, R.font.nunito));

        pieChart.setDrawCenterText(false);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.getLegend().setEnabled(false);
        pieChart.animateY(1000, Easing.EaseInOutCubic);

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                String labelSelected = ((PieEntry) e).getLabel();
                LinearLayout portfolioLayout =
                        AccountStatementActivity.this.findViewById(R.id.porfolio_summary);
                View stocklayoutView = portfolioLayout.getChildAt(1);
                View cashlayoutView = portfolioLayout.getChildAt(2);

                if (labelSelected.equals("Stocks")) {
                    stocklayoutView.animate().scaleX(1.05f).scaleY(1.05f);
                    cashlayoutView.animate().scaleX(1f).scaleY(1f);
                } else if (labelSelected.equals("Cash")) {
                    stocklayoutView.animate().scaleX(1f).scaleY(1f);
                    cashlayoutView.animate().scaleX(1.05f).scaleY(1.05f);
                }
            }

            @Override
            public void onNothingSelected() {
                LinearLayout portfolioLayout =
                        AccountStatementActivity.this.findViewById(R.id.porfolio_summary);
                View stocklayoutView = portfolioLayout.getChildAt(1);
                View cashlayoutView = portfolioLayout.getChildAt(2);

                stocklayoutView.animate().scaleX(1f).scaleY(1f);
                cashlayoutView.animate().scaleX(1f).scaleY(1f);
            }
        });
    }

    /**
     * Initializes and displays a pie chart describing the various stocks the user currently owns in
     * his/her portfolio.
     * Elements in this pie chart can be expanded to display more information.
     *
     * @param stocks a mapping of all the stock symbols a user owns as well as the total current
     *               market value of that position.
     */
    public void setUpStockPieChart(Map<String, Double> stocks) {
        PieChart pieChart = this.findViewById(R.id.stocks_pie_chart);

        final List<PieEntry> stockEntries = new ArrayList<>();
        for (String stockSymbol : stocks.keySet()) {
            double totalStockEquity = stocks.get(stockSymbol);
            stockEntries.add(new PieEntry((float) totalStockEquity, stockSymbol));
        }
        PieDataSet pieDataSet = new PieDataSet(stockEntries, "stocks");
        pieDataSet.setColors(ColorTemplate.PASTEL_COLORS);
        pieDataSet.setValueTextColor(Color.BLACK);
        pieDataSet.setValueTextSize(16f);
        pieDataSet.setSliceSpace(5f);
        pieDataSet.setDrawValues(false);

        PieData pieData = new PieData(pieDataSet);

        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDragDecelerationFrictionCoef(0.65f);
        pieChart.setEntryLabelTypeface(ResourcesCompat.getFont(this, R.font.nunito));

        pieChart.setDrawCenterText(false);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.getLegend().setEnabled(false);   // Hide the legend
        pieChart.animateY(1000, Easing.EaseInOutCubic);

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                LinearLayout stockLayout =
                        (LinearLayout) AccountStatementActivity.this.findViewById(R.id.pie_chart_info);
                stockLayout.removeAllViews();

                String symbol = ((PieEntry) e).getLabel();
                inflateTextView("Name:", mDatabase.getCompanyName(symbol), null, stockLayout);
                inflateTextView("Symbol:", symbol, null, stockLayout);
                inflateTextView("Shares:", Util.formatShareCountText(mDatabase.getShareCount(symbol)), null, stockLayout);
                inflateTextView("Average Cost:", Util.formatPriceText(mDatabase.getAverageCost(symbol), true, true), null, stockLayout);
                inflateTextView("Price:", Util.formatPriceText(mDatabase.getCurrentPrice(symbol), true, true), null, stockLayout);
                double totalCost = mDatabase.getStockCost(symbol);
                double totalEquity = mDatabase.getStockEquity(symbol);
                inflateTextView("Total Equity:", Util.formatPriceText(totalEquity, true, true), null, stockLayout);
                inflateTextView("Total Return:",  Util.getPercentChangeText(totalCost, totalEquity, true), null, stockLayout);
            }

            @Override
            public void onNothingSelected() {
                LinearLayout stockLayout = (LinearLayout) AccountStatementActivity.this.findViewById(R.id.pie_chart_info);
                stockLayout.removeAllViews();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }
}