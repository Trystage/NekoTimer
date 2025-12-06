package cn.trystage.nekotimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class AlarmService extends Service {

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private NotificationManager notificationManager;
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "alarm_channel";
    private static final String ACTION_STOP_ALARM = "STOP_ALARM";

    // 添加一个静态方法检查是否在运行
    private static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        isRunning = true;
        Log.d("AlarmService", "Service 创建");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("AlarmService", "Service 启动命令");

        // 检查是否是停止命令
        if (intent != null && ACTION_STOP_ALARM.equals(intent.getAction())) {
            Log.d("AlarmService", "收到停止命令");
            stopAlarm();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        // 正常启动报警
        startAlarm();
        showNotification();
        return START_STICKY;
    }

    private void startAlarm() {
        Log.d("AlarmService", "开始报警");
        try {
            // 1. 播放声音
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound);
            if (mediaPlayer == null) {
                Log.e("AlarmService", "无法创建 MediaPlayer，检查音频文件是否存在");
                // 如果音频文件不存在，使用系统默认声音
                mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI);
            }

            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(1.0f, 1.0f);
                mediaPlayer.start();
                Log.d("AlarmService", "声音开始播放");
            } else {
                Log.e("AlarmService", "MediaPlayer 创建失败");
            }

            // 2. 震动
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                // 检查权限（在Manifest中已经添加）
                long[] pattern = {0, 1000, 500, 1000}; // 立即震动1秒，暂停0.5秒，震动1秒...

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8.0+ 使用新API
                    VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0);
                    vibrator.vibrate(effect);
                } else {
                    // 旧版本
                    vibrator.vibrate(pattern, 0);
                }
                Log.d("AlarmService", "震动开始");
            } else {
                Log.e("AlarmService", "设备不支持震动或没有震动权限");
            }

        } catch (Exception e) {
            Log.e("AlarmService", "启动报警失败", e);
            e.printStackTrace();
        }
    }

    private void showNotification() {
        Log.d("AlarmService", "显示通知");
        // 创建通知渠道（Android 8.0+）
        createNotificationChannel();

        // 创建点击返回应用的Intent
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 创建停止报警的Intent
        Intent stopIntent = new Intent(this, AlarmService.class);
        stopIntent.setAction(ACTION_STOP_ALARM);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 构建通知
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)  // 使用系统图标确保存在
                .setContentTitle("⏰ 计时结束")
                .setContentText("点击停止报警")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(false)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_ff, "停止", stopPendingIntent)
                .setContentIntent(pendingIntent)
                .build();

        // 显示通知（前台服务）
        startForeground(NOTIFICATION_ID, notification);
        Log.d("AlarmService", "前台服务启动");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "计时报警",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("计时时间到通知");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
            Log.d("AlarmService", "通知渠道创建");
        }
    }

    @Override
    public void onDestroy() {
        Log.d("AlarmService", "Service 销毁");
        stopAlarm();
        isRunning = false;
        super.onDestroy();
    }

    private void stopAlarm() {
        Log.d("AlarmService", "停止报警");
        // 停止播放
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mediaPlayer = null;
            }
        }

        // 停止震动
        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 发送广播通知MainActivity报警已停止
        Intent intent = new Intent("ALARM_STOPPED");
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isAlarmRunning() {
        return isRunning;
    }
}