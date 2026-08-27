package io.github.ethan2258.motootaidentity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupMenuState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

private enum class ProfileSection(val title: String) {
    BASIC("基础"),
    BUILD("构建"),
    MOTOROLA("Motorola"),
}

private data class FieldSpec(
    val key: String,
    val label: String,
    val section: ProfileSection,
    val placeholder: String = "",
    val multiline: Boolean = false,
)

private enum class NoticeKind { SAFE, INFO, ERROR, ACTIVE }

private data class UiNotice(val text: String, val kind: NoticeKind)

private val fieldSpecs = listOf(
    FieldSpec("profileName", "配置名称", ProfileSection.BASIC, "例如：RETEU donor"),
    FieldSpec("carrier", "软件通道", ProfileSection.BASIC, "reteu"),
    FieldSpec("model", "设备型号", ProfileSection.BASIC, "真实 donor 设备型号"),
    FieldSpec("fingerprint", "Build fingerprint", ProfileSection.BASIC, multiline = true),
    FieldSpec("otaSourceSha1", "OTA source SHA-1", ProfileSection.BASIC, "ro.mot.build.guid"),
    FieldSpec("securityVersion", "安全补丁", ProfileSection.BASIC, "YYYY-MM-DD"),

    FieldSpec("buildDevice", "Build device", ProfileSection.BUILD),
    FieldSpec("buildId", "Build ID", ProfileSection.BUILD),
    FieldSpec("buildDisplayId", "显示版本", ProfileSection.BUILD),
    FieldSpec("buildIncrementalVersion", "增量版本", ProfileSection.BUILD),
    FieldSpec("releaseVersion", "Android 版本", ProfileSection.BUILD),
    FieldSpec("bootloaderVersion", "Bootloader 版本", ProfileSection.BUILD),
    FieldSpec("radioVersion", "基带版本", ProfileSection.BUILD),

    FieldSpec("canonicalName", "Canonical product name", ProfileSection.MOTOROLA),
    FieldSpec("ro.mot.build.device", "Motorola build device", ProfileSection.MOTOROLA),
    FieldSpec("ro.mot.build.oem.product", "OEM product", ProfileSection.MOTOROLA),
    FieldSpec("ro.mot.build.system.product", "System product", ProfileSection.MOTOROLA),
    FieldSpec("ro.mot.build.product.increment", "Product increment", ProfileSection.MOTOROLA),
    FieldSpec("ro.mot.version", "Motorola 版本", ProfileSection.MOTOROLA),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MotoOtaApp() }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MotoOtaApp() {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else if (dark) {
            darkColorScheme(primary = Color(0xFF82D5C7), tertiary = Color(0xFFFFB4A8))
        } else {
            lightColorScheme(primary = Color(0xFF006B5E), tertiary = Color(0xFF9C4234))
        }

    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = zeroLetterSpacingTypography(),
    ) {
        ProfileScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(ProfileContract.PREFS, Context.MODE_PRIVATE) }
    val values = remember {
        mutableStateMapOf<String, String>().apply {
            ProfileContract.FIELDS.forEach { put(it, "") }
            put("userLocation", "Non-CN")
        }
    }
    var enabled by rememberSaveable { mutableStateOf(false) }
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var notice by remember {
        mutableStateOf(UiNotice("覆盖关闭，OTA 查询保持原样", NoticeKind.SAFE))
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val requiredKeys = remember { ProfileContract.REQUIRED.toSet() }
    val missingKeys by remember {
        derivedStateOf { requiredKeys.filter { values[it].isNullOrBlank() } }
    }
    val requiredCount = requiredKeys.size
    val completedCount = requiredCount - missingKeys.size

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun profileJson(): JSONObject = JSONObject().also { json ->
        ProfileContract.FIELDS.forEach { key -> json.put(key, values[key].orEmpty().trim()) }
    }

    fun populate(json: JSONObject) {
        ProfileContract.FIELDS.forEach { key -> values[key] = json.optString(key, "") }
        if (values["userLocation"] !in setOf("CN", "Non-CN")) values["userLocation"] = "Non-CN"
    }

    fun validateProfile(): Boolean {
        showErrors = true
        val error = ProfileValidator.validate(profileJson())
        notice = if (error == null) {
            UiNotice("配置完整，可以保存；安装校验不会被绕过", NoticeKind.INFO)
        } else {
            UiNotice(error, NoticeKind.ERROR)
        }
        showMessage(error ?: "配置检查通过")
        return error == null
    }

    fun saveProfile() {
        val profile = profileJson()
        if (enabled) {
            showErrors = true
            val error = ProfileValidator.validate(profile)
            if (error != null) {
                notice = UiNotice("未启用：$error", NoticeKind.ERROR)
                showMessage(error)
                return
            }
        }
        prefs.edit()
            .putString(ProfileContract.PREF_JSON, profile.toString())
            .putBoolean(ProfileContract.PREF_ENABLED, enabled)
            .apply()
        notice = if (enabled) {
            UiNotice("身份覆盖已启用，仅对 Motorola OTA 查询生效", NoticeKind.ACTIVE)
        } else {
            UiNotice("配置已保存，覆盖仍然关闭", NoticeKind.SAFE)
        }
        showMessage("已保存")
    }

    fun disableOverride() {
        enabled = false
        prefs.edit().putBoolean(ProfileContract.PREF_ENABLED, false).apply()
        notice = UiNotice("覆盖已关闭，OTA 查询保持原样", NoticeKind.SAFE)
        showMessage("覆盖已关闭")
    }

    LaunchedEffect(Unit) {
        enabled = prefs.getBoolean(ProfileContract.PREF_ENABLED, false)
        try {
            populate(JSONObject(prefs.getString(ProfileContract.PREF_JSON, "{}") ?: "{}"))
            notice = if (enabled) {
                UiNotice("身份覆盖已启用，仅对 Motorola OTA 查询生效", NoticeKind.ACTIVE)
            } else {
                UiNotice("覆盖关闭，OTA 查询保持原样", NoticeKind.SAFE)
            }
        } catch (_: Exception) {
            enabled = false
            prefs.edit().putBoolean(ProfileContract.PREF_ENABLED, false).apply()
            notice = UiNotice("保存的 JSON 无法解析，覆盖保持关闭", NoticeKind.ERROR)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Moto OTA Identity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Shield, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ActionBar(
                onValidate = { validateProfile() },
                onSave = { saveProfile() },
                onDisable = { disableOverride() },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "intro") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "OTA 查询身份",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "只修改 com.motorola.ccc.ota 发出的设备选择 JSON。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item(key = "status") {
                StatusPanel(
                    notice = notice,
                    completed = completedCount,
                    total = requiredCount,
                )
            }

            item(key = "switch") {
                OverrideSwitch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        notice = if (it) {
                            UiNotice("尚未保存；启用前会检查所有必要字段", NoticeKind.INFO)
                        } else {
                            UiNotice("尚未保存；保存后覆盖将关闭", NoticeKind.SAFE)
                        }
                    },
                )
            }

            item(key = "safety") { SafetyNotice() }

            item(key = "quick-title") { SectionLabel("配置数据") }
            item(key = "quick-actions") {
                ButtonGroup(
                    overflowIndicator = { ButtonGroupOverflowIndicator(it) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    clickableItem(
                        onClick = {
                            fillCurrentDevice(values)
                            enabled = false
                            showErrors = false
                            notice = UiNotice("已填入本机参考值；请勿把它当作 RETEU donor", NoticeKind.INFO)
                            showMessage("已填入本机参考值")
                        },
                        label = "本机",
                        icon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
                        weight = 1f,
                    )
                    clickableItem(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                            if (text.isNullOrBlank()) {
                                showMessage("剪贴板为空")
                            } else {
                                try {
                                    populate(JSONObject(text))
                                    enabled = false
                                    showErrors = false
                                    notice = UiNotice("JSON 已导入但尚未启用", NoticeKind.INFO)
                                    showMessage("JSON 已导入")
                                } catch (error: Exception) {
                                    notice = UiNotice("JSON 格式错误", NoticeKind.ERROR)
                                    showMessage("JSON 格式错误：${error.message.orEmpty()}")
                                }
                            }
                        },
                        label = "导入",
                        icon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                        weight = 1f,
                    )
                    clickableItem(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard.setPrimaryClip(ClipData.newPlainText("Moto OTA profile", profileJson().toString()))
                            showMessage("当前 JSON 已复制")
                        },
                        label = "复制",
                        icon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                        weight = 1f,
                    )
                }
            }

            item(key = "tabs") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ProfileSection.entries.forEachIndexed { index, section ->
                        SegmentedButton(
                            selected = selectedSection == index,
                            onClick = {
                                selectedSection = index
                                showErrors = false
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, ProfileSection.entries.size),
                            label = { Text(section.title, maxLines = 1) },
                        )
                    }
                }
            }

            item(key = "section-heading") {
                val section = ProfileSection.entries[selectedSection]
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionLabel(section.title)
                    Text(
                        when (section) {
                            ProfileSection.BASIC -> "设备选择的核心字段与来源版本"
                            ProfileSection.BUILD -> "Android Build 与基带信息"
                            ProfileSection.MOTOROLA -> "Motorola 产品映射字段"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (selectedSection == ProfileSection.BASIC.ordinal) {
                item(key = "userLocation") {
                    LocationSelector(
                        value = values["userLocation"].orEmpty(),
                        onValueChange = { values["userLocation"] = it },
                    )
                }
            }

            items(
                items = fieldSpecs.filter { it.section.ordinal == selectedSection },
                key = { it.key },
            ) { field ->
                ProfileField(
                    spec = field,
                    value = values[field.key].orEmpty(),
                    required = field.key in requiredKeys,
                    showError = showErrors && field.key in missingKeys,
                    onValueChange = { values[field.key] = it },
                )
            }

            item(key = "footer-divider") {
                HorizontalDivider(Modifier.padding(top = 8.dp))
            }
            item(key = "footer") {
                Text(
                    "服务器响应、OTA 签名、分区哈希、回滚保护和 update_engine 校验始终保持原样。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusPanel(notice: UiNotice, completed: Int, total: Int) {
    val scheme = MaterialTheme.colorScheme
    val colors = when (notice.kind) {
        NoticeKind.SAFE -> scheme.secondaryContainer to scheme.onSecondaryContainer
        NoticeKind.INFO -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        NoticeKind.ERROR -> scheme.errorContainer to scheme.onErrorContainer
        NoticeKind.ACTIVE -> scheme.primaryContainer to scheme.onPrimaryContainer
    }
    Surface(
        color = colors.first,
        contentColor = colors.second,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LoadingIndicator(
                progress = { if (total == 0) 0f else completed.toFloat() / total },
                modifier = Modifier.size(44.dp),
                color = colors.second,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "$completed / $total 个必要字段",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(notice.text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun OverrideSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Security, contentDescription = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("查询身份覆盖", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (checked) "保存后在限定作用域内生效" else "当前保持原始 OTA 请求",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SafetyNotice() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            "所有字段必须来自同一台真实 RETEU donor。资料不完整时模块会拒绝启用。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun LocationSelector(value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("区域判定", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Non-CN" to "非中国区", "CN" to "中国区").forEachIndexed { index, option ->
                SegmentedButton(
                    selected = value == option.first,
                    onClick = { onValueChange(option.first) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                    label = { Text(option.second) },
                )
            }
        }
    }
}

@Composable
private fun ProfileField(
    spec: FieldSpec,
    value: String,
    required: Boolean,
    showError: Boolean,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 512) onValueChange(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(if (required) "${spec.label} *" else spec.label) },
        placeholder = if (spec.placeholder.isBlank()) null else ({ Text(spec.placeholder) }),
        supportingText = {
            Text(if (showError) "必要字段不能为空" else spec.key)
        },
        isError = showError,
        singleLine = !spec.multiline,
        minLines = if (spec.multiline) 2 else 1,
        maxLines = if (spec.multiline) 4 else 1,
        keyboardOptions = KeyboardOptions(imeAction = if (spec.multiline) ImeAction.Default else ImeAction.Next),
        shape = RoundedCornerShape(8.dp),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionBar(onValidate: () -> Unit, onSave: () -> Unit, onDisable: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
        ButtonGroup(
            overflowIndicator = { ButtonGroupOverflowIndicator(it) },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            clickableItem(
                onClick = onValidate,
                label = "检查",
                icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
                weight = 1f,
            )
            clickableItem(
                onClick = onSave,
                label = "保存",
                icon = { Icon(Icons.Rounded.Save, contentDescription = null) },
                weight = 1f,
            )
            clickableItem(
                onClick = onDisable,
                label = "关闭",
                icon = { Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null) },
                weight = 1f,
            )
        }
    }
}

@Composable
private fun ButtonGroupOverflowIndicator(state: ButtonGroupMenuState) {
    FilledIconButton(onClick = { if (state.isExpanded) state.dismiss() else state.show() }) {
        Icon(Icons.Rounded.MoreVert, contentDescription = "更多操作")
    }
}

private fun fillCurrentDevice(values: MutableMap<String, String>) {
    values["profileName"] = "current-device-reference"
    values["carrier"] = "reteu"
    values["model"] = Build.MODEL
    values["fingerprint"] = Build.FINGERPRINT
    values["bootloaderVersion"] = Build.BOOTLOADER
    values["radioVersion"] = Build.getRadioVersion().orEmpty()
    values["buildDevice"] = Build.DEVICE
    values["buildId"] = Build.ID
    values["buildDisplayId"] = Build.DISPLAY
    values["buildIncrementalVersion"] = Build.VERSION.INCREMENTAL
    values["releaseVersion"] = Build.VERSION.RELEASE
    values["otaSourceSha1"] = readProperty("ro.mot.build.guid")
    values["userLocation"] = "Non-CN"
    values["canonicalName"] = readProperty("ro.product.name.canonical")
    values["ro.mot.build.device"] = readProperty("ro.mot.build.device")
    values["ro.mot.build.oem.product"] = readProperty("ro.mot.build.oem.product")
    values["ro.mot.build.system.product"] = readProperty("ro.mot.build.system.product")
    values["ro.mot.build.product.increment"] = readProperty("ro.mot.build.product.increment")
    values["ro.mot.version"] = readProperty("ro.mot.version")
    values["securityVersion"] = Build.VERSION.SECURITY_PATCH
}

private fun readProperty(key: String): String = try {
    val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
    BufferedReader(InputStreamReader(process.inputStream)).use { it.readLine()?.trim().orEmpty() }
} catch (_: Exception) {
    ""
}

@Composable
private fun zeroLetterSpacingTypography(): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(letterSpacing = 0.sp),
        displayMedium = base.displayMedium.copy(letterSpacing = 0.sp),
        displaySmall = base.displaySmall.copy(letterSpacing = 0.sp),
        headlineLarge = base.headlineLarge.copy(letterSpacing = 0.sp),
        headlineMedium = base.headlineMedium.copy(letterSpacing = 0.sp),
        headlineSmall = base.headlineSmall.copy(letterSpacing = 0.sp),
        titleLarge = base.titleLarge.copy(letterSpacing = 0.sp),
        titleMedium = base.titleMedium.copy(letterSpacing = 0.sp),
        titleSmall = base.titleSmall.copy(letterSpacing = 0.sp),
        bodyLarge = base.bodyLarge.copy(letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.copy(letterSpacing = 0.sp),
        bodySmall = base.bodySmall.copy(letterSpacing = 0.sp),
        labelLarge = base.labelLarge.copy(letterSpacing = 0.sp),
        labelMedium = base.labelMedium.copy(letterSpacing = 0.sp),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.sp),
    )
}
