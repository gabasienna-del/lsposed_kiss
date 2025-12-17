package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TARGET_PKG = "sinet.startup.inDriver";

    static {
        XposedBridge.log("🔥 laibandis.gaba HookEntry loaded");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

        if (!TARGET_PKG.equals(lpparam.packageName)) return;

        XposedBridge.log("✅ laibandis.gaba hooked: " + lpparam.packageName);

        /* ===============================
           🔑 HOOK HEADERS (TOKEN)
           =============================== */
        try {
            Class<?> builderCls = XposedHelpers.findClass(
                    "okhttp3.Request$Builder",
                    lpparam.classLoader
            );

            XposedBridge.hookAllMethods(
                    builderCls,
                    "addHeader",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {

                            String key = String.valueOf(param.args[0]);
                            String value = String.valueOf(param.args[1]);

                            String k = key.toLowerCase();

                            if (k.contains("token") || k.contains("authorization")) {
                                XposedBridge.log(
                                        "🔑 GABA TOKEN ▶ " + key + " = " + value
                                );
                            }
                        }
                    }
            );

            XposedBridge.log("✅ Header hook installed");

        } catch (Throwable t) {
            XposedBridge.log("❌ Header hook failed: " + t);
        }

        /* ===============================
           📦 HOOK REQUEST BODY (JSON)
           =============================== */
        try {
            Class<?> requestBodyCls = XposedHelpers.findClass(
                    "okhttp3.RequestBody",
                    lpparam.classLoader
            );

            Class<?> bufferedSinkCls = XposedHelpers.findClass(
                    "okio.BufferedSink",
                    lpparam.classLoader
            );

            XposedHelpers.findAndHookMethod(
                    requestBodyCls,
                    "writeTo",
                    bufferedSinkCls,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {

                            try {
                                Object buffer = XposedHelpers.newInstance(
                                        XposedHelpers.findClass(
                                                "okio.Buffer",
                                                lpparam.classLoader
                                        )
                                );

                                XposedHelpers.callMethod(
                                        param.thisObject,
                                        "writeTo",
                                        buffer
                                );

                                String json = (String) XposedHelpers.callMethod(
                                        buffer,
                                        "readUtf8"
                                );

                                if (json != null &&
                                        (json.contains("order")
                                                || json.contains("intercity")
                                                || json.contains("confirmed")
                                                || json.contains("accept")
                                                || json.contains("bid_accept"))) {

                                    XposedBridge.log("📦 GABA JSON ▶ " + json);
                                }

                            } catch (Throwable t) {
                                XposedBridge.log("❌ JSON hook error: " + t);
                            }
                        }
                    }
            );

            XposedBridge.log("✅ RequestBody hook installed");

        } catch (Throwable t) {
            XposedBridge.log("❌ RequestBody hook failed: " + t);
        }
    }
}
