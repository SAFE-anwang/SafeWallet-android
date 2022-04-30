package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class AnWangUtils {

    public static String lastOpenGroupId = "";
    public static boolean isLeaveAnwangGroup = false;
    public static long anwangChatId = -1;
    public static boolean isCheckInAnwangGroup = false;

    /**
     * 检查是否加入安网群
     * @param array
     */
    public static void checkIsInAnWangGroup(Activity activity, ArrayList<TLRPC.Dialog> array) {
        if (isCheckInAnwangGroup) return;
        if (array == null || array.isEmpty()) return;
        if (anwangChatId == -1) return;

        boolean isInAnWang = false;
        for (int i = 0; i < array.size(); i++){
            TLRPC.Dialog dialog = array.get(i);
            if (dialog.id == -anwangChatId) {
                isInAnWang = true;
                break;
            }
        }
        if (!isInAnWang) {
            Intent intent = new Intent(activity, LaunchActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://t.me/safeanwang"));
            activity.startActivity(intent);
        }
        isCheckInAnwangGroup = true;
    }

    public static void loadLastOpenChatId() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("lastOpenGroupId", Activity.MODE_PRIVATE);
        lastOpenGroupId = preferences.getString("lastOpenGroupId", "");
        isLeaveAnwangGroup = preferences.getBoolean("isLeaveAnwangGroup", false);
        anwangChatId = preferences.getLong("anwangChatId", -1);
    }

    public static void saveLastOpenChatId(String groupLink) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("lastOpenGroupId", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("lastOpenGroupId", groupLink);
        editor.apply();
        lastOpenGroupId = groupLink;
    }

    public static void joinGroup(String username, long chatId) {
        if ("safeanwang".equals(username)) {
            saveIsLeaveAnwangGroup(false, chatId);
        }
    }
    public static void leaveGroup(String username, long chatId) {
        if ("safeanwang".equals(username)) {
            saveIsLeaveAnwangGroup(true, chatId);
        }
    }

    private static void saveIsLeaveAnwangGroup(boolean isLeave, long chatId) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("lastOpenGroupId", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLeaveAnwangGroup", isLeave);
        editor.putLong("anwangChatId", chatId);
        editor.apply();
        isLeaveAnwangGroup = isLeave;
        anwangChatId = chatId;
    }
}
