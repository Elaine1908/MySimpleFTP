package utils;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;

/**
 * 简单封装一下toast，让这一次toast的时候马上显示
 */
public class ToastUtil {

    private static Toast lastToast;

    public static void showToast(Context mContext, String text, int duration) {
        synchronized (ToastUtil.class) {
            if (lastToast != null) {
                lastToast.cancel();
            }
            Toast thisToast = Toast.makeText(mContext, text, duration);
            lastToast = thisToast;
            thisToast.show();
        }


    }

}
