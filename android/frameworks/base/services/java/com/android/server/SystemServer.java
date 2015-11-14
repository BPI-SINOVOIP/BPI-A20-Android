/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server;

import android.accounts.AccountManagerService;
import android.app.ActivityManagerNative;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.res.Configuration;
import android.media.AudioService;
import android.net.wifi.p2p.WifiP2pService;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SchedulingPolicyService;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.server.search.SearchManagerService;
import android.service.dreams.DreamService;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.view.DisplayManagerAw;
import android.view.WindowManager;
import android.os.DynamicPManager;

import com.android.internal.os.BinderInternal;
import com.android.internal.os.SamplingProfilerIntegration;
import com.android.internal.widget.LockSettingsService;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.BatteryStatsService;
import com.android.server.display.DisplayManagerService;
import com.android.server.dreams.DreamManagerService;
import com.android.server.input.InputManagerService;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.power.PowerManagerService;
import com.android.server.power.ShutdownThread;
import com.android.server.usb.UsbService;
import com.android.server.wm.WindowManagerService;
import com.android.server.WifiDisplayManagerService;

import dalvik.system.VMRuntime;
import dalvik.system.Zygote;

import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import com.softwinner.SecureFile;
import java.io.FileReader;

/* add by Gary. start {{----------------------------------- */
/* 2011-11-18 */
/* init audio output channel */
import android.media.AudioManager;
import android.provider.Settings;
import android.view.DispList;
/* add by Gary. end   -----------------------------------}} */

class ServerThread extends Thread {
    private static final String TAG = "SystemServer";
    private static final String ENCRYPTING_STATE = "trigger_restart_min_framework";
    private static final String ENCRYPTED_STATE = "1";
    /* add by Gary. start {{----------------------------------- */
    /* 2012-5-9 */
    /* support the YPbPr */
    private final boolean mDeviceHasYpbpr = false;
    /* add by Gary. end   -----------------------------------}} */

    ContentResolver mContentResolver;

    void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Log.wtf(TAG, "BOOT FAILURE " + msg, e);
    }

    @Override
    public void run() {
        EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_SYSTEM_RUN,
            SystemClock.uptimeMillis());

        Looper.prepareMainLooper();

        android.os.Process.setThreadPriority(
                android.os.Process.THREAD_PRIORITY_FOREGROUND);

        BinderInternal.disableBackgroundScheduling(true);
        android.os.Process.setCanSelfBackground(false);

        // Check whether we failed to shut down last time we tried.
        {
            final String shutdownAction = SystemProperties.get(
                    ShutdownThread.SHUTDOWN_ACTION_PROPERTY, "");
            if (shutdownAction != null && shutdownAction.length() > 0) {
                boolean reboot = (shutdownAction.charAt(0) == '1');

                final String reason;
                if (shutdownAction.length() > 1) {
                    reason = shutdownAction.substring(1, shutdownAction.length());
                } else {
                    reason = null;
                }

                ShutdownThread.rebootOrShutdown(reboot, reason);
            }
        }

        String factoryTestStr = SystemProperties.get("ro.factorytest");
        int factoryTest = "".equals(factoryTestStr) ? SystemServer.FACTORY_TEST_OFF
                : Integer.parseInt(factoryTestStr);
        final boolean headless = "1".equals(SystemProperties.get("ro.config.headless", "0"));

        Installer installer = null;
        AccountManagerService accountManager = null;
        ContentService contentService = null;
        LightsService lights = null;
        PowerManagerService power = null;
        DynamicPManagerService dpm = null;
        DisplayManagerService display = null;
        BatteryService battery = null;
        VibratorService vibrator = null;
        AlarmManagerService alarm = null;
        MountService mountService = null;
        NetworkManagementService networkManagement = null;
        NetworkStatsService networkStats = null;
        NetworkPolicyManagerService networkPolicy = null;
        ConnectivityService connectivity = null;
        WifiP2pService wifiP2p = null;
        WifiService wifi = null;
        EthernetService ethernet = null;	/*  EthernetService (add by shugeLinux@gmail.com)  */
        NsdService serviceDiscovery= null;
        IPackageManager pm = null;
        Context context = null;
        WindowManagerService wm = null;
        BluetoothManagerService bluetooth = null;
        DockObserver dock = null;
        UsbService usb = null;
        SerialService serial = null;
        TwilightService twilight = null;
        UiModeManagerService uiMode = null;
        RecognitionManagerService recognition = null;
        ThrottleService throttle = null;
        NetworkTimeUpdateService networkTimeUpdater = null;
        CommonTimeManagementService commonTimeMgmtService = null;
        InputManagerService inputManager = null;
        TelephonyRegistry telephonyRegistry = null;

        // Create a shared handler thread for UI within the system server.
        // This thread is used by at least the following components:
        // - WindowManagerPolicy
        // - KeyguardViewManager
        // - DisplayManagerService
        HandlerThread uiHandlerThread = new HandlerThread("UI");
        uiHandlerThread.start();
        Handler uiHandler = new Handler(uiHandlerThread.getLooper());
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                //Looper.myLooper().setMessageLogging(new LogPrinter(
                //        Log.VERBOSE, "WindowManagerPolicy", Log.LOG_ID_SYSTEM));
                android.os.Process.setThreadPriority(
                        android.os.Process.THREAD_PRIORITY_FOREGROUND);
                android.os.Process.setCanSelfBackground(false);

                // For debug builds, log event loop stalls to dropbox for analysis.
                if (StrictMode.conditionallyEnableDebugLogging()) {
                    Slog.i(TAG, "Enabled StrictMode logging for UI Looper");
                }
            }
        });

        // Create a handler thread just for the window manager to enjoy.
        HandlerThread wmHandlerThread = new HandlerThread("WindowManager");
        wmHandlerThread.start();
        Handler wmHandler = new Handler(wmHandlerThread.getLooper());
        wmHandler.post(new Runnable() {
            @Override
            public void run() {
                //Looper.myLooper().setMessageLogging(new LogPrinter(
                //        android.util.Log.DEBUG, TAG, android.util.Log.LOG_ID_SYSTEM));
                android.os.Process.setThreadPriority(
                        android.os.Process.THREAD_PRIORITY_DISPLAY);
                android.os.Process.setCanSelfBackground(false);

                // For debug builds, log event loop stalls to dropbox for analysis.
                if (StrictMode.conditionallyEnableDebugLogging()) {
                    Slog.i(TAG, "Enabled StrictMode logging for WM Looper");
                }
            }
        });

        // Critical services...
        boolean onlyCore = false;
        try {
            // Wait for installd to finished starting up so that it has a chance to
            // create critical directories such as /data/user with the appropriate
            // permissions.  We need this to complete before we initialize other services.
            Slog.i(TAG, "Waiting for installd to be ready.");
            installer = new Installer();
            installer.ping();

            Slog.i(TAG, "Entropy Mixer");
            ServiceManager.addService("entropy", new EntropyMixer());

            Slog.i(TAG, "Power Manager");
            power = new PowerManagerService();
            ServiceManager.addService(Context.POWER_SERVICE, power);

            Slog.i(TAG, "Activity Manager");
            context = ActivityManagerService.main(factoryTest);

            Slog.i(TAG, "Display Manager");
            display = new DisplayManagerService(context, wmHandler, uiHandler);
            ServiceManager.addService(Context.DISPLAY_SERVICE, display, true);

            Slog.i(TAG, "Telephony Registry");
            telephonyRegistry = new TelephonyRegistry(context);
            ServiceManager.addService("telephony.registry", telephonyRegistry);

            Slog.i(TAG, "Scheduling Policy");
            ServiceManager.addService(Context.SCHEDULING_POLICY_SERVICE,
                    new SchedulingPolicyService());

            AttributeCache.init(context);

            if (!display.waitForDefaultDisplay()) {
                reportWtf("Timeout waiting for default display to be initialized.",
                        new Throwable());
            }

            Slog.i(TAG, "Package Manager");
            // Only run "core" apps if we're encrypting the device.
            String cryptState = SystemProperties.get("vold.decrypt");
            if (ENCRYPTING_STATE.equals(cryptState)) {
                Slog.w(TAG, "Detected encryption in progress - only parsing core apps");
                onlyCore = true;
            } else if (ENCRYPTED_STATE.equals(cryptState)) {
                Slog.w(TAG, "Device encrypted - only parsing core apps");
                onlyCore = true;
            }

            pm = PackageManagerService.main(context, installer,
                    factoryTest != SystemServer.FACTORY_TEST_OFF,
                    onlyCore);
            boolean firstBoot = false;
            try {
                firstBoot = pm.isFirstBoot();
            } catch (RemoteException e) {
            }

            ActivityManagerService.setSystemProcess();

            Slog.i(TAG, "User Service");
            ServiceManager.addService(Context.USER_SERVICE,
                    UserManagerService.getInstance());


            mContentResolver = context.getContentResolver();

            // The AccountManager must come before the ContentService
            try {
                Slog.i(TAG, "Account Manager");
                accountManager = new AccountManagerService(context);
                ServiceManager.addService(Context.ACCOUNT_SERVICE, accountManager);
            } catch (Throwable e) {
                Slog.e(TAG, "Failure starting Account Manager", e);
            }

            Slog.i(TAG, "Content Manager");
            contentService = ContentService.main(context,
                    factoryTest == SystemServer.FACTORY_TEST_LOW_LEVEL);

            Slog.i(TAG, "System Content Providers");
            ActivityManagerService.installSystemProviders();

            Slog.i(TAG, "Lights Service");
            lights = new LightsService(context);

            Slog.i(TAG, "Battery Service");
            battery = new BatteryService(context, lights);
            ServiceManager.addService("battery", battery);

            Slog.i(TAG, "DynamicPManager");
            dpm = new DynamicPManagerService(context);
            ServiceManager.addService(DynamicPManager.DPM_SERVICE, dpm);

            Slog.i(TAG, "Vibrator Service");
            vibrator = new VibratorService(context);
            ServiceManager.addService("vibrator", vibrator);

            // only initialize the power service after we have started the
            // lights service, content providers and the battery service.
            power.init(context, lights, ActivityManagerService.self(), battery,
                    BatteryStatsService.getService(), display);

            Slog.i(TAG, "Alarm Manager");
            alarm = new AlarmManagerService(context);
            ServiceManager.addService(Context.ALARM_SERVICE, alarm);

            Slog.i(TAG, "Init Watchdog");
            Watchdog.getInstance().init(context, battery, power, alarm,
                    ActivityManagerService.self());

            Slog.i(TAG, "Input Manager");
            inputManager = new InputManagerService(context, wmHandler);

            Slog.i(TAG, "Window Manager");
            wm = WindowManagerService.main(context, power, display, inputManager,
                    uiHandler, wmHandler,
                    factoryTest != SystemServer.FACTORY_TEST_LOW_LEVEL,
                    !firstBoot, onlyCore);
            ServiceManager.addService(Context.WINDOW_SERVICE, wm);
            ServiceManager.addService(Context.INPUT_SERVICE, inputManager);

            if (SystemProperties.get("ro.display.switch").equals("1")) {
                Slog.i(TAG, "Display Manager");
                DisplayManagerServiceAw display_aw = new DisplayManagerServiceAw(context, power);
                ServiceManager.addService(Context.DISPLAY_SERVICE_AW, display_aw);

                try {
                    display_aw.systemReady();
                } catch (Throwable e) {
                    Slog.e(TAG, "Failure starting AW Display Manager", e);
                }
            }

            if (SystemProperties.get("ro.wifidisplay.switch").equals("1")) {
                Slog.i(TAG, "Display Manager");
                WifiDisplayManagerService wifidisplay = new WifiDisplayManagerService(context,
                        power);
                ServiceManager.addService(Context.WIFIDISPLAY_SERVICE, wifidisplay);
            }

            ActivityManagerService.self().setWindowManager(wm);

            inputManager.setWindowManagerCallbacks(wm.getInputMonitor());
            inputManager.start();

            display.setWindowManager(wm);
            display.setInputManager(inputManager);

            // Skip Bluetooth if we have an emulator kernel
            // TODO: Use a more reliable check to see if this product should
            // support Bluetooth - see bug 988521
            if (SystemProperties.get("ro.kernel.qemu").equals("1")) {
                Slog.i(TAG, "No Bluetooh Service (emulator)");
            } else if (factoryTest == SystemServer.FACTORY_TEST_LOW_LEVEL) {
                Slog.i(TAG, "No Bluetooth Service (factory test)");
            } else {
                Slog.i(TAG, "Bluetooth Manager Service");
                bluetooth = new BluetoothManagerService(context);
                ServiceManager.addService(BluetoothAdapter.BLUETOOTH_MANAGER_SERVICE, bluetooth);
            }

        } catch (RuntimeException e) {
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting core service", e);
        }

        DevicePolicyManagerService devicePolicy = null;
        StatusBarManagerService statusBar = null;
        InputMethodManagerService imm = null;
        AppWidgetService appWidget = null;
        NotificationManagerService notification = null;
        WallpaperManagerService wallpaper = null;
        LocationManagerService location = null;
        CountryDetectorService countryDetector = null;
        TextServicesManagerService tsms = null;
        LockSettingsService lockSettings = null;
        DreamManagerService dreamy = null;

        // Bring up services needed for UI.
        if (factoryTest != SystemServer.FACTORY_TEST_LOW_LEVEL) {
            try {
                Slog.i(TAG, "Input Method Service");
                imm = new InputMethodManagerService(context, wm);
                ServiceManager.addService(Context.INPUT_METHOD_SERVICE, imm);
            } catch (Throwable e) {
                reportWtf("starting Input Manager Service", e);
            }

            try {
                Slog.i(TAG, "Accessibility Manager");
                ServiceManager.addService(Context.ACCESSIBILITY_SERVICE,
                        new AccessibilityManagerService(context));
            } catch (Throwable e) {
                reportWtf("starting Accessibility Manager", e);
            }
        }

        try {
            wm.displayReady();
        } catch (Throwable e) {
            reportWtf("making display ready", e);
        }

        try {
            pm.performBootDexOpt();
        } catch (Throwable e) {
            reportWtf("performing boot dexopt", e);
        }

        try {
            ActivityManagerNative.getDefault().showBootMessage(
                    context.getResources().getText(
                            com.android.internal.R.string.android_upgrading_starting_apps),
                            false);
        } catch (RemoteException e) {
        }

        if (factoryTest != SystemServer.FACTORY_TEST_LOW_LEVEL) {
            if (!"0".equals(SystemProperties.get("system_init.startmountservice"))) {
                try {
                    /*
                     * NotificationManagerService is dependant on MountService,
                     * (for media / usb notifications) so we must start MountService first.
                     */
                    Slog.i(TAG, "Mount Service");
                    mountService = new MountService(context);
                    ServiceManager.addService("mount", mountService);
                } catch (Throwable e) {
                    reportWtf("starting Mount Service", e);
                }
            }

            try {
                Slog.i(TAG,  "LockSettingsService");
                lockSettings = new LockSettingsService(context);
                ServiceManager.addService("lock_settings", lockSettings);
            } catch (Throwable e) {
                reportWtf("starting LockSettingsService service", e);
            }

            try {
                Slog.i(TAG, "Device Policy");
                devicePolicy = new DevicePolicyManagerService(context);
                ServiceManager.addService(Context.DEVICE_POLICY_SERVICE, devicePolicy);
            } catch (Throwable e) {
                reportWtf("starting DevicePolicyService", e);
            }

            try {
                Slog.i(TAG, "Status Bar");
                statusBar = new StatusBarManagerService(context, wm);
                ServiceManager.addService(Context.STATUS_BAR_SERVICE, statusBar);
            } catch (Throwable e) {
                reportWtf("starting StatusBarManagerService", e);
            }

            try {
                Slog.i(TAG, "Clipboard Service");
                ServiceManager.addService(Context.CLIPBOARD_SERVICE,
                        new ClipboardService(context));
            } catch (Throwable e) {
                reportWtf("starting Clipboard Service", e);
            }

            try {
                Slog.i(TAG, "NetworkManagement Service");
                networkManagement = NetworkManagementService.create(context);
                ServiceManager.addService(Context.NETWORKMANAGEMENT_SERVICE, networkManagement);
            } catch (Throwable e) {
                reportWtf("starting NetworkManagement Service", e);
            }

            try {
                Slog.i(TAG, "Text Service Manager Service");
                tsms = new TextServicesManagerService(context);
                ServiceManager.addService(Context.TEXT_SERVICES_MANAGER_SERVICE, tsms);
            } catch (Throwable e) {
                reportWtf("starting Text Service Manager Service", e);
            }

            try {
                Slog.i(TAG, "NetworkStats Service");
                networkStats = new NetworkStatsService(context, networkManagement, alarm);
                ServiceManager.addService(Context.NETWORK_STATS_SERVICE, networkStats);
            } catch (Throwable e) {
                reportWtf("starting NetworkStats Service", e);
            }

            try {
                Slog.i(TAG, "NetworkPolicy Service");
                networkPolicy = new NetworkPolicyManagerService(
                        context, ActivityManagerService.self(), power,
                        networkStats, networkManagement);
                ServiceManager.addService(Context.NETWORK_POLICY_SERVICE, networkPolicy);
            } catch (Throwable e) {
                reportWtf("starting NetworkPolicy Service", e);
            }

           try {
                Slog.i(TAG, "Wi-Fi P2pService");
                wifiP2p = new WifiP2pService(context);
                ServiceManager.addService(Context.WIFI_P2P_SERVICE, wifiP2p);
            } catch (Throwable e) {
                reportWtf("starting Wi-Fi P2pService", e);
            }

		   /* Begin (add by shugeLinux@gmail.com) */
           try {
                Slog.i(TAG, "Ethernet Service");
                ethernet = new EthernetService(context);
                ServiceManager.addService(Context.ETHERNET_SERVICE, ethernet);
            } catch (Throwable e) {
                reportWtf("starting Ethernet Service", e);
            }
		   /* End (add by shugeLinux@gmail.com) */

           try {
                Slog.i(TAG, "Wi-Fi Service");
                wifi = new WifiService(context);
                ServiceManager.addService(Context.WIFI_SERVICE, wifi);
            } catch (Throwable e) {
                reportWtf("starting Wi-Fi Service", e);
            }

            try {
                Slog.i(TAG, "Connectivity Service");
                connectivity = new ConnectivityService(
                        context, networkManagement, networkStats, networkPolicy);
                ServiceManager.addService(Context.CONNECTIVITY_SERVICE, connectivity);
                networkStats.bindConnectivityManager(connectivity);
                networkPolicy.bindConnectivityManager(connectivity);
                wifi.checkAndStartWifi();
                wifiP2p.connectivityServiceReady();
            } catch (Throwable e) {
                reportWtf("starting Connectivity Service", e);
            }

            try {
                Slog.i(TAG, "Network Service Discovery Service");
                serviceDiscovery = NsdService.create(context);
                ServiceManager.addService(
                        Context.NSD_SERVICE, serviceDiscovery);
            } catch (Throwable e) {
                reportWtf("starting Service Discovery Service", e);
            }

            try {
                Slog.i(TAG, "Throttle Service");
                throttle = new ThrottleService(context);
                ServiceManager.addService(
                        Context.THROTTLE_SERVICE, throttle);
            } catch (Throwable e) {
                reportWtf("starting ThrottleService", e);
            }

            try {
                Slog.i(TAG, "UpdateLock Service");
                ServiceManager.addService(Context.UPDATE_LOCK_SERVICE,
                        new UpdateLockService(context));
            } catch (Throwable e) {
                reportWtf("starting UpdateLockService", e);
            }

            /*
             * MountService has a few dependencies: Notification Manager and
             * AppWidget Provider. Make sure MountService is completely started
             * first before continuing.
             */
            if (mountService != null) {
                mountService.waitForAsecScan();
            }

            try {
                if (accountManager != null)
                    accountManager.systemReady();
            } catch (Throwable e) {
                reportWtf("making Account Manager Service ready", e);
            }

            try {
                if (contentService != null)
                    contentService.systemReady();
            } catch (Throwable e) {
                reportWtf("making Content Service ready", e);
            }

            try {
                Slog.i(TAG, "Notification Manager");
                notification = new NotificationManagerService(context, statusBar, lights);
                ServiceManager.addService(Context.NOTIFICATION_SERVICE, notification);
                networkPolicy.bindNotificationManager(notification);
            } catch (Throwable e) {
                reportWtf("starting Notification Manager", e);
            }

            try {
                Slog.i(TAG, "Device Storage Monitor");
                ServiceManager.addService(DeviceStorageMonitorService.SERVICE,
                        new DeviceStorageMonitorService(context));
            } catch (Throwable e) {
                reportWtf("starting DeviceStorageMonitor service", e);
            }

            try {
                Slog.i(TAG, "Location Manager");
                location = new LocationManagerService(context);
                ServiceManager.addService(Context.LOCATION_SERVICE, location);
            } catch (Throwable e) {
                reportWtf("starting Location Manager", e);
            }

            try {
                Slog.i(TAG, "Country Detector");
                countryDetector = new CountryDetectorService(context);
                ServiceManager.addService(Context.COUNTRY_DETECTOR, countryDetector);
            } catch (Throwable e) {
                reportWtf("starting Country Detector", e);
            }

            try {
                Slog.i(TAG, "Search Service");
                ServiceManager.addService(Context.SEARCH_SERVICE,
                        new SearchManagerService(context));
            } catch (Throwable e) {
                reportWtf("starting Search Service", e);
            }

            try {
                Slog.i(TAG, "DropBox Service");
                ServiceManager.addService(Context.DROPBOX_SERVICE,
                        new DropBoxManagerService(context, new File("/data/system/dropbox")));
            } catch (Throwable e) {
                reportWtf("starting DropBoxManagerService", e);
            }

            if (context.getResources().getBoolean(
                        com.android.internal.R.bool.config_enableWallpaperService)) {
                try {
                    Slog.i(TAG, "Wallpaper Service");
                    if (!headless) {
                        wallpaper = new WallpaperManagerService(context);
                        ServiceManager.addService(Context.WALLPAPER_SERVICE, wallpaper);
                    }
                } catch (Throwable e) {
                    reportWtf("starting Wallpaper Service", e);
                }
            }

            if (!"0".equals(SystemProperties.get("system_init.startaudioservice"))) {
                try {
                    Slog.i(TAG, "Audio Service");
                    ServiceManager.addService(Context.AUDIO_SERVICE, new AudioService(context));
                } catch (Throwable e) {
                    reportWtf("starting Audio Service", e);
                }
            }

            try {
                Slog.i(TAG, "Dock Observer");
                // Listen for dock station changes
                dock = new DockObserver(context);
            } catch (Throwable e) {
                reportWtf("starting DockObserver", e);
            }

            try {
                Slog.i(TAG, "Wired Accessory Manager");
                // Listen for wired headset changes
                inputManager.setWiredAccessoryCallbacks(
                        new WiredAccessoryManager(context, inputManager));
            } catch (Throwable e) {
                reportWtf("starting WiredAccessoryManager", e);
            }

			try {
                Slog.i(TAG, "Audio device manager Observer");
                // Listen for wired headset changes
                AudioDeviceManagerObserver.getInstance(context);
            } catch (Throwable e) {
                reportWtf("starting AudioDeviceManagerObserver", e);
            }

            try {
                Slog.i(TAG, "USB Service");
                // Manage USB host and device support
                usb = new UsbService(context);
                ServiceManager.addService(Context.USB_SERVICE, usb);
            } catch (Throwable e) {
                reportWtf("starting UsbService", e);
            }

            try {
                Slog.i(TAG, "Usb Camera device manager Observer");
                // Listen for wired headset changes
                UsbCameraDeviceManagerObserver.getInstance(context);
            } catch (Throwable e) {
                reportWtf("starting UsbCameraDeviceManagerObserver", e);
            }

            try {
                Slog.i(TAG, "Serial Service");
                // Serial port support
                serial = new SerialService(context);
                ServiceManager.addService(Context.SERIAL_SERVICE, serial);
            } catch (Throwable e) {
                Slog.e(TAG, "Failure starting SerialService", e);
            }

            try {
                Slog.i(TAG, "Twilight Service");
                twilight = new TwilightService(context);
            } catch (Throwable e) {
                reportWtf("starting TwilightService", e);
            }

            try {
                Slog.i(TAG, "UI Mode Manager Service");
                // Listen for UI mode changes
                uiMode = new UiModeManagerService(context, twilight);
            } catch (Throwable e) {
                reportWtf("starting UiModeManagerService", e);
            }

            try {
                Slog.i(TAG, "Backup Service");
                ServiceManager.addService(Context.BACKUP_SERVICE,
                        new BackupManagerService(context));
            } catch (Throwable e) {
                Slog.e(TAG, "Failure starting Backup Service", e);
            }

            try {
                Slog.i(TAG, "AppWidget Service");
                appWidget = new AppWidgetService(context);
                ServiceManager.addService(Context.APPWIDGET_SERVICE, appWidget);
            } catch (Throwable e) {
                reportWtf("starting AppWidget Service", e);
            }

            try {
                Slog.i(TAG, "Recognition Service");
                recognition = new RecognitionManagerService(context);
            } catch (Throwable e) {
                reportWtf("starting Recognition Service", e);
            }

            try {
                Slog.i(TAG, "DiskStats Service");
                ServiceManager.addService("diskstats", new DiskStatsService(context));
            } catch (Throwable e) {
                reportWtf("starting DiskStats Service", e);
            }

            try {
                // need to add this service even if SamplingProfilerIntegration.isEnabled()
                // is false, because it is this service that detects system property change and
                // turns on SamplingProfilerIntegration. Plus, when sampling profiler doesn't work,
                // there is little overhead for running this service.
                Slog.i(TAG, "SamplingProfiler Service");
                ServiceManager.addService("samplingprofiler",
                            new SamplingProfilerService(context));
            } catch (Throwable e) {
                reportWtf("starting SamplingProfiler Service", e);
            }

            try {
                Slog.i(TAG, "NetworkTimeUpdateService");
                networkTimeUpdater = new NetworkTimeUpdateService(context);
            } catch (Throwable e) {
                reportWtf("starting NetworkTimeUpdate service", e);
            }

            try {
                Slog.i(TAG, "CommonTimeManagementService");
                commonTimeMgmtService = new CommonTimeManagementService(context);
                ServiceManager.addService("commontime_management", commonTimeMgmtService);
            } catch (Throwable e) {
                reportWtf("starting CommonTimeManagementService service", e);
            }

            try {
                Slog.i(TAG, "CertBlacklister");
                CertBlacklister blacklister = new CertBlacklister(context);
            } catch (Throwable e) {
                reportWtf("starting CertBlacklister", e);
            }

            if (context.getResources().getBoolean(
                    com.android.internal.R.bool.config_dreamsSupported)) {
                try {
                    Slog.i(TAG, "Dreams Service");
                    // Dreams (interactive idle-time views, a/k/a screen savers)
                    dreamy = new DreamManagerService(context, wmHandler);
                    ServiceManager.addService(DreamService.DREAM_SERVICE, dreamy);
                } catch (Throwable e) {
                    reportWtf("starting DreamManagerService", e);
                }
            }
        }

        // Before things start rolling, be sure we have decided whether
        // we are in safe mode.
        final boolean safeMode = wm.detectSafeMode();
        if (safeMode) {
            ActivityManagerService.self().enterSafeMode();
            // Post the safe mode state in the Zygote class
            Zygote.systemInSafeMode = true;
            // Disable the JIT for the system_server process
            VMRuntime.getRuntime().disableJitCompilation();
        } else {
            // Enable the JIT for the system_server process
            VMRuntime.getRuntime().startJitCompilation();
        }

        // It is now time to start up the app processes...

        try {
            vibrator.systemReady();
        } catch (Throwable e) {
            reportWtf("making Vibrator Service ready", e);
        }

        try {
            lockSettings.systemReady();
        } catch (Throwable e) {
            reportWtf("making Lock Settings Service ready", e);
        }

        if (devicePolicy != null) {
            try {
                devicePolicy.systemReady();
            } catch (Throwable e) {
                reportWtf("making Device Policy Service ready", e);
            }
        }

        if (notification != null) {
            try {
                notification.systemReady();
            } catch (Throwable e) {
                reportWtf("making Notification Service ready", e);
            }
        }

        try {
            wm.systemReady();
        } catch (Throwable e) {
            reportWtf("making Window Manager Service ready", e);
        }

        if (safeMode) {
            ActivityManagerService.self().showSafeModeOverlay();
        }

        // Update the configuration for this context by hand, because we're going
        // to start using it before the config change done in wm.systemReady() will
        // propagate to it.
        Configuration config = wm.computeNewConfiguration();
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager w = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        w.getDefaultDisplay().getMetrics(metrics);
        context.getResources().updateConfiguration(config, metrics);

        try {
            power.systemReady(twilight, dreamy);
        } catch (Throwable e) {
            reportWtf("making Power Manager Service ready", e);
        }

        try {
            pm.systemReady();
        } catch (Throwable e) {
            reportWtf("making Package Manager Service ready", e);
        }

        try {
            display.systemReady(safeMode, onlyCore);
        } catch (Throwable e) {
            reportWtf("making Display Manager Service ready", e);
        }

        // These are needed to propagate to the runnable below.
        final Context contextF = context;
        final MountService mountServiceF = mountService;
        final BatteryService batteryF = battery;
        final NetworkManagementService networkManagementF = networkManagement;
        final NetworkStatsService networkStatsF = networkStats;
        final NetworkPolicyManagerService networkPolicyF = networkPolicy;
        final ConnectivityService connectivityF = connectivity;
        final DockObserver dockF = dock;
        final UsbService usbF = usb;
        final ThrottleService throttleF = throttle;
        final TwilightService twilightF = twilight;
        final UiModeManagerService uiModeF = uiMode;
        final AppWidgetService appWidgetF = appWidget;
        final WallpaperManagerService wallpaperF = wallpaper;
        final InputMethodManagerService immF = imm;
        final RecognitionManagerService recognitionF = recognition;
        final LocationManagerService locationF = location;
        final CountryDetectorService countryDetectorF = countryDetector;
        final NetworkTimeUpdateService networkTimeUpdaterF = networkTimeUpdater;
        final CommonTimeManagementService commonTimeMgmtServiceF = commonTimeMgmtService;
        final TextServicesManagerService textServiceManagerServiceF = tsms;
        final StatusBarManagerService statusBarF = statusBar;
        final DreamManagerService dreamyF = dreamy;
        final InputManagerService inputManagerF = inputManager;
        final TelephonyRegistry telephonyRegistryF = telephonyRegistry;

        // We now tell the activity manager it is okay to run third party
        // code.  It will call back into us once it has gotten to the state
        // where third party code can really run (but before it has actually
        // started launching the initial applications), for us to complete our
        // initialization.
        ActivityManagerService.self().systemReady(new Runnable() {
            public void run() {
                Slog.i(TAG, "Making services ready");

                if (!headless) startSystemUi(contextF);
                try {
                    if (mountServiceF != null) mountServiceF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Mount Service ready", e);
                }
                try {
                    if (batteryF != null) batteryF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Battery Service ready", e);
                }
                try {
                    if (networkManagementF != null) networkManagementF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Network Managment Service ready", e);
                }
                try {
                    if (networkStatsF != null) networkStatsF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Network Stats Service ready", e);
                }
                try {
                    if (networkPolicyF != null) networkPolicyF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Network Policy Service ready", e);
                }
                try {
                    if (connectivityF != null) connectivityF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Connectivity Service ready", e);
                }
                try {
                    if (dockF != null) dockF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Dock Service ready", e);
                }
                try {
                    if (usbF != null) usbF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making USB Service ready", e);
                }
                try {
                    if (twilightF != null) twilightF.systemReady();
                } catch (Throwable e) {
                    reportWtf("makin Twilight Service ready", e);
                }
                try {
                    if (uiModeF != null) uiModeF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making UI Mode Service ready", e);
                }
                try {
                    if (recognitionF != null) recognitionF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Recognition Service ready", e);
                }
                Watchdog.getInstance().start();

                // It is now okay to let the various system services start their
                // third party code...

                try {
                    if (appWidgetF != null) appWidgetF.systemReady(safeMode);
                } catch (Throwable e) {
                    reportWtf("making App Widget Service ready", e);
                }
                try {
                    if (wallpaperF != null) wallpaperF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Wallpaper Service ready", e);
                }
                try {
                    if (immF != null) immF.systemReady(statusBarF);
                } catch (Throwable e) {
                    reportWtf("making Input Method Service ready", e);
                }
                try {
                    if (locationF != null) locationF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Location Service ready", e);
                }
                try {
                    if (countryDetectorF != null) countryDetectorF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Country Detector Service ready", e);
                }
                try {
                    if (throttleF != null) throttleF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Throttle Service ready", e);
                }
                try {
                    if (networkTimeUpdaterF != null) networkTimeUpdaterF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Network Time Service ready", e);
                }
                try {
                    if (commonTimeMgmtServiceF != null) commonTimeMgmtServiceF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Common time management service ready", e);
                }
                try {
                    if (textServiceManagerServiceF != null) textServiceManagerServiceF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Text Services Manager Service ready", e);
                }
                try {
                    if (dreamyF != null) dreamyF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making DreamManagerService ready", e);
                }
                try {
                    // TODO(BT) Pass parameter to input manager
                    if (inputManagerF != null) inputManagerF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making InputManagerService ready", e);
                }
                try {
                    if (telephonyRegistryF != null) telephonyRegistryF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making TelephonyRegistry ready", e);
                }
            }
        });

        /*
         * Author : huanglong@allwinnertech.com
         * Description : Initialize the DisplayManagerAw, to set the display area.
         * Date : 2013-3-15
         */
        DisplayManagerAw displayManager = new DisplayManagerAw();
        if (displayManager == null) {
            Log.w(TAG, "Failed in creating DisplayManager.");

            // init the display area
            int ratio = getDispRatio();
            Slog.i(TAG, "Display area ratio = " + ratio);
            displayManager.setDisplayAreaPercent(0, ratio);
        }

        /* add by Gary. start {{----------------------------------- */
        /* 2011-12-10 */
        /* init display output */
        boolean ypbprIsConnected = false;
        boolean cvbsIsConnected = false;
        boolean hdmiIsConnected = false;
        DispList.DispFormat finalFormat = null;

        // DisplayManager displayManager =
        // (DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager == null)
            Log.w(TAG, "Fail in creating DisplayManager.");

        /* check cable's connection status */
        if (displayManager.getHdmiHotPlugStatus() != 0)
            hdmiIsConnected = true;

        int tvStatus = displayManager.getTvHotPlugStatus();
        Log.d(TAG, "tv connect status = " + tvStatus);
        if (tvStatus == DisplayManagerAw.DISPLAY_TVDAC_CVBS)
            cvbsIsConnected = true;
        else if (tvStatus == DisplayManagerAw.DISPLAY_TVDAC_YPBPR)
            ypbprIsConnected = true;
        else if (tvStatus == DisplayManagerAw.DISPLAY_TVDAC_ALL) {
            cvbsIsConnected = true;
            ypbprIsConnected = true;
        }
        Log.d(TAG, "HDMI connect status = " + hdmiIsConnected + ", av connnect status = "
                + cvbsIsConnected + ", YPbPr connect status = " + ypbprIsConnected);

        DispList.DispFormat savedFormat = null;
        String savedFormatName = Settings.System.getString(context.getContentResolver(),
                Settings.System.DISPLY_OUTPUT_FORMAT);
        if (savedFormatName == null) {
            Log.w(TAG, "Fail in getting saved display format.");
            savedFormat = DispList.HDMI_DEFAULT_FORMAT;
        } else {
            savedFormat = DispList.ItemName2Code(savedFormatName);
        }
        Log.d(TAG, "saved display = " + DispList.ItemCode2Name(savedFormat));
        int savedType = savedFormat.mOutputType;
        DispList.DispFormat curFormat = new DispList.DispFormat(
                displayManager.getDisplayOutputType(0),
                displayManager.getDisplayOutputFormat(0));
        Log.d(TAG, "current format is " + curFormat);

        int savedIntType = DispList.getAdvancedDisplayType(savedFormat);
        int finalIntType = DispList.ADVANCED_DISPLAY_TYPE_HDMI;
        if (hdmiIsConnected && DispList.isHDMI(curFormat)) {
            finalIntType = DispList.ADVANCED_DISPLAY_TYPE_HDMI;
            finalFormat = curFormat;
        } else if (cvbsIsConnected && DispList.isCVBS(curFormat)) {
            finalIntType = DispList.ADVANCED_DISPLAY_TYPE_CVBS;
            finalFormat = curFormat;
        } else if (mDeviceHasYpbpr && ypbprIsConnected && DispList.isYPbPr(curFormat)) {
            finalIntType = DispList.ADVANCED_DISPLAY_TYPE_YPBPR;
            finalFormat = curFormat;
        } else if (!mDeviceHasYpbpr && ypbprIsConnected && DispList.isVGA(curFormat)) {
            finalIntType = DispList.ADVANCED_DISPLAY_TYPE_VGA;
            finalFormat = DispList.VGA_DEFAULT_FORMAT;
        } else if (hdmiIsConnected) {
            finalIntType = DispList.ADVANCED_DISPLAY_TYPE_HDMI;
            finalFormat = DispList.HDMI_DEFAULT_FORMAT;
        } else if (cvbsIsConnected) {
            finalIntType = DispList.ADVANCED_DISPLAY_TYPE_CVBS;
            finalFormat = DispList.CVBS_DEFAULT_FORMAT;
        } else if (mDeviceHasYpbpr && ypbprIsConnected) {
            finalIntType = DispList.ADVANCED_DISPLAY_TYPE_YPBPR;
            finalFormat = DispList.YPBPR_DEFAULT_FORMAT;
        } else if (!mDeviceHasYpbpr && ypbprIsConnected){
            finalIntType = DispList.ADVANCED_DISPLAY_TYPE_VGA;
            finalFormat = DispList.VGA_DEFAULT_FORMAT;
        } else {
            finalIntType = savedIntType;
            finalFormat = savedFormat;
        }

        if (finalIntType == savedIntType)
            finalFormat = savedFormat;

        if (finalFormat.mOutputType == DisplayManagerAw.DISPLAY_OUTPUT_TYPE_HDMI) {
            boolean isSupport = (displayManager.isSupportHdmiMode(finalFormat.mFormat) != 0);
            if (!isSupport) {
                Log.d(TAG, "HDMI mode " + DispList.ItemCode2Name(finalFormat)
                        + " is NOT supported by the TV.");
                finalFormat = DispList.HDMI_DEFAULT_FORMAT;
            }
        }
        Log.d(TAG, "final format is " + DispList.ItemCode2Name(finalFormat));

        final String srcPath = "/data/displaysetmode";
        final File FILE = new File(srcPath);
        final String values = new String(finalFormat.mOutputType + "\n" + finalFormat.mFormat);
        try {
            FileOutputStream fos = new FileOutputStream(FILE);
            fos.write(values.getBytes());
            fos.flush();
            fos.getFD().sync();
            fos.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

        Settings.System.putString(context.getContentResolver(),
                Settings.System.DISPLY_OUTPUT_FORMAT,
                DispList.ItemCode2Name(finalFormat));
        displayManager.saveDisplayOutputTypeAndFormat(0, finalFormat.mOutputType, finalFormat.mFormat);

        AudioManager audioManager = new AudioManager(context);
        if(audioManager == null){
            Log.e(TAG, "audioManager is null");
        }else {
            /* modified by chenjd,chenjd@allwinnertech.com,20120710, start {{--------
            * init audio output mode,may be multi channels */
            String audioOutputChannelName = Settings.System.getString(context.getContentResolver(),
                Settings.System.AUDIO_OUTPUT_CHANNEL);
            if(audioOutputChannelName == null) {
                audioOutputChannelName = AudioManager.AUDIO_NAME_CODEC;
                Log.d(TAG,"use default audio output channel:" + audioOutputChannelName);
            }
            Log.d(TAG, "saved audio channel is " + audioOutputChannelName);
            ArrayList<String> audioOutputChannels = new ArrayList<String>();
            String[] audioList = audioOutputChannelName.split(",");
            for(String audio:audioList){
                if(!"".equals(audio)){
                    audioOutputChannels.add(audio);
                }
            }
            Log.d(TAG, "active the saved audio channels " + audioOutputChannels);
            ArrayList<String> activedChannels = new ArrayList<String>();
            boolean  usbAudioOutputUsedAndExist = false;
            if(audioOutputChannelName.contains("USB")){
                String audioUsb = String.format("AUDIO_USB_");
                String usbCardId = audioOutputChannelName.substring(audioUsb.length());
                char[] buffer = new char[1024];
                String cardName = null;
                for (int card = 0; card < 8; card++) {
                    try {
                        String newCard = String.format("/sys/class/sound/card%d/",card);
                        String newCardId = newCard + "id";

                        FileReader file = new FileReader(newCardId);
                        int len = file.read(buffer, 0, 1024);
                        file.close();

                        cardName = new String(buffer, 0, len).trim();
                        if (usbCardId.contains(cardName))
                        {
                            usbAudioOutputUsedAndExist = true;
                            activedChannels = audioOutputChannels;
                            Log.d(TAG,"USB Audio Device  " + cardName + " exist");
                        }
                    } catch (FileNotFoundException e) {
                        break;
                    } catch (Exception e) {
                    }
                }
            }
            if(!usbAudioOutputUsedAndExist){
                 if(audioOutputChannels.contains(AudioManager.AUDIO_NAME_SPDIF)){
                     activedChannels.add(AudioManager.AUDIO_NAME_SPDIF);
                 }
                 if(audioOutputChannels.contains(AudioManager.AUDIO_NAME_CODEC)){
                     activedChannels.add(AudioManager.AUDIO_NAME_CODEC);
                 }
                 if(finalFormat.mOutputType == DisplayManagerAw.DISPLAY_OUTPUT_TYPE_HDMI){
                     activedChannels.add(AudioManager.AUDIO_NAME_HDMI);
                 }else if(!audioOutputChannels.contains(AudioManager.AUDIO_NAME_CODEC)){
                     activedChannels.add(AudioManager.AUDIO_NAME_CODEC);
                 }
            }
            audioManager.setAudioDeviceActive(activedChannels,AudioManager.AUDIO_OUTPUT_ACTIVE);
            /* record the audio output channel */
            String st = null;
            for(int i = 0; i < activedChannels.size(); i++){
                if(st == null){
                    st = activedChannels.get(i);
                }else{
                    st = st + "," + activedChannels.get(i);
                }
            }
            Settings.System.putString(context.getContentResolver(), Settings.System.AUDIO_OUTPUT_CHANNEL,st);
            Log.d(TAG,"audio channels change to {" + st + "}");
        }


        // For debug builds, log event loop stalls to dropbox for analysis.
        if (StrictMode.conditionallyEnableDebugLogging()) {
            Slog.i(TAG, "Enabled StrictMode for system server main thread.");
        }

        Looper.loop();
        Slog.d(TAG, "System ServerThread is exiting!");
    }

    static final void startSystemUi(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.systemui",
                    "com.android.systemui.SystemUIService"));
        //Slog.d(TAG, "Starting service: " + intent);
        context.startServiceAsUser(intent, UserHandle.OWNER);
    }

    private int getDispRatio() {
        int defaultRatio = 95;
        int maxRatio = 100;
        String DISPLAY_AREA_RADIO = "display.area_radio";
        SecureFile file = new SecureFile(DISPLAY_AREA_RADIO);
        if (!file.exists())
        {
            return defaultRatio;
        }
        byte[] ret = new byte[255];
        file.read(ret);
        String st = new String(ret).substring(0, 2);
        int retInt = defaultRatio;
        try {
            retInt = Integer.valueOf(st).intValue();
            if (retInt == 10)
                retInt = maxRatio;
        } catch (Exception e) {
        }
        return retInt;
    }
}

public class SystemServer {
    private static final String TAG = "SystemServer";

    public static final int FACTORY_TEST_OFF = 0;
    public static final int FACTORY_TEST_LOW_LEVEL = 1;
    public static final int FACTORY_TEST_HIGH_LEVEL = 2;

    static Timer timer;
    static final long SNAPSHOT_INTERVAL = 60 * 60 * 1000; // 1hr

    // The earliest supported time.  We pick one day into 1970, to
    // give any timezone code room without going into negative time.
    private static final long EARLIEST_SUPPORTED_TIME = 86400 * 1000;

    /**
     * This method is called from Zygote to initialize the system. This will cause the native
     * services (SurfaceFlinger, AudioFlinger, etc..) to be started. After that it will call back
     * up into init2() to start the Android services.
     */
    native public static void init1(String[] args);

    public static void main(String[] args) {
        if (System.currentTimeMillis() < EARLIEST_SUPPORTED_TIME) {
            // If a device's clock is before 1970 (before 0), a lot of
            // APIs crash dealing with negative numbers, notably
            // java.io.File#setLastModified, so instead we fake it and
            // hope that time from cell towers or NTP fixes it
            // shortly.
            Slog.w(TAG, "System clock is before 1970; setting to 1970.");
            SystemClock.setCurrentTimeMillis(EARLIEST_SUPPORTED_TIME);
        }

        if (SamplingProfilerIntegration.isEnabled()) {
            SamplingProfilerIntegration.start();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SamplingProfilerIntegration.writeSnapshot("system_server", null);
                }
            }, SNAPSHOT_INTERVAL, SNAPSHOT_INTERVAL);
        }

        // Mmmmmm... more memory!
        dalvik.system.VMRuntime.getRuntime().clearGrowthLimit();

        // The system server has to run all of the time, so it needs to be
        // as efficient as possible with its memory usage.
        VMRuntime.getRuntime().setTargetHeapUtilization(0.8f);

        System.loadLibrary("android_servers");
        init1(args);
    }

    public static final void init2() {
        Slog.i(TAG, "Entered the Android system server!");
        Thread thr = new ServerThread();
        thr.setName("android.server.ServerThread");
        thr.start();
    }
}
