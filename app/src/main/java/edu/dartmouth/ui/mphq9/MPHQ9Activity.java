package edu.dartmouth.ui.mphq9;

import static edu.dartmouth.MainActivity.isNightMode;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;

import edu.dartmouth.R;
import edu.dartmouth.data.entities.MPHQ9Entity;

public class MPHQ9Activity extends AppCompatActivity {

    private MPHQ9ViewModel mphq9ViewModel;

    private Slider sliderQuestion;
    private TextView textViewQuestion;
    private TextView textViewTimeFrame;
    private TextView textViewSliderValue;
    private Button btnBack;
    private Button btnNext;

    private int currentQuestionIndex = 0;
    private final int totalQuestions = 9;

    private String[] questions = new String[totalQuestions];
    private int[] responses = new int[totalQuestions];
    private boolean[] hasTouched = new boolean[totalQuestions];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mphq9);

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Assessment");
        setSupportActionBar(toolbar);

        // Enable the Up button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Manually set the navigation icon tint to ensure visibility
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.white));
            }
        }

        mphq9ViewModel = new ViewModelProvider(this).get(MPHQ9ViewModel.class);

        // Initialize questions without numbers or timeframes
        questions[0] = "Little interest or pleasure in doing things.";
        questions[1] = "Feeling down, depressed, or hopeless.";
        questions[2] = "Trouble falling or staying asleep, or sleeping too much.";
        questions[3] = "Feeling tired or having little energy.";
        questions[4] = "Poor appetite or overeating.";
        questions[5] = "Feeling bad about yourself, or that you're a failure or have let yourself or your family down.";
        questions[6] = "Trouble concentrating on things, such as reading or watching TV.";
        questions[7] = "Moving or speaking so slowly that other people could have noticed? Or the opposite - being fidgety or restless.";
        questions[8] = "Thoughts that you would be better off dead, or thoughts of hurting yourself in some way.";

        // Initialize UI elements
        textViewQuestion = findViewById(R.id.textViewQuestion);
        textViewTimeFrame = findViewById(R.id.textViewTimeFrame);
        sliderQuestion = findViewById(R.id.sliderQuestion);
        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);
        textViewSliderValue = findViewById(R.id.textViewSliderValue);

        // Set default slider value to 50
        sliderQuestion.setValue(50);

        // Initialize the hasTouched array
        for (int i = 0; i < totalQuestions; i++) {
            hasTouched[i] = false;
        }

        // Set up slider listener
        sliderQuestion.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                if (!hasTouched[currentQuestionIndex]) {
                    hasTouched[currentQuestionIndex] = true;

                    // Make the thumb visible by setting its tint to the primary color and restore halo
                    slider.setThumbTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
                    slider.setHaloRadius(getResources().getDimensionPixelSize(R.dimen.default_halo_radius));
                    updateSliderValueDisplay((int) slider.getValue());
                }
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                // Save the response
                responses[currentQuestionIndex] = (int) slider.getValue();
                updateSliderValueDisplay((int) slider.getValue());
            }
        });

        sliderQuestion.addOnChangeListener((slider, value, fromUser) -> {
            if (hasTouched[currentQuestionIndex]) {
                updateSliderValueDisplay((int) value);
            }
        });

        btnBack.setOnClickListener(v -> {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                updateQuestion();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (!hasTouched[currentQuestionIndex]) {
                Toast.makeText(this, "Please adjust the slider before proceeding.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentQuestionIndex < totalQuestions - 1) {
                currentQuestionIndex++;
                updateQuestion();
            } else {
                submitAssessment();
            }
        });

        updateQuestion();

        // Handle night mode for line under toolbar
        View lineUnderToolbar = findViewById(R.id.lineUnderToolbar);

        if (isNightMode(this)) {
            lineUnderToolbar.setVisibility(View.VISIBLE);
        } else {
            lineUnderToolbar.setVisibility(View.GONE);
        }
    }

    private void updateSliderValueDisplay(int value) {
        textViewSliderValue.setText(String.format("Value: %d", value));
    }

    private void updateQuestion() {
        // Update timeframe text with question number
        textViewTimeFrame.setText((currentQuestionIndex + 1) + ") In the past 4 hours...");

        // Update question text
        textViewQuestion.setText(questions[currentQuestionIndex]);

        // Update slider value
        if (hasTouched[currentQuestionIndex]) {
            sliderQuestion.setValue(responses[currentQuestionIndex]);
            // Make thumb visible
            sliderQuestion.setThumbTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
            sliderQuestion.setHaloRadius(getResources().getDimensionPixelSize(R.dimen.default_halo_radius));
            updateSliderValueDisplay(responses[currentQuestionIndex]);
        } else {
            sliderQuestion.setValue(50); // Default value
            // Set thumb to fully transparent initially
            sliderQuestion.setThumbTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            sliderQuestion.setHaloRadius(0); // Remove halo effect
            textViewSliderValue.setText("");  // Clear the value display
        }

        // Update back button visibility
        if (currentQuestionIndex == 0) {
            btnBack.setVisibility(View.GONE);
        } else {
            btnBack.setVisibility(View.VISIBLE);
        }

        // Update next button text
        if (currentQuestionIndex == totalQuestions - 1) {
            btnNext.setText("Submit");
        } else {
            btnNext.setText("Next");
        }
    }

    // Handle the Up button and back press
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            confirmCancel();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        confirmCancel();
    }

    private void confirmCancel() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Assessment")
                .setMessage("Are you sure you want to cancel this assessment?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    private void submitAssessment() {
        for (int i = 0; i < totalQuestions; i++) {
            if (!hasTouched[i]) {
                Toast.makeText(this, "Please answer all questions.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Calculate the average score
        float total = 0;
        for (int score : responses) {
            total += score;
        }
        float average = total / responses.length;

        // Create a new MPHQ9Entity with the calculated average score
        MPHQ9Entity assessment = new MPHQ9Entity();
        assessment.timestamp = System.currentTimeMillis();
        assessment.q1 = responses[0];
        assessment.q2 = responses[1];
        assessment.q3 = responses[2];
        assessment.q4 = responses[3];
        assessment.q5 = responses[4];
        assessment.q6 = responses[5];
        assessment.q7 = responses[6];
        assessment.q8 = responses[7];
        assessment.q9 = responses[8];
        assessment.averageScore = average;

        // Insert into the database via ViewModel
        mphq9ViewModel.insert(assessment);

        Toast.makeText(this, "Assessment Submitted. Average Score: " + average + " / 100", Toast.LENGTH_LONG).show();
        finish(); // Close activity
    }
}
