package com.fxzs.lingxiagent.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleManager {
    
    private static Context appContext;
    
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }
    
    public static void setLocale(String languageCode) {
        if (appContext == null) {
            return;
        }
        
        Locale locale = getLocaleFromCode(languageCode);
        Locale.setDefault(locale);
        
        Resources resources = appContext.getResources();
        Configuration config = resources.getConfiguration();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
    
    private static Locale getLocaleFromCode(String languageCode) {
        switch (languageCode) {
            case "zh-CN":
                return Locale.SIMPLIFIED_CHINESE;
            case "zh-TW":
                return Locale.TRADITIONAL_CHINESE;
            case "en":
                return Locale.ENGLISH;
            default:
                return Locale.SIMPLIFIED_CHINESE;
        }
    }
    
    public static void applyLanguage(Context context) {
        String languageCode = SharedPreferencesUtil.getLanguage();
        setLocale(languageCode);
    }
}