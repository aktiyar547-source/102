package com.middleeastcontainer.ui.camera

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.middleeastcontainer.ui.components.FileImage
import com.middleeastcontainer.ui.theme.BrandGold
import java.util.concurrent.TimeUnit

/**
 * Full-screen in-app camera.
 *
 * For unlimited shooting the shutter re-arms as soon as the frame is captured, not
 * when it finishes being written — watermarking and encoding happen on a queue
 * behind the viewfinder. That is the difference between a camera that feels
 * instant and one that pauses for half a second after every shot.
 *
 * Tapping the preview drives autofocus at that point, so an inspector can focus on
 * the damage rather than whatever happens to be in the centre of the frame.
 */
@Composable
fun CameraScreen(
    onDone: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()

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

    // Single-shot mode leaves as soon as the photo is stored.
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    val imageCapture = remember(viewModel.prefersQuality) {
        ImageCapture.Builder()
            .setCaptureMode(
                if (viewModel.prefersQuality) {
                    // A distant number is only a few pixels tall, and OCR cannot
                    // recover what the capture threw away.
                    ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                } else {
                    // Unlimited shooting: the shutter must stay responsive.
                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                }
            )
            .build()
    }
    // Held so a tap on the preview can drive focus.
    val cameraRef = remember { mutableStateOf<Camera?>(null) }
    // Guards the capture itself only — cleared in the callback, not after saving.
    var capturing by remember { mutableStateOf(false) }

    fun shoot() {
        if (capturing) return
        capturing = true
        val target = viewModel.newCaptureFile()
        val options = ImageCapture.OutputFileOptions.Builder(target).build()
        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    capturing = false
                    viewModel.onPhotoTaken(target)
                }

                override fun onError(exc: ImageCaptureException) {
                    capturing = false
                    viewModel.onCameraError(exc.message ?: "Could not take the photo")
                }
            },
        )
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
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
                            viewModel.onCameraError(e.message ?: "Camera unavailable")
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    // Pinch to zoom: the only way to read a number across the yard
                    // without walking to it.
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

                    // Tap to focus where the inspector is actually looking.
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
        } else {
            Text(
                "Camera permission is required to photograph containers.",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }

        // What is being photographed, and how many so far.
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color(0xAA000000), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                viewModel.containerLabel,
                color = BrandGold,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row {
                Text(
                    viewModel.targetLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (viewModel.isBurst && state.shotCount > 0) {
                    Text(
                        "  ·  ${state.shotCount} taken",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (state.pending > 0) {
                Text(
                    "saving ${state.pending}",
                    color = BrandGold,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // Last stored photo: proof the shot landed, without interrupting shooting.
        if (viewModel.isBurst && state.lastThumbnail != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(64.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(6.dp))
            ) {
                FileImage(
                    absolutePath = state.lastThumbnail,
                    contentDescription = "Last photo",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        state.error?.let { message ->
            Text(
                message,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCCB00020), RoundedCornerShape(8.dp))
                    .padding(16.dp),
            )
        }

        // Shutter. In burst mode this is ready again the moment the frame is
        // captured; the previous photo finishes saving in the background.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .size(80.dp)
                .background(Color(0x55FFFFFF), CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .clickable(
                    enabled = hasPermission && !state.saving && !state.finishing,
                    onClick = { shoot() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (state.saving || state.finishing) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            } else {
                Box(Modifier.size(62.dp).background(Color.White, CircleShape))
            }
        }

        Button(
            onClick = { viewModel.onFinish(onDone) },
            enabled = !state.finishing,
            modifier = Modifier.align(Alignment.BottomStart).padding(24.dp),
        ) {
            Text(
                when {
                    state.finishing -> "Saving…"
                    viewModel.isBurst && state.shotCount > 0 -> "Done"
                    else -> "Back"
                }
            )
        }
    }
}
