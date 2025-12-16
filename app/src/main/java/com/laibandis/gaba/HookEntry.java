package com.laibandis.gaba;

import android.app.AndroidAppHelper;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "KISS";
    private static final int MIN_PRICE = 7000;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {

        if (!"sinet.startup.inDriver".equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": loaded into " + lpparam.packageName);

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
                        if (n == null || n.extras == null) return;

                        String title = String.valueOf(n.extras.get(Notification.EXTRA_TITLE));
                        CharSequence big = n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
                        String text = big != null
                                ? big.toString()
                                : String.valueOf(n.extras.get(Notification.EXTRA_TEXT));

                        if (title == null || text == null) return;
                        if (!title.contains("Новый заказ")) return;

                        int price = extractPrice(text);

                        XposedBridge.log(TAG + ": parsed price = " + price + " | text=" + text);

                        if (price < MIN_PRICE) {
                            XposedBridge.log(TAG + ": ignore cheap order = " + price);
                            return;
                        }

                        // проверка межгорода (тире - или –)
                        if (!(text.contains("-") || text.contains("–"))) {
                            XposedBridge.log(TAG + ": ignore city order");
                            return;
                        }

                        XposedBridge.log(TAG + ": ACCEPT intercity " + price);
                        autoDial();
                    }
                }
        );
    }

    private int extractPrice(String text) {
        try {
            // убираем ВСЁ кроме цифр
            String clean = text
                    .replace('\u00A0', ' ')   // неразрывный пробел
                    .replaceAll("[^0-9]", "");

            Pattern p = Pattern.compile("(\\d{3,})");
            Matcher m = p.matcher(clean);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": price parse error " + t);
        }
        return 0;
    }

    private void autoDial() {
        try {
            Context ctx = AndroidAppHelper.currentApplication();
            if (ctx == null) return;

            Intent i = new Intent(Intent.ACTION_DIAL);
            i.setData(Uri.parse("tel:"));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);

            XposedBridge.log(TAG + ": auto dial started");

        } catch (Throwable t) {
            XposedBridge.log(TAG + ": dial error " + t);
        }
    }
}
