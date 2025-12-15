package com.laibandis.gaba;

import android.app.Notification;
import android.app.NotificationManager;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final int MIN_PRICE = 5000; // 💰 фильтр от 5000 тг

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        // 🎯 ПАКЕТ ТАКСИ (замени если нужно)
        if (!lpparam.packageName.contains("sinet.startup.inDriver")) return;

        XposedBridge.log("KISS: loaded -> " + lpparam.packageName);

        Class<?> nms = XposedHelpers.findClass(
                "android.app.NotificationManager",
                lpparam.classLoader
        );

        XposedHelpers.findAndHookMethod(
                nms,
                "notify",
                String.class,
                int.class,
                Notification.class,
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Notification n = (Notification) param.args[2];
                        if (n == null || n.extras == null) return;

                        CharSequence textCs = n.extras.getCharSequence(Notification.EXTRA_TEXT);
                        if (textCs == null) return;

                        String text = textCs.toString();

                        // 📌 интересует только новый заказ
                        if (!text.contains("НОВЫЙ ЗАКАЗ")) return;

                        // 🚫 убираем городские
                        if (!text.contains("-")) {
                            XposedBridge.log("KISS: skip city -> " + text);
                            param.setResult(null);
                            return;
                        }

                        // 💰 парсим цену
                        int price = extractPrice(text);

                        if (price < MIN_PRICE) {
                            XposedBridge.log("KISS: skip cheap " + price + " -> " + text);
                            param.setResult(null);
                            return;
                        }

                        // ✅ МЕЖГОРОД ПРОШЁЛ
                        XposedBridge.log("KISS: ACCEPT " + price + " -> " + text);
                    }
                }
        );
    }

    private int extractPrice(String text) {
        try {
            int idx = text.indexOf("тг");
            if (idx == -1) return 0;

            String num = text.substring(0, idx).replaceAll("[^0-9]", "");
            return Integer.parseInt(num);
        } catch (Throwable t) {
            return 0;
        }
    }
}
