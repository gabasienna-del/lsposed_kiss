package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        // Целевое приложение (поменяем если нужно)
        if (!lpparam.packageName.equals("com.gaba.kiss")) {
            return;
        }

        XposedBridge.log("KISS Intercity Call: loaded for " + lpparam.packageName);
    }
}
