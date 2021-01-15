package com.pocketprofit.source.database;

import android.provider.BaseColumns;

/**
 * Information about all the tables that will be used in the PocketProfit database.
 */
public class DatabaseTables {

    // No instances of this class shall be created.
    // This class exists so that we can utilize the public constants
    // of the inner classes which represent the tables in our database.
    private DatabaseTables() {}

    // all of the stocks that the user currently owns.
    public static final class Stock implements BaseColumns {
        public static final String TABLE_NAME = "stocks";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_SYMBOL = "symbol";
        public static final String COLUMN_QUANTITY = "quantity";
        public static final String COLUMN_PRICE_PAID = "price";
        public static final String COLUMN_CURRENT_PRICE = "current_price";
        public static final String COLUMN_DATE = "date";
    }

    // record of all the stock transactions that have been made.
    public static final class Transaction implements BaseColumns {
        public static final String TABLE_NAME = "transactions";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_SYMBOL = "symbol";
        public static final String COLUMN_ORDER_TYPE = "order_type";
        public static final String COLUMN_QUANTITY = "quantity";
        public static final String COLUMN_PRICE = "price";
        public static final String COLUMN_DATE = "date";
    }

    // all of the pending stock splits that are to occur.
    public static final class StockSplits implements  BaseColumns {
        public static final String TABLE_NAME = "stockSplits";
        public static final String COLUMN_SYMBOL = "symbol";
        public static final String COLUMN_EX_DATE = "exDate";
        public static final String COLUMN_DESCRIPTION = "description";  // ex. '7-for-1 split'
        public static final String COLUMN_FROM_FACTOR = "fromFactor";
        public static final String COLUMN_TO_FACTOR = "toFactor";
    }

    // stores the value of the profit/loss of all the transactions that have been made in
    // the most recent day of trading.
    public static final class DailyTransactionProfitLog implements  BaseColumns {
        public static final String TABLE_NAME = "dailyTransactionLog";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_VALUE = "value";
    }

}