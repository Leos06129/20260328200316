package com.example.randomscreensaver

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.randomscreensaver.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化UI
        initUI()
        
        // 检查并请求权限
        checkAndRequestPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
    }
    
    private fun initUI() {
        // 设置按钮点击事件
        binding.btnStartService.setOnClickListener {
            if (checkOverlayPermission()) {
                startScreenService()
            } else {
                showOverlayPermissionDialog()
            }
        }
        
        binding.btnStopService.setOnClickListener {
            stopScreenService()
        }
        
        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        // 更新UI状态
        updateUI()
    }
    
    private fun updateUI() {
        // 检查服务状态
        val isServiceRunning = ScreenService.isServiceRunning
        
        binding.tvServiceStatus.text = if (isServiceRunning) {
            "服务状态: 运行中"
        } else {
            "服务状态: 已停止"
        }
        
        // 更新按钮状态
        binding.btnStartService.isEnabled = !isServiceRunning
        binding.btnStopService.isEnabled = isServiceRunning
        
        // 更新当前设置显示
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentMessage = prefs.getString(PREF_MESSAGE, getString(R.string.default_message)) 
            ?: getString(R.string.default_message)
        binding.tvCurrentMessage.text = "当前语句: $currentMessage"
        
        val minInterval = prefs.getInt(PREF_MIN_INTERVAL, DEFAULT_MIN_INTERVAL)
        val maxInterval = prefs.getInt(PREF_MAX_INTERVAL, DEFAULT_MAX_INTERVAL)
        binding.tvCurrentInterval.text = "显示间隔: ${minInterval}-${maxInterval}秒"
        
        // 更新权限状态
        binding.tvPermissionStatus.text = if (checkOverlayPermission()) {
            "权限状态: 已授权"
        } else {
            "权限状态: 未授权（需要设置）"
        }
    }
    
    private fun checkAndRequestPermissions() {
        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showOverlayPermissionDialog()
            }
        }
        
        // 检查忽略电池优化权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                requestIgnoreBatteryOptimizations()
            }
        }
    }
    
    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要权限")
            .setMessage("应用需要\"显示在其他应用上层\"权限才能正常工作")
            .setPositiveButton("去设置") { _, _ ->
                requestOverlayPermission()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)

        }
    }
    
    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
    
    private fun startScreenService() {
        val intent = Intent(this, ScreenService::class.java).apply {
            action = ScreenService.ACTION_START
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
        
        Toast.makeText(this, "屏保服务已启动", Toast.LENGTH_SHORT).show()
        updateUI()
    }
    
    private fun stopScreenService() {
        val intent = Intent(this, ScreenService::class.java).apply {
            action = ScreenService.ACTION_STOP
        }
        stopService(intent)
        
        Toast.makeText(this, "屏保服务已停止", Toast.LENGTH_SHORT).show()
        updateUI()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (checkOverlayPermission()) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要权限才能正常运行", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 1001
        const val PREFS_NAME = "ScreenSaverPrefs"
        const val PREF_MESSAGE = "message"
        const val PREF_MAX_INTERVAL = "max_interval"
        const val PREF_MIN_INTERVAL = "min_interval"
        const val DEFAULT_MAX_INTERVAL = 20
        const val DEFAULT_MIN_INTERVAL = 10
    }
}