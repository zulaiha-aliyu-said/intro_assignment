package com.capcutmini.app.media.export

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.capcutmini.app.media.effects.EffectsFactory
import com.capcutmini.app.media.model.EditDocument
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

sealed class ExportOutcome {
    data class Success(val outputFile: File) : ExportOutcome()
    data class Failure(val message: String, val cause: Throwable? = null) : ExportOutcome()
}

@UnstableApi
class VideoExporter(
    private val context: Context
) {
    private val mainExecutor = Executor { command ->
        Handler(Looper.getMainLooper()).post(command)
    }

    suspend fun export(document: EditDocument): ExportOutcome {
        if (document.clips.isEmpty()) {
            return ExportOutcome.Failure("No clips to export")
        }
        val outDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val outFile = File(outDir, "capcutmini_${System.currentTimeMillis()}.mp4")
        if (outFile.exists()) outFile.delete()

        return try {
            suspendCancellableCoroutine { cont ->
                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (cont.isActive) cont.resume(ExportOutcome.Success(outFile))
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            if (cont.isActive) {
                                cont.resume(
                                    ExportOutcome.Failure(
                                        exportException.message ?: "Export failed",
                                        exportException
                                    )
                                )
                            }
                        }
                    })
                    .build()

                val composition = EffectsFactory.buildComposition(context, document)

                cont.invokeOnCancellation {
                    runCatching { transformer.cancel() }
                }

                mainExecutor.execute {
                    transformer.start(composition, outFile.absolutePath)
                }
            }
        } catch (t: Throwable) {
            ExportOutcome.Failure(t.message ?: "Export failed", t)
        }
    }
}
