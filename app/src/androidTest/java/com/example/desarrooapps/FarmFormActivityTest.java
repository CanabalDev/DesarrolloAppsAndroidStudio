package com.example.desarrooapps;

import android.os.SystemClock;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FarmFormActivityTest {
    @Rule
    public ActivityScenarioRule<FarmFormActivity> activityRule =
            new ActivityScenarioRule<>(FarmFormActivity.class);

    @Test
    public void selectingCountryAndDepartmentLoadsDependentOptions() {
        waitForLocationOptions();

        activityRule.getScenario().onActivity(activity -> {
            MaterialAutoCompleteTextView country =
                    activity.findViewById(R.id.farm_form_country);
            MaterialAutoCompleteTextView department =
                    activity.findViewById(R.id.farm_form_department);
            MaterialAutoCompleteTextView city =
                    activity.findViewById(R.id.farm_form_city);
            ScrollView formScroll = activity.findViewById(R.id.main);
            formScroll.scrollTo(0, 1000);

            country.performClick();
            assertTrue(country.isPopupShowing());
            selectOption(activity, country, "Argentina");
            assertTrue(department.isEnabled());
            assertTrue(adapterContains(department, "Jujuy"));

            department.performClick();
            assertTrue(department.isPopupShowing());
            selectOption(activity, department, "Jujuy");
            assertTrue(city.isEnabled());
            assertTrue(adapterContains(city, "Humahuaca"));
        });
    }

    private void selectOption(
            FarmFormActivity activity,
            MaterialAutoCompleteTextView input,
            String value
    ) {
        ListAdapter adapter = input.getAdapter();
        int position = findPosition(adapter, value);
        assertTrue(position >= 0);
        input.setText(value, false);
        ListView parent = new ListView(activity);
        parent.setAdapter(adapter);
        AdapterView.OnItemClickListener listener = input.getOnItemClickListener();
        listener.onItemClick(parent, null, position, adapter.getItemId(position));
        input.dismissDropDown();
    }

    private boolean adapterContains(MaterialAutoCompleteTextView input, String value) {
        return input.getAdapter() != null && findPosition(input.getAdapter(), value) >= 0;
    }

    private int findPosition(ListAdapter adapter, String value) {
        if (adapter == null) {
            return -1;
        }
        for (int position = 0; position < adapter.getCount(); position++) {
            if (value.contentEquals(adapter.getItem(position).toString())) {
                return position;
            }
        }
        return -1;
    }

    private void waitForLocationOptions() {
        AtomicBoolean enabled = new AtomicBoolean(false);
        long timeout = SystemClock.uptimeMillis() + 20000;
        while (SystemClock.uptimeMillis() < timeout && !enabled.get()) {
            activityRule.getScenario().onActivity(activity ->
                    enabled.set(activity.findViewById(R.id.farm_form_country).isEnabled()));
            if (!enabled.get()) {
                SystemClock.sleep(200);
            }
        }
        assertTrue("Los selectores de ubicación no terminaron de cargar", enabled.get());
    }
}
