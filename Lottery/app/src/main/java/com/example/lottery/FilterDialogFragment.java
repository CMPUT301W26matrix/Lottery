package com.example.lottery;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class FilterDialogFragment extends DialogFragment implements CalendarAdapter.OnDateSelectedListener {

    private TextView tvDateRange, tvCurrentMonth;
    private EditText etCapacityValue;
    private GridView gvCalendarDays;
    private ImageButton btnPreviousMonth, btnNextMonth;
    private Button btnConfirm, btnCancel;
    private CalendarAdapter calendarAdapter;
    private Calendar currentCalendar;
    private SimpleDateFormat monthYearFormat;
    private SimpleDateFormat dateRangeFormat;
    private FilterListener listener;

    public interface FilterListener {
        void onFilterApplied(String capacity, Date startDate, Date endDate);

        void onFilterCancelled();
    }

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_filter, null);

        initViews(view);
        setupCalendar();
        setupListeners();

        builder.setView(view);

        // Remove default buttons since we have custom ones
        return builder.create();
    }

    private void initViews(View view) {
        tvDateRange = view.findViewById(R.id.tvDateRange);
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth);
        etCapacityValue = view.findViewById(R.id.etCapacityValue);
        gvCalendarDays = view.findViewById(R.id.gvCalendarDays);
        btnPreviousMonth = view.findViewById(R.id.btnPreviousMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        btnConfirm = view.findViewById(R.id.btnConfirm);
        btnCancel = view.findViewById(R.id.btnCancel);

        monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        dateRangeFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
    }

    private void setupCalendar() {
        currentCalendar = Calendar.getInstance();
        // Set to August 2026 as shown in the design
        currentCalendar.set(2026, Calendar.AUGUST, 1);

        calendarAdapter = new CalendarAdapter(getContext(), currentCalendar, this);
        gvCalendarDays.setAdapter(calendarAdapter);
        updateMonthDisplay();
    }

    private void setupListeners() {
        btnPreviousMonth.setOnClickListener(v -> {
            calendarAdapter.previousMonth();
            updateMonthDisplay();
        });

        btnNextMonth.setOnClickListener(v -> {
            calendarAdapter.nextMonth();
            updateMonthDisplay();
        });

        btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                String capacity = etCapacityValue.getText().toString();
                // You need to track the selected dates - modify CalendarAdapter to store them
                // For now, pass null. You'll need to add getSelectedStartDate() and getSelectedEndDate() methods
                listener.onFilterApplied(capacity, null, null);
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFilterCancelled();
            }
            dismiss();
        });
    }

    private void updateMonthDisplay() {
        Calendar currentCal = calendarAdapter.getCurrentCalendar();
        tvCurrentMonth.setText(monthYearFormat.format(currentCal.getTime()));
    }

    @Override
    public void onDateRangeSelected(Date start, Date end) {
        if (start != null && end != null) {
            String startStr = dateRangeFormat.format(start);
            String endStr = dateRangeFormat.format(end);
            tvDateRange.setText(startStr + " - " + endStr);
        } else if (start != null) {
            String startStr = dateRangeFormat.format(start);
            tvDateRange.setText(startStr + " (select end date)");
        } else {
            tvDateRange.setText("No dates selected");
        }
    }
}
