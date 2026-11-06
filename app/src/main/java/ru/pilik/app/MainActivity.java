package ru.pilik.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(10, 15, 18);
    private static final int CARD = Color.rgb(24, 31, 34);
    private static final int GREEN = Color.rgb(0, 229, 154);
    private static final int WHITE = Color.rgb(235, 244, 241);
    private static final int MUTED = Color.rgb(142, 165, 156);
    private SharedPreferences prefs;
    private TextView nextText;
    private TextView permissionsText;
    private Button exactButton;
    private Button notifyButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateNext = new Runnable() {
        @Override public void run() {
            refreshNext();
            handler.postDelayed(this, 30_000L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = ChimeSettings.get(this);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        createScreen();
        AlarmScheduler.schedule(this);
    }

    @Override protected void onResume() {
        super.onResume();
        AlarmScheduler.schedule(this);
        refreshPermissions();
        handler.removeCallbacks(updateNext);
        handler.post(updateNext);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(updateNext);
        super.onPause();
    }

    private int dp(float value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private GradientDrawable shape(int fill, int corner, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(corner));
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private LinearLayout.LayoutParams full() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView label(String text, int size, int color, boolean mono) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        if (mono) t.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        return t;
    }

    private void gap(LinearLayout parent, int height) {
        View spacer = new View(this);
        parent.addView(spacer, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout panel = column();
        panel.setPadding(dp(18), dp(19), dp(18), dp(19));
        panel.setBackground(shape(CARD, 20, Color.TRANSPARENT));
        parent.addView(panel, full());
        return panel;
    }

    private Button button(String text, boolean outline) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(outline ? GREEN : BACKGROUND);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(shape(outline ? CARD : GREEN, 12,
                outline ? GREEN : Color.TRANSPARENT));
        b.setPadding(dp(15), dp(4), dp(15), dp(4));
        b.setMinHeight(dp(46));
        b.setMinimumHeight(dp(46));
        return b;
    }

    @SuppressWarnings("deprecation")
    private Switch toggle(String title, boolean active) {
        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextColor(WHITE);
        sw.setTextSize(16);
        sw.setChecked(active);
        sw.setThumbTintList(ColorStateList.valueOf(GREEN));
        sw.setTrackTintList(ColorStateList.valueOf(Color.rgb(41, 88, 72)));
        return sw;
    }

    private void createScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        LinearLayout main = column();
        main.setPadding(dp(18), dp(22), dp(18), dp(35));
        scroll.addView(main);
        setContentView(scroll);

        TextView brand = label("● ПИЛИК", 25, GREEN, true);
        brand.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        main.addView(brand);
        TextView subtitle = label("Сигнал времени · без подписки", 13, MUTED, false);
        main.addView(subtitle);
        gap(main, 22);

        LinearLayout clockPanel = card(main);
        TextView clockTitle = label("ЭЛЕКТРОННЫЕ ЧАСЫ", 12, MUTED, true);
        clockPanel.addView(clockTitle);
        gap(clockPanel, 3);
        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(GREEN);
        clock.setTextSize(60);
        clock.setGravity(Gravity.CENTER);
        clock.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        clockPanel.addView(clock, full());
        nextText = label("", 13, WHITE, false);
        nextText.setGravity(Gravity.CENTER);
        clockPanel.addView(nextText, full());
        gap(main, 16);

        LinearLayout general = card(main);
        Switch master = toggle("Включить пиликанье", prefs.getBoolean(ChimeSettings.ON, true));
        general.addView(master, full());
        master.setOnCheckedChangeListener((view, checked) -> {
            prefs.edit().putBoolean(ChimeSettings.ON, checked).apply();
            applySchedule();
        });
        gap(general, 18);
        general.addView(label("ИНТЕРВАЛ СИГНАЛА", 12, MUTED, true));
        gap(general, 7);
        Spinner intervalSpinner = spinner(new String[]{
                "Каждый час", "Каждые 30 минут", "Каждые 15 минут"});
        general.addView(intervalSpinner, full());
        int interval = ChimeSettings.interval(this);
        intervalSpinner.setSelection(interval == 15 ? 2 : interval == 30 ? 1 : 0);
        intervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                int value = pos == 2 ? 15 : pos == 1 ? 30 : 60;
                if (prefs.getInt(ChimeSettings.INTERVAL, 60) != value) {
                    prefs.edit().putInt(ChimeSettings.INTERVAL, value).apply();
                    applySchedule();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        gap(main, 16);

        createTimeSection(main, "БУДНИ · ПОНЕДЕЛЬНИК–ПЯТНИЦА", "weekdays", 7 * 60);
        gap(main, 16);
        createTimeSection(main, "ВЫХОДНЫЕ · СУББОТА–ВОСКРЕСЕНЬЕ", "weekends", 10 * 60);
        gap(main, 16);

        LinearLayout audio = card(main);
        audio.addView(label("ЗВУК", 12, MUTED, true));
        gap(audio, 10);
        Spinner soundSpinner = spinner(new String[]{"Один пилик", "Два пилика"});
        audio.addView(soundSpinner, full());
        soundSpinner.setSelection(ChimeSettings.soundId(this));
        soundSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                if (prefs.getInt(ChimeSettings.SOUND, 1) != pos) {
                    prefs.edit().putInt(ChimeSettings.SOUND, pos).apply();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        gap(audio, 12);
        Switch vibrate = toggle("Вибрация вместе со звуком",
                prefs.getBoolean(ChimeSettings.VIBRATION, false));
        audio.addView(vibrate, full());
        vibrate.setOnCheckedChangeListener((view, checked) ->
                prefs.edit().putBoolean(ChimeSettings.VIBRATION, checked).apply());
        gap(audio, 17);
        Button test = button("▶ Проверить звук", false);
        audio.addView(test, full());
        test.setOnClickListener(v -> testSound());
        gap(main, 16);

        LinearLayout permissions = card(main);
        permissions.addView(label("РАБОТА В ФОНЕ", 12, MUTED, true));
        gap(permissions, 9);
        permissionsText = label("", 14, WHITE, false);
        permissions.addView(permissionsText, full());
        gap(permissions, 9);
        exactButton = button("Разрешить точные сигналы", true);
        permissions.addView(exactButton, full());
        exactButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            android.net.Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                    Toast.makeText(this, "Откройте раздел «Будильники и напоминания»",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        gap(permissions, 8);
        notifyButton = button("Разрешить уведомления", true);
        permissions.addView(notifyButton, full());
        notifyButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 123);
            }
        });
        gap(permissions, 11);
        TextView tip = label("Для надёжной работы на некоторых телефонах нужно "
                + "разрешить приложению работу в фоне и не ограничивать расход батареи. "
                + "Режим «Не беспокоить» и беззвучный режим телефона могут заглушить сигнал.",
                12, MUTED, false);
        permissions.addView(tip, full());
        gap(main, 19);
        TextView footer = label("ПИЛИК 1.0  •  ВСЁ НА РУССКОМ  •  БЕЗ ИНТЕРНЕТА", 10, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        main.addView(footer, full());
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, values) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView text = label(getItem(position), 17, GREEN, false);
                text.setPadding(dp(12), dp(12), dp(12), dp(12));
                return text;
            }
            @Override public View getDropDownView(int position, View convertView,
                                                   ViewGroup parent) {
                TextView text = label(getItem(position), 16, WHITE, false);
                text.setBackgroundColor(CARD);
                text.setPadding(dp(15), dp(15), dp(15), dp(15));
                return text;
            }
        };
        spinner.setAdapter(adapter);
        spinner.setBackground(shape(Color.rgb(17, 24, 27), 10, Color.rgb(54, 72, 67)));
        return spinner;
    }

    private void createTimeSection(LinearLayout main, String title,
                                   String prefix, int defaultStart) {
        LinearLayout panel = card(main);
        panel.addView(label(title, 12, MUTED, true));
        gap(panel, 10);
        Switch active = toggle("Пиликать в эти дни",
                prefs.getBoolean(prefix + "_on", true));
        panel.addView(active, full());
        active.setOnCheckedChangeListener((view, enabled) -> {
            prefs.edit().putBoolean(prefix + "_on", enabled).apply();
            applySchedule();
        });
        gap(panel, 11);
        createTimeRow(panel, "Начинать с", prefix + "_start", defaultStart,
                prefix + "_end", 22 * 60, true);
        gap(panel, 9);
        createTimeRow(panel, "Заканчивать до", prefix + "_end", 22 * 60,
                prefix + "_start", defaultStart, false);
        gap(panel, 8);
        TextView note = label("Время окончания не включается в расписание.",
                12, MUTED, false);
        panel.addView(note);
    }

    private void createTimeRow(LinearLayout parent, String caption,
                               String key, int defaultValue, String otherKey,
                               int otherDefault, boolean isStart) {
        LinearLayout line = row();
        TextView name = label(caption, 15, WHITE, false);
        line.addView(name, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button time = button(timeText(prefs.getInt(key, defaultValue)), true);
        line.addView(time, new LinearLayout.LayoutParams(dp(115), dp(46)));
        parent.addView(line, full());
        time.setOnClickListener(v -> {
            int current = prefs.getInt(key, defaultValue);
            new TimePickerDialog(this, (picker, hour, minute) -> {
                int chosen = hour * 60 + minute;
                int other = prefs.getInt(otherKey, otherDefault);
                if ((isStart && chosen >= other) || (!isStart && chosen <= other)) {
                    Toast.makeText(this, "Начало должно быть раньше окончания",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                prefs.edit().putInt(key, chosen).apply();
                time.setText(timeText(chosen));
                applySchedule();
            }, current / 60, current % 60, true).show();
        });
    }

    private String timeText(int minutes) {
        return String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
    }

    private void applySchedule() {
        AlarmScheduler.schedule(this);
        refreshNext();
    }

    private void refreshNext() {
        if (nextText == null) return;
        long next = AlarmScheduler.findNext(this, System.currentTimeMillis());
        if (next < 0) {
            nextText.setText(ChimeSettings.enabled(this)
                    ? "Нет активных часов для сигнала" : "Сигнал выключен");
        } else {
            String formatted = new SimpleDateFormat("EEE, d MMM · HH:mm",
                    Locale.forLanguageTag("ru-RU")).format(new Date(next));
            nextText.setText("Следующий сигнал: " + formatted);
        }
    }

    private void refreshPermissions() {
        if (permissionsText == null) return;
        boolean exact = AlarmScheduler.canBeExact(this);
        boolean notifications = Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        String exactStatus = exact ? "✓ Точные сигналы разрешены" :
                "! Точные сигналы не разрешены (возможна задержка)";
        String noticeStatus = notifications ? "✓ Уведомления разрешены" :
                "! Разрешите уведомления, чтобы слышать пилик";
        permissionsText.setText(exactStatus + "\n" + noticeStatus);
        exactButton.setVisibility(exact ? View.GONE : View.VISIBLE);
        notifyButton.setVisibility(notifications ? View.GONE : View.VISIBLE);
    }

    private void testSound() {
        MediaPlayer player = MediaPlayer.create(this, ChimeReceiver.rawSound(this));
        if (player == null) {
            Toast.makeText(this, "Не удалось воспроизвести сигнал", Toast.LENGTH_SHORT).show();
            return;
        }
        player.setOnCompletionListener(mp -> mp.release());
        player.setOnErrorListener((mp, what, extra) -> {
            mp.release();
            return true;
        });
        player.start();
    }

    @Override public void onRequestPermissionsResult(int requestCode,
                                                       String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshPermissions();
    }
}
