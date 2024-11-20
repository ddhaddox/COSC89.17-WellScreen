package edu.dartmouth.ui.other;

import static edu.dartmouth.MainActivity.isNightMode;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import edu.dartmouth.R;
import edu.dartmouth.ui.appusage.AppUsageActivity;
import edu.dartmouth.ui.mphq9.MPHQ9ListActivity;
import edu.dartmouth.ui.screenstate.ScreenStateActivity;

public class ViewDataActivity extends AppCompatActivity {

    private Button btnViewAppUsage;
    private Button btnViewScreenState;
    private Button btnViewPHQ9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("View Data");
        setSupportActionBar(toolbar);

        // Enable the Up button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Manually set the navigation icon tint to ensure visibility
            toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.white));
        }

        btnViewAppUsage = findViewById(R.id.btnViewAppUsage);
        btnViewAppUsage.setOnClickListener(v -> {
            Intent intent = new Intent(ViewDataActivity.this, AppUsageActivity.class);
            startActivity(intent);
        });

        btnViewScreenState = findViewById(R.id.btnViewScreenState);
        btnViewScreenState.setOnClickListener(v -> {
            Intent intent = new Intent(ViewDataActivity.this, ScreenStateActivity.class);
            startActivity(intent);
        });

        btnViewPHQ9 = findViewById(R.id.btnViewMPHQ9);
        btnViewPHQ9.setOnClickListener(v -> {
            Intent intent = new Intent(ViewDataActivity.this, MPHQ9ListActivity.class);
            startActivity(intent);
        });

        View lineUnderToolbar = findViewById(R.id.lineUnderToolbar);

        if (isNightMode(this)) {
            lineUnderToolbar.setVisibility(View.VISIBLE);
        } else {
            lineUnderToolbar.setVisibility(View.GONE);
        }
    }

    // Handle the Up button click
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
