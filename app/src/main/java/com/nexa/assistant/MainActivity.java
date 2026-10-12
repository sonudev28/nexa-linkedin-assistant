package com.nexa.assistant;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(60, 60, 60, 60);
        layout.setBackgroundColor(Color.parseColor("#0b1120"));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚡ Nexa LinkedIn Assistant");
        tvTitle.setTextSize(22);
        tvTitle.setTextColor(Color.parseColor("#38bdf8"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        layout.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("This app attaches a floating AI bubble over LinkedIn. Whenever you open a chat or profile, tap the bubble in bottom-right to get an instant tailored reply.");
        tvDesc.setTextSize(14);
        tvDesc.setTextColor(Color.parseColor("#94a3b8"));
        tvDesc.setGravity(Gravity.CENTER);
        tvDesc.setPadding(0, 24, 0, 48);
        layout.addView(tvDesc);

        Button btnEnable = new Button(this);
        btnEnable.setText("🚀 Enable Accessibility Service");
        btnEnable.setTextColor(Color.parseColor("#0f172a"));
        btnEnable.setTextSize(16);
        btnEnable.setTypeface(null, android.graphics.Typeface.BOLD);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#10b981"));
        btnBg.setCornerRadius(20);
        btnEnable.setBackground(btnBg);
        btnEnable.setPadding(0, 36, 0, 36);

        btnEnable.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Find 'Nexa LinkedIn Assistant' and turn it ON", Toast.LENGTH_LONG).show();
        });

        layout.addView(btnEnable, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button btnLaunchLinkedIn = new Button(this);
        btnLaunchLinkedIn.setText("🔗 Open LinkedIn App");
        btnLaunchLinkedIn.setTextColor(Color.WHITE);
        btnLaunchLinkedIn.setTextSize(15);
        btnLaunchLinkedIn.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable btnLaunchBg = new GradientDrawable();
        btnLaunchBg.setColor(Color.parseColor("#0284c7"));
        btnLaunchBg.setCornerRadius(20);
        btnLaunchLinkedIn.setBackground(btnLaunchBg);
        btnLaunchLinkedIn.setPadding(0, 32, 0, 32);
        btnLaunchLinkedIn.setOnClickListener(v -> {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.linkedin.android");
            if (launchIntent == null) {
                launchIntent = getPackageManager().getLaunchIntentForPackage("com.linkedin.android.lite");
            }
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
            } else {
                try {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.linkedin.com"));
                    viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(viewIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not open LinkedIn", Toast.LENGTH_SHORT).show();
                }
            }
        });

        LinearLayout.LayoutParams launchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        launchParams.setMargins(0, 24, 0, 0);
        layout.addView(btnLaunchLinkedIn, launchParams);

        Button btnPermissions = new Button(this);
        btnPermissions.setText("⚙️ App Permissions (Allow Pop-ups)");
        btnPermissions.setTextColor(Color.parseColor("#38bdf8"));
        btnPermissions.setTextSize(13);
        GradientDrawable permBg = new GradientDrawable();
        permBg.setColor(Color.parseColor("#1e293b"));
        permBg.setCornerRadius(20);
        permBg.setStroke(2, Color.parseColor("#334155"));
        btnPermissions.setBackground(permBg);
        btnPermissions.setPadding(0, 28, 0, 28);
        btnPermissions.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });

        LinearLayout.LayoutParams permParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        permParams.setMargins(0, 20, 0, 0);
        layout.addView(btnPermissions, permParams);

        Button btnToggle = new Button(this);
        btnToggle.setText("👁️ Show / Hide Floating Icon");
        btnToggle.setTextColor(Color.WHITE);
        btnToggle.setTextSize(14);
        btnToggle.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable toggleBg = new GradientDrawable();
        toggleBg.setColor(Color.parseColor("#475569"));
        toggleBg.setCornerRadius(20);
        btnToggle.setBackground(toggleBg);
        btnToggle.setPadding(0, 30, 0, 30);
        btnToggle.setOnClickListener(v -> {
            if (LinkedInFloatingBotService.instance != null) {
                LinkedInFloatingBotService.instance.toggleBubble();
            } else {
                Toast.makeText(this, "Please enable Accessibility Service first!", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        toggleParams.setMargins(0, 20, 0, 0);
        layout.addView(btnToggle, toggleParams);

        setContentView(layout);
    }
}
