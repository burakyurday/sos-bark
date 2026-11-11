package com.mutfakajans.sos;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final long HOLD_MS = 2000L;
    private EditText keyInput;
    private EditText senderInput;
    private TextView statusText;
    private TextView holdHint;
    private TextView sosButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingHold;
    private boolean didTrigger;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        senderInput = findViewById(R.id.senderInput);
        keyInput = findViewById(R.id.keyInput);
        statusText = findViewById(R.id.statusText);
        holdHint = findViewById(R.id.holdHint);
        sosButton = findViewById(R.id.sosButton);
        SharedPreferences settings = getSharedPreferences(SosService.PREFS, MODE_PRIVATE);
        keyInput.setText(settings.getString("bark_key", ""));
        senderInput.setText(settings.getString("sender_name", ""));

        Button save = findViewById(R.id.saveButton);
        Button test = findViewById(R.id.testButton);
        save.setOnClickListener(v -> saveSettings());
        test.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Kritik test alarmı")
                .setMessage("iPhone sessiz olsa bile 30 saniyeye kadar ses çıkabilir. Testi gönderelim mi?")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Test gönder", (dialog, which) -> sendTest())
                .show());
        sosButton.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    didTrigger = false;
                    holdHint.setText("Basılı tutmaya devam et…");
                    sosButton.setAlpha(0.82f);
                    pendingHold = () -> {
                        didTrigger = true;
                        sosButton.setAlpha(1f);
                        triggerSos();
                    };
                    handler.postDelayed(pendingHold, HOLD_MS);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (pendingHold != null) handler.removeCallbacks(pendingHold);
                    pendingHold = null;
                    sosButton.setAlpha(1f);
                    holdHint.setText(didTrigger
                            ? "Alarm gönderimi başlatıldı"
                            : "Göndermek için 2 saniye basılı tut");
                    return true;
                default:
                    return true;
            }
        });
        sosButton.setOnClickListener(v -> triggerSos());
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
    }

    private boolean saveSettings() {
        String name = senderInput.getText().toString().trim();
        String key = parseKey(keyInput.getText().toString());
        if (name.isEmpty()) {
            senderInput.setError("Gönderici adı gerekli");
            return false;
        }
        if (key == null) {
            keyInput.setError("Geçerli Bark anahtarı veya api.day.app test URL'si gerekli");
            return false;
        }
        getSharedPreferences(SosService.PREFS, MODE_PRIVATE).edit()
                .putString("sender_name", name).putString("bark_key", key).apply();
        keyInput.setText(key);
        Toast.makeText(this, "Ayarlar bu cihazda kaydedildi", Toast.LENGTH_SHORT).show();
        return true;
    }

    private String parseKey(String input) {
        String key = input.trim();
        if (key.startsWith("https://") || key.startsWith("http://")) {
            Uri uri = Uri.parse(key);
            if (!"https".equals(uri.getScheme()) || !"api.day.app".equals(uri.getHost())
                    || uri.getPathSegments().isEmpty()) return null;
            key = uri.getPathSegments().get(0);
        }
        return key.matches("[A-Za-z0-9_-]{6,200}") ? key : null;
    }

    private void triggerSos() {
        if (!saveSettings()) {
            statusText.setText("Önce gönderici adı ve iPhone Bark anahtarını kaydet.");
            return;
        }
        String key = getSharedPreferences(SosService.PREFS, MODE_PRIVATE)
                .getString("bark_key", "");
        String name = senderInput.getText().toString().trim();
        Intent service = new Intent(this, SosService.class)
                .setAction(SosService.ACTION_START)
                .putExtra(SosService.EXTRA_KEY, key)
                .putExtra(SosService.EXTRA_NAME, name);
        try {
            startForegroundService(service);
            statusText.setText("İstek başlatıldı. 5 kritik alarm 45 saniye arayla yollanacak."
                    + " Gönderim sonucunu üst bildirimden takip et.");
        } catch (Exception ignored) {
            statusText.setText("Gönderim başlatılamadı. Telefonun izinlerini kontrol et.");
        }
    }

    private void sendTest() {
        if (!saveSettings()) return;
        String key = getSharedPreferences(SosService.PREFS, MODE_PRIVATE)
                .getString("bark_key", "");
        statusText.setText("Test isteği Bark'a gönderiliyor…");
        new Thread(() -> {
            boolean accepted = BarkClient.send(key, "Test", 1, 1, true);
            runOnUiThread(() -> statusText.setText(accepted
                    ? "Test isteği Bark tarafından kabul edildi. iPhone'a ulaştığını kontrol et."
                    : "Test gönderilemedi. İnterneti ve Bark anahtarını kontrol et."));
        }, "sos-bark-test").start();
    }

    @Override protected void onResume() {
        super.onResume();
        String status = getSharedPreferences(SosService.PREFS, MODE_PRIVATE)
                .getString(SosService.LAST_STATUS, "");
        if (statusText != null && !status.isEmpty()) statusText.setText(status);
    }
    @Override protected void onDestroy() {
        if (pendingHold != null) handler.removeCallbacks(pendingHold);
        super.onDestroy();
    }
}
