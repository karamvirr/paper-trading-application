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
import com.pocketprofit.source.entries.NewsEntry;

import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private Context mContext;
    private List<NewsEntry> mList;
    private SearchResultAdapter.OnItemClickListener mListener;

    public void setOnItemClickListener(SearchResultAdapter.OnItemClickListener listener) {
        mListener = listener;
    }

    public NewsAdapter(List<NewsEntry> list, Context context) {
        this.mContext = context;
        this.mList = list;
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {
        public TextView sourceText;
        public ImageView newsImage;
        public TextView headlineText;
        public TextView summaryText;

        public NewsViewHolder(@NonNull View itemView, final SearchResultAdapter.OnItemClickListener listener) {
            super(itemView);

            sourceText = itemView.findViewById(R.id.source);
            newsImage = itemView.findViewById(R.id.news_image);
            headlineText = itemView.findViewById(R.id.headline);
            summaryText = itemView.findViewById(R.id.summary);

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
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.news_entry, parent, false);
        return new NewsViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsEntry entry = mList.get(position);

        Glide.with(mContext).load(entry.getImageURL()).into(holder.newsImage);
        holder.sourceText.setText(entry.getSource());
        holder.headlineText.setText(entry.getHeadline());
        holder.summaryText.setText(entry.getSummary());
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }
}