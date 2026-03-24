package com.example.lottery;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class FilterActivity extends AppCompatActivity implements CalendarAdapter.OnDateSelectedListener {

    private TextView tvDateRange, tvCurrentMonth;
    private EditText etCapacityValue;
    private GridView gvCalendarDays;
    private ImageButton btnPreviousMonth, btnNextMonth;
    private Button btnConfirm, btnCancel;
    private CalendarAdapter calendarAdapter;
    private Calendar currentCalendar;
    private SimpleDateFormat monthYearFormat;
    private SimpleDateFormat dateRangeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        initViews();
        setupCalendar();
        setupListeners();
    }

    private void initViews() {
        tvDateRange = findViewById(R.id.tvDateRange);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        etCapacityValue = findViewById(R.id.etCapacityValue);
        gvCalendarDays = findViewById(R.id.gvCalendarDays);
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnCancel = findViewById(R.id.btnCancel);

        monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        dateRangeFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
    }

    private void setupCalendar() {
        currentCalendar = Calendar.getInstance();
        // Set to August 2026 as shown in the design
        currentCalendar.set(2026, Calendar.AUGUST, 1);

        calendarAdapter = new CalendarAdapter(this, currentCalendar, this);
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
            String capacity = etCapacityValue.getText().toString();
            String dateRange = tvDateRange.getText().toString();
            // Handle confirm action - pass data back to previous activity or process
            finish();
        });

        btnCancel.setOnClickListener(v -> {
            finish();
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
