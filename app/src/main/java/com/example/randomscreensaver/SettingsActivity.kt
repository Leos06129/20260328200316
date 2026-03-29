package com.example.randomscreensaver

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.randomscreensaver.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "设置"
        
        // 加载当前设置
        loadCurrentSettings()
        
        // 设置保存按钮点击事件
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
    }
    
    private fun loadCurrentSettings() {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)

        // 加载语句1
        val message = prefs.getString(
            MainActivity.PREF_MESSAGE,
            getString(R.string.default_message)
        ) ?: getString(R.string.default_message)
        binding.etMessage.setText(message)

        // 加载语句2
        val message2 = prefs.getString(
            MainActivity.PREF_MESSAGE2,
            getString(R.string.default_message2)
        ) ?: getString(R.string.default_message2)
        binding.etMessage2.setText(message2)

        // 加载最小间隔
        val minInterval = prefs.getInt(
            MainActivity.PREF_MIN_INTERVAL,
            MainActivity.DEFAULT_MIN_INTERVAL
        )
        binding.etMinInterval.setText(minInterval.toString())

        // 加载最大间隔
        val maxInterval = prefs.getInt(
            MainActivity.PREF_MAX_INTERVAL,
            MainActivity.DEFAULT_MAX_INTERVAL
        )
        binding.etMaxInterval.setText(maxInterval.toString())

        // 加载自动启动设置
        val autoStart = prefs.getBoolean("auto_start", false)
        binding.switchAutoStart.isChecked = autoStart
    }
    
    private fun saveSettings() {
        val message = binding.etMessage.text.toString().trim()
        val message2 = binding.etMessage2.text.toString().trim()
        val minIntervalText = binding.etMinInterval.text.toString().trim()
        val maxIntervalText = binding.etMaxInterval.text.toString().trim()

        // 验证输入：至少要有一条文字
        if (message.isEmpty() && message2.isEmpty()) {
            Toast.makeText(this, "请至少输入一条要显示的语句", Toast.LENGTH_SHORT).show()
            return
        }

        if (minIntervalText.isEmpty() || maxIntervalText.isEmpty()) {
            Toast.makeText(this, "请输入时间间隔", Toast.LENGTH_SHORT).show()
            return
        }

        val minInterval = minIntervalText.toIntOrNull()
        val maxInterval = maxIntervalText.toIntOrNull()

        if (minInterval == null || maxInterval == null) {
            Toast.makeText(this, "时间间隔必须是数字", Toast.LENGTH_SHORT).show()
            return
        }

        if (minInterval < 5) {
            Toast.makeText(this, "最小间隔不能小于5秒", Toast.LENGTH_SHORT).show()
            return
        }

        if (maxInterval < minInterval) {
            Toast.makeText(this, "最大间隔不能小于最小间隔", Toast.LENGTH_SHORT).show()
            return
        }

        if (maxInterval > 60) {
            Toast.makeText(this, "最大间隔不能超过60秒", Toast.LENGTH_SHORT).show()
            return
        }

        // 保存设置
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()

        // 保存两条文字（如果为空则使用默认文字）
        editor.putString(MainActivity.PREF_MESSAGE, message.ifEmpty { getString(R.string.default_message) })
        editor.putString(MainActivity.PREF_MESSAGE2, message2.ifEmpty { getString(R.string.default_message2) })
        editor.putInt(MainActivity.PREF_MIN_INTERVAL, minInterval)
        editor.putInt(MainActivity.PREF_MAX_INTERVAL, maxInterval)
        editor.putBoolean("auto_start", binding.switchAutoStart.isChecked)

        if (editor.commit()) {
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()

            // 如果服务正在运行，可能需要重启服务以应用新设置
            val isServiceRunning = prefs.getBoolean("service_running", false)
            if (isServiceRunning) {
                Toast.makeText(this, "新设置在下次显示时生效", Toast.LENGTH_SHORT).show()
            }

            finish()
        } else {
            Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}