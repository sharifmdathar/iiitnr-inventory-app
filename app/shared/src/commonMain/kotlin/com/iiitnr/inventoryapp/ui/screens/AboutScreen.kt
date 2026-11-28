package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.ChangelogEntry
import com.iiitnr.inventoryapp.data.ChangelogRepository
import com.iiitnr.inventoryapp.data.GeneratedVersion
import com.iiitnr.inventoryapp.data.checkForUpdate
import com.iiitnr.inventoryapp.shared.Res
import com.iiitnr.inventoryapp.shared.app_icon
import com.iiitnr.inventoryapp.shared.developer
import com.iiitnr.inventoryapp.shared.faculty_in_charge
import com.iiitnr.inventoryapp.ui.components.common.AppTopBar
import com.iiitnr.inventoryapp.ui.components.common.GithubIcon
import com.iiitnr.inventoryapp.ui.components.common.NarrowContentWidth
import com.iiitnr.inventoryapp.ui.components.common.WhatsNewDialog
import com.iiitnr.inventoryapp.ui.components.common.handCursor
import com.iiitnr.inventoryapp.utils.LaunchResult
import com.iiitnr.inventoryapp.utils.createIntentLauncher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val REPO_URL = "https://github.com/sharifmdathar/iiitnr-inventory-app"
private const val DEVELOPER_GITHUB_URL = "https://github.com/sharifmdathar"
private const val FACULTY_PAGE_URL = "https://www.iiitnr.ac.in/content/shailesh-khapre"
private const val REPORT_BUG_URL = "$REPO_URL/issues/new?template=bug_report.yml"
private const val REQUEST_FEATURE_URL = "$REPO_URL/issues/new?template=feature_request.yml"

private const val TEAM_EMAIL = "inventory@iiitnr.ac.in"
private const val DEVELOPER_EMAIL = "md24100@iiitnr.edu.in"
private const val FACULTY_IN_CHARGE_NAME = "Dr. Shailesh Khapre"
private const val DEVELOPER_NAME = "Md Athar Sharif"
private const val DOWNLOAD_URL = "https://inventory.iiitnr.ac.in/download"

private sealed interface UpdateCheck {
    data object Idle : UpdateCheck

    data object Checking : UpdateCheck

    data object UpToDate : UpdateCheck

    data class Available(
        val serverVersion: String,
    ) : UpdateCheck
}

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onUpdateAction: (() -> Unit)? = null,
    viewModel: ProfileViewModel = koinViewModel(),
    changelogRepository: ChangelogRepository = koinInject(),
) {
    val user = viewModel.user
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val intentLauncher = remember { createIntentLauncher() }
    val version = GeneratedVersion.CURRENT_VERSION

    var updateState by remember { mutableStateOf<UpdateCheck>(UpdateCheck.Idle) }

    @Suppress("TooGenericExceptionCaught")
    fun checkNow() {
        scope.launch {
            updateState = UpdateCheck.Checking
            updateState =
                try {
                    checkForUpdate()
                        ?.let(UpdateCheck::Available)
                        ?: UpdateCheck.UpToDate
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    updateState = UpdateCheck.Idle
                    snackbarHostState.showSnackbar("Couldn't check for updates")
                    UpdateCheck.Idle
                }
        }
    }

    fun openLink(url: String) {
        when (val result = intentLauncher.launchUrl(url)) {
            LaunchResult.Launched -> {}
            LaunchResult.NoAppAvailable ->
                scope.launch { snackbarHostState.showSnackbar("Couldn't open a browser for this link") }
            is LaunchResult.Error ->
                scope.launch { snackbarHostState.showSnackbar(result.message) }
        }
    }

    fun performUpdate() {
        if (onUpdateAction != null) {
            onUpdateAction()
        } else {
            openLink(DOWNLOAD_URL)
        }
    }

    LaunchedEffect(Unit) { checkNow() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "About",
                onNavigateBack = onNavigateBack,
                maxContentWidth = NarrowContentWidth,
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppCard(version)
                UpdatesCard(
                    state = updateState,
                    onCheck = ::checkNow,
                    onUpdate = ::performUpdate,
                    changelogRepository = changelogRepository,
                )
                TeamCard(
                    onOpenFacultyPage = { openLink(FACULTY_PAGE_URL) },
                    onOpenDeveloperPage = { openLink(DEVELOPER_GITHUB_URL) },
                )
                GitHubCard { openLink(REPO_URL) }
                FeedbackCard(
                    onReportBug = { openLink(REPORT_BUG_URL) },
                    onRequestFeature = { openLink(REQUEST_FEATURE_URL) },
                    onEmailTeam = { openLink(mailtoFeedbackUrl(TEAM_EMAIL, version, user?.email)) },
                    onEmailDeveloper = {
                        openLink(mailtoFeedbackUrl(DEVELOPER_EMAIL, version, user?.email))
                    },
                )
            }
        }
    }
}

@Composable
private fun UpdatesCard(
    state: UpdateCheck,
    onCheck: () -> Unit,
    onUpdate: () -> Unit,
    changelogRepository: ChangelogRepository,
) {
    var showChangelog by remember { mutableStateOf(false) }
    var changelogEntries by remember { mutableStateOf<List<ChangelogEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        changelogEntries = changelogRepository.entries()
    }

    AboutCard {
        if (state is UpdateCheck.Available) {
            UpdateAvailableRow(state.serverVersion, onUpdate)
        }
        WhatsNewRow(
            onClick = { if (changelogEntries.isNotEmpty()) showChangelog = true },
        )
        if (state !is UpdateCheck.Available) {
            UpdateCheckRow(state, onCheck)
        }
    }

    if (showChangelog) {
        WhatsNewDialog(entries = changelogEntries, onDismiss = { showChangelog = false })
    }
}

@Composable
private fun WhatsNewRow(onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text("What's new") },
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.HistoryEdu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.handCursor().clickable(onClick = onClick),
    )
}

@Composable
private fun UpdateAvailableRow(
    serverVersion: String,
    onUpdate: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = "Update available — v$serverVersion",
                color = MaterialTheme.colorScheme.primary,
            )
        },
        supportingContent = { Text("Tap to update the app") },
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.handCursor().clickable(onClick = onUpdate),
    )
}

@Composable
private fun UpdateCheckRow(
    state: UpdateCheck,
    onCheck: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                when (state) {
                    UpdateCheck.UpToDate -> "You're on the latest version"
                    UpdateCheck.Checking -> "Checking for updates…"
                    else -> "Check for update"
                },
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            if (state is UpdateCheck.Checking) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier =
            Modifier
                .handCursor()
                .clickable(onClick = onCheck, enabled = state !is UpdateCheck.Checking),
    )
}

@Composable
private fun AppCard(version: String) {
    AboutCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = "App icon",
                modifier = Modifier.size(56.dp).clip(CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "IIITNR Inventory App",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Version $version",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TeamCard(
    onOpenFacultyPage: () -> Unit,
    onOpenDeveloperPage: () -> Unit,
) {
    AboutCard {
        Text(
            text = "Team",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        ) {
            PersonColumn(
                name = FACULTY_IN_CHARGE_NAME,
                role = "Faculty in Charge",
                photo = Res.drawable.faculty_in_charge,
                onClick = onOpenFacultyPage,
                modifier = Modifier.weight(1f),
            )
            PersonColumn(
                name = DEVELOPER_NAME,
                role = "Developer",
                photo = Res.drawable.developer,
                onClick = onOpenDeveloperPage,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PersonColumn(
    name: String,
    role: String,
    photo: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .handCursor()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(photo),
            contentDescription = null,
            modifier = Modifier.size(72.dp).clip(CircleShape),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = role,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GitHubCard(onOpenRepository: () -> Unit) {
    AboutCard {
        AboutNavRow(
            label = "GitHub repository",
            supporting = "Check out the project or star the repo",
            icon = GithubIcon,
            onClick = onOpenRepository,
        )
    }
}

@Composable
private fun FeedbackCard(
    onReportBug: () -> Unit,
    onRequestFeature: () -> Unit,
    onEmailTeam: () -> Unit,
    onEmailDeveloper: () -> Unit,
) {
    AboutCard {
        Text(
            text = "Feedback",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )
        AboutNavRow(
            label = "Report a bug",
            supporting = "Opens a GitHub issue template",
            icon = Icons.Outlined.BugReport,
            onClick = onReportBug,
        )
        AboutNavRow(
            label = "Request a feature",
            supporting = "Opens a GitHub issue template",
            icon = Icons.Outlined.Lightbulb,
            onClick = onRequestFeature,
        )
        AboutNavRow(
            label = "Email the team",
            supporting = TEAM_EMAIL,
            icon = Icons.Outlined.Email,
            onClick = onEmailTeam,
        )
        AboutNavRow(
            label = "Email the developer",
            supporting = DEVELOPER_EMAIL,
            icon = Icons.Outlined.Email,
            onClick = onEmailDeveloper,
        )
    }
}

@Composable
private fun AboutCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun AboutNavRow(
    label: String,
    supporting: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(supporting) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.handCursor().clickable(onClick = onClick),
    )
}

private fun mailtoFeedbackUrl(
    email: String,
    version: String,
    userEmail: String?,
): String {
    val subject = "Feedback (app v$version)"
    val body =
        buildString {
            appendLine("App version: $version")
            if (!userEmail.isNullOrBlank()) {
                appendLine("Signed in as: $userEmail")
            }
            appendLine()
            append("Write your feedback here:")
        }
    return "mailto:$email?subject=${encode(subject)}&body=${encode(body)}"
}

private fun encode(value: String): String =
    value
        .replace("%", "%25")
        .replace(" ", "%20")
        .replace("\n", "%0A")
