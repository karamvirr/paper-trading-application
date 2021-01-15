package com.pocketprofit.source;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pocketprofit.source.database.DatabaseHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class Util {
    public static final String TAG = "Util";

    public static final List<String> EXCHANGES_NOT_SUPPORTED =
            Collections.unmodifiableList(Arrays.asList("MIC", "PINX", "US OTC", "OTC PINK CURRENT",
                    "OTC GREY MARKET", "OTCQX MARKETPLACE", "OTC PINK LIMITED",
                    "OTC PINK NO INFORMATION", "CAVEAT EMPTOR", "OTCQB MARKETPLACE"));

    // these sectors cause 503 errors (response from PocketProfit is too large to process)
    public static final List<String> SECTORS_NOT_SUPPORTED =
            Collections.unmodifiableList(Arrays.asList("Miscellaneous", "Government", "Finance", "N/A"));

    public static final String MARKET_HOURS_URL =
            "https://www.tradinghours.com/exchanges/nasdaq/trading-hours";

    public static final String EXTRA_SYMBOL =
            "com.example.application.pocketprofit.EXTRA_SYMBOL";
    public static final String EXTRA_NAME =
            "com.example.application.pocketprofit.EXTRA_NAME";
    public static final String EXTRA_STOCK_PRICE =
            "com.example.application.pocketprofit.EXTRA_STOCK_PRICE";
    public static final String EXTRA_PREVIOUS_CLOSE =
            "com.example.application.pocketprofit.EXTRA_PREVIOUS_CLOSE";
    public static final String EXTRA_IS_BUY_TRANSACTION =
            "com.example.application.pocketprofit.EXTRA_IS_BUY_TRANSACTION";
    public static final String EXTRA_DATE =
            "com.example.application.pocketprofit.EXTRA_DATE";
    public static final String EXTRA_SECTOR =
            "com.example.application.pocketprofit.EXTRA_SECTOR";

    public static final String SHARED_PREFERENCES = "sharedPreferences";
    public static final String CASH_VALUE = "cashValue";
    public static final String WATCHLIST_STOCKS = "watchlist";
    public static final String DATE_JOINED = "dateJoined";

    public static final String NOT_SET = "Date joined not set.";

    public static boolean DISPLAY_ADS = true;
    public static final double AD_FREQUENCY = 8;  // % of encountering an ad.

    public static final float OPACITY = 0.5f;

    public static final double STARTING_VALUE = 15000;

    private static RequestQueue mRequestQueue;

    // No instances of this class shall be created.
    private Util() {}

    /**
     * Displays a toast message on the users screen containing the text given as a parameter.
     *
     * @param context the context of which to display the toast message.
     * @param message the content of the toast message.
     */
    public static void displayToast(Context context, String message) {
        final Toast t = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        t.show();
    }

    /**
     * Calculates the value and percentage increase/decrease from priceA to priceB and uses that
     * information to build a string that is then returned.
     *
     * @param priceA the starting price.
     * @param priceB the ending price.
     * @param includeSign boolean which indicates if the percentage should have a +/- prefix.
     * @return       string in the form '(x/-)$(x,xxx.xx) ((x/-)?(x.xx)%)'
     */
    public static String getPercentChangeText(double priceA, double priceB, boolean includeSign) {
        String change = "";
        double delta = Math.abs(priceB - priceA);
        change += (priceA <= priceB) ? "+$" : "-$";
        change += Util.formatPriceText(delta, false, false) + " (";
        if (includeSign) {
            change += (priceA <= priceB) ? "+" : "-";
        }
        double deltaPercentage = Math.abs(((priceB - priceA) / priceA) * 100.00);
        change += Util.formatPercentageText(deltaPercentage) + ")";
        return change;
    }

    /**
     * Formats the value given in as a parameter as comma separated text.
     * Includes boolean parameters to customize the formatted text.
     *
     * @param value the value to format.
     * @param includeDollarSign determines if a dollar sign should be included in a returned string.
     *                          if true, includes dollar sign. otherwise, does not.
     * @param precision         determines if the number of sig figs is increased if the value is
     *                          less than 1 (&& >= 0). if true, it will go 4 places past the decimal
     *                          if false, then only 2 places past decimal in this case.
     * @return                  returns a formatted string of the value passed in as a parameter.
     */
    public static String formatPriceText(double value, boolean includeDollarSign,
                                                                                boolean precision) {
        DecimalFormat formatter;
        if (precision && (value > 0.00 && value < 1.00)) {
            formatter = new DecimalFormat("0.0000");
        } else {
            formatter = new DecimalFormat("#,##0.00");
        }
        return (includeDollarSign ? "$" : "") + formatter.format(value);
    }

    /**
     * Returns the integer given in as a parameter as comma separated text.
     *
     * @param shareCount    the number of shares to format
     * @return              formatted sharecount with commas.
     */
    public static String formatShareCountText(int shareCount) {
        return new DecimalFormat("#,###").format(shareCount);
    }

    /**
     * Formats the given market cap and returns that text.
     * Uses units to shorten the text. (ex. 1500000 -> 1.5M)
     *
     * @param marketCap the market cap as a double.
     * @return          returns a the market cap in formatted text.
     */
    public static String formatMarketCap(double marketCap) {
        long million = 1000000L;
        long billion = 1000000000L;
        long trillion = 1000000000000L;
        long divisor;
        String units;
        if (marketCap >= trillion) {
            divisor = trillion;
            units = "T";
        } else if (marketCap >= billion) {
            divisor = billion;
            units = "B";
        } else {
            divisor = million;
            units = "M";
        }
        return new DecimalFormat("#,###.00")
                .format((double) (marketCap) / divisor) + units;
    }

    /**
     * Formats the given percentage value and returns that text.
     *
     * @param percent   the percent value to format.
     * @return          a formatted text of the percent.
     */
    public static String formatPercentageText(double percent) {
        return new DecimalFormat("#,##0.00").format(percent) + "%";
    }

    /**
     * Formats the given volume and returns that text.
     * Uses units to shorten the text. (ex. 1,500 -> 1.5k)
     *
     * @param volume    the volume value to format.
     * @return          a formatted text of the volume.
     */
    public static String formatVolume(long volume) {
        long thousand = 1000L;
        long million = 1000000L;
        long billion = 1000000000L;
        long divisor;
        String units;
        if (volume >= billion) {
            divisor = billion;
            units = "B";
        } else if (volume >= million){
            divisor = million;
            units = "M";
        } else if (volume >= thousand){
            divisor = thousand;
            units = "K";
        } else {
            return "" + volume;
        }
        return "" + new DecimalFormat("#,###.00")
                .format((double) volume / divisor) + units;
    }

    /**
     * Uses the given context to retrieve and return the current portfolio value.
     *
     * @param context   the context of the activity.
     * @return          the current portfolio value.
     */
    public static double getPortfolioValue(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(Util.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        double cashValue =
                Double.longBitsToDouble(sharedPreferences
                        .getLong(Util.CASH_VALUE, Double.doubleToLongBits(Util.STARTING_VALUE)));
        // calculate asset value
        DatabaseHelper db = DatabaseHelper.getInstance(context);        // close ;
        double equityValue = db.getTotalEquity();
        return cashValue + equityValue;
    }

    /**
     * Uses the given context to retrieve and return the current cash balance.
     *
     * @param context   the context of the activity.
     * @return          the amount of cash available.
     */
    public static double getCashAvailable(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(Util.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        double cashValue =
                Double.longBitsToDouble(sharedPreferences
                        .getLong(Util.CASH_VALUE, Double.doubleToLongBits(Util.STARTING_VALUE)));
        return cashValue;
    }

    /**
     * Uses the given context to set the date the user joined PocketProfit.
     * Joined in this case is defined as when the user launched PocketProfit for the first time.
     *
     * @param context   the context of the activity.
     */
    public static void setDateJoined(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(Util.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        if (sharedPreferences.getString(Util.DATE_JOINED, NOT_SET).equals(NOT_SET)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Util.DATE_JOINED, Util.getTodaysDate());
            editor.apply();
        }
    }

    /**
     * Uses the given context to retrieve and return the date the user joined PocketProfit.
     * Joined in this case is defined as when the user launched PocketProfit for the first time.
     *
     * @param context   the context of the activity.
     * @return          the date the user joined PocketProfit.
     */
    public static String getDateJoined(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(Util.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Util.DATE_JOINED, Util.getTodaysDate());
    }

    /**
     * Uses the given context to reset the cash balance of the portfolio to the starting cash
     * value.
     *
     * @param context   the context of the activity.
     */
    public static void resetCashAvailable(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(Util.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putLong(Util.CASH_VALUE, Double.doubleToLongBits(Util.STARTING_VALUE));
        editor.apply();
    }

    /**
     * Uses the given context to retrieve and return the set of stocks the user currently has
     * under his/her watchlist.
     *
     * @param context   the context of the activity.
     * @return          the set of stocks under watch.
     */
    public static Set<String> getWatchlist(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(Util.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getStringSet(Util.WATCHLIST_STOCKS, new TreeSet<String>());
    }

    /**
     * Uses the context to add the positive/negative value given as a parameter to the user's
     * current portfolio cash balance.
     *
     * @param context   the context of the activity.
     * @param change    the value to add to the portfolio (pos/neg value).
     */
    public static void updateCashAvailable(Context context, double change) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(Util.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        double cashValue =
                Double.longBitsToDouble(sharedPreferences.getLong
                        (Util.CASH_VALUE,
                        Double.doubleToLongBits(Util.STARTING_VALUE)));

        SharedPreferences.Editor editor = sharedPreferences.edit();
        double updatedValue = cashValue + change;
        editor.putLong(Util.CASH_VALUE, Double.doubleToLongBits(updatedValue));

        editor.apply();
    }

    /**
     * Uses the context to clear the watchlist.
     * It clears by removing every stock that the user currently has in his/her watchlist.
     *
     * @param context   the context of the activity.
     */
    public static void clearWatchlist(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(Util.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putStringSet(Util.WATCHLIST_STOCKS, new TreeSet<String>());
        editor.apply();
    }

    /**
     * Uses the context and the given ticker symbol to update the watchlist.
     * If the symbol given is not in the watchlist, it is added to the list.
     * Otherwise, if the symbol is in the watchlist, it is removed from the list.
     * In either case, a toast message is displayed to the given context giving feedback as to
     * what event occurred.
     *
     * @param context   the context of the activity.
     * @param symbol    the symbol to update in the watchlist.
     */
    public static void updateWatchList(Context context, String symbol) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(Util.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> watchlist =
                new TreeSet<String>
                    (sharedPreferences.getStringSet(Util.WATCHLIST_STOCKS, new TreeSet<String>()));

        if (watchlist.contains(symbol)) {
            Util.displayToast(context, symbol + " removed from the watchlist.");
            watchlist.remove(symbol);
        } else {
            Util.displayToast(context, symbol + " added to the watchlist.");
            watchlist.add(symbol);
        }

        editor.putStringSet(Util.WATCHLIST_STOCKS, watchlist);
        editor.apply();
    }

    /**
     * Uses the context to check if the given ticker symbol is currently in the watchlist. If so,
     * returns true, false otherwise.
     *
     * @param context   the context of the activity.
     * @param symbol    the ticker symbol to check against the watchlist.
     * @return          true if the symbol is in the watchlist, false otherwise.
     */
    public static boolean currentlyOnWatchlist(Context context, String symbol) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(Util.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        Set<String> watchlist =
                sharedPreferences.getStringSet(Util.WATCHLIST_STOCKS, new TreeSet<String>());
        return watchlist.contains(symbol);
    }

    /**
     * Returns today's date in the 'MMM DD(+ suffix), YYYY' format.
     *
     * @return  today's date.
     */
    public static String getTodaysDate() {
        Calendar calendar = Calendar.getInstance();
        String month = calendar
                .getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR);
        return month + " " + dayOfMonth + Util.getDayOfMonthSuffix(dayOfMonth) + ", " + year;
    }

    /**
     * Converts the date given to the date format that is used by PocketProfit and returns it.
     *
     * @param date  the date text to normalize.
     * @return      the normalized date format that is used by PocketProfit.
     */
    public static String normalizeDate(String date) {
        // the date given is usually the date retrieved from the JSON response of an IEX Cloud API
        // call. this date is in a different format than the format used by PocketProfit. so this
        // method will normalize that for us.
        // September 18, 2020 -> September 18th, 2020 - non market hours
        // HH:MM:MS AM/PM     -> (today's date) - during market hours.
        if (!date.contains("" + Calendar.getInstance().get(Calendar.YEAR))) {
            return Util.getTodaysDate();
        }

        String[] split = date.split(" ");
        String month = split[0];
        int dayOfMonth = Integer.parseInt(split[1].substring(0, split[1].indexOf(",")));
        String year = split[2];

        return month + " " + dayOfMonth + Util.getDayOfMonthSuffix(dayOfMonth) + ", " + year;
    }

    /**
     * pre-condition: dayOfMonth >= 1 && dayOfMonth <= 31
     * Uses the given day of month value to determine and return the associated suffix ('st', 'nd',
     * 'rd', 'th').
     *
     * @param dayOfMonth    the day of month value.
     * @return              the suffix associated with the parameter given.
     */
    public static String getDayOfMonthSuffix(int dayOfMonth) {
        if (dayOfMonth >= 11 && dayOfMonth <= 13) {
            return "th";
        }
        switch (dayOfMonth % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

    /**
     * Converts military time to standard time and returns it.
     *
     * @param time  the military time text.
     * @return      the result of converting the military time to standard time.
     */
    public static String convertMilitaryToStandard(String time) {
        if (time == null) {
            return time;
        }
        String[] split = time.split(":");
        String hour = split[0];
        String minutes = split[1];
        String meridian = "AM";

        if (hour.substring(0,2).equals("00")) {
            hour = "12";
        } else if (hour.substring(0,1).equals("1") || hour.substring(0,1).equals("2")) {
            meridian = "PM";
            Integer militaryHour = Integer.parseInt(hour);
            Integer convertedHour = null;

            if (militaryHour > 12) {
                convertedHour = (militaryHour - 12);

                hour = String.valueOf(convertedHour);
            }
        }
        time = hour + ":" + minutes + " " + meridian;
        return time;
    }

    /**
     * Makes an API call to communicate with the PocketProfit server by using the given endpoint.
     * The PocketProfit server will then fetch JSON data based on the endpoint given.
     * If the PocketProfit server is able to successfuly retrieve and send the JSON data back to
     * the client, then the callback function given as a parameter will execute. Otherwise,
     * nothing will happen.
     *
     * @param url       PocketProfit server endpoint url.
     * @param callback  the callback function to execute upon a successful API call.
     */
    private static void fetchPocketProfitServerData(final String url,
                                                    final JSONArrayCallback callback) {
        StringRequest request =
                new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    callback.onSuccess(new JSONArray(response));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    Log.e(TAG, "TimeoutError || NoConnectionError");
                    Log.i(TAG, "TimeoutError || NoConnectionError");
                } else if (error instanceof AuthFailureError) {
                    //TODO
                    Log.e(TAG, "AuthFailureError");
                    Log.i(TAG, "AuthFailureError");
                } else if (error instanceof ServerError) {
                    //TODO
                    Log.e(TAG, "ServerError");
                    Log.i(TAG, "ServerError");
                } else if (error instanceof NetworkError) {
                    //TODO
                    Log.e(TAG, "Network Error");
                    Log.i(TAG, "Network Error");
                } else if (error instanceof ParseError) {
                    //TODO
                    Log.e(TAG, "ParseError " + error.getMessage());
                    Log.i(TAG, "ParseError " + error.getMessage());
                }
                error.printStackTrace();
            }
        });
        request.setRetryPolicy(
                new DefaultRetryPolicy(750, 5, 3));
        mRequestQueue.add(request);
        mRequestQueue
                .addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {
            @Override
            public void onRequestFinished(Request<Object> request) {
                mRequestQueue.getCache().clear();
            }
        });
    }

    /**
     * Makes an API call to communicate with the PocketProfit server by using the given endpoint.
     * The PocketProfit server will then fetch JSON data based on the endpoint given.
     * If the PocketProfit server is able to successfuly retrieve and send the JSON data back to
     * the client, then the callback function given as a parameter will execute. Otherwise,
     * nothing will happen.
     *
     * @param url       PocketProfit server endpoint url.
     * @param callback  the callback function to execute upon a successful API call.
     */
    private static void fetchPocketProfitServerData(String url, final JSONObjectCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET, url,
                                        new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    callback.onSuccess(new JSONObject(response));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    Log.e(TAG, "TimeoutError || NoConnectionError");
                    Log.i(TAG, "TimeoutError || NoConnectionError");
                } else if (error instanceof AuthFailureError) {
                    //TODO
                    Log.e(TAG, "AuthFailureError");
                    Log.i(TAG, "AuthFailureError");
                } else if (error instanceof ServerError) {
                    //TODO
                    Log.e(TAG, "ServerError");
                    Log.i(TAG, "ServerError");
                } else if (error instanceof NetworkError) {
                    //TODO
                    Log.e(TAG, "Network Error");
                    Log.i(TAG, "Network Error");
                } else if (error instanceof ParseError) {
                    //TODO
                    Log.e(TAG, "ParseError " + error.getMessage());
                    Log.i(TAG, "ParseError " + error.getMessage());
                }
                error.printStackTrace();
            }
        });
        request.setRetryPolicy(
                new DefaultRetryPolicy(750, 5, 3));
        mRequestQueue.add(request);
        mRequestQueue
                .addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {
            @Override
            public void onRequestFinished(Request<Object> request) {
                mRequestQueue.getCache().clear();
            }
        });
    }

    /**
     * Returns the url of the security logo given by its ticker symbol as a parameter.
     *
     * @param symbol    the security ticker symbol.
     * @return          the url of the company logo.
     */
    public static String getCompanyLogoURL(String symbol) {
        String baseURL = "https://storage.googleapis.com/iexcloud-hl37opg/api/logos/{symbol}.png";
        return baseURL.replace("{symbol}", symbol);
    }

    /**
     * Retrieves information about stocks that have lost the most value in the most recent trading
     * day (including today).
     * If the PocketProfit server successfully returns back the JSON data regarding this query,
     * the given callback code will be executed. Otherwise, it will not.
     * The context it used to create and execute the call to the PocketProfit server.
     *
     * @param context   the context of the activity.
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchTopLosers(Context context, final JSONArrayCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        fetchPocketProfitServerData(ServerConfig.TOP_LOSERS, callback);
    }

    /**
     * Retrieves information about stocks that have gained the most value in the most recent trading
     * day (including today).
     * If the PocketProfit server successfully returns back the JSON data regarding this query,
     * the given callback code will be executed. Otherwise, it will not.
     * The context it used to create and execute the call to the PocketProfit server.
     *
     * @param context   the context of the activity.
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchTopGainers(Context context, final JSONArrayCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        fetchPocketProfitServerData(ServerConfig.TOP_GAINERS, callback);
    }

    /**
     * Retrieves recent articles about the security given by its ticker symbol as a parameter.
     * If the PocketProfit server successfully returns back the JSON data regarding this query,
     * the given callback code will be executed. Otherwise, it will not.
     * The context it used to create and execute the call to the PocketProfit server.
     *
     * @param context   the context of the activity.
     * @param symbol    the security ticker symbol, of which data will be retrieved.
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchCompanyNews(Context context, String symbol,
                                      final JSONArrayCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        fetchPocketProfitServerData(ServerConfig.COMPANY_NEWS + "?symbol=" + symbol, callback);
    }

    /**
     * Retrieves information about the security given by its ticker symbol as a parameter.
     * If the PocketProfit server successfully returns back the JSON data regarding this query,
     * the given callback code will be executed. Otherwise, it will not.
     * The context it used to create and execute the call to the PocketProfit server.
     *
     * @param context   the context of the activity.
     * @param symbol    the security ticker symbol, of which data will be retrieved.
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchInfoAboutCompany(Context context, String symbol,
                                           final JSONObjectCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        fetchPocketProfitServerData(ServerConfig.COMPANY_INFORMATION + "?symbol=" + symbol,
                        callback);
    }

    /**
     * Retrieves the search result information of a query that a user has made.
     * The search result in this case would consist of securities that match the given query.
     * If the PocketProfit server successfully returns back the JSON data regarding this query,
     * the given callback code will be executed. Otherwise, it will not.
     * The context it used to create and execute the call to the PocketProfit server.
     *
     * @param context   the context of the activity.
     * @param query     the query that the user has made.
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchSearchResults(Context context, String query,
                                          final JSONArrayCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        String url = ServerConfig.PROCESS_QUERY + "?inputQuery=" + query;
        fetchPocketProfitServerData(url.replaceAll(" ", "%20"), callback);
    }

    /**
     * ...
     *
     * @param context   the context of the activity.
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchSectorList(Context context, final JSONArrayCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        fetchPocketProfitServerData(ServerConfig.SECTOR_LIST, callback);
    }

    /**
     * ...
     *
     * @param context   the context of the activity.
     * @param sector
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchSectorInformation(Context context, String sector, final JSONArrayCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        String url = ServerConfig.SECTOR_INFORMATION + "?sector=" + sector;
        fetchPocketProfitServerData(url.replaceAll(" ", "%20"), callback);
    }

    /**
     * Retrieves the stock quote data of the security given by its ticker symbol as a parameter.
     * If the PocketProfit server successfully returns back the JSON data regarding this security,
     * the given callback code will be executed. Otherwise, it will not.
     * The context it used to create and execute the call to the PocketProfit server.
     *
     * @param context   the context of the activity.
     * @param symbol    the security ticker symbol, of which data will be retrieved.
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchStockQuote(final Context context, final String symbol,
                                     final JSONObjectCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        fetchPocketProfitServerData(ServerConfig.STOCK_QUOTE + "?symbol=" + symbol, callback);
    }

    /**
     * Retrieves the intraday chart data of the security given by its ticker symbol as a parameter.
     * If the PocketProfit server successfully returns back the JSON data regarding this security,
     * the given callback code will be executed. Otherwise, it will not.
     * The context it used to create and execute the call to the PocketProfit server.
     *
     * @param context   the context of the activity.
     * @param symbol    the security ticker symbol, of which data will be retrieved.
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchIntradayChartData(final Context context, final String symbol,
                                          final JSONArrayCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        fetchPocketProfitServerData(ServerConfig.INTRADAY_DATA + "?symbol=" + symbol,
                callback);
    }

    /**
     * Retrieves the five dya chart data of the security given by its ticker symbol as a parameter.
     * If the PocketProfit server successfully returns back the JSON data regarding this security,
     * the given callback code will be executed. Otherwise, it will not.
     * The context it used to create and execute the call to the PocketProfit server.
     *
     * @param context   the context of the activity.
     * @param symbol    the security ticker symbol, of which data will be retrieved.
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchFiveDayChartData(final Context context, final String symbol,
                                           final JSONArrayCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        fetchPocketProfitServerData(ServerConfig.FIVE_DAY_DATA + "?symbol=" + symbol,
                callback);
    }

    /**
     * Retrieves the one month chart data of the security given by its ticker symbol as a parameter.
     * If the PocketProfit server successfully returns back the JSON data regarding this security,
     * the given callback code will be executed. Otherwise, it will not.
     * The context it used to create and execute the call to the PocketProfit server.
     *
     * @param context   the context of the activity.
     * @param symbol    the security ticker symbol, of which data will be retrieved.
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchOneMonthChartData(final Context context, final String symbol,
                                            final JSONArrayCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        fetchPocketProfitServerData(ServerConfig.ONE_MONTH_DATA + "?symbol=" + symbol,
                callback);
    }

    /**
     * Retrieves the six month chart data of the security given by its ticker symbol as a parameter.
     * If the PocketProfit server successfully returns back the JSON data regarding this security,
     * the given callback code will be executed. Otherwise, it will not.
     * The context it used to create and execute the call to the PocketProfit server.
     *
     * @param context   the context of the activity.
     * @param symbol    the security ticker symbol, of which data will be retrieved.
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchSixMonthChartData(final Context context, final String symbol,
                                            final JSONArrayCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        fetchPocketProfitServerData(ServerConfig.SIX_MONTH_DATA + "?symbol=" + symbol,
                callback);
    }

    /**
     * Retrieves the one year chart data of the security given by its ticker symbol as a parameter.
     * If the PocketProfit server successfully returns back the JSON data regarding this security,
     * the given callback code will be executed. Otherwise, it will not.
     * The context it used to create and execute the call to the PocketProfit server.
     *
     * @param context   the context of the activity.
     * @param symbol    the security ticker symbol, of which data will be retrieved.
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchOneYearChartData(final Context context, final String symbol,
                                           final JSONArrayCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        fetchPocketProfitServerData(ServerConfig.ONE_YEAR_DATA + "?symbol=" + symbol,
                callback);
    }

    /**
     * Retrieves the five year chart data of the security given by its ticker symbol as a parameter.
     * If the PocketProfit server successfully returns back the JSON data regarding this security,
     * the given callback code will be executed. Otherwise, it will not.
     * The context it used to create and execute the call to the PocketProfit server.
     *
     * @param context   the context of the activity.
     * @param symbol    the security ticker symbol, of which data will be retrieved.
     * @param callback  the callback function to execute upon a successful API call.
     */
    public static void fetchFiveYearChartData(final Context context, final String symbol,
                                            final JSONArrayCallback callback) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        fetchPocketProfitServerData(ServerConfig.FIVE_YEAR_DATA + "?symbol=" + symbol,
                callback);
    }
}