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
           🔑 Request.Builder headers
           =============================== */
        try {
            Class<?> reqBuilder = XposedHelpers.findClass(
                    "okhttp3.Request$Builder",
                    lpparam.classLoader
            );

            XposedBridge.hookAllMethods(reqBuilder, "addHeader", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    logHeader(p);
                }
            });

            XposedBridge.hookAllMethods(reqBuilder, "header", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    logHeader(p);
                }
            });

            XposedBridge.log("✅ Request.Builder hooks OK");

        } catch (Throwable t) {
            XposedBridge.log("❌ Request.Builder hook failed: " + t);
        }

        /* ===============================
           🔑 Headers.Builder.add
           =============================== */
        try {
            Class<?> headersBuilder = XposedHelpers.findClass(
                    "okhttp3.Headers$Builder",
                    lpparam.classLoader
            );

            XposedBridge.hookAllMethods(headersBuilder, "add", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    logHeader(p);
                }
            });

            XposedBridge.log("✅ Headers.Builder hook OK");

        } catch (Throwable t) {
            XposedBridge.log("❌ Headers.Builder hook failed: " + t);
        }

        /* ===============================
           🌐 RealInterceptorChain (JSON BODY)
           =============================== */
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

                    Object body = XposedHelpers.callMethod(request, "body");
                    if (body == null) return;

                    Class<?> bufferCls = XposedHelpers.findClass(
                            "okio.Buffer",
                            lpparam.classLoader
                    );

                    Object buffer = bufferCls.newInstance();

                    XposedHelpers.callMethod(body, "writeTo", buffer);

                    String bodyStr = (String) XposedHelpers.callMethod(
                            buffer, "readUtf8"
                    );

                    if (bodyStr == null || bodyStr.isEmpty()) return;

                    String low = bodyStr.toLowerCase();

                    if (low.contains("token")
                            || low.contains("authorization")
                            || low.contains("order")
                            || low.contains("trip")
                            || low.contains("city")) {

                        XposedBridge.log(
                                "📦 GABA ▶ JSON BODY ▶ " + url + "\n" + bodyStr
                        );
                    }
                }
            });

            XposedBridge.log("✅ RealInterceptorChain hook OK");

        } catch (Throwable t) {
            XposedBridge.log("❌ InterceptorChain hook failed: " + t);
        }
    }

    /* ===============================
       🔎 Header logger
       =============================== */
    private void logHeader(XC_MethodHook.MethodHookParam p) {
        if (p.args == null || p.args.length < 2) return;

        String key = String.valueOf(p.args[0]);
        String val = String.valueOf(p.args[1]);

        String k = key.toLowerCase();

        if (k.contains("token") || k.contains("authorization")) {
            XposedBridge.log("🔑 GABA ▶ HEADER ▶ " + key + " = " + val);
        }
    }
}
