package com.laibandis.gaba;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    // ====== НАСТРОЙКИ ======
    private static final int MIN_PRICE = 5000;
    private static final boolean ONLY_INTERCITY = true;
    private static final boolean IGNORE_CITY = true;

    // шаблон цены: 7 000 ₸ / 7000т / 8000 тг
    private static final Pattern PRICE_PATTERN =
            Pattern.compile("(\\d{1,3}(?:[ \\u00A0]?\\d{3})*)\\s*[₸тТгГ]");

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!"sinet.startup.inDriver".equals(lpparam.packageName)) return;

        XposedBridge.log("KISS: loaded into " + lpparam.packageName);

        try {
            XposedHelpers.findAndHookMethod(
                    "sinet.startup.inDriver.services.push.AppFcmListenerService",
                    lpparam.classLoader,
                    "onMessageReceived",
                    Object.class,
                    new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {

                            Object msg = param.args[0];
                            if (msg == null) return;

                            String raw = msg.toString();
                            XposedBridge.log("KISS FCM RAW → " + raw);

                            int price = parsePrice(raw);
                            boolean intercity = isIntercity(raw);

                            XposedBridge.log("KISS parsed price = " + price +
                                    " | intercity=" + intercity);

                            if (price < MIN_PRICE) {
                                XposedBridge.log("KISS: ignore cheap order = " + price);
                                return;
                            }

                            if (ONLY_INTERCITY && !intercity) {
                                XposedBridge.log("KISS: ignore city order");
                                return;
                            }

                            if (IGNORE_CITY && raw.contains("Отправить посылку")) {
                                XposedBridge.log("KISS: ignore parcel");
                                return;
                            }

                            XposedBridge.log("KISS ACCEPT → " + price);
                            // дальше: автопринятие / автозвонок

                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("KISS ERROR: " + t);
        }
    }

    // ====== PARSE PRICE ======
    private int parsePrice(String text) {
        if (TextUtils.isEmpty(text)) return 0;

        Matcher m = PRICE_PATTERN.matcher(text);
        if (!m.find()) return 0;

        try {
            String num = m.group(1).replace(" ", "").replace("\u00A0", "");
            return Integer.parseInt(num);
        } catch (Throwable e) {
            return 0;
        }
    }

    // ====== INTERCITY CHECK ======
    private boolean isIntercity(String text) {
        return text.contains("Алматы") && text.contains("Тараз");
    }
}
