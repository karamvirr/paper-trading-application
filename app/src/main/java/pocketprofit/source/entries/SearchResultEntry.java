package com.pocketprofit.source.entries;

/**
 * SearchResultEntry is used to store result information of a query that was made by the user
 * in the search box of SearchActivity.
 */
public class SearchResultEntry {

    private String mHeader;
    private String mSubheader;

    public SearchResultEntry(String header, String subheader) {
        mHeader = header;
        mSubheader = subheader;
    }

    public String getHeader() {
        return mHeader;
    }

    public String getSubheader()  {
        return mSubheader;
    }

}