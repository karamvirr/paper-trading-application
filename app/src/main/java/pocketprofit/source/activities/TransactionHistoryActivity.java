package com.pocketprofit.source.activities;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.pocketprofit.R;
import com.pocketprofit.source.Util;
import com.pocketprofit.source.adapters.TransactionHistoryAdapter;
import com.pocketprofit.source.database.DatabaseHelper;

public class TransactionHistoryActivity extends AppCompatActivity {

    private String mSymbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

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

        mSymbol = getIntent().getStringExtra(Util.EXTRA_SYMBOL);
        setUpRecyclerView();
    }

    /**
     * Builds a recycler view that will be used to store information of all the transactions the
     * user had made.
     */
    public void setUpRecyclerView() {
        DatabaseHelper db = DatabaseHelper.getInstance(this);       // close this
        RecyclerView transactionRecyclerView = this.findViewById(R.id.transaction_recycler_view);
        transactionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        Cursor transactionHistoryCursor;
        if (mSymbol == null) {
            transactionHistoryCursor = db.getAllTransactions();
        } else {
            transactionHistoryCursor = db.getAllTransactions(mSymbol);
        }
        if (transactionHistoryCursor.getCount() == 0) {
            this.findViewById(R.id.no_transactions).setVisibility(View.VISIBLE);
        }
        final TransactionHistoryAdapter adapter = new TransactionHistoryAdapter(this, transactionHistoryCursor);
        transactionRecyclerView.setAdapter(adapter);
        transactionRecyclerView.setNestedScrollingEnabled(false);

        adapter.setOnItemClickListener(new TransactionHistoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String name = adapter.getCompanyName(position);
                String symbol = adapter.getCompanySymbol(position);
                String orderType = adapter.getOrderType(position);
                int quantity = adapter.getQuantity(position);
                double price = adapter.getPrice(position);
                String date = adapter.getDate(position);

                displayTransactionReceipt(name, symbol, orderType, quantity, price, date);
            }
        });
    }

    /**
     * This method is called whenever a user selects on a transaction history element.
     * Creates a popup that contains additional information on the transaction history element
     * selected.
     *
     * @param name the name of the security traded.
     * @param symbol ticker symbol of the security traded.
     * @param orderType the order type of the transaction (buy order or sell order)
     * @param quantity the number of stocks traded.
     * @param price the price at which they were traded
     * @param date the date the transaction was filled.
     */
    public void displayTransactionReceipt(String name, final String symbol, String orderType, int quantity, double price, String date) {
        final Dialog receipt = new Dialog(this);
        receipt.setContentView(R.layout.transaction_receipt_popup);
        receipt.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView receiptHeader = (TextView) receipt.findViewById(R.id.transaction_receipt_header);
        receiptHeader.setText(orderType);

        double totalNotional = price * quantity;
        if (orderType.equals("Free Stock")) {
            totalNotional = 0.0;
        }
        LinearLayout layout = (LinearLayout) receipt.findViewById(R.id.receipt_layout);
        inflateText("Name:", name, layout);
        inflateText("Symbol:", symbol, layout);
        inflateText("Quantity:", Util.formatShareCountText(quantity), layout);
        inflateText("Price:", Util.formatPriceText(price, true, true), layout);
        inflateText("Total Notional:", Util.formatPriceText(totalNotional, true, true), layout);
        inflateText("Date:", date, layout);

        ImageView closePopup = (ImageView) receipt.findViewById(R.id.close_popup);
        closePopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                receipt.dismiss();
            }
        });

        TextView chartLinkText = (TextView) receipt.findViewById(R.id.chart_link);
        if (mSymbol == null) {
            chartLinkText.setText("View " + symbol);
            chartLinkText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), StockInformationActivity.class);
                    intent.putExtra(Util.EXTRA_SYMBOL, symbol);
                    startActivity(intent);
                }
            });
        } else {
            chartLinkText.setVisibility(View.GONE);
        }

        receipt.show();
    }

    /**
     * Inflates and sets a layout based on information passed in the parameter and appends to the
     * given layout.
     *
     * @param descriptor text describing the order type of the transaction
     * @param content    the total notional of the order
     * @param layout     the layout to append the inflated layout to.
     */
    private void inflateText(String descriptor, String content, LinearLayout layout) {
        View inflatedLayout = getLayoutInflater().inflate(R.layout.transaction_receipt_text_entry, layout, false);
        TextView descriptorText = (TextView) inflatedLayout.findViewById(R.id.row_name);
        TextView contentText = (TextView) inflatedLayout.findViewById(R.id.row_content);

        descriptorText.setText(descriptor);
        contentText.setText(content);

        layout.addView(inflatedLayout);
    }
}