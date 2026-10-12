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

        setContentView(layout);
    }
}
