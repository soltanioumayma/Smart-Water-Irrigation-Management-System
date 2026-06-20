package com.example.smart_water_projet.managers;

import com.example.smart_water_projet.models.Alert;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

public class AlertManager {

    private DatabaseReference alertsRef;
    // Garde en mémoire les types d'alertes actives pour éviter les doublons
    private final Set<String> activeAlertTypes = new HashSet<>();
    private boolean initialized = false;

    public AlertManager() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance(
                "https://smartwater-53b6b-default-rtdb.europe-west1.firebasedatabase.app");
        alertsRef = firebaseDatabase.getReference("alerts");
        loadExistingAlertTypes();
    }

    /**
     * Charge les types d'alertes déjà actives dans Firebase au démarrage
     */
    private void loadExistingAlertTypes() {
        alertsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                activeAlertTypes.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        Alert alert = child.getValue(Alert.class);
                        if (alert != null && alert.active) {
                            activeAlertTypes.add(alert.type);
                        }
                    } catch (Exception e) {
                        // Ignorer les alertes mal formées
                    }
                }
                initialized = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                initialized = true;
            }
        });
    }

    /**
     * Génère des alertes basées sur les valeurs des capteurs.
     * Évite les doublons : une seule alerte active par type.
     * Supprime automatiquement les alertes quand les valeurs reviennent à la normale.
     */
    public void checkAndGenerateAlerts(float ph, float conductivity, float temperature) {
        android.util.Log.d("ALERT_MANAGER", "checkAndGenerateAlerts called - initialized: " + initialized);
        android.util.Log.d("ALERT_MANAGER", String.format("Values - pH: %.2f, Cond: %.2f, Temp: %.2f", ph, conductivity, temperature));
        if (!initialized) return;

        // ── pH ──
        if (ph < 6.5f) {
            android.util.Log.d("ALERT_MANAGER", "pH too acidic: " + ph);
            clearAlertByType("PH_BASIQUE"); // si pH était basique avant, supprimer
            createAlertIfNew("pH_ACIDE", "high", "pH", ph, 6.5f,
                    "⚠️ pH trop acide",
                    "Un pH inférieur à 6.5 rend l'eau trop acide. L'acidité excessive interfère avec " +
                            "l'absorption des nutriments essentiels par les racines des plantes.",
                    "L'eau acide peut endommager les racines et empêcher l'absorption du calcium, " +
                            "du magnésium et du potassium. Les plantes souffrent de carence nutritionnelle.",
                    "Ajouter du calcaire (CaCO₃) ou de la chaux pour augmenter le pH. Utiliser de l'eau " +
                            "de pluie neutralisée ou installer un système de filtration alcaline.");
        } else if (ph > 7.5f) {
            android.util.Log.d("ALERT_MANAGER", "pH too basic: " + ph);
            clearAlertByType("pH_ACIDE");
            createAlertIfNew("PH_BASIQUE", "medium", "pH", ph, 7.5f,
                    "⚠️ pH trop basique",
                    "Un pH supérieur à 7.5 rend l'eau trop basique (alcaline). L'alcalinité excessive " +
                            "rend les nutriments indisponibles pour les plantes.",
                    "L'eau basique réduit la disponibilité du fer, du manganèse et du zinc. Les plantes " +
                            "développent une chlorose (jaunissement des feuilles).",
                    "Ajouter du vinaigre blanc ou un acide faible pour diminuer le pH. Filtrer l'eau " +
                            "avec du charbon actif ou des résines échangeuses d'ions.");
        } else {
            android.util.Log.d("ALERT_MANAGER", "pH in normal range");
            // pH dans la norme → supprimer les alertes pH
            clearAlertByType("pH_ACIDE");
            clearAlertByType("PH_BASIQUE");
        }

        // ── Conductivité ──
        if (conductivity < 300f) {
            clearAlertByType("COND_HAUTE");
            createAlertIfNew("COND_BASSE", "low", "Conductivity", conductivity, 300f,
                    "ℹ️ Conductivité trop faible",
                    "Une conductivité inférieure à 300 µS/cm indique une eau très peu minéralisée. " +
                            "L'eau manque de nutriments essentiels.",
                    "L'eau pauvre en minéraux ne fournit pas assez de nutriments aux plantes. " +
                            "Celles-ci peuvent souffrir de carences multiples et croître lentement.",
                    "Ajouter un engrais hydrosoluble ou des minéraux. Utiliser de l'eau du robinet " +
                            "légèrement minéralisée ou un complément nutritif.");
        } else if (conductivity > 600f) {
            clearAlertByType("COND_BASSE");
            createAlertIfNew("COND_HAUTE", "high", "Conductivity", conductivity, 600f,
                    "⚠️ Conductivité trop élevée",
                    "Une conductivité supérieure à 600 µS/cm indique une eau très minéralisée ou salée. " +
                            "L'excès de sels provoque le stress osmotique.",
                    "L'eau trop salée crée une pression osmotique qui déshydrate les racines. " +
                            "Les plantes se fanent et leurs feuilles jaunissent malgré l'eau disponible.",
                    "Diluer l'eau avec de l'eau déminéralisée. Filtrer avec un système d'osmose inverse. " +
                            "Vérifier la source d'eau - peut-être de l'eau saumâtre ou contaminée.");
        } else {
            // Conductivité dans la norme
            clearAlertByType("COND_BASSE");
            clearAlertByType("COND_HAUTE");
        }

        // ── Température ──
        if (temperature < 15f) {
            clearAlertByType("TEMP_HAUTE");
            createAlertIfNew("TEMP_BASSE", "medium", "Temperature", temperature, 15f,
                    "❄️ Eau trop froide",
                    "Une température inférieure à 15°C ralentit tous les processus biologiques. " +
                            "Les racines ne peuvent pas absorber efficacement l'eau et les nutriments.",
                    "L'eau froide ralentit la respiration racinaire et l'absorption des nutriments. " +
                            "Les plantes deviennent molles et susceptibles aux maladies fongiques.",
                    "Laisser l'eau exposée au soleil quelques heures avant irrigation. Utiliser un " +
                            "chauffeur d'eau pour les systèmes d'irrigation en climat froid.");
        } else if (temperature > 30f) {
            clearAlertByType("TEMP_BASSE");
            createAlertIfNew("TEMP_HAUTE", "high", "Temperature", temperature, 30f,
                    "🔥 Eau trop chaude",
                    "Une température supérieure à 30°C favorise la prolifération d'algues et de bactéries. " +
                            "L'eau chaude contient moins d'oxygène dissous.",
                    "L'eau chaude provoque un stress thermique aux racines. Elle favorise les maladies " +
                            "et réduit l'absorption d'oxygène. Les racines pourrissent plus facilement.",
                    "Laisser l'eau refroidir à l'ombre. Aérer le réservoir pour augmenter l'oxygène dissous. " +
                            "Installer un système de refroidissement ou d'ombrage.");
        } else {
            // Température dans la norme
            clearAlertByType("TEMP_BASSE");
            clearAlertByType("TEMP_HAUTE");
        }
    }

    /**
     * Crée une alerte SEULEMENT si aucune alerte active du même type n'existe
     */
    private void createAlertIfNew(String type, String severity, String parameter,
                                  double value, double threshold, String message,
                                  String explanation, String impact, String solution) {

        android.util.Log.d("ALERT_MANAGER", "createAlertIfNew called for type: " + type);
        if (activeAlertTypes.contains(type)) {
            android.util.Log.d("ALERT_MANAGER", "Alert already active, skipping: " + type);
            // Alerte déjà active — on ne crée pas de doublon
            return;
        }

        String alertId = "alert_" + type;

        Alert alert = new Alert(alertId, type, severity, parameter, value, threshold,
                message, explanation, impact, solution);

        android.util.Log.d("ALERT_MANAGER", "Creating alert: " + alertId);
        alertsRef.child(alertId).setValue(alert);
        activeAlertTypes.add(type);
        android.util.Log.d("ALERT_MANAGER", "Alert created and saved to Firebase");
    }

    /**
     * Supprime / désactive les alertes d'un type donné quand la valeur revient à la normale
     */
    private void clearAlertByType(String type) {
        if (activeAlertTypes.contains(type)) {
            String alertId = "alert_" + type;
            alertsRef.child(alertId).removeValue();
            activeAlertTypes.remove(type);
        }
    }

    /**
     * Désactiver une alerte manuellement
     */
    public void dismissAlert(String alertId) {
        alertsRef.child(alertId).child("active").setValue(false);
    }

    /**
     * Obtenir la référence Firebase pour les alertes
     */
    public DatabaseReference getAlertsRef() {
        return alertsRef;
    }
}