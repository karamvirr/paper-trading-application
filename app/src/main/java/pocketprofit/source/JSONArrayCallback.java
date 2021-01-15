package com.pocketprofit.source;

import org.json.JSONArray;

/**
 * The callback function that will execute code when an API call is successful and the JSON
 * response has been successfully retrieved.
 */
public interface JSONArrayCallback {
    void onSuccess(JSONArray result);
}