package ren.hieu.sensenapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ren.hieu.sensenapp.widget.updateAllScheduleWidgets
import ren.hieu.sensenapp.notification.SenSenNotifications
import ren.hieu.sensenapp.ui.AppUpdateDialog
import ren.hieu.sensenapp.ui.ScheduleViewModel
import ren.hieu.sensenapp.ui.SettingsResultDialogHost
import ren.hieu.sensenapp.ui.SettingsSyncOverlay
import ren.hieu.sensenapp.ui.curriculum.CurriculumScreen
import ren.hieu.sensenapp.ui.theme.SenSenSchoolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SenSenApplication
        val factory = ScheduleViewModel.Factory(
            app.container.scheduleRepository,
            app.container.remoteConfigRepository,
            app.container.settingsStore,
        )
        lifecycleScope.launch {
            updateAllScheduleWidgets(applicationContext)
        }

        setContent {
            SenSenSchoolTheme {
                val vm: ScheduleViewModel = viewModel(factory = factory)
                val uiState by vm.uiState.collectAsState()
                val updatePrompt by vm.updatePrompt.collectAsStateWithLifecycle()
                val notificationPermission = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        vm.setClassReminderEnabled(true)
                    } else {
                        vm.setClassReminderEnabled(false)
                    }
                }

                AppUpdateDialog(
                    prompt = updatePrompt,
                    onDismiss = vm::dismissUpdatePrompt,
                )

                SettingsResultDialogHost(
                    dialog = uiState.settingsResultDialog,
                    onDismiss = vm::dismissSettingsResultDialog,
                )

                Box(Modifier.fillMaxSize()) {
                    CurriculumScreen(
                    uiState = uiState,
                    onPrevWeek = { vm.changeWeekByDelta(-1) },
                    onNextWeek = { vm.changeWeekByDelta(1) },
                    onOpenWeekPicker = vm::openWeekPicker,
                    onCloseWeekPicker = vm::closeWeekPicker,
                    onSelectWeek = vm::setWeek,
                    onCloseStyleSheet = vm::closeStyleSheet,
                    onSelectStyle = vm::selectCardStyle,
                    onCourseClick = vm::openCourseDetail,
                    onBlankCellClick = vm::openBlankCourseEditor,
                    onSaveCourse = vm::saveCourse,
                    onDeleteCourse = vm::deleteCourse,
                    onCloseCourseEditor = vm::closeCourseEditor,
                    onOpenSettings = vm::openSettingsSheet,
                    onCloseSettings = vm::closeSettingsSheet,
                    onOpenStyleFromSettings = vm::openStyleFromSettings,
                    onOpenHolidaySettings = vm::openHolidaySettings,
                    onCloseHolidaySettings = vm::closeHolidaySettings,
                    onOpenTermSettings = vm::openTermSettings,
                    onCloseTermSettings = vm::closeTermSettings,
                    onSaveTermFirstMonday = vm::saveTermFirstMonday,
                    onAddHolidayMapping = vm::addHolidayMapping,
                    onRemoveHolidayMapping = vm::removeHolidayMapping,
                    onOpenResetConfirm = vm::openResetConfirm,
                    onCloseResetConfirm = vm::closeResetConfirm,
                    onResetSchedule = vm::resetSchedule,
                    onRefreshCloud = vm::refreshRemoteConfig,
                    onRefreshWidgets = vm::refreshWidgets,
                    onHolidayAdjustmentsChanged = vm::setHolidayAdjustmentsEnabled,
                    onClassReminderChanged = { enabled ->
                        if (!enabled) {
                            vm.setClassReminderEnabled(false)
                            return@CurriculumScreen
                        }
                        if (SenSenNotifications.canPost(this@MainActivity)) {
                            vm.setClassReminderEnabled(true)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            vm.setClassReminderEnabled(true)
                        }
                    },
                    onWeekFromPager = { week ->
                        if (week != uiState.currentWeek) vm.setWeek(week)
                    },
                    )
                    SettingsSyncOverlay(action = uiState.settingsSyncAction)
                }
            }
        }
    }
}
