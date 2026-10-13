package com.nexa.assistant;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(60, 50, 60, 50);
        layout.setBackgroundColor(Color.parseColor("#0b1120"));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚡ Nexa LinkedIn Assistant");
        tvTitle.setTextSize(22);
        tvTitle.setTextColor(Color.parseColor("#38bdf8"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        layout.addView(tvTitle);

        tvStatus = new TextView(this);
        tvStatus.setTextSize(13.5f);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(24, 20, 24, 20);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, 20, 0, 20);
        layout.addView(tvStatus, statusParams);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("100% Cloud-Powered (No Laptop Needed). If Xiaomi/MIUI ever shows 'Error' or stops the icon overnight, tap the green button below and turn 'Nexa LinkedIn Assistant' OFF and ON once.");
        tvDesc.setTextSize(13);
        tvDesc.setTextColor(Color.parseColor("#94a3b8"));
        tvDesc.setGravity(Gravity.CENTER);
        tvDesc.setPadding(0, 8, 0, 32);
        layout.addView(tvDesc);

        Button btnEnable = new Button(this);
        btnEnable.setText("🚀 Enable / Restart Accessibility Service");
        btnEnable.setTextColor(Color.parseColor("#0f172a"));
        btnEnable.setTextSize(15);
        btnEnable.setTypeface(null, android.graphics.Typeface.BOLD);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#10b981"));
        btnBg.setCornerRadius(20);
        btnEnable.setBackground(btnBg);
        btnEnable.setPadding(0, 32, 0, 32);

        btnEnable.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Open 'Downloaded Apps' -> 'Nexa LinkedIn Assistant' -> Turn OFF & ON once!", Toast.LENGTH_LONG).show();
        });

        layout.addView(btnEnable, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button btnBattery = new Button(this);
        btnBattery.setText("🔋 Keep Alive 24/7 (Disable Battery Saver)");
        btnBattery.setTextColor(Color.parseColor("#fbbf24"));
        btnBattery.setTextSize(13.5f);
        btnBattery.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable batBg = new GradientDrawable();
        batBg.setColor(Color.parseColor("#1e293b"));
        batBg.setCornerRadius(20);
        batBg.setStroke(2, Color.parseColor("#d97706"));
        btnBattery.setBackground(batBg);
        btnBattery.setPadding(0, 28, 0, 28);
        btnBattery.setOnClickListener(v -> requestBatteryExemption());

        LinearLayout.LayoutParams batParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        batParams.setMargins(0, 20, 0, 0);
        layout.addView(btnBattery, batParams);

        Button btnLaunchLinkedIn = new Button(this);
        btnLaunchLinkedIn.setText("🔗 Open LinkedIn App");
        btnLaunchLinkedIn.setTextColor(Color.WHITE);
        btnLaunchLinkedIn.setTextSize(15);
        btnLaunchLinkedIn.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable btnLaunchBg = new GradientDrawable();
        btnLaunchBg.setColor(Color.parseColor("#0284c7"));
        btnLaunchBg.setCornerRadius(20);
        btnLaunchLinkedIn.setBackground(btnLaunchBg);
        btnLaunchLinkedIn.setPadding(0, 30, 0, 30);
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
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com"));
                    viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(viewIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not open LinkedIn", Toast.LENGTH_SHORT).show();
                }
            }
        });

        LinearLayout.LayoutParams launchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        launchParams.setMargins(0, 20, 0, 0);
        layout.addView(btnLaunchLinkedIn, launchParams);

        Button btnToggle = new Button(this);
        btnToggle.setText("👁️ Show / Hide Floating Icon");
        btnToggle.setTextColor(Color.WHITE);
        btnToggle.setTextSize(14);
        btnToggle.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable toggleBg = new GradientDrawable();
        toggleBg.setColor(Color.parseColor("#475569"));
        toggleBg.setCornerRadius(20);
        btnToggle.setBackground(toggleBg);
        btnToggle.setPadding(0, 28, 0, 28);
        btnToggle.setOnClickListener(v -> {
            if (LinkedInFloatingBotService.instance != null) {
                LinkedInFloatingBotService.instance.toggleBubble();
            } else {
                Toast.makeText(this, "⚠️ Phone paused the service! In 'Downloaded Apps', turn Nexa OFF and ON once.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });

        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        toggleParams.setMargins(0, 20, 0, 0);
        layout.addView(btnToggle, toggleParams);

        setContentView(layout);
        updateStatusBanner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusBanner();
    }

    private void updateStatusBanner() {
        if (tvStatus == null) return;
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setCornerRadius(16);
        if (LinkedInFloatingBotService.instance != null) {
            tvStatus.setText("🟢 Status: ACTIVE & Connected to 24/7 Cloud");
            tvStatus.setTextColor(Color.parseColor("#10b981"));
            statusBg.setColor(Color.parseColor("#064e3b"));
            statusBg.setStroke(2, Color.parseColor("#10b981"));
        } else {
            tvStatus.setText("🟠 Status: Paused by Phone Battery Saver\nTap green button -> Turn Nexa OFF & ON once");
            tvStatus.setTextColor(Color.parseColor("#fbbf24"));
            statusBg.setColor(Color.parseColor("#451a03"));
            statusBg.setStroke(2, Color.parseColor("#d97706"));
        }
        tvStatus.setBackground(statusBg);
    }

    private void requestBatteryExemption() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    return;
                }
            }
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Set Battery Saver to 'No restrictions' & enable 'Autostart'", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
}
