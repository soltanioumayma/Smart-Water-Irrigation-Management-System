package com.example.smart_water_projet.models;

public class Alert {
    public String id;
    public String type;           // pH_ACIDE, PH_BASIQUE, TEMP_HAUTE, TEMP_BASSE, COND_BASSE, COND_HAUTE
    public String severity;       // low, medium, high
    public String parameter;      // pH, Temperature, Conductivity
    public double value;          // Valeur actuelle
    public double threshold;      // Seuil dépassé
    public String message;        // Titre court
    public String explanation;    // Explication scientifique
    public String impact;         // Impact sur la plante
    public String solution;       // Solution recommandée
    public long timestamp;
    public boolean active;

    public Alert() {}

    public Alert(String id, String type, String severity, String parameter,
                 double value, double threshold, String message,
                 String explanation, String impact, String solution) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.parameter = parameter;
        this.value = value;
        this.threshold = threshold;
        this.message = message;
        this.explanation = explanation;
        this.impact = impact;
        this.solution = solution;
        this.timestamp = System.currentTimeMillis();
        this.active = true;
    }
}