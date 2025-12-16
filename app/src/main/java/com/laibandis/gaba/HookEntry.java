package com.laibandis.gaba;

import android.app.AndroidAppHelper;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "KISS";
    private static final int MIN_PRICE = 7000; // минимальная цена

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {

        // пакет водителя
        if (!lpparam.packageName.equals("sinet.startup.inDriver")) return;

        XposedBridge.log(TAG + ": loaded into " + lpparam.packageName);

        try {
            XposedHelpers.findAndHookMethod(
                    NotificationManager.class,
                    "notify",
                    String.class,
                    int.class,
                    Notification.class,
                    new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {

                            Notification n = (Notification) param.args[2];
                            if (n == null) return;

                            Bundle extras = n.extras;
                            if (extras == null) return;

                            String title = String.valueOf(extras.get(Notification.EXTRA_TITLE));
                            String text  = String.valueOf(extras.get(Notification.EXTRA_TEXT));

                            if (title == null || text == null) return;

                            // только новые заказы
                            if (!title.contains("Новый заказ")) return;

                            int price = extractPrice(text);
                            if (price < MIN_PRICE) {
                                XposedBridge.log(TAG + ": ignore cheap order = " + price);
                                return;
                            }

                            // должен быть маршрут А–Б
                            if (!text.contains("–") && !text.contains("-")) {
                                XposedBridge.log(TAG + ": ignore city order");
                                return;
                            }

                            XposedBridge.log(TAG + " NOTIF → title=" + title + " text=" + text);

                            autoDial();
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook error " + Log.getStackTraceString(t));
        }
    }

    private int extractPrice(String text) {
        try {
            Pattern p = Pattern.compile("(\\d{4,})");
            Matcher m = p.matcher(text.replace(" ", ""));
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private void autoDial() {
        try {
            Context ctx = AndroidAppHelper.currentApplication();
            if (ctx == null) return;

            Intent i = new Intent(Intent.ACTION_DIAL);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setData(Uri.parse("tel:"));
            ctx.startActivity(i);

            XposedBridge.log(TAG + ": auto dial started");

        } catch (Throwable t) {
            XposedBridge.log(TAG + ": dial error " + t);
        }
    }
}
