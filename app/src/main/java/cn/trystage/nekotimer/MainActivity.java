package cn.trystage.nekotimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private TextView timer1Display, timer2Display, timer3Display;

    private Button timer1Add1h, timer1Add10h, timer1Add1m, timer1Add10m, timer1Add1s, timer1Add10s,
            timer1Decr1h, timer1Decr10h, timer1Decr1m, timer1Decr10m, timer1Decr1s, timer1Decr10s,
            timer1Start, timer1Reset;
    private Button timer2Add1h, timer2Add10h, timer2Add1m, timer2Add10m, timer2Add1s, timer2Add10s,
            timer2Decr1h, timer2Decr10h, timer2Decr1m, timer2Decr10m, timer2Decr1s, timer2Decr10s,
            timer2Start, timer2Reset;
    private Button timer3Add1h, timer3Add10h, timer3Add1m, timer3Add10m, timer3Add1s, timer3Add10s,
            timer3Decr1h, timer3Decr10h, timer3Decr1m, timer3Decr10m, timer3Decr1s, timer3Decr10s,
            timer3Start, timer3Reset;

    // 三个计时器的状态
    private int[] timerSeconds = {60, 60, 60}; // 默认60秒
    private boolean[] timerRunning = {false, false, false};
    private Handler handler = new Handler();
    private Runnable[] timerRunnables = new Runnable[3];

    // 广播接收器
    private AlarmReceiver alarmReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViews();
        updateAllDisplays();
        setupClickListeners();
        setupTimerRunnables();

        // 注册广播接收器
        alarmReceiver = new AlarmReceiver();
        IntentFilter filter = new IntentFilter("ALARM_STOPPED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(alarmReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            }
        } else {
            registerReceiver(alarmReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消注册广播接收器
        if (alarmReceiver != null) {
            unregisterReceiver(alarmReceiver);
        }

        // 停止所有计时器
        for (int i = 0; i < 3; i++) {
            if (timerRunning[i]) {
                timerRunning[i] = false;
                if (timerRunnables[i] != null) {
                    handler.removeCallbacks(timerRunnables[i]);
                }
            }
        }
    }

    private void findViews() {
        // 计时器1
        timer1Display = findViewById(R.id.timer_1_display);
        timer1Add1h = findViewById(R.id.timer_1_a1h);
        timer1Add10h = findViewById(R.id.timer_1_a10h);
        timer1Add1m = findViewById(R.id.timer_1_a1m);
        timer1Add10m = findViewById(R.id.timer_1_a10m);
        timer1Add1s = findViewById(R.id.timer_1_a1s);
        timer1Add10s = findViewById(R.id.timer_1_a10s);

        timer1Decr1h = findViewById(R.id.timer_1_d1h);
        timer1Decr10h = findViewById(R.id.timer_1_d10h);
        timer1Decr1m = findViewById(R.id.timer_1_d1m);
        timer1Decr10m = findViewById(R.id.timer_1_d10m);
        timer1Decr1s = findViewById(R.id.timer_1_d1s);
        timer1Decr10s = findViewById(R.id.timer_1_d10s);

        timer1Start = findViewById(R.id.timer_1_start);
        timer1Reset = findViewById(R.id.timer_1_reset);

        // 计时器2
        timer2Display = findViewById(R.id.timer_2_display);
        timer2Add1h = findViewById(R.id.timer_2_a1h);
        timer2Add10h = findViewById(R.id.timer_2_a10h);
        timer2Add1m = findViewById(R.id.timer_2_a1m);
        timer2Add10m = findViewById(R.id.timer_2_a10m);
        timer2Add1s = findViewById(R.id.timer_2_a1s);
        timer2Add10s = findViewById(R.id.timer_2_a10s);

        timer2Decr1h = findViewById(R.id.timer_2_d1h);
        timer2Decr10h = findViewById(R.id.timer_2_d10h);
        timer2Decr1m = findViewById(R.id.timer_2_d1m);
        timer2Decr10m = findViewById(R.id.timer_2_d10m);
        timer2Decr1s = findViewById(R.id.timer_2_d1s);
        timer2Decr10s = findViewById(R.id.timer_2_d10s);

        timer2Start = findViewById(R.id.timer_2_start);
        timer2Reset = findViewById(R.id.timer_2_reset);

        // 计时器3
        timer3Display = findViewById(R.id.timer_3_display);
        timer3Add1h = findViewById(R.id.timer_3_a1h);
        timer3Add10h = findViewById(R.id.timer_3_a10h);
        timer3Add1m = findViewById(R.id.timer_3_a1m);
        timer3Add10m = findViewById(R.id.timer_3_a10m);
        timer3Add1s = findViewById(R.id.timer_3_a1s);
        timer3Add10s = findViewById(R.id.timer_3_a10s);

        timer3Decr1h = findViewById(R.id.timer_3_d1h);
        timer3Decr10h = findViewById(R.id.timer_3_d10h);
        timer3Decr1m = findViewById(R.id.timer_3_d1m);
        timer3Decr10m = findViewById(R.id.timer_3_d10m);
        timer3Decr1s = findViewById(R.id.timer_3_d1s);
        timer3Decr10s = findViewById(R.id.timer_3_d10s);

        timer3Start = findViewById(R.id.timer_3_start);
        timer3Reset = findViewById(R.id.timer_3_reset);
    }

    private void setupTimerRunnables() {
        for (int i = 0; i < 3; i++) {
            final int timerIndex = i;
            timerRunnables[i] = new Runnable() {
                @Override
                public void run() {
                    if (timerRunning[timerIndex]) {
                        timerSeconds[timerIndex]--;
                        updateDisplay(timerIndex + 1);

                        if (timerSeconds[timerIndex] <= 0) {
                            timerSeconds[timerIndex] = 0;
                            timerRunning[timerIndex] = false;
                            playFinishAlert(timerIndex + 1);
                        }

                        if (timerRunning[timerIndex]) {
                            handler.postDelayed(this, 1000);
                        }
                    }
                }
            };
        }
    }

    private void setupClickListeners() {
        // 计时器1的调整按钮
        timer1Add1h.setOnClickListener(v -> adjustTime(1, 3600));
        timer1Add10h.setOnClickListener(v -> adjustTime(1, 36000));
        timer1Add1m.setOnClickListener(v -> adjustTime(1, 60));
        timer1Add10m.setOnClickListener(v -> adjustTime(1, 600));
        timer1Add1s.setOnClickListener(v -> adjustTime(1, 1));
        timer1Add10s.setOnClickListener(v -> adjustTime(1, 10));

        timer1Decr1h.setOnClickListener(v -> adjustTime(1, -3600));
        timer1Decr10h.setOnClickListener(v -> adjustTime(1, -36000));
        timer1Decr1m.setOnClickListener(v -> adjustTime(1, -60));
        timer1Decr10m.setOnClickListener(v -> adjustTime(1, -600));
        timer1Decr1s.setOnClickListener(v -> adjustTime(1, -1));
        timer1Decr10s.setOnClickListener(v -> adjustTime(1, -10));

        // 计时器2的调整按钮
        timer2Add1h.setOnClickListener(v -> adjustTime(2, 3600));
        timer2Add10h.setOnClickListener(v -> adjustTime(2, 36000));
        timer2Add1m.setOnClickListener(v -> adjustTime(2, 60));
        timer2Add10m.setOnClickListener(v -> adjustTime(2, 600));
        timer2Add1s.setOnClickListener(v -> adjustTime(2, 1));
        timer2Add10s.setOnClickListener(v -> adjustTime(2, 10));

        timer2Decr1h.setOnClickListener(v -> adjustTime(2, -3600));
        timer2Decr10h.setOnClickListener(v -> adjustTime(2, -36000));
        timer2Decr1m.setOnClickListener(v -> adjustTime(2, -60));
        timer2Decr10m.setOnClickListener(v -> adjustTime(2, -600));
        timer2Decr1s.setOnClickListener(v -> adjustTime(2, -1));
        timer2Decr10s.setOnClickListener(v -> adjustTime(2, -10));

        // 计时器3的调整按钮
        timer3Add1h.setOnClickListener(v -> adjustTime(3, 3600));
        timer3Add10h.setOnClickListener(v -> adjustTime(3, 36000));
        timer3Add1m.setOnClickListener(v -> adjustTime(3, 60));
        timer3Add10m.setOnClickListener(v -> adjustTime(3, 600));
        timer3Add1s.setOnClickListener(v -> adjustTime(3, 1));
        timer3Add10s.setOnClickListener(v -> adjustTime(3, 10));

        timer3Decr1h.setOnClickListener(v -> adjustTime(3, -3600));
        timer3Decr10h.setOnClickListener(v -> adjustTime(3, -36000));
        timer3Decr1m.setOnClickListener(v -> adjustTime(3, -60));
        timer3Decr10m.setOnClickListener(v -> adjustTime(3, -600));
        timer3Decr1s.setOnClickListener(v -> adjustTime(3, -1));
        timer3Decr10s.setOnClickListener(v -> adjustTime(3, -10));

        // 控制按钮
        timer1Start.setOnClickListener(v -> startTimer(1));
        timer1Reset.setOnClickListener(v -> resetTimer(1));

        timer2Start.setOnClickListener(v -> startTimer(2));
        timer2Reset.setOnClickListener(v -> resetTimer(2));

        timer3Start.setOnClickListener(v -> startTimer(3));
        timer3Reset.setOnClickListener(v -> resetTimer(3));
    }

    // 调整时间的通用方法
    private void adjustTime(int timerNum, int secondsToAdd) {
        int index = timerNum - 1;

        // 确保不会变成负数
        if (secondsToAdd < 0 && timerSeconds[index] < Math.abs(secondsToAdd)) {
            timerSeconds[index] = 0;
        } else {
            timerSeconds[index] += secondsToAdd;
        }

        // 设置一个最大限制（比如24小时=86400秒）
        if (timerSeconds[index] > 86400) {
            timerSeconds[index] = 86400;
        }

        updateDisplay(timerNum);
    }

    // 开始计时器
    private void startTimer(int timerNum) {
        int index = timerNum - 1;

        if (!timerRunning[index] && timerSeconds[index] > 0) {
            timerRunning[index] = true;
            // 移除之前的回调
            if (timerRunnables[index] != null) {
                handler.removeCallbacks(timerRunnables[index]);
            }
            // 立即开始计时
            handler.post(timerRunnables[index]);
        }
    }

    private void playFinishAlert(int timerNum) {
        startAlarmService();
        Toast.makeText(this, "计时器 " + timerNum + "号 时间到！", Toast.LENGTH_LONG).show();

        // 记录是哪个计时器触发的
        SharedPreferences prefs = getSharedPreferences("alarm_info", MODE_PRIVATE);
        prefs.edit().putInt("last_alarm_timer", timerNum).apply();
    }

    // 重置计时器
    private void resetTimer(int timerNum) {
        int index = timerNum - 1;
        timerRunning[index] = false;
        // 移除回调
        if (timerRunnables[index] != null) {
            handler.removeCallbacks(timerRunnables[index]);
        }
        timerSeconds[index] = 60;
        updateDisplay(timerNum);
        stopAlarmService();
    }

    // 更新显示
    private void updateDisplay(int timerNum) {
        int index = timerNum - 1;
        int hours = timerSeconds[index] / 3600;
        int minutes = (timerSeconds[index] % 3600) / 60;
        int seconds = timerSeconds[index] % 60;
        String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        switch (timerNum) {
            case 1:
                timer1Display.setText(time);
                break;
            case 2:
                timer2Display.setText(time);
                break;
            case 3:
                timer3Display.setText(time);
                break;
        }
    }

    private void updateAllDisplays() {
        updateDisplay(1);
        updateDisplay(2);
        updateDisplay(3);
    }

    // 启动报警服务
    private void startAlarmService() {
        Log.d("MainActivity", "startAlarmService: ");
        Intent serviceIntent = new Intent(this, AlarmService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    // 停止报警服务
    private void stopAlarmService() {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        stopService(serviceIntent);
    }

    // 广播接收器类
    private class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ALARM_STOPPED".equals(intent.getAction())) {
                // 报警已停止，可以在这里更新UI
                Toast.makeText(MainActivity.this, "报警已停止", Toast.LENGTH_SHORT).show();
            }
        }
    }
}