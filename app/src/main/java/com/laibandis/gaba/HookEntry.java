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

        /* =================================================
           🔑 Request.Builder.header(key, value)
           ================================================= */
        try {
            XposedHelpers.findAndHookMethod(
                    "okhttp3.Request$Builder",
                    lpparam.classLoader,
                    "header",
                    String.class,
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {

                            String key = String.valueOf(param.args[0]);
                            String value = String.valueOf(param.args[1]);

                            if ("Authorization".equalsIgnoreCase(key)
                                    || key.toLowerCase().contains("token")) {

                                XposedBridge.log(
                                        "🔑 GABA ▶ header() ▶ " + key + " = " + value
                                );
                            }
                        }
                    }
            );

            XposedBridge.log("✅ header() hook installed");

        } catch (Throwable t) {
            XposedBridge.log("❌ header() hook failed: " + t);
        }

        /* =================================================
           🔑 Headers.Builder.add(key, value)
           ================================================= */
        try {
            XposedHelpers.findAndHookMethod(
                    "okhttp3.Headers$Builder",
                    lpparam.classLoader,
                    "add",
                    String.class,
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {

                            String key = String.valueOf(param.args[0]);
                            String value = String.valueOf(param.args[1]);

                            if ("Authorization".equalsIgnoreCase(key)
                                    || "X-Access-Token".equalsIgnoreCase(key)
                                    || key.toLowerCase().contains("token")) {

                                XposedBridge.log(
                                        "🔑 GABA ▶ Headers.add() ▶ " + key + " = " + value
                                );
                            }
                        }
                    }
            );

            XposedBridge.log("✅ Headers.add() hook installed");

        } catch (Throwable t) {
            XposedBridge.log("❌ Headers.add() hook failed: " + t);
        }

        /* =================================================
           🔑 Interceptor.intercept(chain)
           ================================================= */
        try {
            Class<?> interceptorCls = XposedHelpers.findClass(
                    "okhttp3.Interceptor",
                    lpparam.classLoader
            );

            Class<?> chainCls = XposedHelpers.findClass(
                    "okhttp3.Interceptor$Chain",
                    lpparam.classLoader
            );

            XposedBridge.hookAllMethods(
                    interceptorCls,
                    "intercept",
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param)
                                throws Throwable {

                            Object chain = param.args[0];
                            Object request = XposedHelpers.callMethod(chain, "request");

                            String url = String.valueOf(
                                    XposedHelpers.callMethod(request, "url")
                            );

                            String auth = (String) XposedHelpers.callMethod(
                                    request, "header", "Authorization"
                            );

                            String xToken = (String) XposedHelpers.callMethod(
                                    request, "header", "X-Access-Token"
                            );

                            if (auth != null || xToken != null) {
                                XposedBridge.log(
                                        "🔑 GABA ▶ Interceptor ▶ REQ ▶ " + url
                                                + " | Authorization=" + auth
                                                + " | X-Access-Token=" + xToken
                                );
                            }
                        }
                    }
            );

            XposedBridge.log("✅ Interceptor hook installed");

        } catch (Throwable t) {
            XposedBridge.log("❌ Interceptor hook failed: " + t);
        }
    }
}
