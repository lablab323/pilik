package ru.pilik.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Подаёт звук через уведомление, чтобы срабатывать при выключенном экране. */
public class ChimeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ChimeSettings.enabled(context)) {
            AlarmScheduler.schedule(context);
            return;
        }
        SharedPreferences prefs = ChimeSettings.get(context);
        Calendar now = Calendar.getInstance();
        // Если неточное срабатывание запоздало уже до тихого периода, не шумим.
        if (AlarmScheduler.isWithinWindow(prefs, now)) {
            notifyChime(context);
        }
        AlarmScheduler.schedule(context);
    }

    public static int rawSound(Context context) {
        return ChimeSettings.soundId(context) == 0
                ? R.raw.blip_single : R.raw.blip_double;
    }

    public static void notifyChime(Context context) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        boolean vibrate = ChimeSettings.vibration(context);
        int soundId = ChimeSettings.soundId(context);
        String channelId = "pilik_sound_" + soundId + (vibrate ? "_v" : "_nv") + "_v1";
        Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/"
                + rawSound(context));
        NotificationChannel channel = new NotificationChannel(channelId,
                "Почасовой сигнал — " + (soundId == 0 ? "одинарный" : "двойной")
                + (vibrate ? " + вибрация" : ""), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Звук по выбранному расписанию");
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        channel.setSound(uri, attributes);
        channel.enableVibration(vibrate);
        if (vibrate) channel.setVibrationPattern(new long[]{0, 110, 90, 110});
        manager.createNotificationChannel(channel);

        Intent openApp = new Intent(context, MainActivity.class);
        PendingIntent click = PendingIntent.getActivity(context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String time = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("ru-RU"))
                .format(new Date());
        Notification notification = new Notification.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Пилик · " + time)
                .setContentText("Сигнал времени")
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(click)
                .setAutoCancel(true)
                .setTimeoutAfter(90_000)
                .build();
        try {
            manager.notify((int) (System.currentTimeMillis() / 60_000), notification);
        } catch (SecurityException ignored) {
            // Без разрешения на уведомления система может блокировать звук.
        }
    }
}
