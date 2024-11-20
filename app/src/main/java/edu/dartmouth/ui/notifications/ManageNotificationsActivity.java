package edu.dartmouth.ui.notifications;

import static edu.dartmouth.MainActivity.isNightMode;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import edu.dartmouth.R;
import edu.dartmouth.ui.notifications.NotificationTimeAdapter;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ManageNotificationsActivity extends AppCompatActivity implements NotificationTimeAdapter.OnTimeChangeListener {

    private RecyclerView recyclerViewNotificationTimes;
    private NotificationTimeAdapter adapter;
    private List<Calendar> timeList;
    private Button buttonAddTime, buttonSaveSettings;

    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_NOTIFICATION_TIMES = "notification_times";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_notifications);

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the Up button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.white));
        }

        // Initialize views
        recyclerViewNotificationTimes = findViewById(R.id.recyclerViewNotificationTimes);
        buttonAddTime = findViewById(R.id.buttonAddTime);
        buttonSaveSettings = findViewById(R.id.buttonSaveNotificationSettings);

        // Initialize time list
        timeList = loadNotificationTimes();

        // Set up RecyclerView
        adapter = new NotificationTimeAdapter(timeList, this);
        recyclerViewNotificationTimes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotificationTimes.setAdapter(adapter);

        // Add Time Button Click Listener
        buttonAddTime.setOnClickListener(v -> {
            // Show TimePickerDialog to add a new time
            Calendar currentTime = Calendar.getInstance();
            int hour = currentTime.get(Calendar.HOUR_OF_DAY);
            int minute = currentTime.get(Calendar.MINUTE);

            // Set up TimePickerDialog to use AM/PM format
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, selectedHour, selectedMinute) -> {
                        Calendar newTime = Calendar.getInstance();
                        newTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                        newTime.set(Calendar.MINUTE, selectedMinute);
                        newTime.set(Calendar.SECOND, 0);
                        newTime.set(Calendar.MILLISECOND, 0);
                        timeList.add(newTime);
                        adapter.notifyItemInserted(timeList.size() - 1);
                    }, hour, minute, false); // false indicates 12-hour format (AM/PM)
            timePickerDialog.show();
        });

        // Save Settings Button Click Listener
        buttonSaveSettings.setOnClickListener(v -> {
            saveNotificationSettings(timeList);
            Toast.makeText(this, "Notification settings saved.", Toast.LENGTH_SHORT).show();
            finish();
        });

        // In your Activity or Fragment
        View lineUnderToolbar = findViewById(R.id.lineUnderToolbar);

        if (isNightMode(this)) {
            lineUnderToolbar.setVisibility(View.VISIBLE);
        } else {
            lineUnderToolbar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onTimeRemoved(int position) {
        timeList.remove(position);
        adapter.notifyItemRemoved(position);
    }

    @Override
    public void onTimeEdited(int position, Calendar newTime) {
        timeList.set(position, newTime);
        adapter.notifyItemChanged(position);
    }

    private List<Calendar> loadNotificationTimes() {
        List<Calendar> list = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String timesJson = prefs.getString(KEY_NOTIFICATION_TIMES, null);
        if (timesJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(timesJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    long timeInMillis = jsonArray.getLong(i);
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(timeInMillis);
                    list.add(cal);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    private void saveNotificationSettings(List<Calendar> times) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        JSONArray jsonArray = new JSONArray();
        for (Calendar cal : times) {
            jsonArray.put(cal.getTimeInMillis());
        }
        editor.putString(KEY_NOTIFICATION_TIMES, jsonArray.toString());
        editor.apply();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
