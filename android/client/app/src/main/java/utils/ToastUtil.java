package utils;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;

/**
 * 简单封装一下toast，让这一次toast的时候马上显示
 */
public class ToastUtil {

    private static Toast mToast;

    public static void showToast(Context mContext, String text, int duration) {
        //Android9.0系统已处理，没有该问题，Android10.0又改回9.0以前的实现
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P || mToast == null) {
            mToast = Toast.makeText(mContext, text, duration);
        } else {
            mToast.setText(text);
            mToast.setDuration(duration);
        }
        mToast.show();
    }

}
