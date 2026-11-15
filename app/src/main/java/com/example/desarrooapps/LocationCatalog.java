package com.example.desarrooapps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class LocationCatalog {
    private LocationCatalog() {
    }

    static List<Country> parse(InputStream inputStream) throws IOException, JSONException {
        StringBuilder contents = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contents.append(line);
            }
        }

        JSONArray countriesJson = new JSONArray(contents.toString());
        List<Country> countries = new ArrayList<>(countriesJson.length());
        for (int countryIndex = 0; countryIndex < countriesJson.length(); countryIndex++) {
            JSONObject countryJson = countriesJson.getJSONObject(countryIndex);
            JSONArray statesJson = countryJson.optJSONArray("states");
            List<Department> departments = new ArrayList<>();
            if (statesJson != null) {
                for (int departmentIndex = 0; departmentIndex < statesJson.length(); departmentIndex++) {
                    JSONObject departmentJson = statesJson.getJSONObject(departmentIndex);
                    JSONArray citiesJson = departmentJson.optJSONArray("cities");
                    List<String> cities = new ArrayList<>();
                    if (citiesJson != null) {
                        for (int cityIndex = 0; cityIndex < citiesJson.length(); cityIndex++) {
                            cities.add(citiesJson.getString(cityIndex));
                        }
                    }
                    departments.add(new Department(departmentJson.optString("name"), cities));
                }
            }
            countries.add(new Country(countryJson.optString("name"), departments));
        }
        return countries;
    }

    static final class Country {
        final String name;
        final List<Department> departments;

        Country(String name, List<Department> departments) {
            this.name = name;
            this.departments = departments;
        }
    }

    static final class Department {
        final String name;
        final List<String> cities;

        Department(String name, List<String> cities) {
            this.name = name;
            this.cities = cities;
        }
    }
}
