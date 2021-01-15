package com.pocketprofit.source;

import org.json.JSONObject;

/**
 * The callback function that will execute code when an API call is successful and the JSON
 * response has been successfully retrieved.
 */
public interface JSONObjectCallback {
    void onSuccess(JSONObject result);
}