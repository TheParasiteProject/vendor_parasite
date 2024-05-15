/*
 * Copyright (C) 2022 The Pixel Experience Project
 *               2021-2022 crDroid Android Project
 * Copyright (C) 2022 Paranoid Android
 * Copyright (C) 2022 StatiXOS
 * Copyright (C) 2023 the RisingOS Android Project
 *           (C) 2023 ArrowOS
 *           (C) 2023 The LibreMobileOS Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.custom;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.Application;
import android.app.TaskStackListener;
import android.content.Context;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import org.lineageos.platform.internal.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PixelPropsUtils {

    private static final String TAG = PixelPropsUtils.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final Boolean sEnablePixelProps =
            SystemProperties.getBoolean("persist.sys.pihooks.enable", true);

    private static final Map<String, Object> propsToChangeGeneric;

    private static final Map<String, Object> propsToChangeRecentPixel =
            createGoogleSpoofProps("Pixel 8 Pro",
                    "google/husky/husky:14/UQ1A.240205.004/11269751:user/release-keys");

    private static final Map<String, Object> propsToChangePixelTablet =
            createGoogleSpoofProps("Pixel Tablet",
                    "google/tangorpro/tangorpro:14/UQ1A.240205.002/11224170:user/release-keys");

    private static final Map<String, Object> propsToChangePixel5a =
            createGoogleSpoofProps("Pixel 5a",
                    "google/barbet/barbet:14/UQ1A.240205.002/11224170:user/release-keys");

    private static final Map<String, Object> propsToChangePixelXL =
            createGoogleSpoofProps("Pixel XL",
                    "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");

    private static final Map<String, ArrayList<String>> propsToKeep;

    static {
        propsToKeep = new HashMap<>();
        propsToKeep.put("com.google.android.settings.intelligence", new ArrayList<>(Collections.singletonList("FINGERPRINT")));
        propsToChangeGeneric = new HashMap<>();
        propsToChangeGeneric.put("TYPE", "user");
        propsToChangeGeneric.put("TAGS", "release-keys");
    }

    private static final ArrayList<String> packagesToChangeRecentPixel = 
        new ArrayList<String> (
            Arrays.asList(
                "com.google.android.apps.emojiwallpaper",
                "com.google.android.wallpaper.effects",
                "com.google.pixel.livewallpaper",
                "com.google.android.apps.wallpaper.pixel",
                "com.google.android.apps.wallpaper",
                "com.google.android.apps.bard",
                "com.google.android.apps.customization.pixel",
                "com.google.android.apps.privacy.wildlife",
                "com.google.android.apps.subscriptions.red",
                "com.google.android.apps.photos",
                "com.google.android.gms",
                "com.google.android.googlequicksearchbox"
        ));

    private static final ArrayList<String> extraPackagesToChange = 
        new ArrayList<String> (
            Arrays.asList(
                "com.android.chrome",
                "com.breel.wallpapers20",
                "com.microsoft.android.smsorganizer",
                "com.nothing.smartcenter",
                "com.nhs.online.nhsonline",
                "com.realme.link",
                "com.amazon.avod.thirdpartyclient",
                "com.disney.disneyplus",
                "com.netflix.mediaclient",
                "in.startv.hotstar",
                "jp.id_credit_sp2.android"
        ));

    private static final ArrayList<String> customGoogleCameraPackages = 
        new ArrayList<String> (
            Arrays.asList(
                "com.google.android.MTCL83",
                "com.google.android.UltraCVM",
                "com.google.android.apps.cameralite"
        ));

    private static final ArrayList<String> packagesToKeep = 
        new ArrayList<String> (
            Arrays.asList(
                "com.google.android.as",
                "com.google.android.apps.miphone.aiai.AiaiApplication",
                "com.google.android.apps.motionsense.bridge",
                "com.google.android.euicc",
                "com.google.ar.core",
                "com.google.android.youtube",
                "com.google.android.apps.youtube.kids",
                "com.google.android.apps.youtube.music",
                "com.google.android.apps.wearables.maestro.companion",
                "com.google.android.apps.subscriptions.red",
                "com.google.android.apps.tachyon",
                "com.google.android.apps.tycho",
                "com.google.android.apps.restore",
                "com.google.oslo",
                "it.ingdirect.app",
                "com.google.android.apps.nexuslauncher",
                "com.google.intelligence.sense",
                "com.google.android.apps.tips",
                "com.google.android.apps.dreamliner",
                "com.google.android.apps.dreamlinerupdater",
                "com.google.android.apps.pixelmigrate",
                "com.google.android.backupuses",
                "com.google.android.backuptransport"
        ));

    private static final String sNetflixModel =
            SystemProperties.get("persist.sys.pihooks.netflix_model", "");

    private static final ComponentName GMS_ADD_ACCOUNT_ACTIVITY = ComponentName.unflattenFromString(
            "com.google.android.gms/.auth.uiflows.minutemaid.MinuteMaidActivity");

    private static volatile boolean sIsGms, sIsFinsky, sIsSetupWizard, sIsGoogle, sIsSamsung;

    private static String getBuildID(String fingerprint) {
        Pattern pattern = Pattern.compile("([A-Za-z0-9]+\\.\\d+\\.\\d+\\.\\w+)");
        Matcher matcher = pattern.matcher(fingerprint);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static String getDeviceName(String fingerprint) {
        String[] parts = fingerprint.split("/");
        if (parts.length >= 2) {
            return parts[1];
        }
        return "";
    }

    private static Map<String, Object> createGoogleSpoofProps(String model, String fingerprint) {
        Map<String, Object> props = new HashMap<>();
        props.put("BRAND", "google");
        props.put("MANUFACTURER", "Google");
        props.put("ID", getBuildID(fingerprint));
        props.put("DEVICE", getDeviceName(fingerprint));
        props.put("PRODUCT", getDeviceName(fingerprint));
        props.put("MODEL", model);
        props.put("FINGERPRINT", fingerprint);
        props.put("TYPE", "user");
        props.put("TAGS", "release-keys");
        return props;
    }

    private static boolean isGoogleCameraPackage(String packageName){
        return packageName.startsWith("com.google.android.GoogleCamera") ||
            customGoogleCameraPackages.contains(packageName);
    }

    private static void spoofBuildGms() {
        String[] sCertifiedProps = {
            SystemProperties.get("persist.sys.pihooks.product_name", ""),
            SystemProperties.get("persist.sys.pihooks.product_device", ""),
            SystemProperties.get("persist.sys.pihooks.manufacturer", ""),
            SystemProperties.get("persist.sys.pihooks.brand", ""),
            SystemProperties.get("persist.sys.pihooks.product_model", ""),
            SystemProperties.get("persist.sys.pihooks.build_fingerprint", ""),
            SystemProperties.get("persist.sys.pihooks.security_patch", ""),
            SystemProperties.get("persist.sys.pihooks.first_api_level", ""),
            SystemProperties.get("persist.sys.pihooks.build_id", ""),
            SystemProperties.get("persist.sys.pihooks.build_type", ""),
            SystemProperties.get("persist.sys.pihooks.build_tags", "")
        };

        if (sCertifiedProps == null || sCertifiedProps.length == 0) return;
        // Alter model name and fingerprint to avoid hardware attestation enforcement
        setPropValue("PRODUCT", sCertifiedProps[0].isEmpty() ? getDeviceName(sCertifiedProps[4]) : sCertifiedProps[0]);
        setPropValue("DEVICE", sCertifiedProps[1].isEmpty() ? getDeviceName(sCertifiedProps[4]) : sCertifiedProps[1]);
        setPropValue("MANUFACTURER", sCertifiedProps[2]);
        setPropValue("BRAND", sCertifiedProps[3]);
        setPropValue("MODEL", sCertifiedProps[4]);
        setPropValue("FINGERPRINT", sCertifiedProps[5]);
        if (!sCertifiedProps[6].isEmpty()) {
            setPropValue("SECURITY_PATCH", sCertifiedProps[6]);
        }
        if (!sCertifiedProps[7].isEmpty() && sCertifiedProps[7].matches("\\d+")) {
            setPropValue("DEVICE_INITIAL_SDK_INT", Integer.parseInt(sCertifiedProps[7]));
        }
        setPropValue("ID", sCertifiedProps[8].isEmpty() ? getBuildID(sCertifiedProps[4]) : sCertifiedProps[8]);
        setPropValue("TYPE", sCertifiedProps[9].isEmpty() ? "user" : sCertifiedProps[9]);
        setPropValue("TAGS", sCertifiedProps[10].isEmpty() ? "release-keys" : sCertifiedProps[10]);
    }

    public static void setProps(Context context) {
        if (!sEnablePixelProps) {
            dlog("Pixel props is disabled by config");
            return;
        }

        if (context == null) return;

        final String packageName = context.getPackageName();
        if (packageName == null || packageName.isEmpty()) {
            return;
        }

        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return;
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = null;
        try {
            runningProcesses = manager.getRunningAppProcesses();
        } catch (Exception e) {
            runningProcesses = null;
        }
        if (runningProcesses == null) return;

        String processName = null;
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.pid == android.os.Process.myPid()) {
                processName = processInfo.processName;
                break;
            }
        }
        if (processName == null) return;

        propsToChangeGeneric.forEach((k, v) -> setPropValue(k, v));

        final boolean sIsTablet = isDeviceTablet(context);
        sIsGoogle = packageName.toLowerCase().contains("google") || processName.toLowerCase().contains("google");
        sIsSamsung = packageName.toLowerCase().contains("samsung") || processName.toLowerCase().contains("samsung");
        sIsGms = packageName.equals("com.google.android.gms");
        sIsFinsky = packageName.equals("com.android.vending");
        sIsSetupWizard = packageName.equals("com.google.android.setupwizard");

        if (sIsGms) {
            setPropValue("TIME", System.currentTimeMillis());

            if (processName.toLowerCase().contains("chimera")
                || processName.toLowerCase().contains("unstable")) {
                spoofBuildGms();
                return;
            }
        }

        if (packagesToKeep.contains(packageName)
            || packagesToKeep.contains(processName)) {
            return;
        }

        Map<String, Object> propsToChange = new HashMap<>();
        if (sIsGoogle || sIsSamsung
            || extraPackagesToChange.contains(packageName)
            || extraPackagesToChange.contains(processName)) {

            if (packagesToChangeRecentPixel.contains(packageName)
                || packagesToChangeRecentPixel.contains(processName)) {
                propsToChange = propsToChangeRecentPixel;
            } else if (sIsTablet) {
                propsToChange = propsToChangePixelTablet;
            } else {
                propsToChange = propsToChangePixel5a;
            }
            if (packageName.equals("com.google.android.apps.photos")) {
                if (SystemProperties.getBoolean("persist.sys.pixelprops.gphotos", false)) {
                    propsToChange = propsToChangePixelXL;
                }
            }
            if (sIsGms
                && (processName.toLowerCase().contains("ui")
                || processName.toLowerCase().contains("gservice")
                || processName.toLowerCase().contains("gapps")
                || processName.toLowerCase().contains("learning")
                || processName.toLowerCase().contains("search")
                || processName.toLowerCase().contains("persistent"))) {
                propsToChange = propsToChangePixel5a;
            }
            // Allow process spoofing for GoogleCamera packages
            if (isGoogleCameraPackage(packageName) && (propsToChange == null || propsToChange.isEmpty())) {
                return;
            }
        }
        if (propsToChange == null || propsToChange.isEmpty()) return;
        dlog("Defining props for: " + packageName);
        for (Map.Entry<String, Object> prop : propsToChange.entrySet()) {
            String key = prop.getKey();
            Object value = prop.getValue();
            if (propsToKeep.containsKey(packageName) && propsToKeep.get(packageName).contains(key)) {
                dlog("Not defining " + key + " prop for: " + packageName);
                continue;
            }
            dlog("Defining " + key + " prop for: " + packageName);
            setPropValue(key, value);
        }
        // Set proper indexing fingerprint
        if (packageName.equals("com.google.android.settings.intelligence")) {
            setPropValue("FINGERPRINT", Build.VERSION.INCREMENTAL);
            return;
        }
        if (!sNetflixModel.isEmpty() && packageName.equals("com.netflix.mediaclient")) {
            dlog("Setting model to " + sNetflixModel + " for Netflix");
            setPropValue("MODEL", sNetflixModel);
            return;
        }
    }

    private static boolean isDeviceTablet(Context context) {
        if (context == null) {
            return false;
        }
        Configuration configuration = context.getResources().getConfiguration();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        return (configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE
                || displayMetrics.densityDpi == DisplayMetrics.DENSITY_XHIGH
                || displayMetrics.densityDpi == DisplayMetrics.DENSITY_XXHIGH
                || displayMetrics.densityDpi == DisplayMetrics.DENSITY_XXXHIGH;
    }

    private static void setPropValue(String key, Object value) {
        try {
            if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                dlog(TAG + " Skipping setting empty value for key: " + key);
                return;
            }
            dlog(TAG + " Setting property for key: " + key + ", value: " + value.toString());
            Field field;
            Class<?> targetClass;
            try {
                targetClass = Build.class;
                field = targetClass.getDeclaredField(key);
            } catch (NoSuchFieldException e) {
                targetClass = Build.VERSION.class;
                field = targetClass.getDeclaredField(key);
            }
            if (field != null) {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                if (fieldType == int.class || fieldType == Integer.class) {
                    if (value instanceof Integer) {
                        field.set(null, value);
                    } else if (value instanceof String) {
                        int convertedValue = Integer.parseInt((String) value);
                        field.set(null, convertedValue);
                        dlog(TAG + " Converted value for key " + key + ": " + convertedValue);
                    }
                } else if (fieldType == String.class) {
                    field.set(null, String.valueOf(value));
                }
                field.setAccessible(false);
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            dlog(TAG + " Failed to set prop " + key);
        } catch (NumberFormatException e) {
            dlog(TAG + " Failed to parse value for field " + key);
        }
    }

    private static boolean isGmsAddAccountActivityOnTop() {
        try {
            final ActivityTaskManager.RootTaskInfo focusedTask =
                    ActivityTaskManager.getService().getFocusedRootTaskInfo();
            return focusedTask != null && focusedTask.topActivity != null
                    && focusedTask.topActivity.equals(GMS_ADD_ACCOUNT_ACTIVITY);
        } catch (Exception e) {
            Log.e(TAG, "Unable to get top activity!", e);
        }
        return false;
    }

    public static boolean shouldBypassTaskPermission(Context context) {
        // GMS doesn't have MANAGE_ACTIVITY_TASKS permission
        final int callingUid = Binder.getCallingUid();
        final String callingPackage = context.getPackageManager().getNameForUid(callingUid);
        dlog("shouldBypassTaskPermission: callingPackage:" + callingPackage);
        return callingPackage != null && callingPackage.toLowerCase().contains("google");
    }

    private static boolean isCallerSafetyNet() {
        return sIsGms && Arrays.stream(Thread.currentThread().getStackTrace())
                            .anyMatch(elem -> elem.getClassName().toLowerCase()
                                .contains("droidguard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet or Play Integrity
        if (sIsSetupWizard) {
            Process.killProcess(Process.myPid());
            return;
        }
        if (isCallerSafetyNet() || sIsFinsky) {
            dlog("Blocked key attestation sIsGms=" + sIsGms + " sIsFinsky=" + sIsFinsky);
            throw new UnsupportedOperationException();
        }
    }

    public static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
