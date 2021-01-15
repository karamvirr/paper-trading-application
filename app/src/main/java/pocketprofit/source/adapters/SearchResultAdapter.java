package com.pocketprofit.source.adapters;

import android.content.Context;
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
import com.pocketprofit.source.entries.SearchResultEntry;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder> {

    private Context mContext;
    private List<SearchResultEntry> mList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        public ImageView companyLogo;
        public TextView companyName;
        public TextView companySymbol;

        public SearchResultViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);

            companyLogo = itemView.findViewById(R.id.company_logo);
            companyName = itemView.findViewById(R.id.company_name);
            companySymbol = itemView.findViewById(R.id.company_symbol);

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

    public SearchResultAdapter(List<SearchResultEntry> list, Context context) {
        this.mList = list;
        this.mContext = context;
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_entry, parent, false);
        SearchResultViewHolder holder = new SearchResultViewHolder(v, mListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        SearchResultEntry entry = mList.get(position);

        Glide.with(mContext).load(Util.getCompanyLogoURL(entry.getHeader())).into(holder.companyLogo);
        holder.companySymbol.setText(entry.getHeader());
        holder.companyName.setText(entry.getSubheader());
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

}