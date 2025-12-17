package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TARGET = "sinet.startup.inDriver";

    static {
        XposedBridge.log("🔥 laibandis.gaba HookEntry loaded");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;

        XposedBridge.log("✅ laibandis.gaba hooked: " + lpparam.packageName);

        /* ===============================
           🌐 OkHttp RealCall (UNIVERSAL)
           =============================== */
        try {
            Class<?> realCallCls = XposedHelpers.findClass(
                    "okhttp3.RealCall",
                    lpparam.classLoader
            );

            // execute()
            XposedBridge.hookAllMethods(realCallCls, "execute", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                    logRequest(p.thisObject);
                }
            });

            // enqueue()
            XposedBridge.hookAllMethods(realCallCls, "enqueue", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                    logRequest(p.thisObject);
                }
            });

            XposedBridge.log("✅ RealCall hook OK");

        } catch (Throwable t) {
            XposedBridge.log("❌ RealCall hook failed: " + t);
        }
    }

    /* ===============================
       🔎 Request logger
       =============================== */
    private void logRequest(Object realCall) {
        try {
            Object request = XposedHelpers.callMethod(realCall, "request");

            String url = String.valueOf(
                    XposedHelpers.callMethod(request, "url")
            );

            Object headers = XposedHelpers.callMethod(request, "headers");

            XposedBridge.log(
                    "🌐 GABA ▶ CALL ▶ " + url +
                    "\nHEADERS: " + headers
            );

        } catch (Throwable t) {
            XposedBridge.log("❌ logRequest error: " + t);
        }
    }
}
