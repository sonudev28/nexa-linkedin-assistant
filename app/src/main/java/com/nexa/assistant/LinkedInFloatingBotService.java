package com.nexa.assistant;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LinkedInFloatingBotService extends AccessibilityService {

    private WindowManager windowManager;
    private View floatingBubble;
    private View overlayDialog;
    private boolean isBubbleAttached = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String SERVER_URL = "https://chat-pilot-platform.vercel.app/api/v1/nexa/reply";
    private static final String LOCAL_URL = "http://10.98.172.180:5000/api/reply";
    private static final String LINKEDIN_PKG = "com.linkedin.android";

    private WindowManager.LayoutParams bubbleParams;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createFloatingBubble();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        mainHandler.post(() -> Toast.makeText(this, "⚡ Nexa Assistant Active! Open LinkedIn to use.", Toast.LENGTH_LONG).show());
    }

    private void createFloatingBubble() {
        int size = (int) (58 * getResources().getDisplayMetrics().density);

        Button bubble = new Button(this);
        bubble.setText("⚡");
        bubble.setTextSize(22);
        bubble.setTextColor(Color.WHITE);

        // Circular styling
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(Color.parseColor("#0284c7")); // Sky blue branding
        shape.setStroke(4, Color.parseColor("#38bdf8"));
        bubble.setBackground(shape);

        bubbleParams = new WindowManager.LayoutParams(
                size, size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        // Pin to Bottom-Right corner
        bubbleParams.gravity = Gravity.BOTTOM | Gravity.END;
        bubbleParams.x = (int) (18 * getResources().getDisplayMetrics().density);
        bubbleParams.y = (int) (120 * getResources().getDisplayMetrics().density);

        // Draggable bubble support
        bubble.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isClick;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isClick = true;
                        return true;
                    case android.view.MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false;
                        }
                        bubbleParams.x = initialX - (int) dx;
                        bubbleParams.y = initialY - (int) dy;
                        if (isBubbleAttached) {
                            try {
                                windowManager.updateViewLayout(floatingBubble, bubbleParams);
                            } catch (Exception ignored) {}
                        }
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                        if (isClick) {
                            handleBubbleClick();
                        }
                        return true;
                }
                return false;
            }
        });

        floatingBubble = bubble;
    }

    private void showBubble() {
        if (!isBubbleAttached && floatingBubble != null && bubbleParams != null) {
            try {
                windowManager.addView(floatingBubble, bubbleParams);
                isBubbleAttached = true;
            } catch (Exception ignored) {}
        }
    }

    private void hideBubble() {
        if (isBubbleAttached && floatingBubble != null) {
            try {
                windowManager.removeView(floatingBubble);
            } catch (Exception ignored) {}
            isBubbleAttached = false;
        }
        dismissOverlay();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString().toLowerCase();

        // STRICT PRIVACY: Only show bubble when LinkedIn is actively in foreground!
        if (pkg.contains("linkedin")) {
            mainHandler.post(this::showBubble);
        } else {
            mainHandler.post(this::hideBubble);
        }
    }

    @Override
    public void onInterrupt() {
        hideBubble();
    }

    private void handleBubbleClick() {
        Toast.makeText(this, "⚡ Analyzing LinkedIn screen...", Toast.LENGTH_SHORT).show();

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            Toast.makeText(this, "Could not read screen. Make sure LinkedIn is open.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> texts = new ArrayList<>();
        extractTexts(root, texts);

        String combinedText = String.join("\n", texts);

        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("rawScreenText", combinedText);
                payload.put("context", "Screen captured directly from LinkedIn Android App");

                URL url = new URL(SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(12000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes("utf-8"));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    JSONObject res = new JSONObject(sb.toString());

                    mainHandler.post(() -> showReplyOverlay(res));
                } else {
                    mainHandler.post(() -> Toast.makeText(this, "Server error: " + responseCode, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Connection failed: Check internet connection or try again!", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void extractTexts(AccessibilityNodeInfo node, List<String> list) {
        if (node == null) return;
        CharSequence cs = node.getText();
        if (cs != null && cs.length() > 0) {
            list.add(cs.toString().trim());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            extractTexts(node.getChild(i), list);
        }
    }

    private void showReplyOverlay(JSONObject data) {
        dismissOverlay();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        // Dark modern card style
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0f172a"));
        cardBg.setCornerRadius(28);
        cardBg.setStroke(3, Color.parseColor("#38bdf8"));
        layout.setBackground(cardBg);

        String name = data.optString("personName", "Lead");
        String category = data.optString("category", "General");
        String recommended = data.optString("recommended", "");
        String tip = data.optString("copyNotice", "");

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🎯 " + name + " (" + category.replace("_", " ") + ")");
        tvTitle.setTextColor(Color.parseColor("#38bdf8"));
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTitle);

        if (!tip.isEmpty()) {
            TextView tvTip = new TextView(this);
            tvTip.setText("💡 " + tip);
            tvTip.setTextColor(Color.parseColor("#94a3b8"));
            tvTip.setTextSize(12);
            tvTip.setPadding(0, 8, 0, 16);
            layout.addView(tvTip);
        }

        ScrollView scroll = new ScrollView(this);
        TextView tvMsg = new TextView(this);
        tvMsg.setText(recommended);
        tvMsg.setTextColor(Color.WHITE);
        tvMsg.setTextSize(14.5f);
        tvMsg.setLineSpacing(6, 1.2f);
        scroll.addView(tvMsg);
        layout.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 500));

        // Copy & Close Button
        Button btnCopy = new Button(this);
        btnCopy.setText("📋 Copy & Close");
        btnCopy.setTextColor(Color.parseColor("#0f172a"));
        btnCopy.setTextSize(15);
        btnCopy.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#10b981"));
        btnBg.setCornerRadius(16);
        btnCopy.setBackground(btnBg);
        btnCopy.setPadding(0, 24, 0, 24);

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Nexa Reply", recommended);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied! ✓ Paste in LinkedIn chat", Toast.LENGTH_SHORT).show();
            dismissOverlay();
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 24, 0, 0);
        layout.addView(btnCopy, btnParams);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.BOTTOM;
        params.x = 20;
        params.y = 80;

        overlayDialog = layout;
        windowManager.addView(overlayDialog, params);
    }

    private void dismissOverlay() {
        if (overlayDialog != null) {
            try {
                windowManager.removeView(overlayDialog);
                overlayDialog = null;
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideBubble();
        executor.shutdown();
    }
}
