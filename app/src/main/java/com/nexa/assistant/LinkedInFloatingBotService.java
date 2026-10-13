package com.nexa.assistant;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // 100% Cloud-Only Serverless Endpoint (Zero laptop dependency)
    private static final String SERVER_URL = "https://chat-pilot-platform.vercel.app/api/v1/nexa/reply";

    private WindowManager.LayoutParams bubbleParams;

    public static LinkedInFloatingBotService instance;

    @Override
    public void onCreate() {
        super.onCreate();
        // Prevent any unexpected runtime error from crashing the AccessibilityService (fixes MIUI "Malfunctioning/Error" state)
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                isAnalyzing = false;
            } catch (Throwable ignored) {}
        });
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        startKeepAliveForeground();
        createFloatingBubble();
    }

    private void startKeepAliveForeground() {
        try {
            String channelId = "nexa_assistant_keepalive";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "Nexa Assistant Active Service",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Keeps Nexa LinkedIn Assistant active in background");
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.createNotificationChannel(channel);
                }
                Notification notification = new Notification.Builder(this, channelId)
                        .setContentTitle("⚡ Nexa LinkedIn Assistant")
                        .setContentText("24/7 Cloud AI Bot is ready for LinkedIn")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setOngoing(true)
                        .build();
                startForeground(1001, notification);
            }
        } catch (Throwable ignored) {
            // Safe fallback if foreground notification is restricted
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        startKeepAliveForeground();
        mainHandler.post(() -> {
            try {
                if (floatingBubble == null) {
                    createFloatingBubble();
                }
                showBubble();
                Toast.makeText(this, "⚡ Nexa Assistant Connected (24/7 Cloud Mode)!", Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {}
        });
    }

    public void toggleBubble() {
        mainHandler.post(() -> {
            try {
                if (isBubbleAttached) {
                    hideBubble();
                    Toast.makeText(this, "Floating Bot Hidden", Toast.LENGTH_SHORT).show();
                } else {
                    if (floatingBubble == null) {
                        createFloatingBubble();
                    }
                    showBubble();
                    Toast.makeText(this, "Floating Bot Shown", Toast.LENGTH_SHORT).show();
                }
            } catch (Throwable ignored) {}
        });
    }

    private void createFloatingBubble() {
        int size = (int) (60 * getResources().getDisplayMetrics().density);

        Button bubble = new Button(this);
        bubble.setText("⚡");
        bubble.setTextSize(24);
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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        // Pin to Bottom-Right corner
        bubbleParams.gravity = Gravity.BOTTOM | Gravity.END;
        bubbleParams.x = (int) (18 * getResources().getDisplayMetrics().density);
        bubbleParams.y = (int) (120 * getResources().getDisplayMetrics().density);

        final int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop() * 2;
        bubble.setClickable(true);
        bubble.setFocusable(false);

        // Draggable bubble support & guaranteed tap execution via performClick()
        bubble.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isDrag = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDrag = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.hypot(dx, dy) > touchSlop) {
                            isDrag = true;
                            bubbleParams.x = initialX - (int) dx;
                            bubbleParams.y = initialY - (int) dy;
                            if (isBubbleAttached) {
                                try {
                                    windowManager.updateViewLayout(floatingBubble, bubbleParams);
                                } catch (Exception ignored) {}
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDrag) {
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        });

        bubble.setOnClickListener(v -> {
            mainHandler.post(this::handleBubbleClick);
        });

        floatingBubble = bubble;
    }

    private void showBubble() {
        if (!isBubbleAttached && floatingBubble != null && bubbleParams != null) {
            try {
                windowManager.addView(floatingBubble, bubbleParams);
                isBubbleAttached = true;
            } catch (Exception ignored) {
                isBubbleAttached = false;
            }
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
        if (event == null) return;
        // Keep bubble active and reliable when LinkedIn is visited
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence cs = event.getPackageName();
            if (cs != null && cs.toString().toLowerCase().contains("linkedin")) {
                mainHandler.post(this::showBubble);
            }
        }
    }

    @Override
    public void onInterrupt() {
        // Do not hide bubble on transient system interrupts
    }

    private boolean isAnalyzing = false;

    private void setBubbleLoadingState(boolean loading) {
        isAnalyzing = loading;
        if (floatingBubble instanceof Button) {
            Button btn = (Button) floatingBubble;
            if (loading) {
                btn.setText("⏳");
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.OVAL);
                shape.setColor(Color.parseColor("#d97706")); // Amber orange "thinking" mode
                shape.setStroke(4, Color.parseColor("#fbbf24"));
                btn.setBackground(shape);
            } else {
                btn.setText("⚡");
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.OVAL);
                shape.setColor(Color.parseColor("#0284c7")); // Sky blue normal
                shape.setStroke(4, Color.parseColor("#38bdf8"));
                btn.setBackground(shape);
            }
        }
    }

    private void handleBubbleClick() {
        if (isAnalyzing) {
            Toast.makeText(this, "🧠 Thinking in progress, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            Toast.makeText(this, "⚠️ Please open LinkedIn first!", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence rootPkg = root.getPackageName();
        if (rootPkg != null && !rootPkg.toString().toLowerCase().contains("linkedin")) {
            Toast.makeText(this, "🔒 Nexa Bot only inspects LinkedIn for privacy. Please open LinkedIn!", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> texts = new ArrayList<>();
        extractTexts(root, texts);

        if (texts.isEmpty()) {
            Toast.makeText(this, "⚠️ No readable text found. Open a conversation or profile.", Toast.LENGTH_SHORT).show();
            return;
        }

        setBubbleLoadingState(true);
        Toast.makeText(this, "🧠 Reading conversation & generating response...", Toast.LENGTH_SHORT).show();

        String combinedText = String.join("\n", texts);

        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }

        executor.execute(() -> {
            int maxAttempts = 2;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("rawScreenText", combinedText);
                    payload.put("context", "Screen captured directly from LinkedIn Android App");

                    URL url = new URL(SERVER_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(20000);
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

                        mainHandler.post(() -> {
                            setBubbleLoadingState(false);
                            showReplyOverlay(res);
                        });
                        return;
                    } else if (attempt == maxAttempts) {
                        final int code = responseCode;
                        mainHandler.post(() -> {
                            setBubbleLoadingState(false);
                            Toast.makeText(this, "Cloud busy (" + code + "). Please tap ⚡ once more!", Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    if (attempt == maxAttempts) {
                        mainHandler.post(() -> {
                            setBubbleLoadingState(false);
                            Toast.makeText(this, "Network slow: Please check mobile internet & tap ⚡ again!", Toast.LENGTH_LONG).show();
                        });
                    }
                }
            }
        });
    }

    private void extractTexts(AccessibilityNodeInfo node, List<String> list) {
        if (node == null) return;
        try {
            CharSequence cs = node.getText();
            if (cs != null && cs.length() > 0) {
                String trimmed = cs.toString().trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
            CharSequence desc = node.getContentDescription();
            if (desc != null && desc.length() > 0) {
                String descTrimmed = desc.toString().trim();
                if (!descTrimmed.isEmpty() && (cs == null || !descTrimmed.equalsIgnoreCase(cs.toString().trim()))) {
                    list.add(descTrimmed);
                }
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                extractTexts(node.getChild(i), list);
            }
        } catch (Throwable ignored) {}
    }

    private void showReplyOverlay(JSONObject data) {
        try {
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
            String tip = data.optString("copyNotice", "");

            // Extra guarantee: clean any bullet dashes from display
            final String recommended = data.optString("recommended", "")
                    .replaceAll("(?m)^[\\s]*[-—•*]\\s+", "")
                    .replaceAll("\n[\\s]*[-—•*]\\s+", "\n")
                    .replaceAll(" - ", ", ")
                    .trim();

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

            // Button Row: Close & Copy
            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);

            Button btnClose = new Button(this);
            btnClose.setText("✕ Close");
            btnClose.setTextColor(Color.parseColor("#94a3b8"));
            btnClose.setTextSize(14);
            GradientDrawable closeBg = new GradientDrawable();
            closeBg.setColor(Color.parseColor("#1e293b"));
            closeBg.setCornerRadius(16);
            btnClose.setBackground(closeBg);
            btnClose.setPadding(30, 24, 30, 24);
            btnClose.setOnClickListener(v -> dismissOverlay());

            LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            closeParams.setMargins(0, 24, 12, 0);
            btnRow.addView(btnClose, closeParams);

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

            LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.0f);
            copyParams.setMargins(12, 24, 0, 0);
            btnRow.addView(btnCopy, copyParams);

            layout.addView(btnRow);

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
        } catch (Throwable ignored) {}
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
    public boolean onUnbind(android.content.Intent intent) {
        hideBubble();
        instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideBubble();
        instance = null;
    }
}
