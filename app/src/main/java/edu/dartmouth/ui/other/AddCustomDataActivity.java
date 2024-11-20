package edu.dartmouth.ui.other;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import edu.dartmouth.R;
import edu.dartmouth.data.entities.DailyAppUsageEntity;
import edu.dartmouth.data.entities.DailyCategoryUsageEntity;
import edu.dartmouth.data.entities.DailyScreenTimeEntity;
import edu.dartmouth.data.entities.MPHQ9Entity;
import edu.dartmouth.data.entities.ScreenEventEntity;
import edu.dartmouth.repositories.DailyAppUsageRepository;
import edu.dartmouth.repositories.DailyCategoryUsageRepository;
import edu.dartmouth.repositories.DailyScreenTimeRepository;
import edu.dartmouth.repositories.MPHQ9Repository;
import edu.dartmouth.repositories.ScreenEventRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class AddCustomDataActivity extends AppCompatActivity {

    // UI Components for Daily App Usage
    private EditText etAppUsagePackageName, etAppUsageDate;
    private EditText etAppUsageHours, etAppUsageMinutes, etAppUsageSeconds;
    private EditText etAppUsageCategory;
    private Button btnAddAppUsage;

    // UI Components for Daily Category Usage
    private EditText etCategoryUsageName, etCategoryUsageDate;
    private EditText etCategoryUsageHours, etCategoryUsageMinutes, etCategoryUsageSeconds;
    private Button btnAddCategoryUsage;

    // UI Components for Daily Screen Time
    private EditText etScreenTimeDate;
    private EditText etScreenTimeHours, etScreenTimeMinutes, etScreenTimeSeconds;
    private EditText etScreenTimeTotalTime;
    private Button btnAddScreenTime;

    // UI Components for MPHQ9
    private EditText etQ1, etQ2, etQ3, etQ4, etQ5, etQ6, etQ7, etQ8, etQ9;
    private Button btnAddMPHQ9;
    private EditText etMPHQ9Date, etMPHQ9Time;


    // UI Components for Screen Events
    private EditText etScreenEventDate, etScreenEventTime;
    private EditText etScreenEventType;
    private Button btnAddScreenEvent;
    private EditText etGenerateScreenEventDate;
    private EditText etNumberOfEvents;
    private Button btnGenerateScreenEvents;

    // Repositories
    private DailyAppUsageRepository appUsageRepository;
    private DailyCategoryUsageRepository categoryUsageRepository;
    private DailyScreenTimeRepository screenTimeRepository;
    private MPHQ9Repository mphq9Repository;
    private ScreenEventRepository screenEventRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_custom_data);

        // Set up the Toolbar with Up button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize UI components
        initializeUI();

        // Initialize repositories
        initializeRepositories();

        // Set up button listeners
        btnAddAppUsage.setOnClickListener(v -> saveDailyAppUsage());
        btnAddCategoryUsage.setOnClickListener(v -> saveDailyCategoryUsage());
        btnAddScreenTime.setOnClickListener(v -> saveDailyScreenTime());
        btnAddMPHQ9.setOnClickListener(v -> saveMPHQ9Data());
        btnAddScreenEvent.setOnClickListener(v -> saveScreenEvent());
        btnGenerateScreenEvents.setOnClickListener(v -> generateScreenEvents());
    }

    private void initializeUI() {
        // Daily App Usage
        etAppUsagePackageName = findViewById(R.id.etAppUsagePackageName);
        etAppUsageDate = findViewById(R.id.etAppUsageDate);
        etAppUsageHours = findViewById(R.id.etAppUsageHours);
        etAppUsageMinutes = findViewById(R.id.etAppUsageMinutes);
        etAppUsageSeconds = findViewById(R.id.etAppUsageSeconds);
        etAppUsageCategory = findViewById(R.id.etAppUsageCategory);
        btnAddAppUsage = findViewById(R.id.btnAddAppUsage);

        // Set up date picker dialog for Daily App Usage Date
        etAppUsageDate.setOnClickListener(v -> showDatePickerDialog(etAppUsageDate));

        // Daily Category Usage
        etCategoryUsageName = findViewById(R.id.etCategoryUsageName);
        etCategoryUsageDate = findViewById(R.id.etCategoryUsageDate);
        etCategoryUsageHours = findViewById(R.id.etCategoryUsageHours);
        etCategoryUsageMinutes = findViewById(R.id.etCategoryUsageMinutes);
        etCategoryUsageSeconds = findViewById(R.id.etCategoryUsageSeconds);
        btnAddCategoryUsage = findViewById(R.id.btnAddCategoryUsage);

        // Set up date picker dialog for Daily Category Usage Date
        etCategoryUsageDate.setOnClickListener(v -> showDatePickerDialog(etCategoryUsageDate));

        // Daily Screen Time
        etScreenTimeDate = findViewById(R.id.etScreenTimeDate);
        etScreenTimeHours = findViewById(R.id.etScreenTimeHours);
        etScreenTimeMinutes = findViewById(R.id.etScreenTimeMinutes);
        etScreenTimeSeconds = findViewById(R.id.etScreenTimeSeconds);
        btnAddScreenTime = findViewById(R.id.btnAddScreenTime);

        // Set up date picker dialog for Daily Screen Time Date
        etScreenTimeDate.setOnClickListener(v -> showDatePickerDialog(etScreenTimeDate));

        // MPHQ9
        etQ1 = findViewById(R.id.etQ1);
        etQ2 = findViewById(R.id.etQ2);
        etQ3 = findViewById(R.id.etQ3);
        etQ4 = findViewById(R.id.etQ4);
        etQ5 = findViewById(R.id.etQ5);
        etQ6 = findViewById(R.id.etQ6);
        etQ7 = findViewById(R.id.etQ7);
        etQ8 = findViewById(R.id.etQ8);
        etQ9 = findViewById(R.id.etQ9);
        btnAddMPHQ9 = findViewById(R.id.btnAddMPHQ9);

        // Initialize date and time fields
        etMPHQ9Date = findViewById(R.id.etMPHQ9Date);
        etMPHQ9Time = findViewById(R.id.etMPHQ9Time);

        // Set up date picker dialog for MPHQ9 Date
        etMPHQ9Date.setOnClickListener(v -> showDatePickerDialog(etMPHQ9Date));

        // Set up time picker dialog for MPHQ9 Time
        etMPHQ9Time.setOnClickListener(v -> showTimePickerDialog(etMPHQ9Time));

        // Screen Events
        etScreenEventDate = findViewById(R.id.etScreenEventDate);
        etScreenEventTime = findViewById(R.id.etScreenEventTime);
        etScreenEventType = findViewById(R.id.etScreenEventType);
        btnAddScreenEvent = findViewById(R.id.btnAddScreenEvent);

        // Set up date picker and time picker dialogs for Screen Events
        etScreenEventDate.setOnClickListener(v -> showDatePickerDialog(etScreenEventDate));
        etScreenEventTime.setOnClickListener(v -> showTimePickerDialog(etScreenEventTime));

        // Initialize Generate Screen Events UI components
        etGenerateScreenEventDate = findViewById(R.id.etGenerateScreenEventDate);
        etNumberOfEvents = findViewById(R.id.etNumberOfEvents);
        btnGenerateScreenEvents = findViewById(R.id.btnGenerateScreenEvents);

        // Set up date picker dialog for Generate Screen Event Date
        etGenerateScreenEventDate.setOnClickListener(v -> showDatePickerDialog(etGenerateScreenEventDate));
    }

    private void initializeRepositories() {
        appUsageRepository = new DailyAppUsageRepository(this);
        categoryUsageRepository = new DailyCategoryUsageRepository(this);
        screenTimeRepository = new DailyScreenTimeRepository(this);
        mphq9Repository = new MPHQ9Repository(this);
        screenEventRepository = new ScreenEventRepository(this);
    }

    private void showDatePickerDialog(final EditText dateField) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    String dateText = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    dateField.setText(dateText);
                    dateField.setTag(calendar.getTimeInMillis());
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void showTimePickerDialog(final EditText timeField) {
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                    calendar.set(Calendar.MINUTE, selectedMinute);
                    String timeText = String.format("%02d:%02d", selectedHour, selectedMinute);
                    timeField.setText(timeText);
                    timeField.setTag(calendar.getTimeInMillis());
                },
                hour, minute, true
        );
        timePickerDialog.show();
    }

    private void saveDailyAppUsage() {
        String packageName = etAppUsagePackageName.getText().toString().trim();
        String dateStr = etAppUsageDate.getText().toString().trim();
        String hoursStr = etAppUsageHours.getText().toString().trim();
        String minutesStr = etAppUsageMinutes.getText().toString().trim();
        String secondsStr = etAppUsageSeconds.getText().toString().trim();
        String categoryName = etAppUsageCategory.getText().toString().trim();

        if (packageName.isEmpty() || dateStr.isEmpty() || hoursStr.isEmpty() ||
                minutesStr.isEmpty() || secondsStr.isEmpty() || categoryName.isEmpty()) {
            Toast.makeText(this, "Please fill all fields for Daily App Usage.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Retrieve the date timestamp from the date field's tag
            Object dateTag = etAppUsageDate.getTag();
            if (dateTag == null) {
                Toast.makeText(this, "Please select a valid date.", Toast.LENGTH_SHORT).show();
                return;
            }
            long dateMillis = (long) dateTag;

            // Parse time inputs
            int hours = Integer.parseInt(hoursStr);
            int minutes = Integer.parseInt(minutesStr);
            int seconds = Integer.parseInt(secondsStr);

            // Validate time inputs
            if (hours < 0 || minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60) {
                Toast.makeText(this, "Please enter valid time values.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert time to milliseconds
            long totalTimeMillis = (hours * 3600 + minutes * 60 + seconds) * 1000;

            DailyAppUsageEntity entity = new DailyAppUsageEntity();
            entity.packageName = packageName;
            entity.date = dateMillis;
            entity.totalTimeInForeground = totalTimeMillis;
            entity.categoryName = categoryName;

            appUsageRepository.insert(entity);

            Toast.makeText(this, "Daily App Usage saved successfully.", Toast.LENGTH_SHORT).show();

            // Clear input fields
            etAppUsagePackageName.setText("");
            etAppUsageDate.setText("");
            etAppUsageDate.setTag(null);
            etAppUsageHours.setText("");
            etAppUsageMinutes.setText("");
            etAppUsageSeconds.setText("");
            etAppUsageCategory.setText("");

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for time fields.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving Daily App Usage.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDailyCategoryUsage() {
        String categoryName = etCategoryUsageName.getText().toString().trim();
        String dateStr = etCategoryUsageDate.getText().toString().trim();
        String hoursStr = etCategoryUsageHours.getText().toString().trim();
        String minutesStr = etCategoryUsageMinutes.getText().toString().trim();
        String secondsStr = etCategoryUsageSeconds.getText().toString().trim();

        if (categoryName.isEmpty() || dateStr.isEmpty() || hoursStr.isEmpty() ||
                minutesStr.isEmpty() || secondsStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields for Daily Category Usage.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Retrieve the date timestamp from the date field's tag
            Object dateTag = etCategoryUsageDate.getTag();
            if (dateTag == null) {
                Toast.makeText(this, "Please select a valid date.", Toast.LENGTH_SHORT).show();
                return;
            }
            long dateMillis = (long) dateTag;

            // Parse time inputs
            int hours = Integer.parseInt(hoursStr);
            int minutes = Integer.parseInt(minutesStr);
            int seconds = Integer.parseInt(secondsStr);

            // Validate time inputs
            if (hours < 0 || minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60) {
                Toast.makeText(this, "Please enter valid time values.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert time to milliseconds
            long totalTimeMillis = (hours * 3600 + minutes * 60 + seconds) * 1000;

            DailyCategoryUsageEntity entity = new DailyCategoryUsageEntity();
            entity.categoryName = categoryName;
            entity.date = dateMillis;
            entity.totalTimeInForeground = totalTimeMillis;

            categoryUsageRepository.insert(entity);

            Toast.makeText(this, "Daily Category Usage saved successfully.", Toast.LENGTH_SHORT).show();

            // Clear input fields
            etCategoryUsageName.setText("");
            etCategoryUsageDate.setText("");
            etCategoryUsageDate.setTag(null);
            etCategoryUsageHours.setText("");
            etCategoryUsageMinutes.setText("");
            etCategoryUsageSeconds.setText("");

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for time fields.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving Daily Category Usage.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDailyScreenTime() {
        String dateStr = etScreenTimeDate.getText().toString().trim();
        String hoursStr = etScreenTimeHours.getText().toString().trim();
        String minutesStr = etScreenTimeMinutes.getText().toString().trim();
        String secondsStr = etScreenTimeSeconds.getText().toString().trim();

        if (dateStr.isEmpty() || hoursStr.isEmpty() ||
                minutesStr.isEmpty() || secondsStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields for Daily Screen Time.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Retrieve the date timestamp from the date field's tag
            Object dateTag = etScreenTimeDate.getTag();
            if (dateTag == null) {
                Toast.makeText(this, "Please select a valid date.", Toast.LENGTH_SHORT).show();
                return;
            }
            long dateMillis = (long) dateTag;

            // Parse time inputs
            int hours = Integer.parseInt(hoursStr);
            int minutes = Integer.parseInt(minutesStr);
            int seconds = Integer.parseInt(secondsStr);

            // Validate time inputs
            if (hours < 0 || minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60) {
                Toast.makeText(this, "Please enter valid time values.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert time to milliseconds
            long totalTimeMillis = (hours * 3600 + minutes * 60 + seconds) * 1000;

            DailyScreenTimeEntity entity = new DailyScreenTimeEntity();
            entity.date = dateMillis;
            entity.totalScreenTime = totalTimeMillis;

            screenTimeRepository.insert(entity);

            Toast.makeText(this, "Daily Screen Time saved successfully.", Toast.LENGTH_SHORT).show();

            // Clear input fields
            etScreenTimeDate.setText("");
            etScreenTimeDate.setTag(null);
            etScreenTimeHours.setText("");
            etScreenTimeMinutes.setText("");
            etScreenTimeSeconds.setText("");

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for time fields.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving Daily Screen Time.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveMPHQ9Data() {
        EditText[] questionFields = {etQ1, etQ2, etQ3, etQ4, etQ5, etQ6, etQ7, etQ8, etQ9};
        int[] scores = new int[9];

        // Retrieve and validate scores
        for (int i = 0; i < questionFields.length; i++) {
            String scoreStr = questionFields[i].getText().toString().trim();
            if (scoreStr.isEmpty()) {
                Toast.makeText(this, "Please fill all questions for MPHQ9 Assessment.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int score = Integer.parseInt(scoreStr);
                if (score < 0 || score > 100) {
                    Toast.makeText(this, "Scores must be between 0 and 100.", Toast.LENGTH_SHORT).show();
                    return;
                }
                scores[i] = score;
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers for all questions.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Retrieve date and time
        String dateStr = etMPHQ9Date.getText().toString().trim();
        String timeStr = etMPHQ9Time.getText().toString().trim();

        if (dateStr.isEmpty() || timeStr.isEmpty()) {
            Toast.makeText(this, "Please select date and time for MPHQ9 Assessment.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Retrieve date and time in milliseconds
            Object dateTag = etMPHQ9Date.getTag();
            Object timeTag = etMPHQ9Time.getTag();
            if (dateTag == null || timeTag == null) {
                Toast.makeText(this, "Please select a valid date and time.", Toast.LENGTH_SHORT).show();
                return;
            }
            long dateMillis = (long) dateTag;
            long timeMillis = (long) timeTag;

            // Combine date and time
            Calendar dateCalendar = Calendar.getInstance();
            dateCalendar.setTimeInMillis(dateMillis);

            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.setTimeInMillis(timeMillis);

            dateCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            dateCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            dateCalendar.set(Calendar.SECOND, 0);
            dateCalendar.set(Calendar.MILLISECOND, 0);

            long timestamp = dateCalendar.getTimeInMillis();

            // Create the MPHQ9Entity with the selected timestamp
            MPHQ9Entity entity = new MPHQ9Entity();
            entity.timestamp = timestamp;
            entity.q1 = scores[0];
            entity.q2 = scores[1];
            entity.q3 = scores[2];
            entity.q4 = scores[3];
            entity.q5 = scores[4];
            entity.q6 = scores[5];
            entity.q7 = scores[6];
            entity.q8 = scores[7];
            entity.q9 = scores[8];
            entity.averageScore = (scores[0] + scores[1] + scores[2] + scores[3] + scores[4] +
                    scores[5] + scores[6] + scores[7] + scores[8]) / 9.0f;

            mphq9Repository.insert(entity);

            Toast.makeText(this, "MPHQ9 Assessment saved successfully.", Toast.LENGTH_SHORT).show();

            // Clear input fields
            for (EditText et : questionFields) {
                et.setText("");
            }
            etMPHQ9Date.setText("");
            etMPHQ9Date.setTag(null);
            etMPHQ9Time.setText("");
            etMPHQ9Time.setTag(null);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving MPHQ9 Assessment.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveScreenEvent() {
        String dateStr = etScreenEventDate.getText().toString().trim();
        String timeStr = etScreenEventTime.getText().toString().trim();
        String eventTypeStr = etScreenEventType.getText().toString().trim();

        if (dateStr.isEmpty() || timeStr.isEmpty() || eventTypeStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields for Screen Event.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Retrieve date and time in milliseconds
            Object dateTag = etScreenEventDate.getTag();
            Object timeTag = etScreenEventTime.getTag();
            if (dateTag == null || timeTag == null) {
                Toast.makeText(this, "Please select a valid date and time.", Toast.LENGTH_SHORT).show();
                return;
            }
            long dateMillis = (long) dateTag;
            long timeMillis = (long) timeTag;

            // Combine date and time
            Calendar dateCalendar = Calendar.getInstance();
            dateCalendar.setTimeInMillis(dateMillis);

            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.setTimeInMillis(timeMillis);

            dateCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            dateCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            dateCalendar.set(Calendar.SECOND, 0);
            dateCalendar.set(Calendar.MILLISECOND, 0);

            long timestamp = dateCalendar.getTimeInMillis();

            boolean isScreenOn;

            if (eventTypeStr.equalsIgnoreCase("true") || eventTypeStr.equalsIgnoreCase("on")) {
                isScreenOn = true;
            } else if (eventTypeStr.equalsIgnoreCase("false") || eventTypeStr.equalsIgnoreCase("off")) {
                isScreenOn = false;
            } else {
                Toast.makeText(this, "Please enter 'on' or 'off' for Event Type.", Toast.LENGTH_SHORT).show();
                return;
            }

            ScreenEventEntity entity = new ScreenEventEntity();
            entity.timestamp = timestamp;
            entity.isScreenOn = isScreenOn;

            screenEventRepository.insert(entity);

            Toast.makeText(this, "Screen Event saved successfully.", Toast.LENGTH_SHORT).show();

            // Clear input fields
            etScreenEventDate.setText("");
            etScreenEventDate.setTag(null);
            etScreenEventTime.setText("");
            etScreenEventTime.setTag(null);
            etScreenEventType.setText("");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving Screen Event.", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Method to generate multiple screen on/off events for a selected date.
     */
    private void generateScreenEvents() {
        String dateStr = etGenerateScreenEventDate.getText().toString().trim();
        String numberOfEventsStr = etNumberOfEvents.getText().toString().trim();

        if (dateStr.isEmpty()) {
            Toast.makeText(this, "Please select a generation date for screen events.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (numberOfEventsStr.isEmpty()) {
            Toast.makeText(this, "Please enter the number of events to generate.", Toast.LENGTH_SHORT).show();
            return;
        }

        int numberOfEvents;
        try {
            numberOfEvents = Integer.parseInt(numberOfEventsStr);
            if (numberOfEvents <= 0) {
                Toast.makeText(this, "Please enter a positive number of events.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for events.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Retrieve the generation date timestamp from the new date field's tag
        Object dateTag = etGenerateScreenEventDate.getTag();
        if (dateTag == null) {
            Toast.makeText(this, "Please select a valid generation date.", Toast.LENGTH_SHORT).show();
            return;
        }
        long dateMillis = (long) dateTag;

        // Generate events
        List<ScreenEventEntity> generatedEvents = createRandomScreenEvents(dateMillis, numberOfEvents);

        // Insert generated events into repository
        try {
            screenEventRepository.insertAll(generatedEvents);
            Toast.makeText(this, numberOfEvents + " Screen Events generated successfully.", Toast.LENGTH_SHORT).show();

            // Optionally, clear the number input field
            etNumberOfEvents.setText("");
            etGenerateScreenEventDate.setText("");
            etGenerateScreenEventDate.setTag(null);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating Screen Events.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Helper method to create a list of ScreenEventEntity with random timestamps and alternating on/off states.
     *
     * @param dateMillis     The date for which events are generated.
     * @param numberOfEvents Number of events to generate.
     * @return List of ScreenEventEntity.
     */
    private List<ScreenEventEntity> createRandomScreenEvents(long dateMillis, int numberOfEvents) {
        List<ScreenEventEntity> events = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);

        // Set to start of the day
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        // Set to end of the day
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long endOfDay = calendar.getTimeInMillis() - 1;

        Random random = new Random();

        boolean isScreenOn = true; // Start with screen on

        for (int i = 0; i < numberOfEvents; i++) {
            // Generate a random timestamp within the day
            long randomTimestamp = startOfDay + (long) (random.nextDouble() * (endOfDay - startOfDay));

            // Create ScreenEventEntity
            ScreenEventEntity event = new ScreenEventEntity();
            event.timestamp = randomTimestamp;
            event.isScreenOn = isScreenOn;

            events.add(event);

            // Toggle screen state for the next event
            isScreenOn = !isScreenOn;
        }

        // Sort events by timestamp to maintain chronological order
        events.sort((e1, e2) -> Long.compare(e1.timestamp, e2.timestamp));

        return events;
    }
}