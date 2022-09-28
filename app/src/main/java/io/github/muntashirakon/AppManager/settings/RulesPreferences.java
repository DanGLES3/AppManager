// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.muntashirakon.AppManager.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.MaterialSharedAxis;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.rules.struct.ComponentRule;
import io.github.muntashirakon.AppManager.utils.AppPref;
import io.github.muntashirakon.AppManager.utils.ArrayUtils;
import io.github.muntashirakon.AppManager.utils.FreezeUtils;
import io.github.muntashirakon.AppManager.utils.UIUtils;
import io.github.muntashirakon.dialog.DialogTitleBuilder;

public class RulesPreferences extends PreferenceFragment {
    private final String[] blockingMethods = new String[]{
            ComponentRule.COMPONENT_TO_BE_BLOCKED_IFW_DISABLE,
            ComponentRule.COMPONENT_TO_BE_BLOCKED_IFW,
            ComponentRule.COMPONENT_TO_BE_DISABLED
    };

    private final Integer[] blockingMethodTitles = new Integer[]{
            R.string.intent_firewall_and_disable,
            R.string.intent_firewall,
            R.string.disable
    };

    private final Integer[] blockingMethodDescriptions = new Integer[]{
            R.string.pref_intent_firewall_and_disable_description,
            R.string.pref_intent_firewall_description,
            R.string.pref_disable_description
    };

    private final Integer[] freezingMethods = new Integer[]{
            FreezeUtils.FREEZE_SUSPEND,
            FreezeUtils.FREEZE_DISABLE,
            FreezeUtils.FREEZE_HIDE
    };

    private final Integer[] freezingMethodTitles = new Integer[]{
            R.string.suspend_app,
            R.string.disable,
            R.string.hide_app
    };

    private final Integer[] freezingMethodDescriptions = new Integer[]{
            R.string.suspend_app_description,
            R.string.disable_app_description,
            R.string.hide_app_description
    };

    private SettingsActivity activity;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_rules);
        getPreferenceManager().setPreferenceDataStore(new SettingsDataStore());
        MainPreferencesViewModel model = new ViewModelProvider(requireActivity()).get(MainPreferencesViewModel.class);
        activity = (SettingsActivity) requireActivity();
        // Default freezing method
        Preference defaultFreezingMethod = Objects.requireNonNull(findPreference("freeze_type"));
        AtomicInteger freezeTypeIdx = new AtomicInteger(ArrayUtils.indexOf(freezingMethods, AppPref.getDefaultFreezingMethod()));
        if (freezeTypeIdx.get() != -1) {
            defaultFreezingMethod.setSummary(freezingMethodTitles[freezeTypeIdx.get()]);
        }
        defaultFreezingMethod.setOnPreferenceClickListener(preference -> {
            CharSequence[] itemDescription = new CharSequence[freezingMethods.length];
            for (int i = 0; i < freezingMethods.length; ++i) {
                itemDescription[i] = UIUtils.getStyledKeyValue(
                        activity,
                        getString(freezingMethodTitles[i]),
                        UIUtils.getSecondaryText(activity, UIUtils.getSmallerText(getString(freezingMethodDescriptions[i]))),
                        "\n");
            }
            new MaterialAlertDialogBuilder(activity)
                    .setCustomTitle(new DialogTitleBuilder(activity)
                            .setTitle(R.string.pref_default_freezing_method)
                            .setSubtitle(R.string.pref_default_freezing_method_description)
                            .build())
                    .setSingleChoiceItems(itemDescription, freezeTypeIdx.get(), (dialog, which) -> {
                        AppPref.set(AppPref.PrefKey.PREF_FREEZE_TYPE_INT, freezingMethods[which]);
                        defaultFreezingMethod.setSummary(freezingMethodTitles[which]);
                        freezeTypeIdx.set(which);
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.close, null)
                    .show();
            return true;
        });
        // Default component blocking method
        Preference defaultBlockingMethod = Objects.requireNonNull(findPreference("default_blocking_method"));
        AtomicInteger csIdx = new AtomicInteger(ArrayUtils.indexOf(blockingMethods, AppPref.getDefaultComponentStatus()));
        if (csIdx.get() != -1) {
            defaultBlockingMethod.setSummary(blockingMethodTitles[csIdx.get()]);
        }
        defaultBlockingMethod.setOnPreferenceClickListener(preference -> {
            CharSequence[] itemDescription = new CharSequence[blockingMethods.length];
            for (int i = 0; i < blockingMethods.length; ++i) {
                itemDescription[i] = UIUtils.getStyledKeyValue(
                        activity,
                        getString(blockingMethodTitles[i]),
                        UIUtils.getSecondaryText(activity, UIUtils.getSmallerText(getString(blockingMethodDescriptions[i]))),
                        "\n");
            }
            new MaterialAlertDialogBuilder(activity)
                    .setCustomTitle(new DialogTitleBuilder(activity)
                            .setTitle(R.string.pref_default_blocking_method)
                            .setSubtitle(R.string.pref_default_blocking_method_description)
                            .build())
                    .setSingleChoiceItems(itemDescription, csIdx.get(), (dialog, which) -> {
                        AppPref.set(AppPref.PrefKey.PREF_DEFAULT_BLOCKING_METHOD_STR, blockingMethods[which]);
                        defaultBlockingMethod.setSummary(blockingMethodTitles[which]);
                        csIdx.set(which);
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.close, null)
                    .show();
            return true;
        });
        // Global blocking enabled
        final SwitchPreferenceCompat gcb = Objects.requireNonNull(findPreference("global_blocking_enabled"));
        gcb.setChecked(AppPref.isGlobalBlockingEnabled());
        gcb.setOnPreferenceChangeListener((preference, isEnabled) -> {
            if ((boolean) isEnabled) {
                model.applyAllRules();
            }
            return true;
        });
        // Remove all rules
        ((Preference) Objects.requireNonNull(findPreference("remove_all_rules"))).setOnPreferenceClickListener(preference -> {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.pref_remove_all_rules)
                    .setMessage(R.string.are_you_sure)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        activity.progressIndicator.show();
                        model.removeAllRules();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
            return true;
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
    }

    @Override
    public int getTitle() {
        return R.string.rules;
    }
}
