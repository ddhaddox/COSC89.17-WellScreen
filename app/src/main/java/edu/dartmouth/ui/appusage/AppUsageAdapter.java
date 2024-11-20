package edu.dartmouth.ui.appusage;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.dartmouth.R;
import edu.dartmouth.data.entities.AppUsageSummary;

public class AppUsageAdapter extends RecyclerView.Adapter<AppUsageAdapter.AppUsageViewHolder> {

    private List<AppUsageSummary> appUsageList;

    public void setAppUsageList(List<AppUsageSummary> appUsageList) {
        this.appUsageList = appUsageList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppUsageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_usage, parent, false);
        return new AppUsageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AppUsageViewHolder holder, int position) {
        if (appUsageList != null) {
            AppUsageSummary currentUsage = appUsageList.get(position);
            PackageManager pm = holder.itemView.getContext().getPackageManager();
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(currentUsage.packageName, 0);
                String appName = pm.getApplicationLabel(appInfo).toString();
                holder.packageNameTextView.setText(appName);
                Drawable appIcon = pm.getApplicationIcon(appInfo);
                holder.appIconImageView.setImageDrawable(appIcon);
            } catch (PackageManager.NameNotFoundException e) {
                holder.packageNameTextView.setText(currentUsage.packageName);
                holder.appIconImageView.setImageResource(R.drawable.baseline_android_24);
            }
            holder.categoryNameTextView.setText(currentUsage.categoryName);
            holder.usageTimeTextView.setText(formatUsageTime(currentUsage.totalTimeInForeground));
        }
    }

    @Override
    public int getItemCount() {
        if (appUsageList != null) {
            return appUsageList.size();
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

    public static class AppUsageViewHolder extends RecyclerView.ViewHolder {
        private final ImageView appIconImageView;
        private final TextView packageNameTextView;
        private final TextView categoryNameTextView;
        private final TextView usageTimeTextView;

        public AppUsageViewHolder(@NonNull View itemView) {
            super(itemView);
            appIconImageView = itemView.findViewById(R.id.imageViewAppIcon);
            packageNameTextView = itemView.findViewById(R.id.textViewPackageName);
            categoryNameTextView = itemView.findViewById(R.id.textViewCategoryName);
            usageTimeTextView = itemView.findViewById(R.id.textViewUsageTime);
        }
    }
}
