package com.pocketprofit.source.entries;

/**
 * NewsEntry is used to store information about a news article regarding a particular stock.
 */
public class NewsEntry {
    private String mSource;
    private String mHeadline;
    private String mSummary;
    private String mImageURL;
    private String mSiteURL;

    public NewsEntry(String source, String headline, String summary, String imageURL, String siteURL) {
        mSource = source;
        mHeadline = headline;
        mSummary = summary;
        mImageURL = imageURL;
        mSiteURL = siteURL;
    }

    public String getSource() {
        return mSource;
    }

    public String getHeadline() {
        return mHeadline;
    }

    public String getSummary() {
        return mSummary;
    }

    public String getImageURL() {
        return mImageURL;
    }

    public String getSiteURL() {
        return mSiteURL;
    }
}