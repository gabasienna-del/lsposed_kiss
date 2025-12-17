package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import okhttp3.Call;
import okhttp3.Request;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TARGET = "sinet.startup.inDriver";

    static {
        XposedBridge.log("🔥 GABA HookEntry loaded");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;

        XposedBridge.log("✅ GABA hooked: " + lpparam.packageName);

        /* ================= CALL.execute ================= */
        XposedHelpers.findAndHookMethod(
                "okhttp3.RealCall",
                lpparam.classLoader,
                "execute",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            Call call = (Call) param.thisObject;
                            Request req = call.request();

                            XposedBridge.log("🌐 GABA URL ▶ " + req.url());
                            XposedBridge.log("🔑 GABA HEADERS ▶\n" + req.headers());

                        } catch (Throwable t) {
                            XposedBridge.log("❌ execute hook error: " + t);
                        }
                    }
                }
        );

        /* ================= CALL.enqueue ================= */
        XposedHelpers.findAndHookMethod(
                "okhttp3.RealCall",
                lpparam.classLoader,
                "enqueue",
                okhttp3.Callback.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            Call call = (Call) param.thisObject;
                            Request req = call.request();

                            XposedBridge.log("🌐 GABA URL ▶ " + req.url());
                            XposedBridge.log("🔑 GABA HEADERS ▶\n" + req.headers());

                            if (req.body() != null) {
                                okio.Buffer buffer = new okio.Buffer();
                                req.body().writeTo(buffer);
                                String body = buffer.readUtf8();

                                if (body.contains("{")) {
                                    XposedBridge.log("📦 GABA BODY ▶ " + body);
                                }
                            }

                        } catch (Throwable t) {
                            XposedBridge.log("❌ enqueue hook error: " + t);
                        }
                    }
                }
        );
    }
}
