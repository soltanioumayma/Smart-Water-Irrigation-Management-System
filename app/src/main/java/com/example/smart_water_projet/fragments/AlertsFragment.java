package com.example.smart_water_projet.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.smart_water_projet.adapters.AlertsAdapter;
import com.example.smart_water_projet.databinding.FragmentAlertsBinding;
import com.example.smart_water_projet.models.Alert;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

public class AlertsFragment extends Fragment {

    private FragmentAlertsBinding binding;
    private DatabaseReference alertsRef;
    private AlertsAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAlertsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase with regional URL (same as MainActivity)
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance(
                "https://smartwater-53b6b-default-rtdb.europe-west1.firebasedatabase.app");
        alertsRef = firebaseDatabase.getReference("alerts");

        // Setup RecyclerView
        adapter = new AlertsAdapter(requireContext());
        binding.recyclerViewAlerts.setAdapter(adapter);

        // Listen for alerts
        loadAlerts();
    }

    private void loadAlerts() {
        alertsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                adapter.clear();

                boolean hasAlerts = false;
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        Alert alert = child.getValue(Alert.class);
                        if (alert != null && alert.active) {
                            adapter.addAlert(alert);
                            hasAlerts = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Show/Hide empty state
                if (hasAlerts) {
                    binding.recyclerViewAlerts.setVisibility(View.VISIBLE);
                    binding.emptyStateContainer.setVisibility(View.GONE);
                } else {
                    binding.recyclerViewAlerts.setVisibility(View.GONE);
                    binding.emptyStateContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                error.toException().printStackTrace();
            }
        });
    }
}