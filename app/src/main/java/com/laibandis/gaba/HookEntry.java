package com.laibandis.gaba;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    // ===== НАСТРОЙКИ =====
    static int MIN_INTERCITY = 5000;
    static int MIN_PARCEL = 3000;
    static int MIN_COMPANION = 5000;

    static boolean ONLY_INTERCITY = true;
    static boolean IGNORE_CITY = true;
    static boolean AUTO_OPEN = true; // ← это и есть автозвонок

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals("sinet.startup.inDriver")) return;

        XposedBridge.log("KISS: loaded into " + lpparam.packageName);

        Class<?> sbnClass = XposedHelpers.findClass(
                "android.service.notification.StatusBarNotification",
                lpparam.classLoader
        );

        XposedHelpers.findAndHookMethod(
                "com.android.server.notification.NotificationManagerService",
                lpparam.classLoader,
                "enqueueNotificationInternal",
                String.class, String.class, int.class, int.class,
                String.class, int.class, Notification.class, int.class,
                boolean.class,
                new XC_MethodHook() {

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                        Notification n = (Notification) param.args[6];
                        if (n == null) return;

                        Bundle extras = n.extras;
                        if (extras == null) return;

                        CharSequence titleCs = extras.getCharSequence(Notification.EXTRA_TITLE);
                        CharSequence textCs = extras.getCharSequence(Notification.EXTRA_TEXT);

                        if (titleCs == null || textCs == null) return;

                        String title = titleCs.toString();
                        String text = textCs.toString();

                        if (!title.contains("Новый заказ")) return;

                        int price = parsePrice(text);

                        boolean isParcel = text.contains("посыл");
                        boolean isCompanion = text.contains("С попутчиками");
                        boolean isIntercity = text.contains("Алматы") && text.contains("Тараз");

                        boolean allow = false;

                        if (isIntercity && price >= MIN_INTERCITY) allow = true;
                        if (isParcel && price >= MIN_PARCEL) allow = true;
                        if (isCompanion && price >= MIN_COMPANION) allow = true;

                        if (!allow) {
                            XposedBridge.log("KISS: ignore cheap order = " + price);
                            return;
                        }

                        XposedBridge.log("KISS: ACCEPT " + text);

                        if (AUTO_OPEN && n.contentIntent != null) {
                            try {
                                n.contentIntent.send();
                                XposedBridge.log("KISS: auto open order → auto call");
                            } catch (Throwable t) {
                                XposedBridge.log("KISS: PendingIntent failed: " + t);
                            }
                        }
                    }
                }
        );
    }

    private static int parsePrice(String text) {
        try {
            String digits = text.replaceAll("[^0-9]", "");
            if (digits.length() == 0) return 0;
            return Integer.parseInt(digits);
        } catch (Throwable t) {
            return 0;
        }
    }
}
