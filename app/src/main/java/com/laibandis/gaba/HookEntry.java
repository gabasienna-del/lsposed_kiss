package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TARGET = "sinet.startup.inDriver";

    static {
        XposedBridge.log("🔥 GABA HookEntry loaded");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;

        XposedBridge.log("✅ GABA hooked: " + lpparam.packageName);

        /* ================= URL ================= */
        XposedHelpers.findAndHookMethod(
                "okhttp3.Request",
                lpparam.classLoader,
                "url",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object url = param.getResult();
                        if (url != null) {
                            String u = url.toString();
                            if (u.startsWith("https://")) {
                                XposedBridge.log("🌐 GABA URL ▶ " + u);
                            }
                        }
                    }
                }
        );

        /* ================= BODY ================= */
        XposedHelpers.findAndHookMethod(
                "okhttp3.RequestBody",
                lpparam.classLoader,
                "writeTo",
                okio.BufferedSink.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            okio.Buffer buffer = new okio.Buffer();
                            Object bodyObj = param.thisObject;

                            XposedHelpers.callMethod(bodyObj, "writeTo", buffer);
                            String body = buffer.readUtf8();

                            if (body != null && body.contains("{")) {
                                XposedBridge.log("📦 GABA BODY ▶ " + body);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("❌ GABA BODY error: " + t);
                        }
                    }
                }
        );

        /* ================= HEADERS / TOKEN ================= */
        XposedHelpers.findAndHookMethod(
                "okhttp3.Headers",
                lpparam.classLoader,
                "toString",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String headers = String.valueOf(param.getResult());
                        String h = headers.toLowerCase();
                        if (h.contains("authorization") || h.contains("token")) {
                            XposedBridge.log("🔑 GABA HEADERS ▶\n" + headers);
                        }
                    }
                }
        );
    }
}
