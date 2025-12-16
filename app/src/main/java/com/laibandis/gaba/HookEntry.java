package com.laibandis.gaba;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.messaging.RemoteMessage;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    // ===== ФИЛЬТРЫ =====
    static int MIN_INTERCITY = 5000;
    static int MIN_PARCEL = 3000;
    static int MIN_COMPANION = 5000;

    static boolean AUTO_OPEN = true;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals("sinet.startup.inDriver")) return;

        XposedBridge.log("KISS: loaded into " + lpparam.packageName);

        Class<?> fcmService = XposedHelpers.findClass(
                "sinet.startup.inDriver.services.push.AppFcmListenerService",
                lpparam.classLoader
        );

        XposedHelpers.findAndHookMethod(
                fcmService,
                "onMessageReceived",
                RemoteMessage.class,
                new XC_MethodHook() {

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                        RemoteMessage msg = (RemoteMessage) param.args[0];
                        if (msg == null) return;

                        Bundle data = new Bundle();
                        for (String k : msg.getData().keySet()) {
                            data.putString(k, msg.getData().get(k));
                        }

                        String text = data.toString();

                        int price = parsePrice(text);

                        boolean isIntercity =
                                text.contains("Алматы") && text.contains("Тараз");

                        boolean isParcel = text.contains("посыл");
                        boolean isCompanion = text.contains("попут");

                        boolean allow = false;

                        if (isIntercity && price >= MIN_INTERCITY) allow = true;
                        if (isParcel && price >= MIN_PARCEL) allow = true;
                        if (isCompanion && price >= MIN_COMPANION) allow = true;

                        if (!allow) {
                            XposedBridge.log("KISS: ignore cheap order = " + price);
                            return;
                        }

                        XposedBridge.log("KISS: ACCEPT → " + text);

                        if (AUTO_OPEN) {
                            try {
                                Intent i = new Intent();
                                i.setClassName(
                                        "sinet.startup.inDriver",
                                        "sinet.startup.inDriver.ui.order.OrderActivity"
                                );
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                XposedHelpers.callMethod(
                                        XposedHelpers.callStaticMethod(
                                                XposedHelpers.findClass(
                                                        "android.app.ActivityThread",
                                                        lpparam.classLoader
                                                ),
                                                "currentApplication"
                                        ),
                                        "startActivity",
                                        i
                                );

                                XposedBridge.log("KISS: auto open → auto call");
                            } catch (Throwable t) {
                                XposedBridge.log("KISS: open failed " + t);
                            }
                        }
                    }
                }
        );
    }

    private static int parsePrice(String text) {
        try {
            String digits = text.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return 0;
            return Integer.parseInt(digits);
        } catch (Throwable t) {
            return 0;
        }
    }
}
