package com.example.desarrooapps;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    private boolean showingSectionPlaceholder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        sessionManager = new SessionManager(this);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (showingSectionPlaceholder) {
                    showSignedInScreen(sessionManager.getUsername());
                } else {
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
        showingSectionPlaceholder = false;
        setContentView(R.layout.activity_dashboard);
        applyWindowInsets();
        TextView welcomeView = findViewById(R.id.session_welcome);
        welcomeView.setText(getString(R.string.session_welcome, username));

        findViewById(R.id.users_card).setOnClickListener(view ->
                showSectionPlaceholder(getString(R.string.dashboard_users)));
        findViewById(R.id.farms_card).setOnClickListener(view ->
                showSectionPlaceholder(getString(R.string.dashboard_farms)));
        findViewById(R.id.logout_button).setOnClickListener(view -> {
            sessionManager.clear();
            showingSectionPlaceholder = false;
            showLoginScreen();
        });
    }

    private void showSectionPlaceholder(String sectionName) {
        showingSectionPlaceholder = true;
        setContentView(R.layout.activity_section_placeholder);
        applyWindowInsets();
        TextView titleView = findViewById(R.id.section_title);
        titleView.setText(sectionName);
        TextView messageView = findViewById(R.id.section_message);
        messageView.setText(getString(R.string.section_coming_soon, sectionName));
        findViewById(R.id.section_back_button).setOnClickListener(view ->
                showSignedInScreen(sessionManager.getUsername()));
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
