package ren.hieu.sensenapp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import ren.hieu.sensenapp.notification.ClassReminderScheduler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
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

        setContent {
            SenSenSchoolTheme {
                val vm: ScheduleViewModel = viewModel(factory = factory)
                val uiState by vm.uiState.collectAsState()
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    vm.refreshForCurrentDate(returnToToday = true)
                    ren.hieu.sensenapp.widget.WidgetRefreshWorker.enqueue(applicationContext)
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(lifecycleOwner) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        while (true) {
                            vm.refreshForCurrentDate()
                            delay(30_000)
                        }
                    }
                }
                val updatePrompt by vm.updatePrompt.collectAsStateWithLifecycle()
                var reminderPermission by remember { mutableStateOf<ReminderPermission?>(null) }
                fun enableReminder() {
                    vm.setClassReminderEnabled(true)
                    reminderPermission = when {
                        !SenSenNotifications.canPostClassReminder(this@MainActivity) -> ReminderPermission.Notifications
                        !ClassReminderScheduler.canScheduleExact(this@MainActivity) -> ReminderPermission.ExactAlarm
                        else -> null
                    }
                }
                var openedNotificationSettings by remember { mutableStateOf(false) }
                val reminderSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    // 从通知设置返回后继续检查准时提醒权限；取消授权时不反复弹窗。
                    if (openedNotificationSettings && uiState.classReminderEnabled && SenSenNotifications.canPostClassReminder(this@MainActivity) &&
                        !ClassReminderScheduler.canScheduleExact(this@MainActivity)) {
                        reminderPermission = ReminderPermission.ExactAlarm
                    }
                }
                LaunchedEffect(uiState.classReminderEnabled) {
                    if (uiState.classReminderEnabled) {
                        reminderPermission = when {
                            !SenSenNotifications.canPostClassReminder(this@MainActivity) -> ReminderPermission.Notifications
                            !ClassReminderScheduler.canScheduleExact(this@MainActivity) -> ReminderPermission.ExactAlarm
                            else -> null
                        }
                    }
                }
                val notificationPermission = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) enableReminder()
                    else {
                        vm.setClassReminderEnabled(true)
                        reminderPermission = ReminderPermission.Notifications
                    }
                }

                reminderPermission?.let { permission ->
                    AlertDialog(
                        onDismissRequest = { reminderPermission = null },
                        title = { Text(if (permission == ReminderPermission.ExactAlarm) "允许准时提醒" else "允许课前通知") },
                        text = { Text(if (permission == ReminderPermission.ExactAlarm)
                            "请在系统设置中允许“闹钟和提醒”，以便在课前准时通知。未允许时，提醒可能延迟。"
                            else "系统通知或“课前提醒”通知频道已关闭，请在系统设置中开启。") },
                        confirmButton = {
                            TextButton(onClick = {
                                val settingsIntent = if (permission == ReminderPermission.ExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !SenSenNotifications.canPost(this@MainActivity)) {
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                        putExtra(Settings.EXTRA_CHANNEL_ID, SenSenNotifications.CHANNEL_CLASS)
                                    }
                                } else {
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                                }
                                openedNotificationSettings = permission == ReminderPermission.Notifications
                                reminderPermission = null
                                runCatching { reminderSettings.launch(settingsIntent) }
                                    .onFailure { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
                            }) { Text("去设置") }
                        },
                        dismissButton = { TextButton(onClick = { reminderPermission = null }) { Text("稍后") } },
                    )
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            enableReminder()
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

private enum class ReminderPermission { Notifications, ExactAlarm }
