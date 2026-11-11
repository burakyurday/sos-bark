package com.mutfakajans.sos;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Minimal, intentional SOS screen. Bark credentials are never displayed
 * or made editable until the user explicitly opens Settings.
 */
public final class MainActivity extends Activity {
    private static final long HOLD_MS = 2000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingHold;
    private boolean didTrigger;
    private TextView statusText;
    private TextView holdHint;
    private TextView connectionStatus;
    private View sosButton;

    private SharedPreferences settings() {
        return getSharedPreferences(SosService.PREFS, MODE_PRIVATE);
    }

    private boolean isConfigured() {
        return !settings().getString("bark_key", "").isEmpty()
                && !settings().getString("sender_name", "").trim().isEmpty();
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        holdHint = findViewById(R.id.holdHint);
        connectionStatus = findViewById(R.id.connectionStatus);
        sosButton = findViewById(R.id.sosButton);
        findViewById(R.id.settingsButton).setOnClickListener(v -> showSettings());

        sosButton.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    didTrigger = false;
                    holdHint.setText("Basılı tutmaya devam et...");
                    sosButton.setAlpha(0.80f);
                    pendingHold = () -> {
                        pendingHold = null;
                        didTrigger = true;
                        sosButton.setAlpha(1f);
                        sosButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        triggerSos();
                    };
                    handler.postDelayed(pendingHold, HOLD_MS);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (pendingHold != null) handler.removeCallbacks(pendingHold);
                    pendingHold = null;
                    sosButton.setAlpha(1f);
                    if (!didTrigger) {
                        holdHint.setText("Göndermek için 2 saniye basılı tut");
                    }
                    if (event.getActionMasked() == MotionEvent.ACTION_UP) view.performClick();
                    return true;
                default:
                    return true;
            }
        });
        // Click never sends an alarm, protecting against accidental taps.
        sosButton.setOnClickListener(v -> {
            if (!didTrigger) holdHint.setText("SOS göndermek için 2 saniye basılı tut");
        });

        refreshConnection();
        if (!isConfigured()) {
            statusText.setText("İlk kullanım: Ayarlar'dan Bark anahtarını kaydet.");
            showSettings();
        } else {
            String last = settings().getString(SosService.LAST_STATUS, "");
            statusText.setText(last.isEmpty()
                    ? "Hazır. Bir acil durumda SOS'a basılı tut."
                    : last);
        }

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
    }

    private void refreshConnection() {
        if (isConfigured()) {
            connectionStatus.setText("●  ALICI BAĞLANTISI KAYITLI");
            connectionStatus.setTextColor(Color.parseColor("#77D3A9"));
        } else {
            connectionStatus.setText("●  KURULUM GEREKLİ");
            connectionStatus.setTextColor(Color.parseColor("#F7B878"));
        }
    }

    private void showSettings() {
        final View content = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        final EditText nameField = content.findViewById(R.id.senderInput);
        final EditText keyField = content.findViewById(R.id.keyInput);
        final TextView keyState = content.findViewById(R.id.keyState);
        final Button changeKeyButton = content.findViewById(R.id.changeKeyButton);
        final Button testButton = content.findViewById(R.id.testButton);

        final String savedKey = settings().getString("bark_key", "");
        final boolean[] changeRequested = {savedKey.isEmpty()};
        nameField.setText(settings().getString("sender_name", ""));
        keyField.setText("");
        keyField.setVisibility(savedKey.isEmpty() ? View.VISIBLE : View.GONE);
        changeKeyButton.setVisibility(savedKey.isEmpty() ? View.GONE : View.VISIBLE);
        keyState.setText(savedKey.isEmpty()
                ? "Henüz bir Bark anahtarı kayıtlı değil."
                : "●  Bark anahtarı kayıtlı ve gizli.");
        changeKeyButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Bark anahtarını değiştir")
                .setMessage("Mevcut anahtar silinmeden önce yenisini girmen gerekecek. Devam edilsin mi?")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Değiştir", (dialog, which) -> {
                    changeRequested[0] = true;
                    keyField.setVisibility(View.VISIBLE);
                    keyField.requestFocus();
                    keyState.setText("Yeni anahtarı gir. Eski anahtar kaydedene kadar korunur.");
                    changeKeyButton.setVisibility(View.GONE);
                }).show());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton("Kaydet", null)
                .setNegativeButton("Kapat", (d, which) -> {})
                .create();
        dialog.setOnShowListener(v -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(
                        new ColorDrawable(Color.parseColor("#15202F")));
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
                if (saveSettingsFromDialog(nameField, keyField, changeRequested[0])) {
                    dialog.dismiss();
                }
            });
        });
        testButton.setOnClickListener(v -> {
            if (saveSettingsFromDialog(nameField, keyField, changeRequested[0])) {
                dialog.dismiss();
                new AlertDialog.Builder(this)
                    .setTitle("Test bildirimi")
                    .setMessage("iPhone'a yüksek sesli kritik test alarmı gönderilsin mi?")
                    .setNegativeButton("Vazgeç", null)
                    .setPositiveButton("Gönder", (d, which) -> sendTest())
                    .show();
            }
        });
        dialog.show();
    }

    private boolean saveSettingsFromDialog(EditText nameField,
                                           EditText keyField,
                                           boolean changeRequested) {
        String name = nameField.getText().toString().trim();
        if (name.isEmpty()) {
            nameField.setError("Gönderici adı gerekli");
            return false;
        }
        String key = settings().getString("bark_key", "");
        if (changeRequested) {
            String parsed = parseKey(keyField.getText().toString());
            if (parsed == null) {
                keyField.setError("Geçerli Bark anahtarı veya https://api.day.app/... URL'si gir");
                return false;
            }
            key = parsed;
        }
        if (key.isEmpty()) {
            keyField.setError("Bark anahtarını gir");
            return false;
        }
        settings().edit().putString("sender_name", name)
                .putString("bark_key", key).apply();
        refreshConnection();
        Toast.makeText(this, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show();
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
        if (!isConfigured()) {
            holdHint.setText("Önce Bark bağlantısını ayarla");
            statusText.setText("Ayarlar'dan Bark anahtarı ve gönderici adı gir.");
            showSettings();
            return;
        }
        String key = settings().getString("bark_key", "");
        String name = settings().getString("sender_name", "");
        Intent service = new Intent(this, SosService.class)
                .setAction(SosService.ACTION_START)
                .putExtra(SosService.EXTRA_KEY, key)
                .putExtra(SosService.EXTRA_NAME, name);
        try {
            startForegroundService(service);
            holdHint.setText("SOS ETKİNLEŞTİRİLDİ");
            statusText.setText("5 kritik alarm gönderiliyor. Durumu telefon bildiriminden takip et.");
        } catch (Exception ignored) {
            holdHint.setText("Gönderim başarısız");
            statusText.setText("Gönderim başlatılamadı. Uygulama izinlerini kontrol et.");
        }
    }

    private void sendTest() {
        if (!isConfigured()) {
            showSettings();
            return;
        }
        String key = settings().getString("bark_key", "");
        statusText.setText("Test alarmı gönderiliyor...");
        new Thread(() -> {
            boolean accepted = BarkClient.send(key, "Test", 1, 1, true);
            runOnUiThread(() -> statusText.setText(accepted
                    ? "Bark test isteğini kabul etti. iPhone'da alarmı kontrol et."
                    : "Test gönderilemedi. İnterneti ve Bark ayarlarını kontrol et."));
        }, "sos-bark-test").start();
    }

    @Override protected void onResume() {
        super.onResume();
        if (connectionStatus != null) refreshConnection();
    }

    @Override protected void onDestroy() {
        if (pendingHold != null) handler.removeCallbacks(pendingHold);
        super.onDestroy();
    }
}
