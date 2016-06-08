package net.sharksystem.android;

import android.content.Context;

/**
 * Created by j4rvis on 18.05.16.
 */
public class Application extends android.app.Application {
        private static Context context;

        public void onCreate() {
            super.onCreate();
            Application.context = getApplicationContext();
        }

        public static Context getAppContext() {
            return Application.context;
        }
}
