package com.pocketprofit.source.adapters;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.pocketprofit.R;
import com.pocketprofit.source.Util;
import com.pocketprofit.source.entries.StockEntry;

import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {
    private Context mContext;
    private List<StockEntry> mStocks;
    private OnItemClickListener mListener;


    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(StockAdapter.OnItemClickListener listener) {
        mListener = listener;
    }

    public StockAdapter(Context context) {
        this(context, null);
    }

    public StockAdapter(Context context, List<StockEntry> stocks) {
        mContext = context;
        mStocks = stocks;
    }

    public static class StockViewHolder extends RecyclerView.ViewHolder {
        public ImageView logo;
        public TextView symbol;
        public TextView subheader;
        public TextView currentPrice;

        public StockViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);

            logo = itemView.findViewById(R.id.entry_logo);
            symbol = itemView.findViewById(R.id.main_text);
            subheader = itemView.findViewById(R.id.sub_text);
            currentPrice = itemView.findViewById(R.id.entry_price);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.stock_entry, parent, false);
        return new StockViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        if (position >= 0 && position < mStocks.size()) {
            StockEntry stockEntry = mStocks.get(position);
            String symbol = stockEntry.getHeader();

            Glide.with(mContext).load(Util.getCompanyLogoURL(symbol)).into(holder.logo);

            holder.symbol.setText(symbol);

            holder.subheader.setText(stockEntry.getSubheader());

            holder.currentPrice.getBackground().setColorFilter(stockEntry.getColor(), PorterDuff.Mode.SRC_ATOP);
            holder.currentPrice.setText(Util.formatPriceText(stockEntry.getPrice(),
                    true, true));
        }
    }

    @Override
    public int getItemCount() {
        if (mStocks == null) {
            return 0;
        }
        return mStocks.size();
    }

    public String getSymbol(int position) {
        if (position < 0 || position >= mStocks.size()) {
            return null;
        }
        return mStocks.get(position).getHeader();
    }

    public void swapStockList(List<StockEntry> newList) {
        mStocks = newList;
        if (mStocks != null) {
            notifyDataSetChanged();
        }
    }
}