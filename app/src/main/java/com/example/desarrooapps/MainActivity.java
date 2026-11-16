package com.example.desarrooapps;

import android.os.Bundle;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.widget.Toast;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SessionManager sessionManager;
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private Button loginButton;
    private ProgressBar progressBar;
    private TextView errorView;
    private int usersRequestGeneration;
    private int farmsRequestGeneration;
    private boolean refreshFarmsOnResume;
    private int userFormRequestGeneration;
    private TextInputLayout userFormUsernameLayout;
    private TextInputLayout userFormNameLayout;
    private TextInputLayout userFormEmailLayout;
    private TextInputLayout userFormPasswordLayout;
    private TextInputLayout userFormPasswordConfirmationLayout;
    private TextInputEditText userFormUsernameInput;
    private TextInputEditText userFormNameInput;
    private TextInputEditText userFormEmailInput;
    private TextInputEditText userFormPasswordInput;
    private TextInputEditText userFormPasswordConfirmationInput;
    private Button userFormSaveButton;
    private Button userFormDeleteButton;
    private Button userFormBackButton;
    private Button userFormRetryButton;
    private ProgressBar userFormProgress;
    private TextView userFormErrorView;
    private String editingUserId;
    private boolean userFormMutationInProgress;
    private Screen currentScreen = Screen.LOGIN;

    private enum Screen {
        LOGIN,
        DASHBOARD,
        SECTION_PLACEHOLDER,
        USERS_LIST,
        USER_FORM,
        FARMS_LIST
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        sessionManager = new SessionManager(this);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                switch (currentScreen) {
                    case SECTION_PLACEHOLDER:
                    case USERS_LIST:
                        showSignedInScreen(sessionManager.getUsername());
                        break;
                    case USER_FORM:
                        if (!userFormMutationInProgress) {
                            showUsersList();
                        }
                        break;
                    case FARMS_LIST:
                        showSignedInScreen(sessionManager.getUsername());
                        break;
                    default:
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        if (sessionManager.getUsername() != null) {
            showSignedInScreen(sessionManager.getUsername());
        } else {
            showLoginScreen();
        }
    }

    private void showLoginScreen() {
        currentScreen = Screen.LOGIN;
        setContentView(R.layout.activity_main);
        applyWindowInsets();

        usernameLayout = findViewById(R.id.username_layout);
        passwordLayout = findViewById(R.id.password_layout);
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        progressBar = findViewById(R.id.login_progress);
        errorView = findViewById(R.id.login_error);

        loginButton.setOnClickListener(view -> submitLogin());
        passwordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitLogin();
                return true;
            }
            return false;
        });

        TextWatcher clearUsernameError = createErrorClearingWatcher(usernameLayout);
        TextWatcher clearPasswordError = createErrorClearingWatcher(passwordLayout);
        usernameInput.addTextChangedListener(clearUsernameError);
        passwordInput.addTextChangedListener(clearPasswordError);
    }

    private TextWatcher createErrorClearingWatcher(TextInputLayout inputLayout) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                inputLayout.setError(null);
                if (errorView != null) {
                    errorView.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
    }

    private void submitLogin() {
        String username = valueOf(usernameInput).trim();
        String password = valueOf(passwordInput);
        usernameLayout.setError(null);
        passwordLayout.setError(null);
        errorView.setVisibility(View.GONE);

        boolean valid = true;
        if (username.isEmpty()) {
            usernameLayout.setError(getString(R.string.login_username_required));
            valid = false;
        }
        if (password.trim().isEmpty()) {
            passwordLayout.setError(getString(R.string.login_password_required));
            valid = false;
        }
        if (!valid) {
            return;
        }

        hideKeyboard();
        setLoading(true);
        executor.execute(() -> {
            String failureMessage = null;
            try {
                ApiClient.ApiResponse response = ApiClient.login(username, password);
                if (response.statusCode == 200) {
                    JSONObject body = new JSONObject(response.body);
                    if (body.optBoolean("autenticado", false)) {
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                sessionManager.saveUsername(username);
                                showSignedInScreen(username);
                            }
                        });
                        return;
                    }
                    failureMessage = getString(R.string.login_invalid_credentials);
                } else if (response.statusCode == 401) {
                    failureMessage = getString(R.string.login_invalid_credentials);
                } else {
                    failureMessage = ApiClient.errorMessage(response.body, response.statusCode);
                }
            } catch (IOException exception) {
                failureMessage = getString(R.string.login_connection_error);
            } catch (JSONException exception) {
                failureMessage = getString(R.string.login_unexpected_response);
            }

            String message = failureMessage;
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    setLoading(false);
                    errorView.setText(message);
                    errorView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private String valueOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        usernameInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setText(loading ? R.string.login_loading : R.string.login_button);
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = getCurrentFocus();
        if (inputMethodManager != null && currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    private void showSignedInScreen(String username) {
        if (username == null) {
            showLoginScreen();
            return;
        }
        currentScreen = Screen.DASHBOARD;
        setContentView(R.layout.activity_dashboard);
        applyWindowInsets();
        TextView welcomeView = findViewById(R.id.session_welcome);
        welcomeView.setText(getString(R.string.session_welcome, username));

        findViewById(R.id.users_card).setOnClickListener(view ->
                showUsersList());
        findViewById(R.id.farms_card).setOnClickListener(view ->
                showFarmsList());
        findViewById(R.id.logout_button).setOnClickListener(view -> {
            sessionManager.clear();
            showLoginScreen();
        });
    }

    private void showSectionPlaceholder(String sectionName) {
        showPlaceholder(
                Screen.SECTION_PLACEHOLDER,
                sectionName,
                getString(R.string.section_coming_soon, sectionName),
                R.string.back_to_dashboard,
                () -> showSignedInScreen(sessionManager.getUsername())
        );
    }

    private void showUsersList() {
        currentScreen = Screen.USERS_LIST;
        int requestGeneration = ++usersRequestGeneration;
        setContentView(R.layout.activity_users);
        applyWindowInsets();
        findViewById(R.id.users_back_button).setOnClickListener(view ->
                showSignedInScreen(sessionManager.getUsername()));
        findViewById(R.id.users_create_button).setOnClickListener(view ->
                showUserForm(null));
        findViewById(R.id.users_refresh_button).setOnClickListener(view -> loadUsers(requestGeneration));
        findViewById(R.id.users_retry_button).setOnClickListener(view -> loadUsers(requestGeneration));
        loadUsers(requestGeneration);
    }

    private void showFarmsList() {
        currentScreen = Screen.FARMS_LIST;
        int requestGeneration = ++farmsRequestGeneration;
        setContentView(R.layout.activity_farms);
        applyWindowInsets();
        findViewById(R.id.farms_back_button).setOnClickListener(view ->
                showSignedInScreen(sessionManager.getUsername()));
        findViewById(R.id.farms_create_button).setOnClickListener(view ->
                openFarmForm(null));
        findViewById(R.id.farms_refresh_button).setOnClickListener(view -> loadFarms(requestGeneration));
        findViewById(R.id.farms_retry_button).setOnClickListener(view -> loadFarms(requestGeneration));
        loadFarms(requestGeneration);
    }

    private void loadFarms(int requestGeneration) {
        if (currentScreen != Screen.FARMS_LIST || requestGeneration != farmsRequestGeneration) {
            return;
        }
        ProgressBar farmsProgress = findViewById(R.id.farms_progress);
        Button refreshButton = findViewById(R.id.farms_refresh_button);
        farmsProgress.setVisibility(View.VISIBLE);
        refreshButton.setEnabled(false);
        findViewById(R.id.farms_error).setVisibility(View.GONE);
        findViewById(R.id.farms_retry_button).setVisibility(View.GONE);
        findViewById(R.id.farms_empty).setVisibility(View.GONE);
        LinearLayout farmsContainer = findViewById(R.id.farms_container);
        farmsContainer.removeAllViews();

        executor.execute(() -> {
            List<Finca> farms = null;
            String errorMessage = null;
            try {
                ApiClient.ApiResponse response = ApiClient.getFarms();
                if (response.statusCode == 200) {
                    farms = ApiClient.parseFarms(response.body);
                } else {
                    errorMessage = ApiClient.errorMessage(response.body, response.statusCode);
                }
            } catch (IOException exception) {
                errorMessage = getString(R.string.farms_connection_error);
            } catch (JSONException exception) {
                errorMessage = getString(R.string.farms_unexpected_response);
            }

            List<Finca> result = farms;
            String failure = errorMessage;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()
                        || currentScreen != Screen.FARMS_LIST
                        || requestGeneration != farmsRequestGeneration) {
                    return;
                }
                updateFarmsList(result, failure);
            });
        });
    }

    private void updateFarmsList(List<Finca> farms, String errorMessage) {
        ProgressBar farmsProgress = findViewById(R.id.farms_progress);
        Button refreshButton = findViewById(R.id.farms_refresh_button);
        TextView errorView = findViewById(R.id.farms_error);
        Button retryButton = findViewById(R.id.farms_retry_button);
        TextView emptyView = findViewById(R.id.farms_empty);
        LinearLayout farmsContainer = findViewById(R.id.farms_container);
        TextView countView = findViewById(R.id.farms_count);

        farmsProgress.setVisibility(View.GONE);
        refreshButton.setEnabled(true);
        if (errorMessage != null) {
            errorView.setText(errorMessage);
            errorView.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.VISIBLE);
            countView.setText("");
            return;
        }

        countView.setText(getResources().getQuantityString(
                R.plurals.farms_count,
                farms.size(),
                farms.size()
        ));
        if (farms.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Finca farm : farms) {
            View row = inflater.inflate(R.layout.item_farm, farmsContainer, false);
            ((TextView) row.findViewById(R.id.farm_item_name)).setText(farm.nombreFinca);
            ((TextView) row.findViewById(R.id.farm_item_area)).setText(getString(
                    R.string.farm_item_area,
                    String.format(Locale.getDefault(), "%.2f", farm.areaHa),
                    String.format(Locale.getDefault(), "%.2f", farm.areaM2)
            ));
            ((TextView) row.findViewById(R.id.farm_item_owner)).setText(getString(
                    R.string.farm_item_owner,
                    farm.propietario == null ? getString(R.string.farm_owner_unknown)
                            : farm.propietario.nombre
            ));
            ((TextView) row.findViewById(R.id.farm_item_manager)).setText(getString(
                    R.string.farm_item_manager,
                    farm.capataz == null ? getString(R.string.farm_manager_unassigned)
                            : farm.capataz.nombre
            ));
            ((TextView) row.findViewById(R.id.farm_item_location)).setText(formatFarmLocation(farm));
            ((TextView) row.findViewById(R.id.farm_item_production)).setText(formatFarmProduction(farm));
            row.setOnClickListener(view -> openFarmForm(farm.id));
            farmsContainer.addView(row);
        }
    }

    private String formatFarmLocation(Finca farm) {
        List<String> parts = new ArrayList<>();
        addLocationPart(parts, farm.ciudad);
        addLocationPart(parts, farm.departamento);
        addLocationPart(parts, farm.pais);
        if (parts.isEmpty()) {
            return getString(R.string.farm_location_unspecified);
        }
        StringBuilder location = new StringBuilder();
        for (String part : parts) {
            if (location.length() > 0) {
                location.append(", ");
            }
            location.append(part);
        }
        return location.toString();
    }

    private void addLocationPart(List<String> parts, String value) {
        if (value != null && !value.trim().isEmpty()) {
            parts.add(value);
        }
    }

    private String formatFarmProduction(Finca farm) {
        List<String> products = new ArrayList<>();
        if (farm.produceLeche) {
            products.add(getString(R.string.farm_production_milk));
        }
        if (farm.produceCereales) {
            products.add(getString(R.string.farm_production_cereals));
        }
        if (farm.produceFrutas) {
            products.add(getString(R.string.farm_production_fruits));
        }
        if (farm.produceVerduras) {
            products.add(getString(R.string.farm_production_vegetables));
        }
        if (products.isEmpty()) {
            return getString(R.string.farm_production_none);
        }
        StringBuilder summary = new StringBuilder();
        for (String product : products) {
            if (summary.length() > 0) {
                summary.append(" · ");
            }
            summary.append(product);
        }
        return summary.toString();
    }

    private void openFarmForm(String farmId) {
        Intent intent = new Intent(this, FarmFormActivity.class);
        if (farmId != null) {
            intent.putExtra(FarmFormActivity.EXTRA_FARM_ID, farmId);
        }
        refreshFarmsOnResume = true;
        startActivity(intent);
    }

    private void loadUsers(int requestGeneration) {
        if (currentScreen != Screen.USERS_LIST || requestGeneration != usersRequestGeneration) {
            return;
        }
        ProgressBar usersProgress = findViewById(R.id.users_progress);
        Button refreshButton = findViewById(R.id.users_refresh_button);
        usersProgress.setVisibility(View.VISIBLE);
        refreshButton.setEnabled(false);
        findViewById(R.id.users_error).setVisibility(View.GONE);
        findViewById(R.id.users_retry_button).setVisibility(View.GONE);
        findViewById(R.id.users_empty).setVisibility(View.GONE);
        LinearLayout usersContainer = findViewById(R.id.users_container);
        usersContainer.removeAllViews();

        executor.execute(() -> {
            List<Usuario> users = null;
            String errorMessage = null;
            try {
                ApiClient.ApiResponse response = ApiClient.getUsers();
                if (response.statusCode == 200) {
                    users = ApiClient.parseUsers(response.body);
                } else {
                    errorMessage = ApiClient.errorMessage(response.body, response.statusCode);
                }
            } catch (IOException exception) {
                errorMessage = getString(R.string.users_connection_error);
            } catch (org.json.JSONException exception) {
                errorMessage = getString(R.string.users_unexpected_response);
            }

            List<Usuario> result = users;
            String failure = errorMessage;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()
                        || currentScreen != Screen.USERS_LIST
                        || requestGeneration != usersRequestGeneration) {
                    return;
                }
                updateUsersList(result, failure);
            });
        });
    }

    private void updateUsersList(List<Usuario> users, String errorMessage) {
        ProgressBar usersProgress = findViewById(R.id.users_progress);
        Button refreshButton = findViewById(R.id.users_refresh_button);
        TextView errorView = findViewById(R.id.users_error);
        Button retryButton = findViewById(R.id.users_retry_button);
        TextView emptyView = findViewById(R.id.users_empty);
        LinearLayout usersContainer = findViewById(R.id.users_container);
        TextView countView = findViewById(R.id.users_count);

        usersProgress.setVisibility(View.GONE);
        refreshButton.setEnabled(true);
        if (errorMessage != null) {
            errorView.setText(errorMessage);
            errorView.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.VISIBLE);
            countView.setText("");
            return;
        }

        countView.setText(getResources().getQuantityString(
                R.plurals.users_count,
                users.size(),
                users.size()
        ));
        if (users.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Usuario user : users) {
            View row = inflater.inflate(R.layout.item_user, usersContainer, false);
            ((TextView) row.findViewById(R.id.user_item_name)).setText(user.nombre);
            ((TextView) row.findViewById(R.id.user_item_username)).setText(
                    getString(R.string.user_item_username, user.nombreUsuario));
            ((TextView) row.findViewById(R.id.user_item_email)).setText(user.correo);
            row.setOnClickListener(view -> showUserForm(user));
            usersContainer.addView(row);
        }
    }

    private void showUserForm(Usuario user) {
        boolean creating = user == null;
        currentScreen = Screen.USER_FORM;
        userFormMutationInProgress = false;
        editingUserId = creating ? null : user.id;
        int requestGeneration = ++userFormRequestGeneration;
        setContentView(R.layout.activity_user_form);
        applyWindowInsets();

        userFormUsernameLayout = findViewById(R.id.user_form_username_layout);
        userFormNameLayout = findViewById(R.id.user_form_name_layout);
        userFormEmailLayout = findViewById(R.id.user_form_email_layout);
        userFormPasswordLayout = findViewById(R.id.user_form_password_layout);
        userFormPasswordConfirmationLayout = findViewById(R.id.user_form_password_confirmation_layout);
        userFormUsernameInput = findViewById(R.id.user_form_username);
        userFormNameInput = findViewById(R.id.user_form_name);
        userFormEmailInput = findViewById(R.id.user_form_email);
        userFormPasswordInput = findViewById(R.id.user_form_password);
        userFormPasswordConfirmationInput = findViewById(R.id.user_form_password_confirmation);
        userFormSaveButton = findViewById(R.id.user_form_save_button);
        userFormDeleteButton = findViewById(R.id.user_form_delete_button);
        userFormBackButton = findViewById(R.id.user_form_back_button);
        userFormRetryButton = findViewById(R.id.user_form_retry_button);
        userFormProgress = findViewById(R.id.user_form_progress);
        userFormErrorView = findViewById(R.id.user_form_error);

        TextView titleView = findViewById(R.id.user_form_title);
        titleView.setText(getString(creating ? R.string.user_create_title : R.string.user_edit_title));
        userFormDeleteButton.setVisibility(creating ? View.GONE : View.VISIBLE);
        userFormPasswordLayout.setHint(getString(
                creating ? R.string.user_form_password : R.string.user_form_password_optional));
        userFormPasswordConfirmationLayout.setHint(getString(
                creating ? R.string.user_form_password_confirmation
                        : R.string.user_form_password_confirmation_optional));
        userFormPasswordLayout.setHelperText(creating
                ? null
                : getString(R.string.user_form_password_helper));
        userFormBackButton.setOnClickListener(view -> {
            if (!userFormMutationInProgress) {
                showUsersList();
            }
        });
        userFormSaveButton.setOnClickListener(view -> submitUserForm(requestGeneration));
        userFormDeleteButton.setOnClickListener(view -> confirmDeleteUser(requestGeneration));
        userFormRetryButton.setOnClickListener(view -> loadUserForEdit(editingUserId, requestGeneration));

        userFormUsernameInput.addTextChangedListener(createFormErrorClearingWatcher(userFormUsernameLayout));
        userFormNameInput.addTextChangedListener(createFormErrorClearingWatcher(userFormNameLayout));
        userFormEmailInput.addTextChangedListener(createFormErrorClearingWatcher(userFormEmailLayout));
        userFormPasswordInput.addTextChangedListener(createPasswordErrorClearingWatcher());
        userFormPasswordConfirmationInput.addTextChangedListener(
                createPasswordErrorClearingWatcher());

        if (creating) {
            setUserFormEnabled(true);
            userFormProgress.setVisibility(View.GONE);
        } else {
            loadUserForEdit(user.id, requestGeneration);
        }
    }

    private TextWatcher createFormErrorClearingWatcher(TextInputLayout inputLayout) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                inputLayout.setError(null);
                if (userFormErrorView != null) {
                    userFormErrorView.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
    }

    private TextWatcher createPasswordErrorClearingWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                userFormPasswordLayout.setError(null);
                userFormPasswordConfirmationLayout.setError(null);
                userFormErrorView.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
    }

    private void loadUserForEdit(String userId, int requestGeneration) {
        if (userId == null || currentScreen != Screen.USER_FORM
                || requestGeneration != userFormRequestGeneration) {
            return;
        }
        setUserFormEnabled(false);
        userFormProgress.setVisibility(View.VISIBLE);
        userFormErrorView.setVisibility(View.GONE);
        userFormRetryButton.setVisibility(View.GONE);

        executor.execute(() -> {
            Usuario user = null;
            String failure = null;
            try {
                ApiClient.ApiResponse response = ApiClient.getUser(userId);
                if (response.statusCode == 200) {
                    user = ApiClient.parseUser(response.body);
                } else {
                    failure = ApiClient.errorMessage(response.body, response.statusCode);
                }
            } catch (IOException exception) {
                failure = getString(R.string.user_form_connection_error);
            } catch (JSONException exception) {
                failure = getString(R.string.user_form_unexpected_response);
            }

            Usuario result = user;
            String error = failure;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || currentScreen != Screen.USER_FORM
                        || requestGeneration != userFormRequestGeneration) {
                    return;
                }
                userFormProgress.setVisibility(View.GONE);
                if (error != null) {
                    userFormErrorView.setText(error);
                    userFormErrorView.setVisibility(View.VISIBLE);
                    userFormRetryButton.setVisibility(View.VISIBLE);
                    return;
                }
                userFormUsernameInput.setText(result.nombreUsuario);
                userFormNameInput.setText(result.nombre);
                userFormEmailInput.setText(result.correo);
                userFormPasswordInput.setText("");
                userFormPasswordConfirmationInput.setText("");
                setUserFormEnabled(true);
            });
        });
    }

    private void submitUserForm(int requestGeneration) {
        userFormUsernameLayout.setError(null);
        userFormNameLayout.setError(null);
        userFormEmailLayout.setError(null);
        userFormPasswordLayout.setError(null);
        userFormPasswordConfirmationLayout.setError(null);
        userFormErrorView.setVisibility(View.GONE);

        String username = valueOf(userFormUsernameInput).trim();
        String name = valueOf(userFormNameInput).trim();
        String email = valueOf(userFormEmailInput).trim();
        String password = valueOf(userFormPasswordInput);
        String passwordConfirmation = valueOf(userFormPasswordConfirmationInput);
        boolean creating = editingUserId == null;
        boolean valid = true;

        if (username.isEmpty()) {
            userFormUsernameLayout.setError(getString(R.string.user_form_username_required));
            valid = false;
        } else if (username.length() > 50) {
            userFormUsernameLayout.setError(getString(R.string.user_form_username_too_long));
            valid = false;
        }
        if (name.isEmpty()) {
            userFormNameLayout.setError(getString(R.string.user_form_name_required));
            valid = false;
        } else if (name.length() > 100) {
            userFormNameLayout.setError(getString(R.string.user_form_name_too_long));
            valid = false;
        }
        if (email.isEmpty()) {
            userFormEmailLayout.setError(getString(R.string.user_form_email_required));
            valid = false;
        } else if (email.length() > 254 || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            userFormEmailLayout.setError(getString(R.string.user_form_email_invalid));
            valid = false;
        }
        if (creating && password.trim().isEmpty()) {
            userFormPasswordLayout.setError(getString(R.string.user_form_password_required));
            valid = false;
        }
        if (creating && passwordConfirmation.trim().isEmpty()) {
            userFormPasswordConfirmationLayout.setError(
                    getString(R.string.user_form_password_confirmation_required));
            valid = false;
        }
        if (!password.equals(passwordConfirmation)) {
            userFormPasswordConfirmationLayout.setError(
                    getString(R.string.user_form_password_mismatch));
            valid = false;
        }
        if (!valid) {
            return;
        }

        hideKeyboard();
        setUserFormBusy(true);
        String userId = editingUserId;
        executor.execute(() -> {
            String failure = null;
            boolean success = false;
            try {
                ApiClient.ApiResponse response = creating
                        ? ApiClient.createUser(username, password, email, name)
                        : ApiClient.updateUser(userId, username, password, email, name);
                success = creating ? response.statusCode == 201 : response.statusCode == 200;
                if (!success) {
                    failure = ApiClient.errorMessage(response.body, response.statusCode);
                }
            } catch (IOException exception) {
                failure = getString(R.string.user_form_connection_error);
            } catch (JSONException exception) {
                failure = getString(R.string.user_form_unexpected_response);
            }

            boolean saved = success;
            String error = failure;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || currentScreen != Screen.USER_FORM
                        || requestGeneration != userFormRequestGeneration) {
                    return;
                }
                if (saved) {
                    Toast.makeText(this, getString(creating
                            ? R.string.user_created_successfully
                            : R.string.user_updated_successfully), Toast.LENGTH_SHORT).show();
                    showUsersList();
                } else {
                    setUserFormBusy(false);
                    userFormErrorView.setText(error);
                    userFormErrorView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void confirmDeleteUser(int requestGeneration) {
        String username = valueOf(userFormUsernameInput).trim();
        new AlertDialog.Builder(this)
                .setTitle(R.string.user_delete_title)
                .setMessage(getString(R.string.user_delete_confirmation, username))
                .setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.delete_button, (dialog, which) ->
                        deleteUser(requestGeneration))
                .show();
    }

    private void deleteUser(int requestGeneration) {
        if (editingUserId == null || currentScreen != Screen.USER_FORM
                || requestGeneration != userFormRequestGeneration) {
            return;
        }
        setUserFormBusy(true);
        String userId = editingUserId;
        executor.execute(() -> {
            String failure = null;
            boolean deleted = false;
            try {
                ApiClient.ApiResponse response = ApiClient.deleteUser(userId);
                deleted = response.statusCode == 204;
                if (!deleted) {
                    failure = ApiClient.errorMessage(response.body, response.statusCode);
                }
            } catch (IOException exception) {
                failure = getString(R.string.user_form_connection_error);
            }

            boolean wasDeleted = deleted;
            String error = failure;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || currentScreen != Screen.USER_FORM
                        || requestGeneration != userFormRequestGeneration) {
                    return;
                }
                if (wasDeleted) {
                    Toast.makeText(this, R.string.user_deleted_successfully, Toast.LENGTH_SHORT).show();
                    showUsersList();
                } else {
                    setUserFormBusy(false);
                    userFormErrorView.setText(error);
                    userFormErrorView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void setUserFormBusy(boolean busy) {
        userFormMutationInProgress = busy;
        setUserFormEnabled(!busy);
        userFormBackButton.setEnabled(!busy);
        userFormProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void setUserFormEnabled(boolean enabled) {
        userFormUsernameInput.setEnabled(enabled);
        userFormNameInput.setEnabled(enabled);
        userFormEmailInput.setEnabled(enabled);
        userFormPasswordInput.setEnabled(enabled);
        userFormPasswordConfirmationInput.setEnabled(enabled);
        userFormSaveButton.setEnabled(enabled);
        userFormDeleteButton.setEnabled(enabled);
    }

    private void showPlaceholder(
            Screen screen,
            String title,
            String message,
            int backButtonText,
            Runnable backAction
    ) {
        currentScreen = screen;
        setContentView(R.layout.activity_section_placeholder);
        applyWindowInsets();
        TextView titleView = findViewById(R.id.section_title);
        titleView.setText(title);
        TextView messageView = findViewById(R.id.section_message);
        messageView.setText(message);
        Button backButton = findViewById(R.id.section_back_button);
        backButton.setText(backButtonText);
        backButton.setOnClickListener(view -> backAction.run());
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (refreshFarmsOnResume && currentScreen == Screen.FARMS_LIST) {
            refreshFarmsOnResume = false;
            loadFarms(farmsRequestGeneration);
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
