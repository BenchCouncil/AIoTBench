package cn.ac.ict.acs.iot.aiot.android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;

/**
 * Created by alanubu on 19-12-25.
 */
public class DialogUtil {

    public static AlertDialog dlg1(
            Context ctx,
            String title, String msg,
            String btnStr, DialogInterface.OnClickListener btnL
    ) {
        return dlg3(ctx, title, msg, btnStr, btnL, null, null, null, null);
    }
    public static AlertDialog dlg2(
            Context ctx,
            String title, String msg,
            String posStr, DialogInterface.OnClickListener posL,
            String negStr, DialogInterface.OnClickListener negL
    ) {
        return dlg3(ctx, title, msg, posStr, posL, null, null, negStr, negL);
    }
    public static AlertDialog dlg3(
            Context ctx,
            String title, String msg,
            String posStr, DialogInterface.OnClickListener posL,
            String midStr, DialogInterface.OnClickListener midL,
            String negStr, DialogInterface.OnClickListener negL
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        if (!TextUtils.isEmpty(title)) builder.setTitle(title);
        if (!TextUtils.isEmpty(msg)) builder.setMessage(msg);
        if (!TextUtils.isEmpty(posStr)) builder.setPositiveButton(posStr, posL);
        if (!TextUtils.isEmpty(midStr)) builder.setNeutralButton(midStr, midL);
        if (!TextUtils.isEmpty(negStr)) builder.setNegativeButton(negStr, negL);
        return builder.create();
    }

    public static AlertDialog dlgList(
            Context ctx,
            String title, String msg,
            String[] items, DialogInterface.OnClickListener itemsL,
            String posStr, DialogInterface.OnClickListener posL,
            String negStr, DialogInterface.OnClickListener negL
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        if (!TextUtils.isEmpty(title)) builder.setTitle(title);
        if (!TextUtils.isEmpty(msg)) builder.setMessage(msg);
        if (items!=null && items.length!=0) builder.setItems(items, itemsL);
        if (!TextUtils.isEmpty(posStr)) builder.setPositiveButton(posStr, posL);
        if (!TextUtils.isEmpty(negStr)) builder.setNegativeButton(negStr, negL);
        return builder.create();
    }
}
