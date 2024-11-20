package edu.dartmouth.ui.mphq9;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.List;

import edu.dartmouth.R;
import edu.dartmouth.data.entities.MPHQ9Entity;

public class MPHQ9Adapter extends RecyclerView.Adapter<MPHQ9Adapter.MPHQ9ViewHolder> {

    private List<MPHQ9Entity> mphq9List;

    public void setMPHQ9List(List<MPHQ9Entity> list) {
        this.mphq9List = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MPHQ9ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mphq9, parent, false);
        return new MPHQ9ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MPHQ9ViewHolder holder, int position) {
        if (mphq9List != null && position < mphq9List.size()) {
            MPHQ9Entity assessment = mphq9List.get(position);
            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(holder.itemView.getContext());
            DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(holder.itemView.getContext());
            String dateTime = dateFormat.format(assessment.timestamp) + " " + timeFormat.format(assessment.timestamp);
            holder.textViewDateTime.setText(dateTime);
            holder.textViewAverageScore.setText("Average Score: " + assessment.averageScore + " / 100");
        }
    }

    @Override
    public int getItemCount() {
        return (mphq9List != null) ? mphq9List.size() : 0;
    }

    public static class MPHQ9ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDateTime;
        TextView textViewAverageScore;

        public MPHQ9ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDateTime = itemView.findViewById(R.id.textViewMPHQ9DateTime);
            textViewAverageScore = itemView.findViewById(R.id.textViewMPHQ9AverageScore);
        }
    }
}
