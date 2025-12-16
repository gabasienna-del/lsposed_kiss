package com.laibandis.gaba;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import de.robv.android.xposed.AndroidAppHelper;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    // ===== НАСТРОЙКИ =====
    private static final int MIN_PRICE = 5000;          // цена от
    private static final boolean ONLY_INTERCITY = true; // только межгород
    private static final boolean IGNORE_CITY = true;    // игнор городских

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"sinet.startup.inDriver".equals(lpparam.packageName)) return;

        XposedBridge.log("KISS: loaded into " + lpparam.packageName);

        try {
            XposedHelpers.findAndHookMethod(
                    "android.app.NotificationManager",
                    lpparam.classLoader,
                    "notify",
                    String.class,
                    int.class,
                    Notification.class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Notification n = (Notification) param.args[2];
                            if (n == null) return;

                            Bundle extras = n.extras;
                            if (extras == null) return;

                            CharSequence titleCs = extras.getCharSequence(Notification.EXTRA_TITLE);
                            CharSequence textCs  = extras.getCharSequence(Notification.EXTRA_TEXT);

                            if (titleCs == null || textCs == null) return;

                            String title = titleCs.toString();
                            String text  = textCs.toString();

                            if (!title.contains("Новый заказ")) return;

                            int price = parsePrice(text);

                            XposedBridge.log("KISS: parsed price = " + price + " | text=" + text);

                            // цена
                            if (price < MIN_PRICE) {
                                XposedBridge.log("KISS: ignore cheap order = " + price);
                                param.setResult(null);
                                return;
                            }

                            boolean intercity =
                                    text.contains("Алматы") &&
                                    (text.contains("Тараз") || text.contains("Шымкент"));

                            if (ONLY_INTERCITY && !intercity) {
                                XposedBridge.log("KISS: ignore city order");
                                param.setResult(null);
                                return;
                            }

                            if (IGNORE_CITY && text.contains("Отправить посылку")) {
                                XposedBridge.log("KISS: ignore city parcel");
                                param.setResult(null);
                                return;
                            }

                            XposedBridge.log("KISS NOTIF → " + title + " | " + text);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("KISS ERROR: " + t);
        }
    }

    // ===== ПАРСИНГ ЦЕНЫ =====
    private int parsePrice(String text) {
        try {
            // примеры: "7 000 ₸", "8000т", "10 000 Т"
            String digits = text.replaceAll("[^0-9]", "");
            if (digits.length() < 3) return 0;
            return Integer.parseInt(digits);
        } catch (Throwable t) {
            return 0;
        }
    }
}
