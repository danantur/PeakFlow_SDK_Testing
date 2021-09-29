package com.dantesting.peakflow_sdk_testing.utils

import android.content.Context
import android.util.Log
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.exceptions.BleException
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.disposables.Disposable

@Suppress("NAME_SHADOWING")
class Client(ctx: Context, private val stateCallback: StateCallback,
             private val searchCallback: SearchCallback? = null,
             private val connectionCallback: ConnectionCallback? = null) {

    private fun safeDispose(vararg subs: Disposable?) {
        for (sub in subs)
            if (sub != null)
                if (!sub.isDisposed)
                    sub.dispose()
    }

    private val TAG: String = this.javaClass.name

    private val bleClient: RxBleClient = RxBleClient.create(ctx)

    private var stateDisposable: Disposable = getState()
    private var searchDisposable: Disposable? = null

    private var device: RxBleDevice? = null

    private var connection: RxBleConnection? = null
    private var connectionDisposable: Disposable? = null
    private var connectionStateDisposable: Disposable? = null

    fun connect(mac: String) {
        device = bleClient.getBleDevice(mac)
        disconnect()
        connectionDisposable = getConnSub()
        connectionStateDisposable = getConnStateSub()
    }

    fun disconnect() {
        safeDispose(connectionDisposable, connectionStateDisposable, stateDisposable)
    }

    fun checkState() {
        handleScanState(bleClient.state)
        stateDisposable = getState()
    }

    private fun handleScanState(state: RxBleClient.State) {
        when (state) {
            RxBleClient.State.READY -> stateCallback.onReady()
            RxBleClient.State.BLUETOOTH_NOT_AVAILABLE -> stateCallback.onError("Bluetooth не доступен на вашем устройстве")
            RxBleClient.State.LOCATION_PERMISSION_NOT_GRANTED -> stateCallback.onLocationPermRequired()
            RxBleClient.State.BLUETOOTH_NOT_ENABLED -> stateCallback.onBluetoothRequired()
            RxBleClient.State.LOCATION_SERVICES_NOT_ENABLED -> stateCallback.onLocationRequired()
        }
    }

    fun startSearching() {
        if (searchCallback != null) {
            safeDispose(searchDisposable)
            stateDisposable = getState()
            searchDisposable = getSearch()
        }
        else
            stateCallback.onError("Клиент инициализирован на поиск!")
    }

    fun endSearching() {
        safeDispose(searchDisposable, stateDisposable)
    }

    private fun getSearch(): Disposable =
        bleClient.scanBleDevices(ScanSettings.Builder().build(), ScanFilter.empty())
            .subscribe(
                { scanResult ->
                    searchCallback?.onDeviceFound(scanResult.bleDevice)
                },
                { throwable ->
                    if (throwable is BleScanException) {
                        stateCallback.onError(
                                when (throwable.reason) {
                                    BleScanException.BLUETOOTH_CANNOT_START -> "ошибка при запуске службы Bluetooth"
                                    BleScanException.SCAN_FAILED_ALREADY_STARTED -> "сканирование уже запущено"
                                    BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "ошибка регистрации приложения"
                                    BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED -> "ваше устройство не поддерживает сканирование через Bluetooth"
                                    BleScanException.SCAN_FAILED_INTERNAL_ERROR -> "Внутренняя ошибка сканирования"
                                    BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Нехватка ресурсов оборудования"
                                    BleScanException.UNDOCUMENTED_SCAN_THROTTLE -> "UNDOCUMENTED_SCAN_THROTTLE"
                                    BleScanException.UNKNOWN_ERROR_CODE -> "Неизвестная ошибка"
                                    else -> return@subscribe
                                })
                    }
                })

    private fun getState(): Disposable =
        bleClient.observeStateChanges()
            .subscribe({ state ->
                handleScanState(state)
            },{ throwable ->
                Log.e(TAG, throwable.stackTraceToString())
            })

    private fun getConnSub(): Disposable =
        device!!.establishConnection(false)
            .subscribe(
                { connection_ ->
                    connection = connection_
                },
                { throwable ->
                    connectionCallback?.onConnectionError(throwable as BleException)
                }
            )

    private fun getConnStateSub(): Disposable =
        device!!.observeConnectionStateChanges()
            .subscribe(
                { state ->
                    connectionCallback?.onConnectionState(state)
                },
                { throwable ->
                    connectionCallback?.onConnectionError(throwable as BleException)
                }
            )

    interface StateCallback {
        fun onError(msg: String)
        fun onLocationPermRequired()
        fun onLocationRequired()
        fun onBluetoothRequired()
        fun onReady()
    }

    interface SearchCallback {
        fun onDeviceFound(rxBleDevice: RxBleDevice)
    }

    interface ConnectionCallback {
        fun onConnectionError(error: BleException)
        fun onConnectionState(state: RxBleConnection.RxBleConnectionState)
    }
}