package me.mavenried.Ryde.service

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object HeartRateManager {

    private val HR_SERVICE     = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
    private val HR_MEASUREMENT = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
    private val CCCD           = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _scanResults = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scanResults: StateFlow<List<BluetoothDevice>> = _scanResults.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var scanner: BluetoothLeScanner? = null

    @SuppressLint("MissingPermission")
    fun startScan(context: Context) {
        val adapter = adapter(context) ?: return
        _scanResults.value = emptyList()
        scanner = adapter.bluetoothLeScanner
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HR_SERVICE))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanner?.stopScan(scanCallback)
        scanner = null
    }

    @SuppressLint("MissingPermission")
    fun connect(context: Context, mac: String) {
        val adapter = adapter(context) ?: return
        gatt?.close()
        val device = try { adapter.getRemoteDevice(mac) } catch (_: Exception) { return }
        gatt = device.connectGatt(context, true, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connected.value = false
        _heartRate.value = null
    }

    private fun adapter(context: Context): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (_scanResults.value.none { it.address == device.address }) {
                _scanResults.value = _scanResults.value + device
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connected.value = true
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connected.value = false
                    _heartRate.value = null
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val char = gatt.getService(HR_SERVICE)?.getCharacteristic(HR_MEASUREMENT) ?: return
            gatt.setCharacteristicNotification(char, true)
            val descriptor = char.getDescriptor(CCCD) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }

        // Android 13+
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid == HR_MEASUREMENT) _heartRate.value = parseHr(value)
        }

        // Android < 13
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (characteristic.uuid == HR_MEASUREMENT)
                _heartRate.value = parseHr(characteristic.value ?: return)
        }
    }

    private fun parseHr(value: ByteArray): Int {
        if (value.isEmpty()) return 0
        val flags = value[0].toInt() and 0xFF
        return if (flags and 0x01 == 0) {
            value.getOrNull(1)?.toInt()?.and(0xFF) ?: 0
        } else {
            val lo = value.getOrNull(1)?.toInt()?.and(0xFF) ?: 0
            val hi = value.getOrNull(2)?.toInt()?.and(0xFF) ?: 0
            lo or (hi shl 8)
        }
    }
}
