package com.example.eeum.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX + MLKit QR 스캐너.
 * - 외부에서 CAMERA 권한을 보장한 뒤 사용하세요.
 * - onCodeScanned는 최초 1회만 콜백됩니다.
 */
@Composable
fun QRScannerView(
    modifier: Modifier = Modifier,
    onCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    AndroidView(
        factory = { previewView.apply { scaleType = PreviewView.ScaleType.FILL_CENTER } },
        modifier = modifier
    ) { pv ->
        startCamera(context, lifecycleOwner, pv, onCodeScanned)
    }

    // 화면 해제 시 카메라 리소스 정리
    DisposableEffect(Unit) {
        onDispose {
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Exception) { }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun startCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    onResult: (String) -> Unit
) {
    val providerFuture = ProcessCameraProvider.getInstance(context)
    providerFuture.addListener({
        val provider = providerFuture.get()

        val preview = Preview.Builder()
            .setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_4_3)
            .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
            .build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = BarcodeScanning.getClient(options)

        val reported = AtomicBoolean(false)

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_4_3)
            .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
            .build().apply {
                setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy: ImageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close(); return@setAnalyzer
                    }
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            if (reported.get()) return@addOnSuccessListener
                            for (code in barcodes) {
                                val raw = code.rawValue
                                if (!raw.isNullOrBlank()) {
                                    if (reported.compareAndSet(false, true)) {
                                        onResult(raw)
                                    }
                                    break
                                }
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                }
            }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        } catch (_: Exception) { }
    }, ContextCompat.getMainExecutor(context))
}
