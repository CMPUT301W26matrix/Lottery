package com.example.lottery;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.fragment.app.DialogFragment;

import java.util.Objects;

public class Notification extends DialogFragment {
    interface CityDialogListener {
        void updateCity(City city, String title, String year);
        void addCity(City city);
    }
    private CityDialogListener listener;

    public static CityDialogFragment newInstance(City city){
            Bundle args = new Bundle();
            args.putSerializable("City", city);
            CityDialogFragment fragment = new CityDialogFragment();
            fragment.setArguments(args);
            return fragment;
    }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            if (context instanceof CityDialogListener){
                listener = (CityDialogListener) context;
            }
            else {
                throw new RuntimeException("Implement listener");
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            View view = getLayoutInflater().inflate(R.layout.fragment_city_details, null);
            EditText editMovieName = view.findViewById(R.id.edit_city_name);
            EditText editMovieYear = view.findViewById(R.id.edit_province);

            String tag = getTag();
            Bundle bundle = getArguments();
            City city;

            if (Objects.equals(tag, "City Details") && bundle != null){
                city = (City) bundle.getSerializable("City");
                assert city != null;
                editMovieName.setText(city.getName());
                editMovieYear.setText(city.getProvince());
            }
            else {
                city = null;}

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            return builder
                    .setView(view)
                    .setTitle("City Details")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Continue", (dialog, which) -> {
                        String title = editMovieName.getText().toString();
                        String year = editMovieYear.getText().toString();
                        if (Objects.equals(tag, "City Details")) {
                            listener.updateCity(city, title, year);
                        } else {
                            listener.addCity(new City(title, year));
                        }
                    })
                    .create();
        }
    }
