package edu.dartmouth.ui.screenstate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.dartmouth.R;
import edu.dartmouth.data.entities.ScreenEventEntity;

public class ScreenStateAdapter extends RecyclerView.Adapter<ScreenStateAdapter.ScreenStateViewHolder> {

    private List<ScreenEventEntity> screenEventList;

    public void setScreenEventList(List<ScreenEventEntity> screenEventList) {
        this.screenEventList = screenEventList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScreenStateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_screen_state, parent, false);
        return new ScreenStateViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ScreenStateViewHolder holder, int position) {
        if (screenEventList != null && position < screenEventList.size()) {
            ScreenEventEntity currentEvent = screenEventList.get(position);

            // Set screen status
            String screenStatus = currentEvent.isScreenOn ? "Screen ON" : "Screen OFF";
            holder.textViewScreenStatus.setText(screenStatus);

            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
            String formattedTimestamp = sdf.format(new Date(currentEvent.timestamp));
            holder.textViewTimestamp.setText(formattedTimestamp);
        }
    }

    @Override
    public int getItemCount() {
        if (screenEventList != null) {
            return screenEventList.size();
        } else {
            return 0;
        }
    }

    public static class ScreenStateViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewScreenStatus;
        private final TextView textViewTimestamp;

        public ScreenStateViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewScreenStatus = itemView.findViewById(R.id.textViewScreenStatus);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
        }
    }
}
