package com.example.qrcodescaner

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.qrcodescaner.ui.theme.QRCodeScanerTheme
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    setContent {
                        QRCodeScanerTheme {
                            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                QRCodeScanner(modifier = Modifier.padding(innerPadding))
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Permission for camera is required to run the app",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun QRCodeScanner(modifier: Modifier = Modifier) {
    var scannedCode by remember { mutableStateOf<String?>(null) }
    var cameraInitialized by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                val previewView = PreviewView(context)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = androidx.camera.core.Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                            .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                            @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                val scanner = BarcodeScanning.getClient()
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.displayValue?.let { value ->
                                                scannedCode = value
                                                Log.d("QRCodeScanner", "QR Code: $value")
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        Log.e("QRCodeScanner", "QR Code scanning failed", it)
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            }
                        }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            context as ComponentActivity,
                            androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )

                        cameraInitialized = true
                    } catch (exc: Exception) {
                        Log.e("QRCodeScanner", "Camera initialization failed", exc)
                        errorMessage = "Camera initialization failed: ${exc.localizedMessage}"
                    }
                }, ContextCompat.getMainExecutor(context))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!cameraInitialized) {
            Text(
                text = "Initializing camera...",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                fontSize = 18.sp
            )
        }

        errorMessage?.let {
            Text(
                text = it,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
                    .background(Color.Red.copy(alpha = 0.7f))
                    .padding(8.dp),
                color = Color.White,
                fontSize = 14.sp
            )
        }

        scannedCode?.let { code ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = "QR Code: $code",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}
