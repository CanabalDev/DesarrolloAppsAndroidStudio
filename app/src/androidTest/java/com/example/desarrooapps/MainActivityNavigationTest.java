package com.example.desarrooapps;

import android.content.Context;
import android.view.View;

import androidx.activity.OnBackPressedDispatcher;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MainActivityNavigationTest {
    @Test
    public void backFromDashboardReturnsToLoginAndSystemInsetsAreApplied() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SessionManager sessionManager = new SessionManager(context);
        String previousUsername = sessionManager.getUsername();
        sessionManager.saveUsername("navigation_test");
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        try {
            scenario.onActivity(activity -> {
                View root = activity.findViewById(R.id.main);
                assertNotNull(activity.findViewById(R.id.users_card));

                WindowInsetsCompat windowInsets = ViewCompat.getRootWindowInsets(root);
                if (windowInsets != null) {
                    Insets systemBars = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.displayCutout()
                    );
                    assertTrue(root.getPaddingTop() >= systemBars.top);
                    assertTrue(root.getPaddingBottom() >= systemBars.bottom);
                }

                OnBackPressedDispatcher dispatcher = activity.getOnBackPressedDispatcher();
                dispatcher.onBackPressed();

                assertNotNull(activity.findViewById(R.id.login_button));
                assertEquals("navigation_test", new SessionManager(activity).getUsername());
            });
        } finally {
            scenario.close();
            if (previousUsername == null) {
                sessionManager.clear();
            } else {
                sessionManager.saveUsername(previousUsername);
            }
        }
    }
}
