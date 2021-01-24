package com.pocketprofit.source.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.pocketprofit.source.database.DatabaseTables.DailyTransactionProfitLog;
import com.pocketprofit.source.database.DatabaseTables.Stock;
import com.pocketprofit.source.database.DatabaseTables.StockSplits;
import com.pocketprofit.source.database.DatabaseTables.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static DatabaseHelper mInstance = null;
    public static final String DATABASE_NAME = "pocketprofit.db";
    public static final int DATABASE_VERSION = 3;

    public static DatabaseHelper getInstance(Context context) {
        // Using context.getApplicationContext(), which will ensure that I don't
        // accidentally leak an Activity's context.
        if (mInstance == null) {
            mInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return mInstance;
    }

    private DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
     }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_STOCK_TABLE = "CREATE TABLE " +
                Stock.TABLE_NAME + " (" +
                Stock._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Stock.COLUMN_NAME + " TEXT, " +
                Stock.COLUMN_SYMBOL + " TEXT, " +
                Stock.COLUMN_QUANTITY + " INTEGER, " +
                Stock.COLUMN_PRICE_PAID + " REAL, " +
                Stock.COLUMN_CURRENT_PRICE + " REAL, " +
                Stock.COLUMN_DATE + " TEXT" +
                ");";

        final String SQL_CREATE_TRANSACTION_TABLE = "CREATE TABLE " +
                Transaction.TABLE_NAME + " (" +
                Transaction._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Transaction.COLUMN_NAME + " TEXT, " +
                Transaction.COLUMN_SYMBOL + " TEXT, " +
                Transaction.COLUMN_ORDER_TYPE + " TEXT, " +
                Transaction.COLUMN_QUANTITY + " INTEGER, " +
                Transaction.COLUMN_PRICE + " REAL, " +
                Transaction.COLUMN_DATE + " TEXT" +
                ");";

        final String SQL_CREATE_STOCK_SPLITS_TABLE = "CREATE TABLE " +
                StockSplits.TABLE_NAME + " (" +
                StockSplits._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                StockSplits.COLUMN_SYMBOL + " TEXT, " +
                StockSplits.COLUMN_EX_DATE + " TEXT, " +
                StockSplits.COLUMN_DESCRIPTION + " TEXT, " +
                StockSplits.COLUMN_FROM_FACTOR + " INTEGER, " +
                StockSplits.COLUMN_TO_FACTOR + " INTEGER" +
                ");";

        final String SQL_CREATE_DAILY_STOCK_TRANSACTION_LOG_TABLE = "CREATE TABLE " +
                DailyTransactionProfitLog.TABLE_NAME + " (" +
                DailyTransactionProfitLog._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DailyTransactionProfitLog.COLUMN_DATE + " TEXT, " +
                DailyTransactionProfitLog.COLUMN_VALUE + " VALUE" +
                ");";

        sqLiteDatabase.execSQL(SQL_CREATE_STOCK_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_TRANSACTION_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_STOCK_SPLITS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_DAILY_STOCK_TRANSACTION_LOG_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Stock.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Transaction.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + StockSplits.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DailyTransactionProfitLog.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    /**
     * Clears all of the tables in the PocketProfit database.
     */
    public void clearTables() {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Stock.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Transaction.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + StockSplits.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DailyTransactionProfitLog.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    /**
     * Logs a new transaction history entry using the information given in as a parameter.
     * Returns true if the transaction history entry was successfully added, false otherwise.
     *
     * @param name      the security name.
     * @param symbol    the security ticker symbol.
     * @param orderType the order type of the transaction made.
     * @param quantity  the number of shares either bought or sold.
     * @param price     the price at which the shares were either bought or sold.
     * @param date      the date the transaction was made.
     * @return          a boolean indicating if the transaction history was successfully added.
     */
    public boolean logTransaction(String name, String symbol, String orderType, int quantity,
                                  double price, String date) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(Transaction.COLUMN_NAME, name);
        cv.put(Transaction.COLUMN_SYMBOL, symbol);
        cv.put(Transaction.COLUMN_ORDER_TYPE, orderType);
        cv.put(Transaction.COLUMN_QUANTITY, quantity);
        cv.put(Transaction.COLUMN_PRICE, price);
        cv.put(Transaction.COLUMN_DATE, date);

        long status = sqLiteDatabase.insert(Transaction.TABLE_NAME, null, cv);
        return (status >= 0);
    }

    /**
     * Makes a stock purchase by logging the information given as a parameter as a stock table
     * entry.
     * Returns true if the stock table entry was successfully added, false otherwise.
     * Typically, the price parameter is equal to the currentPrice parameter unless the stock
     * purchase is a free stock awarded to the user.
     *
     * @param name          the security name.
     * @param symbol        the security ticker symbol.
     * @param quantity      the number of shares bought.
     * @param price         the price paid per share.
     * @param currentPrice  the price per share.
     * @param date          the date the shares were bought.
     * @return              a boolean indicating if the transaction history was successfully added.
     */
    public boolean stockPurchase(String name, String symbol, int quantity, double price,
                                 double currentPrice, String date) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(Stock.COLUMN_NAME, name);
        cv.put(Stock.COLUMN_SYMBOL, symbol);
        cv.put(Stock.COLUMN_QUANTITY, quantity);
        cv.put(Stock.COLUMN_PRICE_PAID, price);
        cv.put(Stock.COLUMN_CURRENT_PRICE, currentPrice);
        cv.put(Stock.COLUMN_DATE, date);

        long status = sqLiteDatabase.insert(Stock.TABLE_NAME, null, cv);
        return (status >= 0);
    }

    /**
     * Sells an 'sharesToSell' amount of shares of 'symbol'.
     * PocketProfit uses the "first in, first out" (FIFO) method. This means that the user's
     * longest-held shares are sold first.
     * Returns the total amount of shares sold.
     *
     * @param symbol        the security ticker symbol.
     * @param sharesToSell  the number of shares to sell.
     * @param previousClose the previous close price.
     * @param latestDate    the shares were sold.
     * @return              the amount of shares sold.
     */
    public double liquidateStock(String symbol, int sharesToSell, double previousClose,
                                 String latestDate) {
        int sharesOwned = getShareCount(symbol);
        if (sharesOwned < sharesToSell) {
            return 0;
        }
        /*
        SELECT *
        FROM Stock.TABLE_NAME
        WHERE Stock.COLUMN_SYMBOL = 'symbol'
        ORDER BY Stock._ID ASC
         */
        String query = "SELECT * " +
                "FROM " + Stock.TABLE_NAME + "  " +
                "WHERE " + Stock.COLUMN_SYMBOL + " = '" + symbol + "' " +
                "ORDER BY " + Stock._ID + " ASC;";
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        boolean done = false;
        double amountChangedToday = 0.0;
        double amountSold = 0.0;
        try {
            while (!done && cursor.moveToNext()) {
                int quantity = cursor.getInt(cursor.getColumnIndex(Stock.COLUMN_QUANTITY));
                double price = cursor.getDouble(cursor.getColumnIndex(Stock.COLUMN_CURRENT_PRICE));
                double pricePaid = cursor.getDouble(cursor.getColumnIndex(Stock.COLUMN_PRICE_PAID));
                double id = cursor.getInt(cursor.getColumnIndex(Stock._ID));
                String date = cursor.getString(cursor.getColumnIndex(Stock.COLUMN_DATE));

                while (!done && quantity != 0) {
                    if (date.equals(latestDate)) {
                        amountChangedToday += (price - pricePaid);
                    } else {
                        amountChangedToday += (price - previousClose);
                    }
                    amountSold += price;
                    quantity--;
                    sharesToSell--;
                    if (sharesToSell == 0) {
                        done = true;
                    }
                }
                /*
                UPDATE Stock.TABLE_NAME
                SET Stock.COLUMN_QUANTITY = quantity
                WHERE Stock._ID = id AND Stock.COLUMN_SYMBOL = 'symbol';
                 */
                String updateEntry = "UPDATE " + Stock.TABLE_NAME + " " +
                        "SET " + Stock.COLUMN_QUANTITY + " = " + quantity + " " +
                        "WHERE " + Stock._ID + " = " + id + " AND " + Stock.COLUMN_SYMBOL + " = '" +
                        symbol + "';";
                sqLiteDatabase.execSQL(updateEntry);
            }
        } finally {
            cursor.close();
        }
        /*
        Cleaning:
        DELETE FROM Stock.TABLE_NAME
        WHERE Stock.QUANTITY = 0;
         */
        String clean = "DELETE FROM " + Stock.TABLE_NAME + " " +
                "WHERE " + Stock.COLUMN_QUANTITY + " = " + 0 + ";";
        sqLiteDatabase.execSQL(clean);
        sqLiteDatabase.close();

        logTodaysStockSaleProfit(latestDate, amountChangedToday);

        return amountSold;
    }

    /**
     * Returns the total profit or loss of the shares sold today.
     * If the returned value is < 0, it means that the shares were sold today at a net loss.
     * If the returned value is > 0, it means that the shares were sold today at a net profit.
     *
     * @return  the total profit/loss of shares sold today.
     */
    public double getTodaysStockSaleProfit() {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        String query = "SELECT * " +
                "FROM " + DailyTransactionProfitLog.TABLE_NAME + ";";
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        double value = 0;
        try {
            if (cursor.moveToFirst()) {
                value = cursor.getDouble(cursor.getColumnIndex(DailyTransactionProfitLog.COLUMN_VALUE));
            }
        } finally {
            cursor.close();
            sqLiteDatabase.close();
        }
        return value;
    }

    /**
     * Returns the daily profit/loss of stock sales for the date given.
     * If the date on record matches the date given, that daily stock liquidation profit/loss is
     * returns.
     * If not, or if the record contains no information, than a brand new record is set with the
     * date given and a value of zero. In this case, zero will be returned.
     *
     * @param date  the date of the stock transaction made.
     * @return      the daily profit/loss from stock sales on the date given.
     */
    public double getTodaysStockSaleProfit(String date) {
        /*
        if date is not equal to the current dated logged on the DailyTransactionProfitLog table.
        the value column is reset to zero and the date column is set to the date provided.
         */
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String query = "SELECT * " +
                "FROM " + DailyTransactionProfitLog.TABLE_NAME + ";";
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);

        double value = 0.0;
        try {
            if (cursor.moveToFirst()) {
                String recordedDate = cursor.getString(cursor.getColumnIndex(DailyTransactionProfitLog.COLUMN_DATE));
                if (recordedDate.equals(date)) {
                    value = cursor.getDouble(cursor.getColumnIndex(DailyTransactionProfitLog.COLUMN_VALUE));
                } else {
                    /*
                    UPDATE DailyTransactionProfitLog.TABLE_NAME
                    SET DailyTransactionProfitLog.COLUMN_DATE = 'date',
                        DailyTransactionProfitLog.COLUMN_VALUE = 0;
                     */
                    query = "UPDATE " + DailyTransactionProfitLog.TABLE_NAME + " " +
                            "SET " + DailyTransactionProfitLog.COLUMN_DATE + " = '" + date + "', " +
                            DailyTransactionProfitLog.COLUMN_VALUE + " = 0;";
                    sqLiteDatabase.execSQL(query);
                }
            }
        } finally {
            cursor.close();
        }
        return value;
    }

    /**
     * Updates the day's profit/loss from selling stock given the date and value given as a
     * parameter. This value is used to calculate the daily portfolio value change information
     * on the main page of PocketProfit.
     * To update, the value is added to the current value stored in record.
     * If the date given is not the date currently in record, or if the record is empty, a brand
     * new record is created for the date and value given.
     *
     * @param date  the date of the stock transaction made.
     * @param value the profit or loss to be added to the daily profit/loss.
     */
    public void logTodaysStockSaleProfit(String date, double value) {
        /*
        +value = stock sold at a profit
        -value = stock sold at a loss
         */
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String query = "SELECT * " +
                "FROM " + DailyTransactionProfitLog.TABLE_NAME + ";";
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        try {
            if (cursor.getCount() == 0) {
                ContentValues cv = new ContentValues();
                cv.put(DailyTransactionProfitLog.COLUMN_DATE, date);
                cv.put(DailyTransactionProfitLog.COLUMN_VALUE, value);

                sqLiteDatabase.insert(DailyTransactionProfitLog.TABLE_NAME, null, cv);
            } else {
                if (cursor.moveToFirst()) {
                    String recordedDate = cursor.getString(cursor.getColumnIndex(DailyTransactionProfitLog.COLUMN_DATE));
                    double recordedValue = cursor.getDouble(cursor.getColumnIndex(DailyTransactionProfitLog.COLUMN_VALUE));

                    if (recordedDate.equals(date)) {
                        /*
                        UPDATE DailyTransactionProfitLog.TABLE_NAME
                        SET DailyTransactionProfitLog.COLUMN_VALUE = updatedValue;
                        */
                        double updatedValue = recordedValue + value;

                        query = "UPDATE " + DailyTransactionProfitLog.TABLE_NAME + " " +
                                "SET " + DailyTransactionProfitLog.COLUMN_VALUE + " = " + updatedValue + ";";
                        sqLiteDatabase.execSQL(query);
                    } else {
                        /*
                        UPDATE DailyTransactionProfitLog.TABLE_NAME
                        SET DailyTransactionProfitLog.COLUMN_DATE = 'date',
                            DailyTransactionProfitLog.COLUMN_VALUE = 0;
                         */
                        query = "UPDATE " + DailyTransactionProfitLog.TABLE_NAME + " " +
                                "SET " + DailyTransactionProfitLog.COLUMN_DATE + " = '" + date + "', " +
                                DailyTransactionProfitLog.COLUMN_VALUE + " = " + value + ";";
                        sqLiteDatabase.execSQL(query);
                    }
                }
            }
        } finally {
            cursor.close();
            sqLiteDatabase.close();
        }
    }

    /**
     * Returns a map containing the key-value pair of the stocks the user currently owns with the
     * current total market value of that holding.
     *
     * @return  Map containing the stock->(total equity) association of all the stocks the user
     *          currently has in his/her portfolio.
     */
    public Map<String, Double> getStocksOwned() {
        Map<String, Double> stocksOwned = new HashMap<>();
        /*
        SELECT Stock.COLUMN_SYMBOL,  SUM(Stock.COLUMN_QUANTITY * Stock.COLUMN_CURRENT_PRICE) AS equity
        FROM Stock.TABLE_NAME
        GROUP BY Stock.COLUMN_SYMBOL;
         */
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        String query = "SELECT " + Stock.COLUMN_SYMBOL + ", SUM(" + Stock.COLUMN_QUANTITY +
                                                "*" + Stock.COLUMN_CURRENT_PRICE + ") AS equity " +
                "FROM " + Stock.TABLE_NAME + " " +
                "GROUP BY " + Stock.COLUMN_SYMBOL + ";";
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        while (cursor.moveToNext()) {
            String symbol = cursor.getString(cursor.getColumnIndex(Stock.COLUMN_SYMBOL));
            double equity = cursor.getDouble(cursor.getColumnIndex("equity"));
            stocksOwned.put(symbol, equity);
        }
        cursor.close();
        return stocksOwned;
    }

    /**
     * Returns a cursor to the table of all the buy/sell transactions that the user has made.
     *
     * @return  Cursor that references the transactions made by the user.
     */
    public Cursor getAllTransactions() {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        String query = "SELECT * " +
                "FROM " + Transaction.TABLE_NAME + " " +
                "ORDER BY " + Transaction._ID + " DESC;";
        return sqLiteDatabase.rawQuery(query, null);
    }

    /**
     * Returns a cursor to the table of all the buy/sell transactions that the user has made for
     * the given symbol.
     *
     * @param symbol    the security ticker symbol.
     * @return          Cursor that references the transactions made by the user.
     */
    public Cursor getAllTransactions(String symbol) {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        String query = "SELECT * " +
                "FROM " + Transaction.TABLE_NAME + " " +
                "WHERE " + Transaction.COLUMN_SYMBOL + " = '" + symbol + "' " +
                "ORDER BY " + Transaction._ID + " DESC;";
        return sqLiteDatabase.rawQuery(query, null);
    }

    /**
     * Returns a stack of all the buy order notional values of the given stock on the given date.
     * (ex. if the user made two buy order transactions of MSFT on Oct 3rd, 2020 at worth $200 (1
     * share) and $600 (3 shares), then a call of getSharesBoughtToday('MSFT', 'Oct 3rd 2020') will
     * return {200, 600}.
     * If the user does not own the stock given, or did not make any purchases of the stock on the
     * date given, will return empty stack.
     *
     * @param symbol    the security ticker symbol.
     * @param date      today's date
     * @return          stack containing all of the purchase prices of the given stock on the
     *                  date provided.
     */
    public Stack<Double> getSharesBoughtToday(String symbol, String date) {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        /*
        SELECT *
        FROM Stock.TABLE_NAME
        WHERE Stock.COLUMN_SYMBOL = 'symbol' AND Stock.COLUMN_DATE = 'name';
         */
        String query = "SELECT * " +
                "FROM " + Stock.TABLE_NAME + " " +
                "WHERE " + Stock.COLUMN_SYMBOL + " = '" + symbol + "' " +
                "AND " + Stock.COLUMN_DATE + " = '" + date + "';";

        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        Stack<Double> stockPurchasePrices = new Stack<>();

        try {
            while (cursor != null && cursor.moveToNext()) {
                int quantity = cursor.getInt(cursor.getColumnIndex(Stock.COLUMN_QUANTITY));
                double pricePaid = cursor.getDouble(cursor.getColumnIndex(Stock.COLUMN_PRICE_PAID));
                while (quantity != 0) {
                    stockPurchasePrices.push(pricePaid);
                    quantity--;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        sqLiteDatabase.close();
        return stockPurchasePrices;
    }

    /**
     * Returns all the stock symbols the user currently owns.
     *
     * @return  a list of all the stock symbols the user has in his/her portfolio.
     */
    public List<String> getAllStockSymbols() {
        List<String> symbols = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();

        String query = "SELECT " + Stock.COLUMN_SYMBOL + " " +
                "FROM " + Stock.TABLE_NAME + " " +
                "GROUP BY " + Stock.COLUMN_SYMBOL + ", " + Stock.COLUMN_CURRENT_PRICE + " " +
                "ORDER BY " + Stock._ID + " ASC;";
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String symbol = cursor.getString(cursor.getColumnIndex(Stock.COLUMN_SYMBOL));
                symbols.add(symbol);
            }
            cursor.close();
        }
        return symbols;
    }

    /**
     * Returns the total equity of the security in the users portfolio given by its ticker symbol
     * as a parameter.
     * If the user does not have the symbol in his/her portfolio, it will return -1.
     *
     * @param symbol    the security ticker symbol.
     * @return          the total
     */
    public double getStockEquity(String symbol) {
        /*
        SELECT SUM(Stock.COLUMN_QUANTITY * Stock.COLUMN_CURRENT_PRICE) AS equity
        FROM Stock.TABLE_NAME
        WHERE Stock.COLUMN_SYMBOL = 'symbol';
         */
        String query = "SELECT SUM(" + Stock.COLUMN_QUANTITY + " * " + Stock.COLUMN_CURRENT_PRICE +
                ") AS equity " +
                "FROM " + Stock.TABLE_NAME + "  " +
                "WHERE " + Stock.COLUMN_SYMBOL + " = '" + symbol + "';";
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            double equity = cursor.getDouble(cursor.getColumnIndex("equity"));
            cursor.close();
            return equity;
        }
        return -1;
    }

    /**
     * Returns the total amount the user has invested into the security given by its ticker symbol
     * as a parameter.
     * If the user does not have the symbol in his/her portfolio, it will return -1.
     *
     * @param symbol    the security ticker symbol.
     * @return          the total price paid to purchase shares of the security given by its symbol.
     */
    public double getStockCost(String symbol) {
        /*
        SELECT SUM(Stock.COLUMN_QUANTITY * Stock.COLUMN_PRICE_PAID) AS equity
        FROM Stock.TABLE_NAME
        WHERE Stock.COLUMN_SYMBOL = 'symbol';
         */
        String query = "SELECT SUM(" + Stock.COLUMN_QUANTITY + " * " + Stock.COLUMN_PRICE_PAID +
                ") AS cost " +
                "FROM " + Stock.TABLE_NAME + "  " +
                "WHERE " + Stock.COLUMN_SYMBOL + " = '" + symbol + "';";
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            double cost = cursor.getDouble(cursor.getColumnIndex("cost"));
            cursor.close();
            return cost;
        }
        return -1;
    }

    /**
     * Returns the total equity currently in the user's possession.
     *
     * @return  the total market value of all the stocks the user owns in his/her portfolio.
     */
    public double getTotalEquity() {
        /*
        SELECT SUM(Stock.COLUMN_QUANTITY * Stock.COLUMN_CURRENT_PRICE)
        FROM Stock.TABLE_NAME
         */
        String query = "SELECT SUM(" + Stock.COLUMN_QUANTITY + " * " +
                Stock.COLUMN_CURRENT_PRICE + ") AS equity " +
                "FROM " + Stock.TABLE_NAME + ";";
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            double equity = cursor.getDouble(cursor.getColumnIndex("equity"));
            cursor.close();
            return equity;
        }
        return 0;
    }

    /**
     * Returns the current (most recent) price of a portfolio stock given by its symbol
     * as a parameter.
     * If the user does not own the stock given, returns -1.
     *
     * @param symbol    the security ticker symbol.
     * @return          the current price of the stock given.
     */
    public double getCurrentPrice(String symbol) {
        /*
        SELECT Stock.CURRENT_PRICE
        FROM Stock.TABLE_NAME
        WHERE Stock.COLUMN_SYMBOL = 'symbol'
        GROUP BY Stock.COLUMN_SYMBOL, STOCK.COLUMN_CURRENT_PRICE;
         */
        String query = "SELECT " + Stock.COLUMN_CURRENT_PRICE + " " +
                "FROM " + Stock.TABLE_NAME + " " +
                "WHERE " + Stock.COLUMN_SYMBOL + " = '" + symbol + "' " +
                "GROUP BY " + Stock.COLUMN_SYMBOL + ", " + Stock.COLUMN_CURRENT_PRICE + ";";
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            double currentPrice =
                    cursor.getDouble(cursor.getColumnIndex(Stock.COLUMN_CURRENT_PRICE));
            cursor.close();
            return currentPrice;
        }
        return -1;
    }

    /**
     * Returns the average purchase price of a portfolio stock given by its ticker symbol
     * as a parameter.
     * If the user does not own the stock given, returns -1.
     *
     * @param symbol    the security ticker symbol.
     * @return          the average purchase price of the stock.
     */
    public double getAverageCost(String symbol) {
        /*
        SELECT SUM(Stock.COLUMN_QUANTITY * Stock.COLUMN_PRICE_PAID) / SUM(Stock.QUANTITY) AS average
        FROM Stock.TABLE_NAME
        WHERE Stock.COLUMN_SYMBOL = 'symbol';
         */
        String query = "SELECT SUM(" + Stock.COLUMN_QUANTITY + " * " +
                Stock.COLUMN_PRICE_PAID + ") " +
                "/ SUM(" + Stock.COLUMN_QUANTITY + ") AS average" + " " +
                "FROM " + Stock.TABLE_NAME + "  " +
                "WHERE " + Stock.COLUMN_SYMBOL + " = '" + symbol + "';";
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            double averageCost = cursor.getDouble(cursor.getColumnIndex("average"));
            cursor.close();
            return averageCost;
        }
        return -1;
    }

    /**
     * Updates the latest price of the given stock symbol to the latest price in the Stocks table.
     *
     * @param symbol        the security ticker symbol.
     * @param latestPrice   the latest price of the given symbol.
     */
    public void updateCurrentPrice(String symbol, double latestPrice) {
        /*
        UPDATE Stock.TABLE_NAME
        SET Stock.COLUMN_CURRENT_PRICE = latestPrice
        WHERE Stock.COLUMN_SYMBOL = 'symbol' AND latestPrice != 0.00;
         */
        String query = "UPDATE " + Stock.TABLE_NAME + " " +
                "SET " + Stock.COLUMN_CURRENT_PRICE + " = " + latestPrice + " " +
                "WHERE " + Stock.COLUMN_SYMBOL + " = '" + symbol + "' AND " +
                    latestPrice + " != 0;";
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        sqLiteDatabase.execSQL(query);
    }

    /**
     * Checks to see if the given ticker symbol is in the user's portfolio. If so, returns true.
     * Otherwise, returns false.
     *
     * @param symbol    the security ticker symbol.
     * @return          true if the stock is currently in the user's portfolio, false otherwise.
     */
    public boolean userOwns(String symbol) {
        /*
        SELECT COUNT(*) AS count
        FROM Stock.TABLE_NAME
        WHERE Stock.COLUMN_SYMBOL = 'symbol';
         */
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        String query = "SELECT COUNT(*) AS count " +
                "FROM " + Stock.TABLE_NAME + " " +
                "WHERE " + Stock.COLUMN_SYMBOL + " = '" + symbol + "';";
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            int count = cursor.getInt(cursor.getColumnIndex("count"));
            cursor.close();
            return count > 0;
        }
        return false;
    }

    /**
     * Checks to see if the given ticker symbol is in the user's transaction history. Meaning,
     * the user has either bought, sold, or received (free stock activity) the given symbol.
     * If so, returns true. Otherwise, false.
     *
     * @param symbol    the security ticker symbol.
     * @return          true if the stock is in the user's transaction history, false otherwise.
     */
    public boolean inStockTransactionHistory(String symbol) {
        /*
        SELECT COUNT(*) AS count
        FROM Transactions.TABLE_NAME
        WHERE Stock.COLUMN_SYMBOL = 'symbol';
         */
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        String query = "SELECT COUNT(*) AS count " +
                "FROM " + Transaction.TABLE_NAME + " " +
                "WHERE " + Transaction.COLUMN_SYMBOL + " = '" + symbol + "';";
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            int count = cursor.getInt(cursor.getColumnIndex("count"));
            cursor.close();
            return count > 0;
        }
        return false;
    }

    /**
     * Given the stock symbol, returns the full name of that security.
     * Assuming that the stock is currently owned in the user's portfolio.
     *
     * @param symbol    the security ticker symbol.
     * @return          returns the security name.
     */
    public String getCompanyName(String symbol) {
        /*
        SELECT Stock.COLUMN_NAME AS name
        FROM Stock.TABLE_NAME
        WHERE Stock.COLUMN_SYMBOL = 'symbol'
        LIMIT 1;
         */
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        String query = "SELECT " + Stock.COLUMN_NAME + " AS name " +
                "FROM " + Stock.TABLE_NAME + " " +
                "WHERE " + Stock.COLUMN_SYMBOL + " = '" + symbol + "' " +
                "LIMIT 1;";
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        String companyName = null;
        if (cursor != null && cursor.moveToFirst()) {
            companyName = cursor.getString(cursor.getColumnIndex(Stock.COLUMN_NAME));
            cursor.close();
        }
        sqLiteDatabase.close();
        return companyName;
    }

    /**
     * Given a stock symbol, returns the number of shares of that stock the user currently owns.
     * If none are owned, returns zero.
     *
     * @param symbol    the security ticker symbol.
     * @return          returns the number of shares owned of 'symbol'.
     */
    public int getShareCount(String symbol) {
        /*
        SELECT SUM('Stock.TABLE_QUANTITY') AS count
        FROM 'Stock.TABLE_NAME'
        WHERE 'Stock.TABLE_SYMBOL' = symbol;
         */
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        String query = "SELECT SUM (" + Stock.COLUMN_QUANTITY + ") AS count " +
                       "FROM " + Stock.TABLE_NAME + " " +
                       "WHERE " + Stock.COLUMN_SYMBOL + " = '" + symbol + "';";
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        int count;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(cursor.getColumnIndex("count"));
            cursor.close();
        } else {
            count = 0;
        }
        sqLiteDatabase.close();
        return count;
    }

    /**
     * Returns true or false depending if the user's transaction history is empty or not.
     * An empty transaction history is defined as one where the user has not yet made any
     * buy or sell transactions.
     *
     * @return  true if transaction history is empty, false otherwise.
     */
    public boolean isTransactionHistoryEmpty() {
        /*
        SELECT COUNT(*) FROM Transactions.TABLE_NAME
         */
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        boolean empty = true;
        String query = "SELECT COUNT(*) FROM " + Transaction.TABLE_NAME + ";";
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            empty = (cursor.getInt (0) == 0);
        }
        if (cursor != null) {
            cursor.close();
        }

        return empty;
    }

}