package com.laibandis.gaba;

import android.app.Notification;
import android.app.NotificationManager;
import android.net.Uri;
import android.os.Build;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.gaba.kiss".equals(lpparam.packageName)) return;

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
                        if (n == null || n.extras == null) return;

                        CharSequence title = n.extras.getCharSequence(Notification.EXTRA_TITLE);
                        CharSequence text  = n.extras.getCharSequence(Notification.EXTRA_TEXT);
                        CharSequence big   = n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

                        String t = (
                                (title != null ? title : "") + " " +
                                (text  != null ? text  : "") + " " +
                                (big   != null ? big   : "")
                        ).toLowerCase();

                        // 🚗 межгород
                        boolean isIntercity =
                                t.contains("новый заказ") &&
                                t.matches(".*[а-яa-z]+\\s*[-–—]\\s*[а-яa-z]+.*");

                        // 💰 цена
                        int price = 0;
                        try {
                            java.util.regex.Matcher m =
                                    java.util.regex.Pattern
                                            .compile("(\\d{1,3}(?:\\s?\\d{3})*)\\s*(тг|₸)")
                                            .matcher(t);
                            if (m.find()) {
                                price = Integer.parseInt(m.group(1).replace(" ", ""));
                            }
                        } catch (Throwable ignored) {}

                        // 🚫 игнор городских
                        if (!isIntercity) {
                            n.sound = null;
                            n.vibrate = null;
                            n.priority = Notification.PRIORITY_MIN;
                            return;
                        }

                        // 📞 межгород ≥ 7000 тг
                        if (price >= 7000) {
                            n.category = Notification.CATEGORY_CALL;
                            n.priority = Notification.PRIORITY_MAX;

                            Uri ring = Uri.parse(
                                    "android.resource://com.laibandis.gaba/raw/intercity"
                            );
                            n.sound = ring;

                            n.vibrate = new long[]{
                                    0, 1000, 500, 1000, 500, 1000
                            };

                            if (Build.VERSION.SDK_INT >= 21) {
                                n.fullScreenIntent = n.contentIntent;
                            }
                        }
                    }
                }
        );
    }
}
