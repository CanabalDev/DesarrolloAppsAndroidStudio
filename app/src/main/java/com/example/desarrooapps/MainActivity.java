package com.example.desarrooapps;

import android.os.Bundle;
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

import androidx.appcompat.app.AppCompatActivity;
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
import java.util.List;
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
    private Screen currentScreen = Screen.LOGIN;

    private enum Screen {
        LOGIN,
        DASHBOARD,
        SECTION_PLACEHOLDER,
        USERS_LIST,
        USER_FORM_PLACEHOLDER
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
                    case USER_FORM_PLACEHOLDER:
                        showUsersList();
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
                showSectionPlaceholder(getString(R.string.dashboard_farms)));
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
                showUserFormPlaceholder(null));
        findViewById(R.id.users_refresh_button).setOnClickListener(view -> loadUsers(requestGeneration));
        findViewById(R.id.users_retry_button).setOnClickListener(view -> loadUsers(requestGeneration));
        loadUsers(requestGeneration);
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
            row.setOnClickListener(view -> showUserFormPlaceholder(user));
            usersContainer.addView(row);
        }
    }

    private void showUserFormPlaceholder(Usuario user) {
        boolean creating = user == null;
        String title = getString(creating ? R.string.user_create_title : R.string.user_edit_title);
        String message = creating
                ? getString(R.string.user_create_placeholder)
                : getString(R.string.user_edit_placeholder, user.nombreUsuario);
        showPlaceholder(
                Screen.USER_FORM_PLACEHOLDER,
                title,
                message,
                R.string.back_to_users,
                this::showUsersList
        );
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
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
