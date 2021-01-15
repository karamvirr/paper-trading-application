package com.pocketprofit.source.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.pocketprofit.R;
import com.pocketprofit.source.Util;
import com.pocketprofit.source.database.DatabaseHelper;

import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;
import nl.dionsegijn.konfetti.models.Size;

public class StockTransactionActivity extends AppCompatActivity implements View.OnTouchListener {
    // max number of shares that can be traded per transaction.
    public static final int MAX = 10000000;

    private boolean mIsBuyOrder;
    private String mSymbol;
    private String mDate;
    private double mPreviousClose;

    private int mShares;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_transaction);

        if (Util.DISPLAY_ADS) {
            AdView adView = (AdView) this.findViewById(R.id.ad_view);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else {
            this.findViewById(R.id.ad_container).setVisibility(View.INVISIBLE);
        }

        double sharePrice = getIntent().getDoubleExtra(Util.EXTRA_STOCK_PRICE, 0);
        TextView sharePriceText = (TextView) this.findViewById(R.id.share_price);
        sharePriceText.setText(Util.formatPriceText(sharePrice, true, true));

        updateReceipt();
        setUpNumericKeypad();

        mIsBuyOrder = getIntent().getBooleanExtra(Util.EXTRA_IS_BUY_TRANSACTION, false);
        mSymbol = getIntent().getStringExtra(Util.EXTRA_SYMBOL);
        mDate = getIntent().getStringExtra(Util.EXTRA_DATE);
        mPreviousClose = getIntent().getDoubleExtra(Util.EXTRA_PREVIOUS_CLOSE, 0);

        setTransactionHeader();

        ImageView backButton = findViewById(R.id.back_icon);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.execute_trade).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.getBackground().setColorFilter(ColorUtils.blendARGB(
                                getResources().getColor(R.color.profit), Color.BLACK, 0.25f),
                                PorterDuff.Mode.SRC_ATOP);
                        return true;
                    case MotionEvent.ACTION_UP:
                        view.playSoundEffect(SoundEffectConstants.CLICK);
                        view.getBackground().setColorFilter(getResources().getColor(R.color.profit),
                                PorterDuff.Mode.SRC_ATOP);
                        executeTrade();
                        view.performClick();
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * Called when the user is trying to execute a transaction.
     * If the user is executing a buy order, it will check to see if the user has enough money to
     * make the transaction, if so the trade is executed and the order details are logged.
     * If the user does not have enough money to complete the transaction, an error popup is
     * displayed to the user notifying them.
     * If the user is executing a sell order, it will check to see if the user has enough shares
     * available to sell. If so, it will liquidate the shares and the order details are logged.
     * If not, an error popup is displayed to the user notifying them.
     */
    public void executeTrade() {
        String name = getIntent().getStringExtra(Util.EXTRA_NAME);

        TextView priceText = (TextView) this.findViewById(R.id.share_price);
        double price = Double.parseDouble(priceText
                        .getText()
                        .toString()
                        .substring(1)
                        .replaceAll(",", ""));
        TextView totalNotional = (TextView) this.findViewById(R.id.total);
        double total = Double.parseDouble(totalNotional
                        .getText()
                        .toString()
                        .substring(1)
                        .replaceAll(",", ""));
        // check if markets are open
        if (mIsBuyOrder) {
            double cashOnHand = Util.getCashAvailable(getBaseContext());
            if (mShares == 0) {
                // not enough shares
                openPopup(false, "Transaction Rejected!",
                        "Enter at least 1 share.");
            } else if (cashOnHand >= total) {
                DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);
                boolean successs = databaseHelper.stockPurchase(name, mSymbol, mShares, price,
                        price, Util.getTodaysDate());

                if (successs) {

                    Dialog dialog = openPopup(true, "Order filled!",
                            Util.formatShareCountText(mShares) + " " +
                                    (mShares == 1 ? "share " : "shares ") + "of " + mSymbol +
                            " successfully purchased for a total price of " + totalNotional.getText() + ".");
                    if (databaseHelper.isTransactionHistoryEmpty()) {
                        displayConfetti();
                    }
                    if (Util.currentlyOnWatchlist(this, mSymbol)) {
                        Util.updateWatchList(this, mSymbol);
                    }
                    Util.updateCashAvailable(getBaseContext(), -total);

                    databaseHelper.logTransaction(name, mSymbol, "Market Buy", mShares,
                            price, Util.getTodaysDate());
                }
                databaseHelper.close();
            } else {
                // insufficient funds
                openPopup(false, "Transaction Rejected!",
                        "Buy order of " + Util.formatShareCountText(mShares) +
                                ((mShares == 1) ? " share" : " shares") +
                                " of " + mSymbol + " cannot be filled due to insufficient funds.");
            }
        } else {
            DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);
            int sharesAvailable = databaseHelper.getShareCount(mSymbol);
            if (sharesAvailable == 0) {
              openPopup(false, "Transaction Rejected!",
                      "You don't have any shares of " + mSymbol + " available to sell.");
            } else if (mShares == 0) {
                openPopup(false, "Transaction Rejected!",
                        "Enter at least 1 share.");
            } else if (mShares > databaseHelper.getShareCount(mSymbol)) {
                openPopup(false, "Transaction Rejected!",
                        "You only have " + Util.formatShareCountText(sharesAvailable) +
                                (sharesAvailable == 1 ? " share " : " shares ") + "available to sell.");
            } else {
                // Sell order transaction can be made.
                double amountSold = databaseHelper.liquidateStock(mSymbol, mShares, mPreviousClose, mDate);
                Util.updateCashAvailable(this, amountSold);
                openPopup(true, "Order filled!",
                        Util.formatShareCountText(mShares) +
                        (mShares == 1 ? " share" : " shares") + " sold for " + priceText.getText().toString() +
                        " per share. " + totalNotional.getText().toString() + " has been added to your cash " +
                                "balance.");
                databaseHelper.logTransaction(name, mSymbol, "Market Sell",
                        mShares, price, Util.getTodaysDate());
            }
            databaseHelper.close();
        }
    }

    /**
     * Displays a popup that contains information to transmit to the user.
     *
     * @param success boolean true/false status of the transaction, if the transaction is a success
     *                the activity will close upon closing the popup.
     * @param header the header of the popup.
     * @param text   the main text of the popup.
     * @return       a reference to the created dialog
     */
    private Dialog openPopup(final boolean success, String header, String text) {
        final Dialog transactionStatusPopup = new Dialog(this);
        transactionStatusPopup.setContentView(R.layout.transaction_popup);
        transactionStatusPopup.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ImageView closePopup = (ImageView) transactionStatusPopup.findViewById(R.id.close_popup);
        closePopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (success) {
                    finish();
                }
                transactionStatusPopup.dismiss();
            }
        });

        ImageView icon = (ImageView) transactionStatusPopup.findViewById(R.id.popup_icon);
        icon.setImageResource(success ? R.drawable.ic_success: R.drawable.ic_fail);

        TextView headerText = (TextView) transactionStatusPopup.findViewById(R.id.popup_header);
        headerText.setText(header);

        TextView messageText = (TextView) transactionStatusPopup.findViewById(R.id.popup_message);
        messageText.setText(text);

        transactionStatusPopup.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (success) {
                    finish();
                }
            }
        });

        transactionStatusPopup.show();

        return transactionStatusPopup;
    }

    /**
     * Updates the receipt displayed on the screen based on the number of shares the user currently
     * has chosen.
     * The receipt is defined as the text describing the number of shares selected, the price of the
     * stock, and the total notional.
     */
    public void updateReceipt() {
        TextView totalShares = (TextView) this.findViewById(R.id.shares);
        totalShares.setText(Util.formatShareCountText(mShares));
        TextView sharesText = this.findViewById(R.id.share_text);
        if (mShares == 0) {
            totalShares.setTextColor(Color.GRAY);
            sharesText.setText("Shares");
        } else if (mShares == 1) {
            totalShares.setTextColor(Color.WHITE);
            sharesText.setText("Share");
        } else {
            totalShares.setTextColor(Color.WHITE);
            sharesText.setText("Shares");
        }

        TextView sharePrice = (TextView) this.findViewById(R.id.share_price);
        double price = Double.parseDouble(sharePrice.getText().toString().substring(1).replaceAll(",", ""));

        TextView totalCost = (TextView) this.findViewById(R.id.total);
        totalCost.setText(Util.formatPriceText((price * mShares), true, true));

        if ((price * mShares) == 0) {
            totalCost.setTextColor(Color.GRAY);
        } else {
            totalCost.setTextColor(Color.WHITE);
        }
    }

    /**
     * Sets up the elements in the numeric keypad on the screen.
     */
    public void setUpNumericKeypad() {
        findViewById(R.id.zero).setOnTouchListener(this);
        findViewById(R.id.one).setOnTouchListener(this);
        findViewById(R.id.two).setOnTouchListener(this);
        findViewById(R.id.three).setOnTouchListener(this);
        findViewById(R.id.four).setOnTouchListener(this);
        findViewById(R.id.five).setOnTouchListener(this);
        findViewById(R.id.six).setOnTouchListener(this);
        findViewById(R.id.seven).setOnTouchListener(this);
        findViewById(R.id.eight).setOnTouchListener(this);
        findViewById(R.id.nine).setOnTouchListener(this);

        findViewById(R.id.backspace).setOnTouchListener(this);
    }

    /**
     * This information is text describing if its a buy or sell trade.
     * If it is a buy, it will also display the cash available.
     * If it is a sell, it will display the number of shares available to liquidate.
     */
    public void setTransactionHeader() {
        TextView topHeader = (TextView) this.findViewById(R.id.top_header);
        topHeader.setText(mIsBuyOrder ? "Buy " + mSymbol : "Sell " + mSymbol);

        TextView bottomHeader = (TextView) this.findViewById(R.id.bottom_header);
        String bottomHeaderText;
        if (mIsBuyOrder) {
            bottomHeaderText = Util.formatPriceText(Util.getCashAvailable(getBaseContext()),
                    true, true) + " Available";
        } else {
            DatabaseHelper db = DatabaseHelper.getInstance(this);
            int count = db.getShareCount(mSymbol);
            bottomHeaderText = Util.formatShareCountText(count);
            bottomHeaderText += (count != 1) ? " Shares Available " : " Share Available";
            db.close();
        }
        bottomHeader.setText(bottomHeaderText);
    }

    /**
     * This method is called whenever the user selects an element in the numeric keypad of the
     * screen.
     * Updates the quantity of stocks selected for the transaction, as well as updation the total
     * notional value.
     *
     * @param view the numeric key pad
     */
    public void keypadClick(View view) {
        String id = view.getResources().getResourceEntryName(view.getId());
        int nextDigit;
        switch (id) {
            case "zero":
                nextDigit = 0;
                break;
            case "one":
                nextDigit = 1;
                break;
            case "two":
                nextDigit = 2;
                break;
            case "three":
                nextDigit = 3;
                break;
            case "four":
                nextDigit = 4;
                break;
            case "five":
                nextDigit = 5;
                break;
            case "six":
                nextDigit = 6;
                break;
            case "seven":
                nextDigit = 7;
                break;
            case "eight":
                nextDigit = 8;
                break;
            case "nine":
                nextDigit = 9;
                break;
            default:
                nextDigit = -1;
        }

        if (nextDigit == -1) {
            // backspace
            mShares = mShares / 10;
        } else {
            mShares = mShares * 10 + nextDigit;
            if (mShares >= MAX) {
                mShares = nextDigit;
            }
        }
        updateReceipt();
    }

    /**
     * Displays confetti on the users screen.
     */
    public void displayConfetti() {
        KonfettiView konfettiView = this.findViewById(R.id.view_konfetti);
        konfettiView.build()
                .addColors(Color.rgb(168, 100, 253),
                        Color.rgb(41, 205, 255),
                        Color.rgb(120, 255, 68),
                        Color.rgb(255, 113, 141),
                        Color.rgb(253, 255, 106))
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 5f)
                .setFadeOutEnabled(true)
                .setTimeToLive(2000L)
                .addShapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
                .addSizes(new Size(12, 5f))
                .setPosition(-50f, konfettiView.getWidth() + 50f, -50f, -50f)
                .streamFor(300, 7500L);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                view.setAlpha(0.65f);
                view.animate().setDuration(50).scaleX(0.85f).scaleY(0.85f);
                return true;
            case MotionEvent.ACTION_UP:
                view.playSoundEffect(SoundEffectConstants.CLICK);
                view.setAlpha(1f);
                keypadClick(view);
                view.performClick();
                view.animate().setDuration(50).scaleX(1f).scaleY(1f);
                return true;
        }
        return false;
    }

}