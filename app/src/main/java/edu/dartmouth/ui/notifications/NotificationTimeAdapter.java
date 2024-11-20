package edu.dartmouth.ui.notifications;

import android.app.TimePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import edu.dartmouth.R;

import java.util.Calendar;
import java.util.List;

public class NotificationTimeAdapter extends RecyclerView.Adapter<NotificationTimeAdapter.TimeViewHolder> {

    private List<Calendar> timeList;
    private OnTimeChangeListener listener;

    public interface OnTimeChangeListener {
        void onTimeRemoved(int position);
        void onTimeEdited(int position, Calendar newTime);
    }

    public NotificationTimeAdapter(List<Calendar> timeList, OnTimeChangeListener listener) {
        this.timeList = timeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_time, parent, false);
        return new TimeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeViewHolder holder, int position) {
        Calendar calendar = timeList.get(position);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        if (hour == 0) {
            hour = 12;
        }
        String amPm = calendar.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
        holder.textViewTime.setText(String.format("%02d:%02d %s", hour, minute, amPm));

        holder.buttonEdit.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(v.getContext(),
                    (view, selectedHour, selectedMinute) -> {
                        Calendar newTime = Calendar.getInstance();
                        newTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                        newTime.set(Calendar.MINUTE, selectedMinute);
                        newTime.set(Calendar.SECOND, 0);
                        newTime.set(Calendar.MILLISECOND, 0);
                        timeList.set(position, newTime);
                        notifyItemChanged(position);
                        listener.onTimeEdited(position, newTime);
                    }, calendar.get(Calendar.HOUR_OF_DAY), minute, false);
            dialog.show();
        });

        holder.buttonRemove.setOnClickListener(v -> {
            listener.onTimeRemoved(position);
            notifyItemRemoved(position);
        });
    }

    @Override
    public int getItemCount() {
        return timeList.size();
    }

    static class TimeViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTime;
        ImageButton buttonEdit;
        ImageButton buttonRemove;

        TimeViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            buttonEdit = itemView.findViewById(R.id.buttonEditTime);
            buttonRemove = itemView.findViewById(R.id.buttonRemoveTime);
        }
    }
}
