package com.laibandis.gaba;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!"sinet.startup.inDriver".equals(lpparam.packageName)) return;

        XposedBridge.log("KISS loaded: " + lpparam.packageName);

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

                        CharSequence cs = n.extras.getCharSequence(Notification.EXTRA_TEXT);
                        if (cs == null) return;

                        String text = cs.toString();

                        Context ctx;
                        try {
                            ctx = (Context)
                                    XposedHelpers.callMethod(
                                            XposedHelpers.getObjectField(param.thisObject, "mContext"),
                                            "getApplicationContext"
                                    );
                        } catch (Throwable t) {
                            return;
                        }

                        SharedPreferences p =
                                ctx.getSharedPreferences("kiss_prefs", Context.MODE_PRIVATE);

                        int minPrice = p.getInt("min_price", 5000);
                        boolean onlyIntercity = p.getBoolean("only_intercity", true);
                        boolean ignoreCity = p.getBoolean("ignore_city", true);

                        // 🚕 межгород
                        boolean isIntercity =
                                text.matches(".*[А-Яа-яA-Za-z]+\\s*-\\s*[А-Яа-zA-Z]+.*");

                        if (onlyIntercity && !isIntercity && ignoreCity) {
                            param.setResult(null);
                            return;
                        }

                        // 💰 цена
                        int price = extractPrice(text);
                        if (price < minPrice) {
                            param.setResult(null);
                            return;
                        }

                        // 📞 звонок
                        try {
                            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                            Ringtone r = RingtoneManager.getRingtone(ctx, uri);
                            if (r != null) r.play();
                        } catch (Throwable ignored) {}

                        // 🔕 скрываем пуш
                        param.setResult(null);
                    }
                }
        );
    }

    private int extractPrice(String text) {
        try {
            Matcher m = Pattern.compile("(\\d{3,6})\\s*(тг|tg)").matcher(text);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Throwable ignored) {}
        return 0;
    }
}
