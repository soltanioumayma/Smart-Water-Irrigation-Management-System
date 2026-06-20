package com.example.smart_water_projet.chat;

import java.util.Locale;

/**
 * <h2>Comment transformer ceci en vrai chatbot IA ?</h2>
 * <p><b>1) API générale (OpenAI, Google Gemini, Anthropic…)</b><br>
 * Vous envoyez le texte du fermier en HTTPS vers un endpoint (souvent via un
 * <b>backend</b> ou <b>Firebase Cloud Function</b> pour ne pas exposer la clé API dans l’APK).
 * Le modèle renvoie une réponse ; vous l’affichez dans le RecyclerView.</p>
 * <p><b>2) RAG (documents agricoles)</b><br>
 * Vous indexez PDF / fiches (pH, irrigation…) et vous concaténez les extraits pertinents
 * au prompt : le bot cite vos sources métier.</p>
 * <p><b>3) Bot 100 % règles (comme ici)</b><br>
 * Rapide, hors ligne possible, pas de coût — limite : pas de reformulation créative.</p>
 * <p><b>4) Hybride</b><br>
 * FAQ locale d’abord ; si aucune règle ne matche → appel IA.</p>
 * <p><b>Sécurité</b> : ne stockez jamais une clé API en dur dans l’app publiée ; passez par un serveur.</p>
 */
public final class FarmerAssistantEngine {

    private FarmerAssistantEngine() {}

    public static String answer(String rawQuestion) {
        if (rawQuestion == null) return fallback();
        String q = rawQuestion.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) return "Posez une question sur l’irrigation, la pompe ou la qualité de l’eau.";

        if (matches(q, "bonjour", "salut", "hello", "coucou")) {
            return "Bonjour ! Je suis l’assistant Aqua Smart. Demandez-moi par exemple : "
                    + "« Quand arroser ? », « Comment lire le pH ? » ou « Comment arrêter la pompe ? ».";
        }

        if (matches(q, "pompe", "arrosage", "arroser", "irrigation", "débit", "debit")) {
            return "Sur l’écran d’accueil vous voyez le débit, la durée et le volume estimé. "
                    + "Pour arrêter manuellement, utilisez « Arrêt pompe » près de « Contrôle intelligent actif » : "
                    + "cela met l’état Firebase à faux et affiche un résumé (durée, volume, heures). "
                    + "L’irrigation automatique dépend aussi des capteurs et du dialogue qualité eau.";
        }

        if (matches(q, "ph", "acidité", "acide", "basique")) {
            return "Le pH mesure l’acidité / basicité de l’eau. En agriculture, une eau trop acide ou trop basique "
                    + "peut nuire aux racines. Sur le tableau de bord, la jauge et le badge indiquent si vous êtes "
                    + "dans une zone sûre. En cas d’alerte, vérifiez la source ou filtrez / traitez selon votre agronome.";
        }

        if (matches(q, "conductivité", "conductivite", "salinité", "sel", "ec")) {
            return "La conductivité (µS/cm) reflète la concentration en sels dissous. Trop basse ou trop élevée "
                    + "peut signaler un déséquilibre pour certaines cultures. Comparez avec les seuils affichés "
                    + "dans l’app et demandez une analyse laboratoire si le doute persiste.";
        }

        if (matches(q, "température", "temperature", "chaud", "froid")) {
            return "La température de l’eau influence le confort des plantes et la dissolution des nutriments. "
                    + "Surveillez les badges « froide / normale / chaude » : des valeurs extrêmes méritent une pause "
                    + "d’irrigation ou une vérification de la retenue / canalisations.";
        }

        if (matches(q, "alerte", "notification", "danger")) {
            return "L’onglet « Alertes » regroupe les anomalies détectées (pH, température, conductivité). "
                    + "Traitez-les par ordre de gravité : d’abord la sécurité de l’eau, puis l’ajustement des réglages.";
        }

        if (matches(q, "météo", "meteo", "vent", "pluie")) {
            return "La météo locale aide à estimer l’évaporation et le besoin en eau. Combinez-la avec l’humidité "
                    + "du sol (ou vos capteurs) pour éviter l’excès d’irrigation après une pluie.";
        }

        if (matches(q, "volume", "litre", "quantité", "quantite")) {
            return "Le volume distribué est estimé à partir du débit (L/min) × durée de marche de la pompe. "
                    + "Après un arrêt manuel, le dialogue récapitule la session pour votre traçabilité.";
        }

        if (matches(q, "firebase", "cloud", "internet")) {
            return "Les capteurs et la pompe sont synchronisés via Firebase Realtime Database. "
                    + "Sans connexion, l’app peut afficher des valeurs obsolètes : vérifiez le réseau du terrain.";
        }

        if (matches(q, "chatbot", "ia", "intelligence", "openai", "gemini")) {
            return "Ce module est une base « FAQ intelligente ». Pour une vraie IA, branchez un endpoint sécurisé "
                    + "(Cloud Function + Gemini / OpenAI) et remplacez la méthode answer() par un appel réseau "
                    + "asynchrone (voir commentaires en tête de FarmerAssistantEngine).";
        }

        return "Je n’ai pas encore de réponse précise pour cette formulation. "
                + "Reformulez avec des mots comme : pompe, pH, conductivité, alerte ou météo. "
                + "Pour une aide sur mesure, un développeur peut brancher une API d’IA sur ce même écran.";
    }

    private static boolean matches(String q, String... keys) {
        for (String k : keys) {
            if (q.contains(k)) return true;
        }
        return false;
    }

    private static String fallback() {
        return "Je suis là pour vulgariser l’irrigation et la qualité de l’eau. Que souhaitez-vous savoir ?";
    }
}
