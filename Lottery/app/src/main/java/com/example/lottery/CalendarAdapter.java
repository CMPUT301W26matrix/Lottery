package com.example.lottery;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarAdapter extends BaseAdapter {
    private Context context;
    private Calendar calendar;
    private ArrayList<Date> days;
    private Set<Date> selectedDates;
    private Date startDate;
    private Date endDate;
    private OnDateSelectedListener listener;
    private Date selectedStartDate;
    private Date selectedEndDate;

    public interface OnDateSelectedListener {
        void onDateRangeSelected(Date start, Date end);
    }

    public CalendarAdapter(Context context, Calendar calendar, OnDateSelectedListener listener) {
        this.context = context;
        this.calendar = calendar;
        this.listener = listener;
        this.days = new ArrayList<>();
        this.selectedDates = new HashSet<>();
        generateDays();
    }

    private void generateDays() {
        days.clear();

        Calendar monthCalendar = (Calendar) calendar.clone();
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1);

        // Get the first day of the month (0 = Sunday, 1 = Monday, etc.)
        int firstDayOfMonth = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1;

        // Add empty days for the first week
        for (int i = 0; i < firstDayOfMonth; i++) {
            days.add(null);
        }

        // Add all days of the month
        int daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= daysInMonth; i++) {
            Calendar dayCalendar = (Calendar) calendar.clone();
            dayCalendar.set(Calendar.DAY_OF_MONTH, i);
            days.add(dayCalendar.getTime());
        }
    }

    public void nextMonth() {
        calendar.add(Calendar.MONTH, 1);
        generateDays();
        notifyDataSetChanged();
    }

    public void previousMonth() {
        calendar.add(Calendar.MONTH, -1);
        generateDays();
        notifyDataSetChanged();
    }

    public void setSelectedDates(Set<Date> dates) {
        this.selectedDates = dates;
        notifyDataSetChanged();
    }

    public Calendar getCurrentCalendar() {
        return calendar;
    }

    public void clearSelection() {
        selectedDates.clear();
        startDate = null;
        endDate = null;
        notifyDataSetChanged();
        if (listener != null) {
            listener.onDateRangeSelected(null, null);
        }
    }

    @Override
    public int getCount() {
        return days.size();
    }

    @Override
    public Object getItem(int position) {
        return days.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView dayView;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            dayView = (TextView) inflater.inflate(R.layout.calendar_day_item, null);
        } else {
            dayView = (TextView) convertView;
        }

        Date date = days.get(position);

        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            dayView.setText(String.valueOf(dayOfMonth));
            dayView.setVisibility(View.VISIBLE);

            // Check if date is selected
            if (isDateSelected(date)) {
                dayView.setBackgroundResource(R.drawable.selected_date_background);
                dayView.setTextColor(Color.WHITE);
            } else if (isDateInRange(date)) {
                dayView.setBackgroundResource(R.drawable.date_range_background);
                dayView.setTextColor(Color.BLACK);
            } else {
                dayView.setBackgroundColor(Color.TRANSPARENT);
                dayView.setTextColor(Color.BLACK);
            }

            // Check if date is in current month
            Calendar currentMonthCal = (Calendar) calendar.clone();
            if (cal.get(Calendar.MONTH) != currentMonthCal.get(Calendar.MONTH)) {
                dayView.setTextColor(Color.GRAY);
            }

            dayView.setOnClickListener(v -> onDateClick(date));
        } else {
            dayView.setText("");
            dayView.setVisibility(View.INVISIBLE);
            dayView.setOnClickListener(null);
        }

        return dayView;
    }

    public Date getSelectedStartDate() {
        return selectedStartDate;
    }

    public Date getSelectedEndDate() {
        return selectedEndDate;
    }


    private boolean isDateSelected(Date date) {
        for (Date selectedDate : selectedDates) {
            if (isSameDay(selectedDate, date)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDateInRange(Date date) {
        if (startDate != null && endDate != null) {
            return date.after(startDate) && date.before(endDate);
        }
        return false;
    }

    // Modify the onDateClick method to store the dates
    private void onDateClick(Date clickedDate) {
        if (selectedStartDate == null) {
            // First date selected
            selectedStartDate = clickedDate;
            selectedDates.clear();
            selectedDates.add(clickedDate);
        } else if (selectedEndDate == null) {
            // Second date selected
            if (clickedDate.after(selectedStartDate)) {
                selectedEndDate = clickedDate;
                // Add all dates in between
                Calendar cal = Calendar.getInstance();
                cal.setTime(selectedStartDate);
                while (cal.getTime().before(selectedEndDate) || cal.getTime().equals(selectedEndDate)) {
                    selectedDates.add(cal.getTime());
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
            } else {
                // Reset selection if clicked date is before start date
                selectedStartDate = clickedDate;
                selectedDates.clear();
                selectedDates.add(clickedDate);
                selectedEndDate = null;
            }
        } else {
            // Reset selection
            selectedStartDate = clickedDate;
            selectedEndDate = null;
            selectedDates.clear();
            selectedDates.add(clickedDate);
        }

        notifyDataSetChanged();

        if (listener != null) {
            listener.onDateRangeSelected(selectedStartDate, selectedEndDate);
        }
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}
