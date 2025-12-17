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

        try {
            Class<?> chainCls = XposedHelpers.findClass(
                    "okhttp3.internal.http.RealInterceptorChain",
                    lpparam.classLoader
            );

            XposedBridge.hookAllMethods(chainCls, "proceed", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam p) throws Throwable {

                    Object request = p.args[0];

                    String url = String.valueOf(
                            XposedHelpers.callMethod(request, "url")
                    );

                    // ЛОГИРУЕМ ВСЕ URL
                    XposedBridge.log("🌐 GABA ▶ URL ▶ " + url);
                }
            });

            XposedBridge.log("✅ URL hook OK");

        } catch (Throwable t) {
            XposedBridge.log("❌ URL hook failed: " + t);
        }
    }
}
