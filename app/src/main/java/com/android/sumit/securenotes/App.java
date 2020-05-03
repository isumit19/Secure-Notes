package com.android.sumit.securenotes;

import android.app.Application;
import android.os.Handler;

import com.android.sumit.securenotes.utils.SessionListener;


public class App extends Application {
    private SessionListener sessionListener;
    private Handler foregroundHandler, backgroundHandler;
    private Runnable foregroundRunnable, backgroundRunnable;


    public void registerListener(SessionListener sessionListener){
        this.sessionListener = sessionListener;
    }

    public void startForegorundSession(){
        final long timeout = 10 * 1000;
        cancelForegroundSession();
        foregroundHandler = new Handler();
        foregroundRunnable = new Runnable() {
            @Override
            public void run() {
                sessionListener.foregroundSessionExpired();
            }
        };
        foregroundHandler.postDelayed(foregroundRunnable, timeout);
    }

    public void cancelForegroundSession(){
        if(foregroundHandler != null) {
            foregroundHandler.removeCallbacks(foregroundRunnable);
            foregroundHandler = null;
            foregroundRunnable = null;
        }
    }

    public void startBackgroundSession(){
        final long timeout = 20 * 1000;
        cancelBackgroundSession();
        backgroundHandler = new Handler();
        backgroundRunnable = new Runnable() {
            @Override
            public void run() {
                    sessionListener.backgroundSessionExpired();
            }
        };
        backgroundHandler.postDelayed(backgroundRunnable, timeout);
    }

    public void cancelBackgroundSession(){
        if(backgroundHandler != null){
            backgroundHandler.removeCallbacks(backgroundRunnable);
            backgroundHandler = null;
            backgroundRunnable = null;
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        /*if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);*/
    }

}
