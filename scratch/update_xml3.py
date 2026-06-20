import re

file_path = r"c:\Users\solta\AndroidStudioProjects\smart_water_projet\app\src\main\res\layout\main_activity.xml"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Replace pH gauge with Needle layout
ph_gauge_regex = r'<com\.google\.android\.material\.progressindicator\.LinearProgressIndicator\s*android:id="@+id/gaugePhView"[^>]*/>'
needle_layout = """<androidx.constraintlayout.widget.ConstraintLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="6dp"
                            android:layout_marginBottom="10dp">

                            <View
                                android:id="@+id/phScaleBar"
                                android:layout_width="match_parent"
                                android:layout_height="12dp"
                                android:background="@drawable/bg_ph_gradient"
                                app:layout_constraintTop_toTopOf="parent"
                                app:layout_constraintBottom_toBottomOf="parent" />

                            <ImageView
                                android:id="@+id/ivPhNeedle"
                                android:layout_width="14dp"
                                android:layout_height="20dp"
                                android:src="@drawable/ic_water_drop"
                                app:tint="@color/text_white"
                                app:layout_constraintStart_toStartOf="@id/phScaleBar"
                                app:layout_constraintEnd_toEndOf="@id/phScaleBar"
                                app:layout_constraintTop_toTopOf="@id/phScaleBar"
                                app:layout_constraintBottom_toBottomOf="@id/phScaleBar"
                                app:layout_constraintHorizontal_bias="0.51" />
                        </androidx.constraintlayout.widget.ConstraintLayout>"""
content = re.sub(ph_gauge_regex, needle_layout, content)

# 2. Update Pump AI text
old_ai_text = "Irrigation sans capteur de sol : durée d'arrosage calculée par l'IA selon la météo (température, vent). S'arrête à la fin du minuteur estimé."
new_ai_text = "Algorithme Prédictif : Si Temp > 30°C → +20% d'eau. Si Prévision Pluie → Arrêt. Décision IA : Ajustement optimisé de la durée."
content = content.replace(old_ai_text, new_ai_text)

# 3. Add flow animation under the AI layout block
ai_block_regex = r'(<TextView\s+android:layout_width="wrap_content"\s+android:layout_height="wrap_content"\s+android:text="Algorithme Prédictif.*?lineSpacingMultiplier="1\.2"\s*/>\s*</LinearLayout>\s*</LinearLayout>)'
flow_animation = r"""\1

                    <!-- Animation of flowing water -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:layout_marginBottom="14dp">
                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text=">> Flux d'eau en mouvement >>"
                            android:textSize="10sp"
                            android:textStyle="bold"
                            android:textColor="@color/brand_teal"
                            android:layout_marginBottom="4dp" />
                        <com.google.android.material.progressindicator.LinearProgressIndicator
                            android:id="@+id/pumpFlowIndicator"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:indeterminate="true"
                            app:trackCornerRadius="4dp"
                            app:trackThickness="8dp"
                            app:indicatorColor="@color/brand_teal"
                            app:trackColor="@color/bg_dark_widget" />
                    </LinearLayout>"""
content = re.sub(ai_block_regex, flow_animation, content, flags=re.DOTALL)

# 4. Remove tvPumpStatusSub (Mode INTELLIGENT)
sub_status_regex = r'<TextView\s*android:id="@+id/tvPumpStatusSub".*?/>'
content = re.sub(sub_status_regex, '', content, flags=re.DOTALL)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("UI enhancements applied successfully.")
