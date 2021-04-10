package com.game7th.swipe;

import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.game7th.metagame.PersistentStorage;

public class IOSLauncher extends IOSApplication.Delegate {
    public static final String PREF_NAME = "com.game7th.swiped.prefs";

    private class IosPersistentStorage implements PersistentStorage {

        public IOSApplication app;

        @Override
        public void put(String key, String value) {
            app.getPreferences(PREF_NAME).putString(key, value);
        }

        @Override
        public String get(String key) {
            return app.getPreferences(PREF_NAME).getString(key);
        }
    }

    @Override
    protected IOSApplication createApplication() {
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();

        IosPersistentStorage storage = new IosPersistentStorage();
        IOSApplication app = new IOSApplication(new SwipeGameGdx(storage), config);
        storage.app = app;
        return app;
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}