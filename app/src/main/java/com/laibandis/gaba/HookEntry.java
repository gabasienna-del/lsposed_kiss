package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import okio.Buffer;
import okio.BufferedSink;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TARGET_PKG = "sinet.startup.inDriver";

    static {
        XposedBridge.log("🔥 laibandis.gaba HookEntry loaded");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals(TARGET_PKG)) return;

        XposedBridge.log("✅ laibandis.gaba hooked: " + lpparam.packageName);

        XposedHelpers.findAndHookMethod(
                "okhttp3.RequestBody",
                lpparam.classLoader,
                "writeTo",
                BufferedSink.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Buffer buffer = new Buffer();
                        ((okhttp3.RequestBody) param.thisObject).writeTo(buffer);
                        String json = buffer.readUtf8();
                        if (json != null && (json.contains("intercity")
                                || json.contains("confirmed")
                                || json.contains("accept")
                                || json.contains("bid_accept"))) {
                            XposedBridge.log("📦 JSON: " + json);
                        }
                    }
                }
        );
    }
}
