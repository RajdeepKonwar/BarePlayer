package com.rajdeepkonwar.bareplayer.core.data.scan

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.room.withTransaction
import com.rajdeepkonwar.bareplayer.core.data.db.BareDatabase
import com.rajdeepkonwar.bareplayer.core.data.db.toEntity
import com.rajdeepkonwar.bareplayer.core.domain.library.LibraryDiff
import com.rajdeepkonwar.bareplayer.core.model.ScanState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Persists the last MediaStore change token so unchanged libraries skip the startup scan. */
interface ScanTokenStore {
    suspend fun read(): String?
    suspend fun write(token: String?)
}

/**
 * Keeps the Room index in sync with MediaStore: a scan on start (skipped when the MediaStore change
 * token is unchanged) and debounced rescans whenever MediaStore reports a change.
 */
class LibrarySync(
    private val database: BareDatabase,
    private val scanner: MediaScanner,
    private val tokenStore: ScanTokenStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val mutex = Mutex()
    private var observer: ContentObserver? = null
    private var pendingRescan: Job? = null

    suspend fun sync(force: Boolean) = mutex.withLock {
        val dao = database.trackDao()
        val token = scanner.changeToken()
        if (!force && token != null && token == tokenStore.read() && dao.count() > 0) return@withLock

        _state.value = ScanState.Scanning
        val start = clock()
        try {
            val scanned = scanner.scan()
            val cached = dao.fingerprints().associate { it.id to it.fingerprint }
            val diff = LibraryDiff.compute(scanned, cached)
            if (!diff.isEmpty) {
                database.withTransaction {
                    diff.deletedIds.chunked(CHUNK).forEach { dao.deleteByIds(it) }
                    diff.upserts.chunked(CHUNK).forEach { chunk -> dao.upsert(chunk.map { it.toEntity() }) }
                }
            }
            tokenStore.write(token)
            _state.value = ScanState.Done(trackCount = scanned.size, durationMs = clock() - start)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = ScanState.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    /** Starts the initial sync and observes MediaStore. Safe to call more than once. */
    fun start(context: Context) {
        if (observer != null) return
        scope.launch { sync(force = false) }
        val handler = Handler(Looper.getMainLooper())
        val contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                pendingRescan?.cancel()
                pendingRescan = scope.launch {
                    delay(RESCAN_DEBOUNCE_MS)
                    sync(force = true)
                }
            }
        }
        context.applicationContext.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver,
        )
        observer = contentObserver
    }

    private companion object {
        const val CHUNK = 500
        const val RESCAN_DEBOUNCE_MS = 2_000L
    }
}
