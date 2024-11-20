package edu.dartmouth.ui.appusage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.dartmouth.R;
import edu.dartmouth.data.entities.CategoryUsageSummary;

public class CategoryUsageAdapter extends RecyclerView.Adapter<CategoryUsageAdapter.CategoryUsageViewHolder> {

    private List<CategoryUsageSummary> categoryUsageList;

    public void setCategoryUsageList(List<CategoryUsageSummary> categoryUsageList) {
        this.categoryUsageList = categoryUsageList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryUsageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_usage, parent, false);
        return new CategoryUsageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryUsageViewHolder holder, int position) {
        if (categoryUsageList != null) {
            CategoryUsageSummary currentUsage = categoryUsageList.get(position);
            holder.categoryNameTextView.setText(currentUsage.categoryName);
            holder.usageTimeTextView.setText(formatUsageTime(currentUsage.totalTimeInForeground));
        }
    }

    @Override
    public int getItemCount() {
        if (categoryUsageList != null) {
            return categoryUsageList.size();
        } else {
            return 0;
        }
    }

    private String formatUsageTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%02dh:%02dm:%02ds", hours, minutes % 60, seconds % 60);
    }

    public static class CategoryUsageViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoryNameTextView;
        private final TextView usageTimeTextView;

        public CategoryUsageViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryNameTextView = itemView.findViewById(R.id.textViewCategoryName);
            usageTimeTextView = itemView.findViewById(R.id.textViewCategoryUsageTime);
        }
    }
}
