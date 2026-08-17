package com.middleeastcontainer.ui.inventory

import android.Manifest
import android.content.pm.PackageManager
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Viewfinder for a yard sweep.
 *
 * Separate from the inspection camera because the two answer different questions:
 * that one photographs a container in front of you, this one reads numbers off a
 * stack that may be some distance away. Hence maximum-quality capture and pinch
 * zoom — a number the capture blurred is one OCR can never recover.
 */
@Composable
fun SweepCamera(
    newTarget: () -> File,
    onCaptured: (File) -> Unit,
    onError: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) requestPermission.launch(Manifest.permission.CAMERA)
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            // Detail decides whether a distant number is readable at all.
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    val cameraRef = remember { mutableStateOf<Camera?>(null) }
    var capturing by remember { mutableStateOf(false) }

    Box(modifier) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.padding(0.dp),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        val provider = future.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        try {
                            provider.unbindAll()
                            cameraRef.value = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture,
                            )
                        } catch (e: Exception) {
                            onError(e.message ?: "Camera unavailable")
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    val scaleDetector = ScaleGestureDetector(
                        ctx,
                        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScale(detector: ScaleGestureDetector): Boolean {
                                val camera = cameraRef.value ?: return true
                                val current = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                                camera.cameraControl.setZoomRatio(current * detector.scaleFactor)
                                return true
                            }
                        },
                    )

                    previewView.setOnTouchListener { view, event ->
                        scaleDetector.onTouchEvent(event)
                        if (!scaleDetector.isInProgress && event.action == MotionEvent.ACTION_UP) {
                            val point = previewView.meteringPointFactory
                                .createPoint(event.x, event.y)
                            cameraRef.value?.cameraControl?.startFocusAndMetering(
                                FocusMeteringAction
                                    .Builder(point, FocusMeteringAction.FLAG_AF)
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                    .build()
                            )
                            view.performClick()
                        }
                        true
                    }
                    previewView
                },
            )

            DisposableEffect(Unit) {
                onDispose {
                    runCatching {
                        ProcessCameraProvider.getInstance(context).get().unbindAll()
                    }
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .size(76.dp)
                .background(Color(0x55FFFFFF), CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .clickable(enabled = hasPermission && enabled && !capturing) {
                    capturing = true
                    val target = newTarget()
                    imageCapture.takePicture(
                        ImageCapture.OutputFileOptions.Builder(target).build(),
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                capturing = false
                                onCaptured(target)
                            }

                            override fun onError(exc: ImageCaptureException) {
                                capturing = false
                                onError(exc.message ?: "Could not take the photo")
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(58.dp).background(Color.White, CircleShape))
        }
    }
}
