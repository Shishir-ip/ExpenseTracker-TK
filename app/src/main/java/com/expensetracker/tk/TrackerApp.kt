package com.expensetracker.tk

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import com.expensetracker.tk.data.Db
import com.expensetracker.tk.data.Prefs
import com.expensetracker.tk.data.SyncEngine
import com.expensetracker.tk.notify.Notify

class TrackerApp : Application() {

    lateinit var db: Db
        private set
    lateinit var prefs: Prefs
        private set
    lateinit var sync: SyncEngine
        private set

    override fun onCreate() {
        super.onCreate()
        db = Db(this)
        prefs = Prefs(this)
        sync = SyncEngine(this, db, prefs)
        Notify.createChannels(this)
        sync.scheduleBackground()
        Notify.scheduleDaily(this)
        Notify.scheduleBorrowReminders(this, db.allBorrows())
        com.expensetracker.tk.data.Recur.schedule(this)
        com.expensetracker.tk.data.Backup.schedule(this)
        db.trashPurgeOlderThan(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
        if (com.expensetracker.tk.data.Recur.runForToday(this) > 0) {
            sync.requestSync()
        }
        registerNetworkSync()
    }

    /**
     * Offline queue drain: fires whenever a usable network appears (app start,
     * wifi/mobile reconnect), so pending local changes upload automatically.
     */
    private fun registerNetworkSync() {
        try {
            val cm = getSystemService(ConnectivityManager::class.java) ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (prefs.cloudConfigured) {
                        Handler(Looper.getMainLooper()).post {
                            sync.requestSync()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            // background sync via WorkManager still covers periodic drains
        }
    }
}
