package com.futsch1.medtimer.feature.ui.scan

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.futsch1.medtimer.core.common.helpers.safeStartActivity
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.core.ui.component.MedTimerTopAppBar
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.futsch1.medtimer.core.ui.R as CoreUiR

/** Scans a medicine package with the camera to refill the matching medicine or assign its PZN. */
@AndroidEntryPoint
class ScanFragment : Fragment() {
    @Inject
    lateinit var timeFormatter: TimeFormatter

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MedTimerTheme {
                    Column {
                        MedTimerTopAppBar(
                            title = stringResource(CoreUiR.string.scan_package),
                            onNavigateUp = { findNavController().navigateUp() }
                        )
                        ScanScreen(
                            viewModel = viewModel,
                            formatDate = timeFormatter::localDateToString,
                            onDone = { messageRes ->
                                Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
                                findNavController().navigateUp()
                            },
                            onOpenSettings = ::openAppSettings
                        )
                    }
                }
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${requireContext().packageName}".toUri())
        safeStartActivity(requireContext(), intent)
    }
}
