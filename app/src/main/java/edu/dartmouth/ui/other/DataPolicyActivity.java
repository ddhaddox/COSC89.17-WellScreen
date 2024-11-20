package edu.dartmouth.ui.other;

import static edu.dartmouth.MainActivity.isNightMode;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import edu.dartmouth.R;

import android.text.Html;
import android.view.View;
import android.widget.TextView;

public class DataPolicyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_policy);

        // Set up the Toolbar with Up button
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.data_policy_intro_header);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.white));
            }
        }

        // Initialize Content TextViews and set HTML content
        TextView introContent = findViewById(R.id.tvIntroContent);
        introContent.setText(Html.fromHtml(getString(R.string.data_policy_intro_content), Html.FROM_HTML_MODE_COMPACT));

        TextView dataWeCollectContent = findViewById(R.id.tvDataWeCollectContent);
        dataWeCollectContent.setText(Html.fromHtml(getString(R.string.data_policy_data_we_collect_content), Html.FROM_HTML_MODE_COMPACT));

        TextView howWeStoreContent = findViewById(R.id.tvHowWeStoreContent);
        howWeStoreContent.setText(Html.fromHtml(getString(R.string.data_policy_how_we_store_your_data_content), Html.FROM_HTML_MODE_COMPACT));

        TextView howWeUseContent = findViewById(R.id.tvHowWeUseContent);
        howWeUseContent.setText(Html.fromHtml(getString(R.string.data_policy_how_we_use_your_data_content), Html.FROM_HTML_MODE_COMPACT));

        TextView dataRetentionContent = findViewById(R.id.tvDataRetentionContent);
        dataRetentionContent.setText(Html.fromHtml(getString(R.string.data_policy_data_retention_content), Html.FROM_HTML_MODE_COMPACT));

        TextView permissionsContent = findViewById(R.id.tvPermissionsContent);
        permissionsContent.setText(Html.fromHtml(getString(R.string.data_policy_permissions_we_require_content), Html.FROM_HTML_MODE_COMPACT));

        TextView userControlContent = findViewById(R.id.tvUserControlContent);
        userControlContent.setText(Html.fromHtml(getString(R.string.data_policy_user_control_content), Html.FROM_HTML_MODE_COMPACT));

        // Handle the visibility of the line under the Toolbar based on Night Mode
        View lineUnderToolbar = findViewById(R.id.lineUnderToolbar);

        if (isNightMode(this)) {
            lineUnderToolbar.setVisibility(View.VISIBLE);
        } else {
            lineUnderToolbar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
