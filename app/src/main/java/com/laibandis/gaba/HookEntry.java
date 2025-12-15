package com.laibandis.gaba;

import android.app.Notification;
import android.app.NotificationManager;
import android.os.Bundle;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {

        // ВАЖНО: укажи РЕАЛЬНЫЙ пакет приложения такси
        if (!lpparam.packageName.contains("sinet.startup.inDriver")) return;

        XposedBridge.log("KISS: loaded into " + lpparam.packageName);

        XposedHelpers.findAndHookMethod(
                NotificationManager.class,
                "notify",
                String.class,
                int.class,
                Notification.class,
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Notification n = (Notification) param.args[2];
                        if (n == null) return;

                        Bundle e = n.extras;
                        if (e == null) return;

                        CharSequence title = e.getCharSequence(Notification.EXTRA_TITLE);
                        CharSequence text = e.getCharSequence(Notification.EXTRA_TEXT);

                        XposedBridge.log("KISS NOTIF → title=" + title + " text=" + text);
                    }
                }
        );
    }
}
