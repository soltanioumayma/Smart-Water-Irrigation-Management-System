package com.example.smart_water_projet.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_water_projet.R;
import com.example.smart_water_projet.models.Alert;

import java.util.ArrayList;
import java.util.List;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertViewHolder> {

    private List<Alert> alerts = new ArrayList<>();
    private Context context;

    public AlertsAdapter(Context context) {
        this.context = context;
    }

    @Override
    public AlertViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert_card, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AlertViewHolder holder, int position) {
        Alert alert = alerts.get(position);
        holder.bind(alert);
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    public void addAlert(Alert alert) {
        alerts.add(alert);
        notifyItemInserted(alerts.size() - 1);
    }

    public void clear() {
        alerts.clear();
        notifyDataSetChanged();
    }

    class AlertViewHolder extends RecyclerView.ViewHolder {

        private View severityBar;
        private TextView tvAlertMessage;
        private TextView tvAlertParameter;
        private TextView tvAlertValue;
        private TextView tvAlertDescription;
        private ImageButton btnAlertDetails;

        public AlertViewHolder(View itemView) {
            super(itemView);
            severityBar = itemView.findViewById(R.id.severityBar);
            tvAlertMessage = itemView.findViewById(R.id.tvAlertMessage);
            tvAlertParameter = itemView.findViewById(R.id.tvAlertParameter);
            tvAlertValue = itemView.findViewById(R.id.tvAlertValue);
            tvAlertDescription = itemView.findViewById(R.id.tvAlertDescription);
            btnAlertDetails = itemView.findViewById(R.id.btnAlertDetails);
        }

        public void bind(Alert alert) {
            // Set severity color
            int colorRes = R.color.status_danger;
            if ("low".equals(alert.severity)) {
                colorRes = R.color.status_ok;
            } else if ("medium".equals(alert.severity)) {
                colorRes = R.color.status_warn;
            }
            severityBar.setBackgroundResource(colorRes);

            // Set text
            tvAlertMessage.setText(alert.message);
            tvAlertParameter.setText(alert.parameter);
            tvAlertValue.setText(String.format("Valeur: %.1f | Seuil: %.1f", alert.value, alert.threshold));
            tvAlertDescription.setText(alert.explanation);

            // Show details dialog
            btnAlertDetails.setOnClickListener(v -> showAlertDetails(alert));
        }

        private void showAlertDetails(Alert alert) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            View dialogView = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_alert_details, null);

            // Set data
            ((TextView) dialogView.findViewById(R.id.tvDetailTitle)).setText(alert.message);
            ((TextView) dialogView.findViewById(R.id.tvDetailValue)).setText(String.format("%.1f", alert.value));
            ((TextView) dialogView.findViewById(R.id.tvDetailThreshold)).setText(String.format("%.1f", alert.threshold));
            ((TextView) dialogView.findViewById(R.id.tvDetailExplanation)).setText(alert.explanation);
            ((TextView) dialogView.findViewById(R.id.tvDetailImpact)).setText(alert.impact);
            ((TextView) dialogView.findViewById(R.id.tvDetailSolution)).setText(alert.solution);

            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            dialogView.findViewById(R.id.btnDismissAlert).setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        }
    }
}