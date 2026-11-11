package com.mutfakajans.sos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Foreground sending continues if the sender turns the screen off. */
public final class SosService extends Service {
    static final String ACTION_START = "com.mutfakajans.sos.START";
    static final String ACTION_CANCEL = "com.mutfakajans.sos.CANCEL";
    static final String EXTRA_KEY = "device_key";
    static final String EXTRA_NAME = "sender_name";
    static final String PREFS = "sos_private";
    static final String LAST_STATUS = "last_status";
    private static final String CHANNEL = "sos_progress";
    private static final int NOTIFICATION_ID = 501;
    private static final int TOTAL = 5;
    private static final long INTERVAL_MS = 45_000L;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private PowerManager.WakeLock wakeLock;

    @Override public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(
                CHANNEL, "SOS alarm gönderimi", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Kritik alarmların gönderim durumunu gösterir");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        if (ACTION_CANCEL.equals(intent.getAction())) {
            cancelled.set(true);
            setStatus("Gönderim durduruluyor. Daha önce yollanan alarmlar geri alınamaz.");
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!ACTION_START.equals(intent.getAction()) || !running.compareAndSet(false, true)) {
            return START_NOT_STICKY;
        }
        String key = intent.getStringExtra(EXTRA_KEY);
        String name = intent.getStringExtra(EXTRA_NAME);
        if (key == null || key.isEmpty() || name == null || name.trim().isEmpty()) {
            running.set(false);
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, progressNotification("SOS hazırlanıyor"));
        PowerManager power = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (power != null) {
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SOS:SendCriticalAlerts");
            wakeLock.acquire(4 * 60 * 1000L);
        }
        worker.execute(() -> {
            int accepted = 0;
            try {
                for (int index = 1; index <= TOTAL && !cancelled.get(); index++) {
                    boolean result = BarkClient.send(key, name, index, TOTAL, false);
                    if (cancelled.get()) break;
                    if (result) accepted++;
                    String update = "Bark kabul etti: " + accepted + "/" + index
                            + " · Toplam " + TOTAL + " alarm";
                    setStatus(update + (result ? "" : " · Son istek başarısız"));
                    getSystemService(NotificationManager.class).notify(
                            NOTIFICATION_ID, progressNotification(update));
                    if (index < TOTAL) Thread.sleep(INTERVAL_MS);
                }
                if (!cancelled.get()) {
                    setStatus(accepted == TOTAL
                            ? "5/5 alarm Bark sunucusuna gönderildi. iPhone teslimi ayrıca doğrulanamaz."
                            : accepted + "/5 alarm Bark tarafından kabul edildi. İnternet ve ayarları kontrol et.");
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                releaseWakeLock();
                running.set(false);
                stopSelf();
            }
        });
        return START_NOT_STICKY;
    }

    private void setStatus(String status) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(LAST_STATUS, status).apply();
    }
    private Notification progressNotification(String status) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openIntent = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, SosService.class).setAction(ACTION_CANCEL);
        PendingIntent stopIntent = PendingIntent.getService(this, 1, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return builder.setContentTitle("SOS · Alarm gönderiliyor")
                .setContentText(status).setSmallIcon(R.drawable.ic_app)
                .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(openIntent)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Gönderimi durdur", stopIntent).build()).build();
    }
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }
    @Override public void onDestroy() {
        cancelled.set(true);
        releaseWakeLock();
        worker.shutdownNow();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
