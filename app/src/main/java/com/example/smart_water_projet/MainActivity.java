package com.example.smart_water_projet;

import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.smart_water_projet.databinding.MainActivityBinding;
import com.example.smart_water_projet.fragments.AlertsFragment;
import com.example.smart_water_projet.fragments.FarmerAssistantFragment;
import com.example.smart_water_projet.managers.AlertManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // ──────────────────────────────────────────────────────────────────
    // CONFIG
    // ──────────────────────────────────────────────────────────────────
    private static final String API_KEY     = "5727b93aee4eed773bad93c11f627ea5";

    // ── SEUILS QUALITÉ EAU POUR IRRIGATION (normes agricoles) ──
    // pH : eau neutre, bonne pour les cultures
    private static final float PH_MIN       = 6.5f;
    private static final float PH_MAX       = 8.4f;
    // Conductivité (µS/cm) : 200-1200 = acceptable pour irrigation
    private static final float COND_MIN     = 200f;
    private static final float COND_MAX     = 1200f;
    // Température eau (°C) : 15-35 = plage sûre pour irrigation
    private static final float TEMP_MIN     = 15f;
    private static final float TEMP_MAX     = 35f;
    private static final String UNITS       = "metric";

    // Coordonnées GPS Tunis Megrine — plus précis que le nom de ville
    private static final String WEATHER_URL =
            "https://api.openweathermap.org/data/2.5/weather"
                    + "?lat=36.7833&lon=10.2333"
                    + "&appid=" + API_KEY
                    + "&units=" + UNITS
                    + "&lang=fr";

    // ──────────────────────────────────────────────────────────────────
    // FIELDS
    // ──────────────────────────────────────────────────────────────────
    private MainActivityBinding binding;
    private final OkHttpClient  httpClient = new OkHttpClient();
    private TextToSpeech textToSpeech;

    // Firebase références
    private DatabaseReference sensorRef;
    private DatabaseReference pompeRef;
    private AlertManager      alertManager;

    // Capteurs
    private float sensorPh           = 7.2f;
    private float sensorConductivity = 420f;
    private float sensorWaterTemp    = 24.3f;
    private int   waterScore         = 80;

    // Pompe
    private float   debitReel       = 0f;
    private double  pumpVolume      = 0.0;
    private long    pompeHeureDebut = 0;
    private boolean pompeActive     = false;

    private boolean isHomeVisible = true;
    private boolean qualityDialogShown = false;  // éviter d'afficher le dialog en boucle

    private final Handler  handler       = new Handler(Looper.getMainLooper());
    private final Runnable clockRunnable = new Runnable() {
        @Override public void run() {
            tickClock();
            handler.postDelayed(this, 1000);
        }
    };

    // ──────────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ──────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        alertManager = new AlertManager();
        initializeTextToSpeech();

        initializeFirebase();
        readSensorsFromFirebase();
        readPompeFromFirebase();
        fetchWeather();
        setupBottomNavigation();
        setupPumpStopButton();

        handler.post(clockRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(clockRunnable);
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // TEXT TO SPEECH
    // ──────────────────────────────────────────────────────────────────
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.FRENCH);
                textToSpeech.setSpeechRate(1.0f);
            }
        });
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // BOTTOM NAVIGATION
    // ──────────────────────────────────────────────────────────────────
    private void setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_accueil) {
                showHome();
                return true;
            } else if (id == R.id.nav_alertes) {
                showFragment(new AlertsFragment());
                return true;
            } else if (id == R.id.nav_assistant) {
                showFragment(new FarmerAssistantFragment());
                return true;
            }

            return false;
        });

        binding.bottomNavigationView.setSelectedItemId(R.id.nav_accueil);
    }

    private void showHome() {
        isHomeVisible = true;
        binding.scrollView.setVisibility(View.VISIBLE);
        binding.fragmentContainer.setVisibility(View.GONE);
    }

    private void showFragment(Fragment fragment) {
        isHomeVisible = false;
        binding.scrollView.setVisibility(View.GONE);
        binding.fragmentContainer.setVisibility(View.VISIBLE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void setupPumpStopButton() {
        binding.btnStopPump.setOnClickListener(v -> stopPumpWithSummary());
    }

    /**
     * Arrêt manuel : Firebase etat = false, puis dialogue récapitulatif pour l'agriculteur.
     */
    private void stopPumpWithSummary() {
        if (!pompeActive) {
            Toast.makeText(this, R.string.pump_already_stopped, Toast.LENGTH_SHORT).show();
            return;
        }

        long debut = pompeHeureDebut > 0 ? pompeHeureDebut : System.currentTimeMillis();
        long   dureeMs     = System.currentTimeMillis() - debut;
        double dureeMin    = dureeMs / 60000.0;
        double volumeFinal = debitReel * dureeMin;

        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String heureDebutStr = tf.format(new Date(debut));
        String heureFinStr   = tf.format(new Date());

        pompeRef.child("etat").setValue(false);
        speak("Pompe arrêtée");

        showPumpSessionDialog(dureeMin, volumeFinal, debitReel, heureDebutStr, heureFinStr);
    }

    private void showPumpSessionDialog(double dureeMin, double volumeLitres,
                                       float debitLMin, String heureDebut, String heureFin) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_pump_session_summary, null);
        TextView tvBody = dialogView.findViewById(R.id.tvPumpSummaryBody);
        com.google.android.material.button.MaterialButton btnClose =
                dialogView.findViewById(R.id.btnPumpSummaryClose);

        String dureeStr = String.format(Locale.getDefault(), "%.1f", dureeMin);
        String volStr   = String.format(Locale.getDefault(), "%.1f", volumeLitres);
        String debitStr = String.format(Locale.getDefault(), "%.2f", debitLMin);

        StringBuilder sb = new StringBuilder();
        sb.append("• Durée d'arrosage : ").append(dureeStr).append(" min\n");
        sb.append("• Volume total estimé : ").append(volStr).append(" L\n");
        sb.append("• Débit nominal : ").append(debitStr).append(" L/min\n\n");
        sb.append("• Début (heure locale) : ").append(heureDebut).append("\n");
        sb.append("• Fin : ").append(heureFin).append("\n\n");
        sb.append("Qualité eau (indicateur) : score ").append(waterScore).append("/100.\n");
        if (waterScore >= 65) {
            sb.append("L'eau distribuée reste dans une plage acceptable pour l'irrigation.");
        } else {
            sb.append("Surveillez la qualité avant la prochaine session.");
        }

        tvBody.setText(sb.toString());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        // Speak the summary
        String spokenSummary = String.format(Locale.getDefault(),
                "Voici un résumé de l'irrigation. Durée d'arrosage : %.1f minutes. Volume total estimé : %.1f litres. Débit nominal : %.2f litres par minute.",
                dureeMin, volumeLitres, debitLMin);
        speak(spokenSummary);
    }

    // ──────────────────────────────────────────────────────────────────
    // FIREBASE — INIT
    // ──────────────────────────────────────────────────────────────────
    private void initializeFirebase() {
        try {
            // Use the correct regional database URL
            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance(
                    "https://smartwater-53b6b-default-rtdb.europe-west1.firebasedatabase.app");

            sensorRef = firebaseDatabase.getReference("capteurs");
            pompeRef  = firebaseDatabase.getReference("pompe");

            android.util.Log.d("FIREBASE_INIT", "Firebase initialized with regional URL");
            android.util.Log.d("FIREBASE_INIT", "Database URL: https://smartwater-53b6b-default-rtdb.europe-west1.firebasedatabase.app");

            // Test connection by trying to read the root
            firebaseDatabase.getReference().addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    android.util.Log.d("FIREBASE_INIT", "Firebase connection successful - root exists: " + snapshot.exists());
                    android.util.Log.d("FIREBASE_INIT", "Root children: " + snapshot.getChildren().toString());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    android.util.Log.e("FIREBASE_INIT", "Firebase connection failed: " + error.getMessage());
                    android.util.Log.e("FIREBASE_INIT", "Error code: " + error.getCode());
                    android.util.Log.e("FIREBASE_INIT", "Error details: " + error.getDetails());
                }
            });

        } catch (Exception e) {
            android.util.Log.e("FIREBASE_INIT", "Exception during Firebase init: " + e.getMessage(), e);
            Toast.makeText(this, "Erreur Firebase: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // FIREBASE — LECTURE CAPTEURS
    // ──────────────────────────────────────────────────────────────────
    private void readSensorsFromFirebase() {
        android.util.Log.d("FIREBASE", "Setting up listener for capteurs");
        sensorRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                android.util.Log.d("FIREBASE", "onDataChange triggered - snapshot exists: " + snapshot.exists());
                try {
                    if (snapshot.exists()) {
                        android.util.Log.d("FIREBASE", "Snapshot keys: " + snapshot.getChildren().toString());

                        if (snapshot.hasChild("ph")) {
                            Object v = snapshot.child("ph").getValue();
                            android.util.Log.d("FIREBASE", "pH value from Firebase: " + v);
                            if (v != null) {
                                sensorPh = ((Number) v).floatValue();
                                android.util.Log.d("FIREBASE", "Updated sensorPh to: " + sensorPh);
                            }
                        } else {
                            android.util.Log.d("FIREBASE", "pH child not found");
                        }

                        if (snapshot.hasChild("temperature")) {
                            Object v = snapshot.child("temperature").getValue();
                            android.util.Log.d("FIREBASE", "Temperature value from Firebase: " + v);
                            if (v != null) {
                                sensorWaterTemp = ((Number) v).floatValue();
                                android.util.Log.d("FIREBASE", "Updated sensorWaterTemp to: " + sensorWaterTemp);
                            }
                        } else {
                            android.util.Log.d("FIREBASE", "Temperature child not found");
                        }

                        if (snapshot.hasChild("conductivity")) {
                            Object v = snapshot.child("conductivity").getValue();
                            android.util.Log.d("FIREBASE", "Conductivity value from Firebase: " + v);
                            if (v != null) {
                                sensorConductivity = ((Number) v).floatValue();
                                android.util.Log.d("FIREBASE", "Updated sensorConductivity to: " + sensorConductivity);
                            }
                        } else {
                            sensorConductivity = 420f;
                            android.util.Log.d("SENSOR",
                                    "Conductivité non définie → défaut 420 µS/cm");
                        }

                        waterScore = calculateWaterQualityScore(
                                sensorPh, sensorConductivity, sensorWaterTemp);

                        alertManager.checkAndGenerateAlerts(
                                sensorPh, sensorConductivity, sensorWaterTemp);

                        updatePhGauge(sensorPh);
                        updateConductGauge(sensorConductivity);
                        updateTempGauge(sensorWaterTemp);
                        updateScore(waterScore);

                        // Vérifier la qualité et proposer l'irrigation
                        checkWaterQualityForIrrigation();

                    } else {
                        android.util.Log.d("FIREBASE", "Snapshot does not exist");
                        Toast.makeText(MainActivity.this,
                                "Aucune donnée capteur disponible",
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    android.util.Log.e("FIREBASE", "Error reading sensors: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this,
                            "Erreur lecture capteurs: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("FIREBASE", "Firebase cancelled: " + error.getMessage());
                Toast.makeText(MainActivity.this,
                        "Erreur Firebase: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────
    // FIREBASE — LECTURE POMPE
    // ──────────────────────────────────────────────────────────────────
    private void readPompeFromFirebase() {
        pompeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {

                    if (snapshot.hasChild("debit_reel")) {
                        Object v = snapshot.child("debit_reel").getValue();
                        if (v != null) {
                            try {
                                if (v instanceof Number) {
                                    debitReel = ((Number) v).floatValue();
                                } else if (v instanceof String) {
                                    String strVal = (String) v;
                                    if (!strVal.equals("Valeur")) {
                                        debitReel = Float.parseFloat(strVal);
                                    } else {
                                        debitReel = 0f; // Default value if "Valeur"
                                    }
                                }
                            } catch (Exception e) {
                                android.util.Log.e("POMPE", "Error parsing debit_reel: " + e.getMessage());
                                debitReel = 0f;
                            }
                        }
                    }

                    boolean nouvelEtat = false;
                    if (snapshot.hasChild("etat")) {
                        Object v = snapshot.child("etat").getValue();
                        if (v instanceof Boolean) nouvelEtat = (Boolean) v;
                    }

                    if (nouvelEtat && !pompeActive) {
                        pompeActive     = true;
                        pompeHeureDebut = System.currentTimeMillis();
                        pumpVolume      = 0;
                        pompeRef.child("heure_debut").setValue(pompeHeureDebut);
                        android.util.Log.d("POMPE", "Pompe allumée");

                    } else if (!nouvelEtat && pompeActive) {
                        pompeActive = false;

                        long   dureeMs     = System.currentTimeMillis() - pompeHeureDebut;
                        double dureeMin    = dureeMs / 60000.0;
                        double volumeFinal = debitReel * dureeMin;

                        pompeRef.child("volume_total").setValue(
                                Math.round(volumeFinal * 10.0) / 10.0);
                        pompeRef.child("heure_debut").setValue(0);

                        android.util.Log.d("POMPE",
                                "Pompe éteinte — " + String.format("%.1f", dureeMin)
                                        + " min — " + String.format("%.1f", volumeFinal) + " L");
                    }

                    updatePompeLive(nouvelEtat);

                } catch (Exception e) {
                    android.util.Log.e("POMPE", "Erreur: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("POMPE", "Annulé: " + error.getMessage());
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────
    // CALCUL SCORE QUALITÉ EAU
    // ──────────────────────────────────────────────────────────────────
    private int calculateWaterQualityScore(float ph, float conductivity, float temperature) {
        int score = 100;

        if (ph < 6.5f || ph > 7.5f)   score -= 15;

        if (conductivity < 300f)        score -= 10;
        else if (conductivity > 600f)   score -= 20;
        else if (conductivity > 450f)   score -= 5;

        if (temperature < 15f)          score -= 10;
        else if (temperature > 30f)     score -= 15;

        return Math.max(0, Math.min(100, score));
    }

    // ──────────────────────────────────────────────────────────────────
    // MÉTÉO — API OPENWEATHER (Tunis Megrine)
    // ──────────────────────────────────────────────────────────────────
    private void fetchWeather() {
        Request request = new Request.Builder().url(WEATHER_URL).build();
        httpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> setWeatherFallback());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> setWeatherFallback());
                    return;
                }
                String body = response.body().string();
                android.util.Log.d("WEATHER", body);
                runOnUiThread(() -> parseAndDisplayWeather(body));
            }
        });
    }

    private void parseAndDisplayWeather(String json) {
        try {
            JSONObject root       = new JSONObject(json);
            JSONObject main       = root.getJSONObject("main");
            JSONObject wind       = root.getJSONObject("wind");
            JSONObject weatherObj = root.getJSONArray("weather").getJSONObject(0);

            int    airTemp   = (int) main.getDouble("temp");
            int    humidity  = main.getInt("humidity");
            int    windKmh   = (int) (wind.getDouble("speed") * 3.6);
            String condition = weatherObj.getString("main");

            binding.tvWeatherTemp.setText(airTemp + "°C");
            binding.tvWeatherHumidity.setText(humidity + "%");
            binding.tvWeatherWind.setText(windKmh + " km/h");

            switch (condition) {
                case "Clear":
                    binding.tvWeatherSky.setText("Ensoleillé");
                    binding.ivWeatherSkyIcon.setImageResource(R.drawable.ic_wb_sunny);
                    binding.ivWeatherSkyIcon.setImageTintList(
                            ContextCompat.getColorStateList(MainActivity.this, R.color.weather_sun));
                    break;
                case "Rain":
                case "Drizzle":
                case "Thunderstorm":
                    binding.tvWeatherSky.setText("Pluvieux");
                    binding.ivWeatherSkyIcon.setImageResource(R.drawable.ic_rainy);
                    binding.ivWeatherSkyIcon.setImageTintList(
                            ContextCompat.getColorStateList(MainActivity.this, R.color.brand_teal));
                    break;
                default:
                    binding.tvWeatherSky.setText("Nuageux");
                    binding.ivWeatherSkyIcon.setImageResource(R.drawable.ic_cloud);
                    binding.ivWeatherSkyIcon.setImageTintList(
                            ContextCompat.getColorStateList(MainActivity.this, R.color.weather_cloud));
                    break;
            }

        } catch (Exception e) {
            android.util.Log.e("WEATHER", "Parse error: " + e.getMessage());
            setWeatherFallback();
        }
    }

    private void setWeatherFallback() {
        binding.tvWeatherTemp.setText("--°C");
        binding.tvWeatherHumidity.setText("--%");
        binding.tvWeatherWind.setText("-- km/h");
        binding.tvWeatherSky.setText("--");
        binding.ivWeatherSkyIcon.setImageResource(R.drawable.ic_cloud);
        binding.ivWeatherSkyIcon.setImageTintList(
                ContextCompat.getColorStateList(MainActivity.this, R.color.weather_cloud));
    }

    // ──────────────────────────────────────────────────────────────────
    // UI — SCORE
    // ──────────────────────────────────────────────────────────────────
    private void updateScore(int score) {
        final int    colorRes;
        final String badge;
        final int    badgeRes;
        final String desc;

        if (score >= 80) {
            colorRes = R.color.status_ok;
            badge    = "Eau excellente";
            badgeRes = R.drawable.ic_badge_green;
            desc     = "Tous les paramètres sont dans la norme. Irrigation recommandée.";
        } else if (score >= 65) {
            colorRes = R.color.status_warn;
            badge    = "Eau acceptable";
            badgeRes = R.drawable.bg_badge_orange;
            desc     = "Conductivité légèrement élevée. Irrigation possible avec surveillance.";
        } else {
            colorRes = R.color.status_danger;
            badge    = "Eau non conforme";
            badgeRes = R.drawable.bg_badge_red;
            desc     = "Paramètres hors norme. Irrigation déconseillée. Vérifiez la source.";
        }

        int color = ContextCompat.getColor(this, colorRes);
        binding.tvScoreBadge.setText(badge);
        binding.tvScoreBadge.setBackgroundResource(badgeRes);
        binding.tvScoreBadge.setTextColor(color);
        binding.tvScoreDesc.setText(desc);
        binding.scoreGaugeView.setProgress(score);
        binding.scoreGaugeView.setIndicatorColor(color);
    }

    // ──────────────────────────────────────────────────────────────────
    // UI — JAUGES CAPTEURS
    // ──────────────────────────────────────────────────────────────────
    private void updatePhGauge(float ph) {
        final int    colorRes;
        final String statusText;
        final int    statusBgRes;

        if (ph < 6.5f) {
            colorRes    = R.color.status_danger;
            statusText  = "Acide";
            statusBgRes = R.drawable.bg_badge_red;
        } else if (ph <= 7.5f) {
            colorRes    = R.color.status_ok;
            statusText  = "Neutre";
            statusBgRes = R.drawable.ic_badge_green;
        } else {
            colorRes    = R.color.status_warn;
            statusText  = "Basique";
            statusBgRes = R.drawable.bg_badge_orange;
        }

        int color = ContextCompat.getColor(this, colorRes);
        binding.tvPhValue.setText(String.format(Locale.getDefault(), "%.1f", ph));
        binding.tvPhValue.setTextColor(color);
        binding.tvPhStatus.setText(statusText);
        binding.tvPhStatus.setTextColor(color);
        binding.tvPhStatus.setBackgroundResource(statusBgRes);
        binding.ivPhDrop.setColorFilter(color);

        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                        binding.ivPhNeedle.getLayoutParams();
        params.horizontalBias = Math.max(0f, Math.min(1f, ph / 14f));
        binding.ivPhNeedle.setLayoutParams(params);
        binding.ivPhNeedle.setColorFilter(color);
    }

    private void updateConductGauge(float cond) {
        final int    colorRes;
        final String statusText;
        final int    statusBgRes;

        if (cond < 300f) {
            colorRes    = R.color.status_warn;
            statusText  = "Faible";
            statusBgRes = R.drawable.bg_badge_orange;
        } else if (cond <= 450f) {
            colorRes    = R.color.status_ok;
            statusText  = "Idéale";
            statusBgRes = R.drawable.ic_badge_green;
        } else if (cond <= 600f) {
            colorRes    = R.color.status_warn;
            statusText  = "Élevée";
            statusBgRes = R.drawable.bg_badge_orange;
        } else {
            colorRes    = R.color.status_danger;
            statusText  = "Critique";
            statusBgRes = R.drawable.bg_badge_red;
        }

        int color = ContextCompat.getColor(this, colorRes);
        binding.tvConductValue.setText(String.valueOf((int) cond));
        binding.tvConductValue.setTextColor(color);
        binding.tvConductStatus.setText(statusText);
        binding.tvConductStatus.setTextColor(color);
        binding.tvConductStatus.setBackgroundResource(statusBgRes);
        binding.gaugeConductView.setProgress(Math.round((cond / 2000f) * 100f));
        binding.gaugeConductView.setIndicatorColor(color);
    }

    private void updateTempGauge(float temp) {
        final int    colorRes;
        final String statusText;
        final int    statusBgRes;

        if (temp < 15f) {
            colorRes    = R.color.status_warn;
            statusText  = "Froide";
            statusBgRes = R.drawable.bg_badge_orange;
        } else if (temp <= 30f) {
            colorRes    = R.color.brand_teal;
            statusText  = "Normale";
            statusBgRes = R.drawable.ic_badge_green;
        } else {
            colorRes    = R.color.status_danger;
            statusText  = "Chaude";
            statusBgRes = R.drawable.bg_badge_red;
        }

        int color = ContextCompat.getColor(this, colorRes);
        binding.tvTempValue.setText(String.format(Locale.getDefault(), "%.1f", temp));
        binding.tvTempValue.setTextColor(color);
        binding.tvTempStatus.setText(statusText);
        binding.tvTempStatus.setTextColor(color);
        binding.tvTempStatus.setBackgroundResource(statusBgRes);
        binding.gaugeTempView.setProgress(Math.round((temp / 50f) * 100f));
        binding.gaugeTempView.setIndicatorColor(color);
    }

    // ──────────────────────────────────────────────────────────────────
    // UI — POMPE
    // ──────────────────────────────────────────────────────────────────
    private void updatePompeLive(boolean pumpOn) {
        if (pumpOn) {
            binding.tvPumpStateBadge.setText("EN MARCHE");
            binding.tvPumpStateBadge.setTextColor(
                    ContextCompat.getColor(this, R.color.status_ok));
            binding.tvPumpStateBadge.setBackgroundResource(R.drawable.ic_badge_green);
            binding.pumpLed.setBackgroundResource(R.drawable.bg_led_green);
            binding.tvPumpStatusText.setText("Pompe active — irrigation en cours");
            binding.pumpFlowIndicator.setVisibility(View.VISIBLE);
        } else {
            binding.tvPumpStateBadge.setText("ARRÊTÉE");
            binding.tvPumpStateBadge.setTextColor(
                    ContextCompat.getColor(this, R.color.status_danger));
            binding.tvPumpStateBadge.setBackgroundResource(R.drawable.bg_badge_red);
            binding.pumpLed.setBackgroundResource(R.drawable.bg_led_red);
            binding.tvPumpStatusText.setText("Pompe arrêtée — session terminée");
            binding.pumpFlowIndicator.setVisibility(View.GONE);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // VÉRIFICATION QUALITÉ EAU POUR IRRIGATION
    // ──────────────────────────────────────────────────────────────────
    private void checkWaterQualityForIrrigation() {
        // Ne pas afficher le dialog si déjà affiché ou si pompe active
        if (qualityDialogShown || pompeActive) return;
        qualityDialogShown = true;

        boolean phOk   = sensorPh >= PH_MIN && sensorPh <= PH_MAX;
        boolean tempOk = sensorWaterTemp >= TEMP_MIN && sensorWaterTemp <= TEMP_MAX;
        boolean condOk = sensorConductivity >= COND_MIN && sensorConductivity <= COND_MAX;
        boolean allOk  = phOk && tempOk && condOk;

        // Inflate le layout du dialog
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_water_quality, null);

        // Récupérer les vues
        android.widget.ImageView ivIcon = dialogView.findViewById(R.id.ivDialogIcon);
        TextView tvTitle   = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        TextView tvPhVal   = dialogView.findViewById(R.id.tvDialogPhValue);
        TextView tvTempVal = dialogView.findViewById(R.id.tvDialogTempValue);
        TextView tvCondVal = dialogView.findViewById(R.id.tvDialogCondValue);
        View indPh         = dialogView.findViewById(R.id.indicatorPh);
        View indTemp       = dialogView.findViewById(R.id.indicatorTemp);
        View indCond       = dialogView.findViewById(R.id.indicatorCond);
        com.google.android.material.button.MaterialButton btnOk =
                dialogView.findViewById(R.id.btnDialogOk);
        com.google.android.material.button.MaterialButton btnCancel =
                dialogView.findViewById(R.id.btnDialogCancel);

        // Remplir les valeurs
        tvPhVal.setText(String.format(Locale.getDefault(), "%.1f", sensorPh));
        tvTempVal.setText(String.format(Locale.getDefault(), "%.1f°C", sensorWaterTemp));
        tvCondVal.setText(String.format(Locale.getDefault(), "%d µS/cm", (int) sensorConductivity));

        // Indicateurs couleur par paramètre
        indPh.setBackgroundResource(phOk ? R.drawable.bg_led_green : R.drawable.bg_led_red);
        indTemp.setBackgroundResource(tempOk ? R.drawable.bg_led_green : R.drawable.bg_led_red);
        indCond.setBackgroundResource(condOk ? R.drawable.bg_led_green : R.drawable.bg_led_red);

        tvPhVal.setTextColor(ContextCompat.getColor(this,
                phOk ? R.color.status_ok : R.color.status_danger));
        tvTempVal.setTextColor(ContextCompat.getColor(this,
                tempOk ? R.color.status_ok : R.color.status_danger));
        tvCondVal.setTextColor(ContextCompat.getColor(this,
                condOk ? R.color.status_ok : R.color.status_danger));

        if (allOk) {
            // ✅ Tout est bon
            ivIcon.setImageResource(R.drawable.ic_check_circle);
            ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_ok));
            tvTitle.setText("✅ Eau conforme pour irrigation");
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.status_ok));
            tvMessage.setText("Tous les paramètres (pH, température, conductivité) sont " +
                    "dans les normes. L'eau est bonne pour irriguer.\n\n" +
                    "Voulez-vous ouvrir la pompe ?");
            btnOk.setText("OK — Ouvrir pompe");
            btnOk.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.status_ok));
        } else {
            // ❌ Eau non conforme
            ivIcon.setImageResource(R.drawable.ic_alert_warning);
            ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_danger));
            tvTitle.setText("⚠️ Eau non conforme");
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.status_danger));

            StringBuilder msg = new StringBuilder(
                    "L'eau n'est PAS bonne pour l'irrigation.\nParamètres hors norme :\n\n");
            if (!phOk) {
                msg.append("• pH = ").append(String.format(Locale.getDefault(), "%.1f", sensorPh))
                        .append(" (norme: ").append(PH_MIN).append(" – ").append(PH_MAX).append(")\n");
            }
            if (!tempOk) {
                msg.append("• Temp = ").append(String.format(Locale.getDefault(), "%.1f°C", sensorWaterTemp))
                        .append(" (norme: ").append((int) TEMP_MIN).append(" – ").append((int) TEMP_MAX).append("°C)\n");
            }
            if (!condOk) {
                msg.append("• Cond = ").append((int) sensorConductivity).append(" µS/cm")
                        .append(" (norme: ").append((int) COND_MIN).append(" – ").append((int) COND_MAX).append(")\n");
            }
            msg.append("\nIrrigation déconseillée !");
            tvMessage.setText(msg.toString());

            btnOk.setText("Compris");
            btnOk.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.status_danger));
        }

        final boolean[] userConfirmedOpenPump = {false};

        // Créer le dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnOk.setOnClickListener(v -> {
            if (allOk) {
                userConfirmedOpenPump[0] = true;
                pompeRef.child("etat").setValue(true);
                Toast.makeText(this, "Pompe ouverte — Irrigation en cours",
                        Toast.LENGTH_SHORT).show();
                speak("Pompe ouverte");
            }
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnDismissListener(d -> {
            if (allOk && !userConfirmedOpenPump[0]) {
                pompeRef.child("etat").setValue(false);
            }
            handler.postDelayed(() -> qualityDialogShown = false, 60000);
        });

        dialog.show();

        // Speak the first phrase when dialog shows
        if (allOk) {
            speak("Eau conforme pour irrigation");
        } else {
            speak("Eau non conforme");
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // HORLOGE — calcul volume en temps réel
    // ──────────────────────────────────────────────────────────────────
    private void tickClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        binding.tvLastUpdate.setText("Aujourd'hui, " + sdf.format(new Date()));

        if (pompeActive && pompeHeureDebut > 0 && debitReel > 0) {

            long   dureeMs  = System.currentTimeMillis() - pompeHeureDebut;
            double dureeMin = dureeMs / 60000.0;
            int    dureeAff = (int) dureeMin;

            pumpVolume = debitReel * dureeMin;

            binding.tvPumpDebit.setText(
                    String.format(Locale.getDefault(), "%.1f", debitReel));
            binding.tvPumpVolume.setText(
                    String.format(Locale.getDefault(), "%.1f", pumpVolume));
            binding.tvPumpDuration.setText(dureeAff + " min");

        } else if (!pompeActive) {
            binding.tvPumpDebit.setText("0.0");
            binding.tvPumpDuration.setText("0 min");
        }
    }
}