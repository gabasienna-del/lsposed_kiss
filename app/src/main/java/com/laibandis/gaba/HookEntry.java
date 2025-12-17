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

        try {
            Class<?> requestBodyCls =
                    XposedHelpers.findClass(
                            "okhttp3.RequestBody",
                            lpparam.classLoader
                    );

            Class<?> bufferedSinkCls =
                    XposedHelpers.findClass(
                            "okio.BufferedSink",
                            lpparam.classLoader
                    );

            XposedHelpers.findAndHookMethod(
                    requestBodyCls,
                    "writeTo",
                    bufferedSinkCls,   // ✅ РЕАЛЬНАЯ СИГНАТУРА
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {

                            try {
                                // okio.Buffer buffer = new Buffer();
                                Object buffer = XposedHelpers.newInstance(
                                        XposedHelpers.findClass(
                                                "okio.Buffer",
                                                lpparam.classLoader
                                        )
                                );

                                // requestBody.writeTo(buffer)
                                XposedHelpers.callMethod(
                                        param.thisObject,
                                        "writeTo",
                                        buffer
                                );

                                // buffer.readUtf8()
                                String json = (String) XposedHelpers.callMethod(
                                        buffer,
                                        "readUtf8"
                                );

                                if (json != null &&
                                        (json.contains("intercity")
                                                || json.contains("confirmed")
                                                || json.contains("accept")
                                                || json.contains("bid_accept"))) {

                                    XposedBridge.log("📦 JSON: " + json);
                                }

                            } catch (Throwable t) {
                                XposedBridge.log("❌ JSON hook error: " + t);
                            }
                        }
                    }
            );

        } catch (Throwable t) {
            XposedBridge.log("❌ Hook setup failed: " + t);
        }
    }
}
