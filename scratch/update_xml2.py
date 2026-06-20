import re

file_path = r"c:\Users\solta\AndroidStudioProjects\smart_water_projet\app\src\main\res\layout\main_activity.xml"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace texts for the Virtual Soil Moisture
content = content.replace('android:text="Humidité du Sol (Senseur)"', 'android:text="Humidité Estimée (Calcul Météo)"')
content = content.replace('android:text="Trop sec"', 'android:text="Estimation: Trop sec"')

# Change the target info slightly to reflect an estimation
content = content.replace('android:text="Actuel: "', 'android:text="Estimé: "')

# Update the Pump description
old_pump_desc = "Irrigation automatisée selon l'humidité du sol et la qualité de l'eau. Arrêt automatique si le sol atteint 65%."
new_pump_desc = "Irrigation sans capteur de sol : durée d'arrosage calculée par l'IA selon la météo (température, vent). S'arrête à la fin du minuteur estimé."
content = content.replace(old_pump_desc, new_pump_desc)

old_pump_status = "Mode INTELLIGENT · Sol < 65%"
new_pump_status = "Mode INTELLIGENT · Durée calculée: 15 min"
content = content.replace(old_pump_status, new_pump_status)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Virtual sensor updates applied successfully.")
