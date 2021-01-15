package com.pocketprofit.source.adapters;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pocketprofit.R;
import com.pocketprofit.source.Util;
import com.pocketprofit.source.database.DatabaseTables;

public class TransactionHistoryAdapter extends RecyclerView.Adapter<TransactionHistoryAdapter.TransactionViewHolder> {
    private Context mContext;
    private Cursor mCursor;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(TransactionHistoryAdapter.OnItemClickListener listener) {
        mListener = listener;
    }

    public TransactionHistoryAdapter(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        public TextView transactionType;
        public TextView transactionDate;
        public TextView transactionValue;

        public TransactionViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);

            transactionType = itemView.findViewById(R.id.transaction_type);
            transactionDate = itemView.findViewById(R.id.transaction_date);
            transactionValue = itemView.findViewById(R.id.transaction_value);

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
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.transaction_history_entry, parent, false);
        return new TransactionViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        if (mCursor.moveToPosition(position)) {
            String orderType = mCursor.getString(mCursor.getColumnIndex(DatabaseTables.Transaction.COLUMN_ORDER_TYPE));
            String companyName = mCursor.getString(mCursor.getColumnIndex(DatabaseTables.Transaction.COLUMN_SYMBOL));
            double orderPrice = mCursor.getDouble(mCursor.getColumnIndex(DatabaseTables.Transaction.COLUMN_PRICE));
            double orderQuantity = mCursor.getDouble(mCursor.getColumnIndex(DatabaseTables.Transaction.COLUMN_QUANTITY));
            String orderDate = mCursor.getString(mCursor.getColumnIndex(DatabaseTables.Transaction.COLUMN_DATE));

            int color = orderType.contains("Sell") ? mContext.getResources().getColor(R.color.loss) :
                    mContext.getResources().getColor(R.color.profit);

            String transactionTypeText = orderType + " - " + companyName;
            SpannableString spannableString = new SpannableString(transactionTypeText);
            ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(color);

            // 'Market Buy - X', 'Market Sell - X', or 'Free Stock - X'
            int endIndex = transactionTypeText.indexOf("-") - 1;
            spannableString.setSpan(foregroundColorSpan, 0, endIndex, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

            holder.transactionType.setText(spannableString);
            String sign = orderType.equals("Market Buy") ? "-" : "+";
            holder.transactionValue.setText(sign + Util.formatPriceText(orderPrice * orderQuantity, true, true));
            holder.transactionDate.setText(orderDate);
        }
    }

    public String getCompanyName(int position) {
        if (!mCursor.moveToPosition(position)) {
            return null;
        }
        return mCursor.getString(mCursor.getColumnIndex(DatabaseTables.Transaction.COLUMN_NAME));
    }

    public String getCompanySymbol(int position) {
        if (!mCursor.moveToPosition(position)) {
            return null;
        }
        return mCursor.getString(mCursor.getColumnIndex(DatabaseTables.Transaction.COLUMN_SYMBOL));
    }

    public String getOrderType(int position) {
        if (!mCursor.moveToPosition(position)) {
            return null;
        }
        return mCursor.getString(mCursor.getColumnIndex(DatabaseTables.Transaction.COLUMN_ORDER_TYPE));
    }

    public int getQuantity(int position) {
        if (!mCursor.moveToPosition(position)) {
            return -1;
        }
        return mCursor.getInt(mCursor.getColumnIndex(DatabaseTables.Transaction.COLUMN_QUANTITY));
    }

    public double getPrice(int position) {
        if (!mCursor.moveToPosition(position)) {
            return -1;
        }
        return mCursor.getDouble(mCursor.getColumnIndex(DatabaseTables.Transaction.COLUMN_PRICE));
    }

    public String getDate(int position) {
        if (!mCursor.moveToPosition(position)) {
            return null;
        }
        return mCursor.getString(mCursor.getColumnIndex(DatabaseTables.Transaction.COLUMN_DATE));
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }
}