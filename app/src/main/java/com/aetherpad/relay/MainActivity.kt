package com.aetherpad.relay

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.aetherpad.relay.ui.GamepadHudScreen
import com.aetherpad.relay.ui.theme.AetherPadTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    companion object {
        // Shared thread-safe gamepad state stream
        val gamepadStateFlow = MutableStateFlow(GamepadInputState())
        var activeControllerId: Int? = null
        val physicalGamepadConnected = MutableStateFlow(false)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force Landscape Gaming Orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        requestRequiredPermissions()
        requestBatteryOptimizationBypass()
        checkConnectedGamepads()

        setContent {
            AetherPadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF05070D)
                ) {
                    GamepadHudScreen(
                        onStartRelay = { ip, port -> startRelayService(ip, port) },
                        onStopRelay = { stopRelayService() }
                    )
                }
            }
        }
    }

    private fun checkConnectedGamepads() {
        val deviceIds = InputDevice.getDeviceIds()
        var foundGamepad = false
        for (id in deviceIds) {
            val device = InputDevice.getDevice(id)
            if (device != null && isGamepadDevice(device.sources)) {
                foundGamepad = true
                activeControllerId = id
                break
            }
        }
        physicalGamepadConnected.value = foundGamepad
    }

    private fun isGamepadDevice(sources: Int): Boolean {
        return (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
               (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.INTERNET,
            Manifest.permission.VIBRATE,
            Manifest.permission.WAKE_LOCK
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun requestBatteryOptimizationBypass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    private fun startRelayService(ip: String, port: Int) {
        val intent = Intent(this, GamepadRelayService::class.java).apply {
            action = GamepadRelayService.ACTION_START
            putExtra(GamepadRelayService.EXTRA_IP, ip)
            putExtra(GamepadRelayService.EXTRA_PORT, port)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopRelayService() {
        val intent = Intent(this, GamepadRelayService::class.java).apply {
            action = GamepadRelayService.ACTION_STOP
        }
        startService(intent)
    }

    // ─────────────────────────────────────────────────────────────────
    // HARDWARE INPUT DISPATCHING (USB Type-C & Bluetooth Gamepads)
    // ─────────────────────────────────────────────────────────────────

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null || !isGamepadDevice(event.source)) {
            return super.onKeyDown(keyCode, event)
        }
        
        // Free tier limitation: lock to single controller
        if (activeControllerId == null) {
            activeControllerId = event.deviceId
            physicalGamepadConnected.value = true
        } else if (activeControllerId != event.deviceId) {
            return true // Ignore additional gamepads
        }

        updateButtonState(keyCode, isPressed = true)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null || !isGamepadDevice(event.source)) {
            return super.onKeyUp(keyCode, event)
        }
        updateButtonState(keyCode, isPressed = false)
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event == null || !isGamepadDevice(event.source)) {
            return super.onGenericMotionEvent(event)
        }

        if (activeControllerId == null) {
            activeControllerId = event.deviceId
            physicalGamepadConnected.value = true
        } else if (activeControllerId != event.deviceId) {
            return true
        }

        val currentState = gamepadStateFlow.value
        val leftX = event.getAxisValue(MotionEvent.AXIS_X)
        val leftY = event.getAxisValue(MotionEvent.AXIS_Y)
        val rightX = event.getAxisValue(MotionEvent.AXIS_Z)
        val rightY = event.getAxisValue(MotionEvent.AXIS_RZ)
        val lTrigger = event.getAxisValue(MotionEvent.AXIS_LTRIGGER).coerceAtLeast(event.getAxisValue(MotionEvent.AXIS_BRAKE))
        val rTrigger = event.getAxisValue(MotionEvent.AXIS_RTRIGGER).coerceAtLeast(event.getAxisValue(MotionEvent.AXIS_GAS))
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        gamepadStateFlow.value = currentState.copy(
            leftStickX = leftX,
            leftStickY = leftY,
            rightStickX = rightX,
            rightStickY = rightY,
            lTrigger = lTrigger,
            rTrigger = rTrigger,
            dpadLeft = hatX == -1.0f || currentState.dpadLeft,
            dpadRight = hatX == 1.0f || currentState.dpadRight,
            dpadUp = hatY == -1.0f || currentState.dpadUp,
            dpadDown = hatY == 1.0f || currentState.dpadDown
        )

        return true
    }

    private fun updateButtonState(keyCode: Int, isPressed: Boolean) {
        val current = gamepadStateFlow.value
        gamepadStateFlow.value = when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> current.copy(buttonA = isPressed)
            KeyEvent.KEYCODE_BUTTON_B -> current.copy(buttonB = isPressed)
            KeyEvent.KEYCODE_BUTTON_X -> current.copy(buttonX = isPressed)
            KeyEvent.KEYCODE_BUTTON_Y -> current.copy(buttonY = isPressed)
            KeyEvent.KEYCODE_BUTTON_L1 -> current.copy(buttonL1 = isPressed)
            KeyEvent.KEYCODE_BUTTON_R1 -> current.copy(buttonR1 = isPressed)
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BACK -> current.copy(buttonBack = isPressed)
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_MENU -> current.copy(buttonStart = isPressed)
            KeyEvent.KEYCODE_BUTTON_MODE -> current.copy(buttonGuide = isPressed)
            KeyEvent.KEYCODE_BUTTON_THUMBL -> current.copy(buttonL3 = isPressed)
            KeyEvent.KEYCODE_BUTTON_THUMBR -> current.copy(buttonR3 = isPressed)
            KeyEvent.KEYCODE_DPAD_UP -> current.copy(dpadUp = isPressed)
            KeyEvent.KEYCODE_DPAD_DOWN -> current.copy(dpadDown = isPressed)
            KeyEvent.KEYCODE_DPAD_LEFT -> current.copy(dpadLeft = isPressed)
            KeyEvent.KEYCODE_DPAD_RIGHT -> current.copy(dpadRight = isPressed)
            else -> current
        }
    }
}
