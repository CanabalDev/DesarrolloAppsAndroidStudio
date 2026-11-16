package com.example.desarrooapps;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FarmFormActivity extends AppCompatActivity {
    static final String EXTRA_FARM_ID = "farm_id";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int loadGeneration;
    private String farmId;
    private boolean updatingAreas;
    private boolean mutationInProgress;
    private TextInputLayout nameLayout;
    private TextInputLayout areaHaLayout;
    private TextInputLayout areaM2Layout;
    private TextInputLayout ownerLayout;
    private TextInputLayout managerLayout;
    private TextInputLayout countryLayout;
    private TextInputLayout departmentLayout;
    private TextInputLayout cityLayout;
    private TextInputEditText nameInput;
    private TextInputEditText areaHaInput;
    private TextInputEditText areaM2Input;
    private MaterialAutoCompleteTextView ownerInput;
    private MaterialAutoCompleteTextView managerInput;
    private MaterialAutoCompleteTextView countryInput;
    private MaterialAutoCompleteTextView departmentInput;
    private MaterialAutoCompleteTextView cityInput;
    private MaterialCheckBox produceMilkInput;
    private MaterialCheckBox produceCerealsInput;
    private MaterialCheckBox produceFruitsInput;
    private MaterialCheckBox produceVegetablesInput;
    private Button saveButton;
    private Button deleteButton;
    private Button retryButton;
    private Button backButton;
    private ProgressBar progressBar;
    private TextView errorView;
    private List<Usuario> users = new ArrayList<>();
    private List<Usuario> ownerOptions = new ArrayList<>();
    private List<Usuario> managerOptions = new ArrayList<>();
    private List<LocationCatalog.Country> countries = new ArrayList<>();
    private Usuario selectedOwner;
    private Usuario selectedManager;
    private String selectedCountry;
    private String selectedDepartment;
    private String selectedCity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        farmId = getIntent().getStringExtra(EXTRA_FARM_ID);
        setContentView(R.layout.activity_farm_form);
        applyWindowInsets();
        bindViews();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!mutationInProgress) {
                    finish();
                }
            }
        });
        configureForm();
        loadFormData();
    }

    private void bindViews() {
        nameLayout = findViewById(R.id.farm_form_name_layout);
        areaHaLayout = findViewById(R.id.farm_form_area_ha_layout);
        areaM2Layout = findViewById(R.id.farm_form_area_m2_layout);
        ownerLayout = findViewById(R.id.farm_form_owner_layout);
        managerLayout = findViewById(R.id.farm_form_manager_layout);
        countryLayout = findViewById(R.id.farm_form_country_layout);
        departmentLayout = findViewById(R.id.farm_form_department_layout);
        cityLayout = findViewById(R.id.farm_form_city_layout);
        nameInput = findViewById(R.id.farm_form_name);
        areaHaInput = findViewById(R.id.farm_form_area_ha);
        areaM2Input = findViewById(R.id.farm_form_area_m2);
        ownerInput = findViewById(R.id.farm_form_owner);
        managerInput = findViewById(R.id.farm_form_manager);
        countryInput = findViewById(R.id.farm_form_country);
        departmentInput = findViewById(R.id.farm_form_department);
        cityInput = findViewById(R.id.farm_form_city);
        produceMilkInput = findViewById(R.id.farm_form_produce_milk);
        produceCerealsInput = findViewById(R.id.farm_form_produce_cereals);
        produceFruitsInput = findViewById(R.id.farm_form_produce_fruits);
        produceVegetablesInput = findViewById(R.id.farm_form_produce_vegetables);
        saveButton = findViewById(R.id.farm_form_save_button);
        deleteButton = findViewById(R.id.farm_form_delete_button);
        retryButton = findViewById(R.id.farm_form_retry_button);
        backButton = findViewById(R.id.farm_form_back_button);
        progressBar = findViewById(R.id.farm_form_progress);
        errorView = findViewById(R.id.farm_form_error);
    }

    private void configureForm() {
        TextView titleView = findViewById(R.id.farm_form_title);
        boolean creating = farmId == null;
        titleView.setText(getString(creating ? R.string.farm_create_title : R.string.farm_edit_title));
        deleteButton.setVisibility(creating ? View.GONE : View.VISIBLE);

        backButton.setOnClickListener(view -> {
            if (!mutationInProgress) {
                finish();
            }
        });
        retryButton.setOnClickListener(view -> loadFormData());
        saveButton.setOnClickListener(view -> saveFarm());
        deleteButton.setOnClickListener(view -> confirmDeleteFarm());

        nameInput.addTextChangedListener(createErrorClearingWatcher(nameLayout));
        areaHaInput.addTextChangedListener(createAreaWatcher(true));
        areaM2Input.addTextChangedListener(createAreaWatcher(false));
        ownerInput.setOnItemClickListener((parent, view, position, id) -> {
            selectedOwner = ownerOptions.get(position);
            ownerLayout.setError(null);
            errorView.setVisibility(View.GONE);
        });
        ownerInput.setOnClickListener(view -> ownerInput.showDropDown());
        managerInput.setOnItemClickListener((parent, view, position, id) -> {
            selectedManager = position == 0 ? null : managerOptions.get(position - 1);
            managerLayout.setError(null);
            errorView.setVisibility(View.GONE);
        });
        managerInput.setOnClickListener(view -> managerInput.showDropDown());
        countryInput.setOnItemClickListener((parent, view, position, id) -> {
            String country = (String) parent.getItemAtPosition(position);
            if (country.equals(selectedCountry)) {
                return;
            }
            selectedCountry = country;
            selectedDepartment = null;
            selectedCity = null;
            countryLayout.setError(null);
            errorView.setVisibility(View.GONE);
            departmentInput.setText("", false);
            cityInput.setText("", false);
            populateDepartmentOptions(selectedCountry, null, null);
        });
        countryInput.setOnClickListener(view -> countryInput.showDropDown());
        departmentInput.setOnItemClickListener((parent, view, position, id) -> {
            String department = (String) parent.getItemAtPosition(position);
            if (department.equals(selectedDepartment)) {
                return;
            }
            selectedDepartment = department;
            selectedCity = null;
            departmentLayout.setError(null);
            errorView.setVisibility(View.GONE);
            cityInput.setText("", false);
            populateCityOptions(selectedCountry, selectedDepartment, null);
        });
        departmentInput.setOnClickListener(view -> departmentInput.showDropDown());
        cityInput.setOnItemClickListener((parent, view, position, id) -> {
            selectedCity = (String) parent.getItemAtPosition(position);
            cityLayout.setError(null);
            errorView.setVisibility(View.GONE);
        });
        cityInput.setOnClickListener(view -> cityInput.showDropDown());
        produceMilkInput.setOnCheckedChangeListener((button, checked) -> errorView.setVisibility(View.GONE));
        produceCerealsInput.setOnCheckedChangeListener((button, checked) -> errorView.setVisibility(View.GONE));
        produceFruitsInput.setOnCheckedChangeListener((button, checked) -> errorView.setVisibility(View.GONE));
        produceVegetablesInput.setOnCheckedChangeListener((button, checked) -> errorView.setVisibility(View.GONE));

        deleteButton.setText(R.string.delete_button);
        setFarmFormEnabled(false);
    }

    private TextWatcher createErrorClearingWatcher(TextInputLayout inputLayout) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                inputLayout.setError(null);
                errorView.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
    }

    private TextWatcher createAreaWatcher(boolean hectaresSource) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                TextInputLayout sourceLayout = hectaresSource ? areaHaLayout : areaM2Layout;
                TextInputLayout targetLayout = hectaresSource ? areaM2Layout : areaHaLayout;
                sourceLayout.setError(null);
                targetLayout.setError(null);
                errorView.setVisibility(View.GONE);
                if (updatingAreas) {
                    return;
                }
                TextInputEditText source = hectaresSource ? areaHaInput : areaM2Input;
                TextInputEditText target = hectaresSource ? areaM2Input : areaHaInput;
                String value = textOf(source).trim();
                if (value.isEmpty()) {
                    updatingAreas = true;
                    target.setText("");
                    updatingAreas = false;
                    return;
                }
                Double numericValue = FarmAreaMath.parse(value);
                if (numericValue == null) {
                    return;
                }
                double calculated = hectaresSource
                        ? FarmAreaMath.squareMetersFromHectares(numericValue)
                        : FarmAreaMath.hectaresFromSquareMeters(numericValue);
                if (!Double.isFinite(calculated)) {
                    return;
                }
                updatingAreas = true;
                target.setText(formatArea(calculated));
                updatingAreas = false;
            }
        };
    }

    private void loadFormData() {
        int generation = ++loadGeneration;
        setFarmFormEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);

        executor.execute(() -> {
            Finca farm = null;
            List<Usuario> loadedUsers = null;
            List<LocationCatalog.Country> loadedCountries = null;
            String failure = null;
            try {
                if (farmId != null) {
                    ApiClient.ApiResponse response = ApiClient.getFarm(farmId);
                    if (response.statusCode == 200) {
                        farm = ApiClient.parseFarm(response.body);
                    } else {
                        failure = ApiClient.errorMessage(response.body, response.statusCode);
                    }
                }
                if (failure == null) {
                    ApiClient.ApiResponse response = ApiClient.getUsers();
                    if (response.statusCode == 200) {
                        loadedUsers = ApiClient.parseUsers(response.body);
                    } else {
                        failure = ApiClient.errorMessage(response.body, response.statusCode);
                    }
                }
                if (failure == null) {
                    try (InputStream inputStream = getAssets().open("SelectedCountries.json")) {
                        try {
                            loadedCountries = LocationCatalog.parse(inputStream);
                        } catch (JSONException exception) {
                            failure = getString(R.string.farm_form_location_error);
                        }
                    } catch (IOException exception) {
                        failure = getString(R.string.farm_form_location_error);
                    }
                }
            } catch (IOException exception) {
                failure = getString(R.string.farm_form_connection_error);
            } catch (JSONException exception) {
                failure = getString(R.string.farm_form_unexpected_response);
            }

            Finca resultFarm = farm;
            List<Usuario> resultUsers = loadedUsers;
            List<LocationCatalog.Country> resultCountries = loadedCountries;
            String error = failure;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || generation != loadGeneration) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                if (error != null) {
                    errorView.setText(error);
                    errorView.setVisibility(View.VISIBLE);
                    retryButton.setVisibility(View.VISIBLE);
                    return;
                }
                users = resultUsers;
                countries = resultCountries;
                setFarmFormEnabled(true);
                populateUserSelectors(resultFarm);
                populateLocationSelectors(resultFarm);
                if (resultFarm != null) {
                    nameInput.setText(resultFarm.nombreFinca);
                    setAreaValues(resultFarm.areaHa, resultFarm.areaM2);
                    produceMilkInput.setChecked(resultFarm.produceLeche);
                    produceCerealsInput.setChecked(resultFarm.produceCereales);
                    produceFruitsInput.setChecked(resultFarm.produceFrutas);
                    produceVegetablesInput.setChecked(resultFarm.produceVerduras);
                } else {
                    selectedOwner = null;
                    selectedManager = null;
                    selectedCountry = null;
                    selectedDepartment = null;
                    selectedCity = null;
                    setAreaValues(null, null);
                }
            });
        });
    }

    private void populateUserSelectors(Finca farm) {
        ownerOptions = new ArrayList<>(users);
        managerOptions = new ArrayList<>(users);
        if (farm != null) {
            addMissingReference(ownerOptions, farm.propietario);
            addMissingReference(managerOptions, farm.capataz);
        }

        List<String> ownerLabels = new ArrayList<>();
        for (Usuario user : ownerOptions) {
            ownerLabels.add(userLabel(user));
        }
        ownerInput.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                ownerLabels
        ));

        List<String> managerLabels = new ArrayList<>();
        managerLabels.add(getString(R.string.farm_manager_unassigned_option));
        for (Usuario user : managerOptions) {
            managerLabels.add(userLabel(user));
        }
        managerInput.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                managerLabels
        ));

        selectedOwner = farm == null ? null : findUserById(ownerOptions,
                farm.propietario == null ? null : farm.propietario.id);
        selectedManager = farm == null || farm.capataz == null ? null
                : findUserById(managerOptions, farm.capataz.id);
        ownerInput.setText(selectedOwner == null ? "" : userLabel(selectedOwner), false);
        managerInput.setText(selectedManager == null
                ? getString(R.string.farm_manager_unassigned_option)
                : userLabel(selectedManager), false);
    }

    private void addMissingReference(List<Usuario> options, Usuario reference) {
        if (reference != null && findUserById(options, reference.id) == null) {
            options.add(reference);
        }
    }

    private Usuario findUserById(List<Usuario> options, String id) {
        if (id == null) {
            return null;
        }
        for (Usuario user : options) {
            if (id.equals(user.id)) {
                return user;
            }
        }
        return null;
    }

    private String userLabel(Usuario user) {
        if (user.nombre == null || user.nombre.trim().isEmpty()) {
            return user.nombreUsuario;
        }
        return user.nombre + " (@" + user.nombreUsuario + ")";
    }

    private void populateLocationSelectors(Finca farm) {
        List<String> countryNames = new ArrayList<>();
        for (LocationCatalog.Country country : countries) {
            countryNames.add(country.name);
        }
        if (farm != null) {
            addIfMissing(countryNames, farm.pais);
        }
        countryInput.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                countryNames
        ));

        selectedCountry = farm == null ? null : farm.pais;
        selectedDepartment = farm == null ? null : farm.departamento;
        selectedCity = farm == null ? null : farm.ciudad;
        countryInput.setText(selectedCountry == null ? "" : selectedCountry, false);
        populateDepartmentOptions(selectedCountry, selectedDepartment, selectedCity);
    }

    private void populateDepartmentOptions(String countryName, String preservedDepartment, String preservedCity) {
        List<String> departmentNames = new ArrayList<>();
        LocationCatalog.Country country = findCountry(countryName);
        if (country != null) {
            for (LocationCatalog.Department department : country.departments) {
                departmentNames.add(department.name);
            }
        }
        addIfMissing(departmentNames, preservedDepartment);
        departmentInput.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                departmentNames
        ));
        selectedDepartment = preservedDepartment;
        departmentInput.setText(selectedDepartment == null ? "" : selectedDepartment, false);
        departmentInput.setEnabled(!departmentNames.isEmpty());
        populateCityOptions(countryName, selectedDepartment, preservedCity);
    }

    private void populateCityOptions(String countryName, String departmentName, String preservedCity) {
        List<String> cityNames = new ArrayList<>();
        LocationCatalog.Department department = findDepartment(countryName, departmentName);
        if (department != null) {
            cityNames.addAll(department.cities);
        }
        addIfMissing(cityNames, preservedCity);
        cityInput.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                cityNames
        ));
        selectedCity = preservedCity;
        cityInput.setText(selectedCity == null ? "" : selectedCity, false);
        cityInput.setEnabled(!cityNames.isEmpty());
    }

    private LocationCatalog.Country findCountry(String name) {
        if (name == null) {
            return null;
        }
        for (LocationCatalog.Country country : countries) {
            if (name.equals(country.name)) {
                return country;
            }
        }
        return null;
    }

    private LocationCatalog.Department findDepartment(String countryName, String departmentName) {
        LocationCatalog.Country country = findCountry(countryName);
        if (country == null || departmentName == null) {
            return null;
        }
        for (LocationCatalog.Department department : country.departments) {
            if (departmentName.equals(department.name)) {
                return department;
            }
        }
        return null;
    }

    private void addIfMissing(List<String> values, String value) {
        if (value != null && !value.trim().isEmpty() && !values.contains(value)) {
            values.add(value);
        }
    }

    private void setAreaValues(Double hectares, Double squareMeters) {
        updatingAreas = true;
        areaHaInput.setText(hectares == null ? "" : formatArea(hectares));
        areaM2Input.setText(squareMeters == null ? "" : formatArea(squareMeters));
        updatingAreas = false;
    }

    private void saveFarm() {
        clearFormErrors();
        String name = textOf(nameInput).trim();
        String hectaresText = textOf(areaHaInput).trim();
        String squareMetersText = textOf(areaM2Input).trim();
        boolean valid = true;

        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.farm_form_name_required));
            valid = false;
        }
        Double hectares = validateArea(hectaresText, areaHaLayout);
        Double squareMeters = validateArea(squareMetersText, areaM2Layout);
        if (hectares == null || squareMeters == null) {
            valid = false;
        } else if (!areasAreCompatible(hectares, squareMeters)) {
            areaHaLayout.setError(getString(R.string.farm_form_area_incompatible));
            areaM2Layout.setError(getString(R.string.farm_form_area_incompatible));
            valid = false;
        }
        if (selectedOwner == null) {
            ownerLayout.setError(getString(R.string.farm_form_owner_required));
            valid = false;
        }
        if (!valid) {
            return;
        }

        hideKeyboard();
        setFormBusy(true);
        String farmName = name;
        double areaHa = hectares;
        double areaM2 = squareMeters;
        String ownerId = selectedOwner.id;
        String managerId = selectedManager == null ? null : selectedManager.id;
        String country = selectedCountry;
        String department = selectedDepartment;
        String city = selectedCity;
        boolean producesMilk = produceMilkInput.isChecked();
        boolean producesCereals = produceCerealsInput.isChecked();
        boolean producesFruits = produceFruitsInput.isChecked();
        boolean producesVegetables = produceVegetablesInput.isChecked();
        boolean creating = farmId == null;
        String currentFarmId = farmId;

        executor.execute(() -> {
            String failure = null;
            boolean success = false;
            try {
                ApiClient.ApiResponse response = creating
                        ? ApiClient.createFarm(farmName, areaHa, areaM2, ownerId, managerId,
                                country, department, city, producesMilk, producesCereals,
                                producesFruits, producesVegetables)
                        : ApiClient.updateFarm(currentFarmId, farmName, areaHa, areaM2,
                                ownerId, managerId, country, department, city, producesMilk,
                                producesCereals, producesFruits, producesVegetables);
                success = creating ? response.statusCode == 201 : response.statusCode == 200;
                if (!success) {
                    failure = ApiClient.errorMessage(response.body, response.statusCode);
                }
            } catch (IOException exception) {
                failure = getString(R.string.farm_form_connection_error);
            } catch (JSONException exception) {
                failure = getString(R.string.farm_form_unexpected_response);
            }

            boolean saved = success;
            String error = failure;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (saved) {
                    Toast.makeText(this, creating
                            ? R.string.farm_created_successfully
                            : R.string.farm_updated_successfully, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    setFormBusy(false);
                    errorView.setText(error);
                    errorView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private Double validateArea(String text, TextInputLayout layout) {
        if (text.isEmpty()) {
            layout.setError(getString(R.string.farm_form_area_required));
            return null;
        }
        Double value = FarmAreaMath.parse(text);
        if (value == null || value < 0 || FarmAreaMath.hasMoreThanTwoDecimals(text)) {
            layout.setError(getString(R.string.farm_form_area_invalid));
            return null;
        }
        return value;
    }

    private boolean areasAreCompatible(double hectares, double squareMeters) {
        return FarmAreaMath.areCompatible(hectares, squareMeters);
    }

    private String formatArea(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private void clearFormErrors() {
        nameLayout.setError(null);
        areaHaLayout.setError(null);
        areaM2Layout.setError(null);
        ownerLayout.setError(null);
        managerLayout.setError(null);
        countryLayout.setError(null);
        departmentLayout.setError(null);
        cityLayout.setError(null);
        errorView.setVisibility(View.GONE);
    }

    private void confirmDeleteFarm() {
        String name = textOf(nameInput).trim();
        new AlertDialog.Builder(this)
                .setTitle(R.string.farm_delete_title)
                .setMessage(getString(R.string.farm_delete_confirmation, name))
                .setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.delete_button, (dialog, which) -> deleteFarm())
                .show();
    }

    private void deleteFarm() {
        if (farmId == null) {
            return;
        }
        setFormBusy(true);
        String currentFarmId = farmId;
        executor.execute(() -> {
            String failure = null;
            boolean deleted = false;
            try {
                ApiClient.ApiResponse response = ApiClient.deleteFarm(currentFarmId);
                deleted = response.statusCode == 204;
                if (!deleted) {
                    failure = ApiClient.errorMessage(response.body, response.statusCode);
                }
            } catch (IOException exception) {
                failure = getString(R.string.farm_form_connection_error);
            }

            boolean wasDeleted = deleted;
            String error = failure;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (wasDeleted) {
                    Toast.makeText(this, R.string.farm_deleted_successfully, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    setFormBusy(false);
                    errorView.setText(error);
                    errorView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void setFormBusy(boolean busy) {
        mutationInProgress = busy;
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        setFarmFormEnabled(!busy);
        backButton.setEnabled(!busy);
    }

    private void setFarmFormEnabled(boolean enabled) {
        nameInput.setEnabled(enabled);
        areaHaInput.setEnabled(enabled);
        areaM2Input.setEnabled(enabled);
        ownerInput.setEnabled(enabled);
        managerInput.setEnabled(enabled);
        countryInput.setEnabled(enabled);
        departmentInput.setEnabled(enabled && hasOptions(departmentInput));
        cityInput.setEnabled(enabled && hasOptions(cityInput));
        produceMilkInput.setEnabled(enabled);
        produceCerealsInput.setEnabled(enabled);
        produceFruitsInput.setEnabled(enabled);
        produceVegetablesInput.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
    }

    private boolean hasOptions(MaterialAutoCompleteTextView input) {
        return input.getAdapter() != null && input.getAdapter().getCount() > 0;
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = getCurrentFocus();
        if (inputMethodManager != null && currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.main);
        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            view.setPadding(
                    initialLeft + systemBars.left,
                    initialTop + systemBars.top,
                    initialRight + systemBars.right,
                    initialBottom + systemBars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

}
