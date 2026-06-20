import re

file_path = r"c:\Users\solta\AndroidStudioProjects\smart_water_projet\app\src\main\res\layout\main_activity.xml"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Mise à jour text
content = content.replace('android:text="Mise à jour : 14:32:05"', 'android:text="Aujourd\'hui, 15:06"')

# 2. CAPTEURS EN TEMPS RÉEL
content = content.replace('android:text="CAPTEURS EN TEMPS RÉEL"', 'android:text="VALEURS DE CAPTEURS EN TEMPS RÉEL"')

# 3. Weather Cards to MaterialCardView with bleu_fauchiat stroke
# We will find the wCardTemp, wCardHumidity, wCardSky, wCardWind, wCardUv and replace androidx.cardview.widget.CardView with com.google.android.material.card.MaterialCardView
# And add the stroke properties just before the closing >
for card_id in ["wCardTemp", "wCardHumidity", "wCardSky", "wCardWind", "wCardUv"]:
    # Find the opening tag of the CardView with this ID
    pattern = r'(<androidx\.cardview\.widget\.CardView[^>]*?android:id="@+id/' + card_id + r'"[^>]*?app:cardElevation="0dp")'
    replacement = r'\1\n                        app:strokeWidth="2dp"\n                        app:strokeColor="@color/bleu_fauchiat"'
    content = re.sub(pattern, replacement, content)

# Also we need to replace the tag names for the weather cards
# We can just replace the opening tags for these specific IDs
for card_id in ["wCardTemp", "wCardHumidity", "wCardSky", "wCardWind", "wCardUv"]:
    pattern = r'<androidx\.cardview\.widget\.CardView([^>]*?android:id="@+id/' + card_id + r'")'
    replacement = r'<com.google.android.material.card.MaterialCardView\1'
    content = re.sub(pattern, replacement, content)

# But wait, we also need to replace the closing tags.
# Since the only remaining androidx.cardview.widget.CardView closing tags before line 300 are the weather cards,
# and cardScore is at line 239 which is a CardView.
# Let's see: cardScore is also an androidx.cardview.widget.CardView.
# Maybe I just change ALL androidx.cardview.widget.CardView in the weather scroll to MaterialCardView?
# Let's just find the block from <HorizontalScrollView to </HorizontalScrollView> and replace all CardView inside it.

weather_start = content.find('<HorizontalScrollView\n                android:id="@+id/weatherScroll"')
weather_end = content.find('</HorizontalScrollView>', weather_start) + len('</HorizontalScrollView>')

weather_block = content[weather_start:weather_end]
weather_block = weather_block.replace('<androidx.cardview.widget.CardView', '<com.google.android.material.card.MaterialCardView')
weather_block = weather_block.replace('</androidx.cardview.widget.CardView>', '</com.google.android.material.card.MaterialCardView>')

content = content[:weather_start] + weather_block + content[weather_end:]

# 4. Remove the Pump Auto/Manual buttons and add AI Automation text
buttons_regex = r'<LinearLayout\s+android:layout_width="match_parent"\s+android:layout_height="wrap_content"\s+android:orientation="horizontal"\s+android:layout_marginBottom="14dp"\s+android:weightSum="2">.*?<com\.google\.android\.material\.button\.MaterialButton[^>]*?btnModeAuto.*?</LinearLayout>'
buttons_replacement = """<LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:background="@drawable/bg_dark_widget_rounded"
                        android:padding="12dp"
                        android:layout_marginBottom="12dp">
                        
                        <ImageView
                            android:layout_width="24dp"
                            android:layout_height="24dp"
                            android:src="@drawable/ic_wb_sunny"
                            app:tint="@color/brand_teal"
                            android:layout_marginEnd="12dp" />
                            
                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical">
                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Contrôle Intelligent Actif"
                                android:textSize="13sp"
                                android:textStyle="bold"
                                android:textColor="@color/text_white" />
                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Irrigation automatisée selon l\'humidité du sol et la qualité de l\'eau. Arrêt automatique si le sol atteint 65%."
                                android:textSize="11sp"
                                android:textColor="@color/text_hint"
                                android:layout_marginTop="2dp"
                                android:lineSpacingMultiplier="1.2" />
                        </LinearLayout>
                    </LinearLayout>"""

content = re.sub(buttons_regex, buttons_replacement, content, flags=re.DOTALL)

# Update pump sub status text
content = content.replace('Mode AUTO · Qualité eau conforme', 'Mode INTELLIGENT · Sol < 65%')

# 5. Add a "Humidité du Sol" card just after the Temperature card.
# The temperature card ends with </com.google.android.material.card.MaterialCardView> before "POMPE D'IRRIGATION"
temp_card_end = content.find('</com.google.android.material.card.MaterialCardView>', content.find('id="@+id/cardTemperature"')) + len('</com.google.android.material.card.MaterialCardView>')

soil_moisture_card = """
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/cardSoilMoisture"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginStart="16dp"
                android:layout_marginEnd="16dp"
                android:layout_marginTop="10dp"
                app:cardCornerRadius="16dp"
                app:cardBackgroundColor="@color/bg_dark_card"
                app:cardElevation="0dp"
                app:strokeWidth="2dp"
                app:strokeColor="@color/bleu_fauchiat">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="14dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:layout_marginBottom="10dp">

                        <ImageView
                            android:layout_width="16dp"
                            android:layout_height="16dp"
                            android:src="@drawable/ic_water_drop"
                            app:tint="@color/brand_teal"
                            android:contentDescription="@null"
                            android:layout_marginEnd="6dp" />

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Humidité du Sol (Senseur)"
                            android:textSize="11sp"
                            android:textColor="@color/text_hint" />

                        <TextView
                            android:id="@+id/tvSoilStatus"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Trop sec"
                            android:textSize="10sp"
                            android:textColor="@color/status_warn"
                            android:background="@drawable/bg_badge_orange"
                            android:paddingStart="8dp"
                            android:paddingEnd="8dp"
                            android:paddingTop="2dp"
                            android:paddingBottom="2dp" />
                    </LinearLayout>

                    <com.google.android.material.progressindicator.LinearProgressIndicator
                        android:id="@+id/gaugeSoilView"
                        android:layout_width="match_parent"
                        android:layout_height="40dp"
                        android:layout_marginBottom="10dp"
                        android:indeterminate="false"
                        app:trackCornerRadius="8dp"
                        app:trackThickness="10dp"
                        android:progress="32"
                        app:indicatorColor="@color/brand_teal"
                        app:trackColor="@color/bg_dark_widget" />

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical">
                        
                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Actuel: "
                            android:textSize="12sp"
                            android:textColor="@color/text_hint" />
                        
                        <TextView
                            android:id="@+id/tvSoilValue"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="32%"
                            android:textSize="18sp"
                            android:textStyle="bold"
                            android:textColor="@color/status_warn" />
                            
                        <View android:layout_width="0dp" android:layout_height="1dp" android:layout_weight="1" />
                        
                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Cible: 65%"
                            android:textSize="12sp"
                            android:textStyle="bold"
                            android:textColor="@color/text_white" />
                    </LinearLayout>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>
"""

content = content[:temp_card_end] + soil_moisture_card + content[temp_card_end:]

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Updates applied to main_activity.xml successfully.")
