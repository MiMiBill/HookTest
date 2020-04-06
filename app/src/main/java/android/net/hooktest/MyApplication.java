package android.net.hooktest;

import android.app.Application;

public class MyApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        //博客地址
        //https://www.jianshu.com/p/2d6ccb7a1864
        HookUtils hookUtils = new HookUtils();
        hookUtils.hookActivityHm();
        hookUtils.hookStartActivity(this);


    }
}
