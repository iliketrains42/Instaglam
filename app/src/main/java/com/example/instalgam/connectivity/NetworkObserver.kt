package com.example.instalgam.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkObserver(
    context: Context,
) : ConnectivityObserver {
    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    override fun observe(): Flow<Boolean> =
        callbackFlow {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.d("networkStatus", "Network is available")
                        trySend(true)
                    }

                    override fun onLost(network: Network) {
                        Log.d("networkStatus", "Network is lost")
                        trySend(false)
                    }

                    override fun onUnavailable() {
                        Log.d("networkStatus", "Network unavailable")
                        trySend(false)
                    }
                }

            connectivityManager.registerDefaultNetworkCallback(callback)

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.distinctUntilChanged()
}

interface ConnectivityObserver {
    fun observe(): Flow<Boolean>
}
