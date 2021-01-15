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
import com.pocketprofit.source.entries.EnhancedStockEntry;

import java.util.List;

public class EnhancedStockAdapter extends RecyclerView.Adapter<EnhancedStockAdapter.TopMoverViewHolder> {

    private Context mContext;
    private List<EnhancedStockEntry> mList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public static class TopMoverViewHolder extends RecyclerView.ViewHolder {
        public ImageView companyLogo;
        public TextView companySymbol;
        public TextView subheader;

        public TextView currentPrice;
        public TextView totalChange;
        public TextView percentageChange;

        public TopMoverViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);

            companyLogo = itemView.findViewById(R.id.logo);
            subheader = itemView.findViewById(R.id.mover_name);
            companySymbol = itemView.findViewById(R.id.mover_symbol);

            currentPrice = itemView.findViewById(R.id.mover_price);
            totalChange = itemView.findViewById(R.id.total_change);
            percentageChange = itemView.findViewById(R.id.percent_change);

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

    public EnhancedStockAdapter(List<EnhancedStockEntry> list, Context context) {
        this.mList = list;
        this.mContext = context;
    }

    @NonNull
    @Override
    public TopMoverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.enhanced_stock_entry, parent,false);
        TopMoverViewHolder vh = new TopMoverViewHolder(v, mListener);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull TopMoverViewHolder holder, int position) {
        EnhancedStockEntry entry = mList.get(position);

        Glide.with(mContext).load(Util.getCompanyLogoURL(entry.getHeader())).into(holder.companyLogo);
        holder.companySymbol.setText(entry.getHeader());
        holder.subheader.setText(entry.getSubheader());

        double currentPriceValue = entry.getPrice();
        double totalChangeValue = entry.getTotalChange();
        double percentChangeValue = entry.getPercentChange();

        int color = (totalChangeValue >= 0) ? mContext.getResources().getColor(R.color.profit) :
                mContext.getResources().getColor(R.color.loss);
        holder.currentPrice.setText(Util.formatPriceText(currentPriceValue, true, true));
        holder.currentPrice.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        String str = "";
        str += (totalChangeValue >= 0) ? "+" : "-";
        str += Util.formatPriceText(Math.abs(totalChangeValue), true, false);
        holder.totalChange.setText(str);
        holder.totalChange.setTextColor(color);
        str = "";
        str += Util.formatPercentageText(percentChangeValue);
        holder.percentageChange.setText(str);
        holder.percentageChange.setTextColor(color);
    }

    public void setList(List<EnhancedStockEntry> newList) {
        mList = newList;
        notifyDataSetChanged();
    }

    public EnhancedStockEntry get(int position) {
        return mList.get(position);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }
}