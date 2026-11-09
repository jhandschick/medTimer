package com.futsch1.medtimer.feature.ui.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futsch1.medtimer.core.common.LogTags
import java.time.LocalDate
import java.util.concurrent.Executors
import com.futsch1.medtimer.core.ui.R as CoreUiR

@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    formatDate: (LocalDate) -> String,
    onDone: (messageRes: Int) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }
    // The permission may have been granted in the system settings meanwhile
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = hasCameraPermission(context) }

    Box(modifier.fillMaxSize()) {
        if (granted) {
            CameraPreview(
                shouldDecode = { viewModel.state.value == ScanUiState.Scanning },
                onRaw = viewModel::onRaw,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                stringResource(CoreUiR.string.scan_hint),
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            PermissionHint(onOpenSettings, Modifier.align(Alignment.Center))
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        ScanUiState.Scanning -> Unit
        is ScanUiState.Found -> RefillDialog(current.outcome, formatDate, viewModel::confirmRefill, viewModel::dismiss)
        is ScanUiState.Unassigned -> AssignDialog(current, viewModel::assignTo, viewModel::dismiss)
        is ScanUiState.Done -> LaunchedEffect(current) { onDone(current.messageRes) }
    }
}

private fun hasCameraPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@Composable
private fun PermissionHint(onOpenSettings: () -> Unit, modifier: Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(CoreUiR.string.scan_camera_permission))
        Button(onClick = onOpenSettings) { Text(stringResource(CoreUiR.string.open_settings)) }
    }
}

@Composable
private fun CameraPreview(shouldDecode: () -> Boolean, onRaw: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val analysis = remember {
        ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, PackageAnalyzer(shouldDecode, onRaw)) }
    }
    val preview = remember { Preview.Builder().build() }
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(lifecycleOwner) {
        providerFuture.addListener({
            try {
                providerFuture.get().bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: IllegalArgumentException) {
                // No back camera on this device
                Log.w(LogTags.MAIN, "Cannot open camera for scanning", e)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            if (providerFuture.isDone) providerFuture.get().unbind(preview, analysis)
            analysis.clearAnalyzer()
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { PreviewView(it).also { view -> preview.surfaceProvider = view.surfaceProvider } },
        modifier = modifier
    )
}

@Composable
private fun RefillDialog(
    outcome: ScanOutcome.Known,
    formatDate: (LocalDate) -> String,
    onConfirm: (takeExpiry: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var takeExpiry by remember { mutableStateOf(true) }
    val expiry = outcome.code.expiry
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(CoreUiR.string.scan_refill_question, outcome.medicine.name)) },
        text = {
            Column {
                outcome.code.batch?.let { Text(stringResource(CoreUiR.string.scan_batch, it)) }
                if (expiry != null) {
                    Row(Modifier.clickable { takeExpiry = !takeExpiry }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = takeExpiry, onCheckedChange = { takeExpiry = it })
                        Text(stringResource(CoreUiR.string.scan_take_expiry, formatDate(expiry)))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(takeExpiry) }) { Text(stringResource(CoreUiR.string.refill_now)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } }
    )
}

@Composable
private fun AssignDialog(state: ScanUiState.Unassigned, onAssign: (medicineId: Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(CoreUiR.string.scan_package)) },
        text = {
            Column {
                Text(stringResource(CoreUiR.string.scan_unknown_pzn, state.outcome.code.pzn))
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(state.medicines, key = { it.id }) { medicine ->
                        Text(
                            medicine.name,
                            modifier = Modifier.fillMaxWidth().clickable { onAssign(medicine.id) }.padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } }
    )
}
