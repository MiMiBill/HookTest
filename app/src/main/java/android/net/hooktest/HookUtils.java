package android.net.hooktest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.hooktest.ProxyActivity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


//博客地址
//https://www.jianshu.com/p/2d6ccb7a1864
public class HookUtils {

    private Context context;

    public void hookStartActivity(Context context) {
        this.context = context;
        try {
            /**
             * 这里注意一下，用我们分析的源码运行不了，所以稍微改了一下，
             * 思路什么都一样，只是源码的属性名做了修改
             */
//            Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
            Class<?> activityManagerClass = Class.forName("android.app.ActivityManagerNative");
            //拿到 IActivityManagerSingleton 属性
//            Field field = activityManagerClass.getDeclaredField("IActivityManagerSingleton");
            Field field = activityManagerClass.getDeclaredField("gDefault");
            field.setAccessible(true);
            //获取到是 Singleton 对象，也就是 field 对应的类型
            Object singletonObj = field.get(null);

            //然后获取 Singletone 的 mInstance 属性
            Class<?> singtonClass = Class.forName("android.util.Singleton");
            Field mInstanceField = singtonClass.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);
            //真正的 hook 点
            Object iActivityManagerObj = mInstanceField.get(singletonObj);

            //hook 第二步，动态代理
            Class<?> iActivityManagerIntercept = Class.forName("android.app.IActivityManager");
            StartActivityHandler startActivityHandler = new StartActivityHandler(iActivityManagerObj);
            Object proxyIActivityManager = Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{iActivityManagerIntercept}, startActivityHandler);
            //在这我们将系统的对象更换成我们生成的动态代理对象，为了是调用动态代理的 invoke 方法，不更换不执行
            mInstanceField.set(singletonObj, proxyIActivityManager);
            Log.d("Hook","hookStartActivity");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Hook","hookStartActivity failed");
        }
    }

    class StartActivityHandler implements InvocationHandler {

        //系统真正的对象
        private Object trueIActivityManager;

        public StartActivityHandler(Object trueIActivityManager) {
            this.trueIActivityManager = trueIActivityManager;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.i("myhook","abc : ---startActivity:" + method.getName());
            if ("startActivity".equals(method.getName())) {
                Log.i("myhook","abc : --------------------- startActivity ---------------------");
                Intent intent = null;
                int index = -1;
                for (int i = 0; i < args.length; i++) {
                    Object obj = args[i];
                    if (obj instanceof Intent) {
                        //找到 startActivity 传递进来的 Intent
                        intent = (Intent) obj;
                        index = i;
                    }
                }
                //瞒天过海，获取想要跳转的意图，进行篡改
                Intent newIntent = new Intent(context, ProxyActivity.class);
                //我们将真实的意图封装在假意图当中
                newIntent.putExtra("oldIntent", intent);
                args[index] = newIntent;
            }
            return method.invoke(trueIActivityManager, args);
        }
    }


    public void hookActivityHm(){
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            //拿到系统的 ActivityThread 对象
            Object activityTreadObj = sCurrentActivityThreadField.get(null);

            //那 Handler
            Field mhField = activityThreadClass.getDeclaredField("mH");
            mhField.setAccessible(true);
            //拿到系统的 Handler，真正的 hook 点已经拿到
            Handler mhHandler = (Handler) mhField.get(activityTreadObj);

            //hook handleMessage 方法
            //先拿到 callback
            Field mCallbackField = Handler.class.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);
            /*
                接下来就是将 handleMessage 方法拉到系统外面来执行了，
                但是注意，由于 callback 就是一个接口，那么我们就用系统的
                这个对象但是重写他里面的方法，就可以完成将系统代码拉到外面来执行的目的了，
                这就是特定的情景才能使用，不一定非要使用动态代理。
             */
            //这里将 Callback 替换成我们自己定义的 Callback
            mCallbackField.set(mhHandler,new MyCallback(mhHandler));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class MyCallback implements Handler.Callback {

        private Handler systemHandler;

        public MyCallback(Handler systemHandler){
            this.systemHandler = systemHandler;
        }

        @Override
        public boolean handleMessage(Message msg) {
            if(msg.what == 100){
                //如果是 100 启动 Activity 的消息，我们需要特殊处理
                handleLaunchActivity(msg);
            }
            //这句话不能忘，其他的全部交给系统处理
            systemHandler.handleMessage(msg);
            return true;
        }

        /**
         * 如果执行到该方法，说明启动 Activity 已经经过 AMS 的检查了，
         * 我们在这里只需要将 Activity 进行还原即可。
         */
        private void handleLaunchActivity(Message msg) {
            /*
                这里有个小知识点，就是如果是启动 Activity 的话，肯定会有如下结构：
                msg 里面有一个 obj,而该 obj 中也会有一个 intent 属性
             */
            Object obj = msg.obj;
            try {
                Field intentField = obj.getClass().getDeclaredField("intent");
                intentField.setAccessible(true);

                //这个获得的是被我们篡改后的 Intent
                Intent tamperIntent = (Intent) intentField.get(obj);
                //从该 Intent 中获取真正想要跳转到的目标 Activity
                Intent oldIntent = tamperIntent.getParcelableExtra("oldIntent");
                if(oldIntent != null){
                    //实现集中式登录
                    SharedPreferences share = context.getSharedPreferences("radish",Context.MODE_PRIVATE);
                    boolean login = share.getBoolean("login", false);
                    if(login){
                        //登录了
                        tamperIntent.setComponent(oldIntent.getComponent());
                    }else{
                        //没登录
                        ComponentName componentName = new ComponentName(context,LoginActivity.class);
                        tamperIntent.putExtra("extraIntent",oldIntent.getComponent().getClassName());
                        tamperIntent.setComponent(componentName);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



}