package com.laibandis.gaba;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

        if (!"sinet.startup.inDriver".equals(lpparam.packageName)) return;

        XposedBridge.log("KISS: loaded into " + lpparam.packageName);

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
                        if (n == null || n.extras == null) return;

                        String text = String.valueOf(
                                n.extras.getCharSequence(Notification.EXTRA_TEXT, "")
                        );

                        Context ctx = AndroidAppHelper.currentApplication();
                        SharedPreferences p =
                                ctx.getSharedPreferences("com.laibandis.gaba_preferences", 0);

                        int price = parsePrice(text);

                        boolean isPackage =
                                text.contains("посыл") || text.contains("пакет");

                        boolean isCompanion =
                                text.contains("С попутчиками");

                        // --- ПОСЫЛКА ---
                        if (isPackage) {
                            if (!p.getBoolean("enable_package", false)) {
                                param.setResult(null);
                                return;
                            }
                            int min = getInt(p, "min_price_package", 4000);
                            if (price < min) {
                                param.setResult(null);
                                return;
                            }
                            XposedBridge.log("KISS: package OK " + price);
                            return;
                        }

                        // --- ПОПУТЧИКИ ---
                        if (isCompanion) {
                            if (!p.getBoolean("enable_companion", true)) {
                                param.setResult(null);
                                return;
                            }
                            int min = getInt(p, "min_price_companion", 6000);
                            if (price < min) {
                                param.setResult(null);
                                return;
                            }
                            XposedBridge.log("KISS: companion OK " + price);
                            return;
                        }

                        // --- ПАССАЖИР ---
                        if (!p.getBoolean("enable_passenger", true)) {
                            param.setResult(null);
                            return;
                        }
                        int min = getInt(p, "min_price_passenger", 5000);
                        if (price < min) {
                            param.setResult(null);
                            return;
                        }

                        XposedBridge.log("KISS: passenger OK " + price);
                    }
                }
        );
    }

    private int parsePrice(String s) {
        try {
            String d = s.replaceAll("[^0-9]", "");
            if (d.length() >= 4) return Integer.parseInt(d);
        } catch (Throwable ignored) {}
        return 0;
    }

    private int getInt(SharedPreferences p, String k, int def) {
        try {
            return Integer.parseInt(p.getString(k, String.valueOf(def)));
        } catch (Throwable e) {
            return def;
        }
    }
}
