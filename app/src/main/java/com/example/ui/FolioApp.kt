package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.FolioDatabase
import com.example.data.local.OfflineBookRepository
import com.example.data.local.PreferencesRepository
import com.example.data.remote.FolioFirestoreRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutFolio
import com.example.ui.circles.CirclesScreen
import com.example.ui.components.ConfirmPdfUploadDialog
import com.example.ui.components.OpenBookLinkDialog
import com.example.ui.components.ShareBookLinkDialog
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.reader.ReaderScreen
import com.example.ui.theme.FolioTheme
import com.example.util.BookCommentNotifier
import com.example.util.ImportedPdfMetadata
import com.example.util.NetworkMonitor
import com.example.util.PdfBookImporter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_LIBRARY = "library"
const val ROUTE_CIRCLES = "circles"

private data class FolioNavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val folioDestinations = listOf(
    FolioNavDestination(
        route = ROUTE_DASHBOARD,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    FolioNavDestination(
        route = ROUTE_LIBRARY,
        labelRes = R.string.nav_library,
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
        testTag = "nav_library"
    ),
    FolioNavDestination(
        route = ROUTE_CIRCLES,
        labelRes = R.string.nav_circles,
        selectedIcon = Icons.Filled.Group,
        unselectedIcon = Icons.Outlined.Group,
        testTag = "nav_circles"
    )
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun FolioApp(
    incomingDeepLink: String?,
    onDeepLinkHandled: () -> Unit
) {
    val context = LocalContext.current
    val prefsRepo = remember(context) { PreferencesRepository(context.applicationContext) }
    val prefs by prefsRepo.preferences.collectAsStateWithLifecycle(
        initialValue = com.example.data.local.ReaderPreferences()
    )

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    FolioTheme(nightMode = prefs.nightMode) {
        val user = currentUser
        if (user == null) {
            AuthScreen(onAuthSuccess = {})
        } else {
            key(user.uid) {
                AuthenticatedFolioShell(
                    currentUserId = user.uid,
                    displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Reader",
                    preferencesRepository = prefsRepo,
                    incomingDeepLink = incomingDeepLink,
                    onDeepLinkHandled = onDeepLinkHandled
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedFolioShell(
    currentUserId: String,
    displayName: String,
    preferencesRepository: PreferencesRepository,
    incomingDeepLink: String?,
    onDeepLinkHandled: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val offlineRepository = remember(context) {
        OfflineBookRepository(FolioDatabase.getInstance(context).offlineDao())
    }

    val viewModel: FolioViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FolioViewModel(
                    appContext = app,
                    firestoreRepository = FolioFirestoreRepository(db),
                    offlineRepository = OfflineBookRepository(FolioDatabase.getInstance(app).offlineDao()),
                    preferencesRepository = preferencesRepository,
                    networkMonitor = NetworkMonitor(app),
                    currentUserId = currentUserId,
                    displayName = displayName
                )
            }
        }
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(currentUserId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !BookCommentNotifier.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val uiState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val shareCircle by viewModel.shareDialogCircle.collectAsStateWithLifecycle()
    val pendingOpenBookId by viewModel.pendingOpenBookId.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    var activeReaderBookId by remember { mutableStateOf<String?>(null) }
    var activeReaderChapter by remember { mutableIntStateOf(0) }
    var showOpenLinkDialog by remember { mutableStateOf(false) }
    var pendingImportedPdf by remember { mutableStateOf<ImportedPdfMetadata?>(null) }
    var isImportingPdf by remember { mutableStateOf(false) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImportingPdf = true
            scope.launch {
                val result = PdfBookImporter.importPdf(context, uri)
                isImportingPdf = false
                result.onSuccess { meta ->
                    pendingImportedPdf = meta
                }.onFailure { err ->
                    viewModel.showStatusMessage(
                        err.localizedMessage ?: "Could not import selected PDF"
                    )
                }
            }
        }
    }

    LaunchedEffect(incomingDeepLink) {
        if (!incomingDeepLink.isNullOrBlank()) {
            viewModel.handleIncomingDeepLink(incomingDeepLink)
            onDeepLinkHandled()
        }
    }

    LaunchedEffect(pendingOpenBookId) {
        val target = pendingOpenBookId
        if (!target.isNullOrBlank()) {
            activeReaderBookId = target
            activeReaderChapter = 0
            viewModel.consumePendingBookId()
        }
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    if (shareCircle != null) {
        ShareBookLinkDialog(
            circle = shareCircle!!,
            onDismiss = { viewModel.dismissShareDialog() },
            onCopied = { msg ->
                viewModel.dismissShareDialog()
                scope.launchWithBanner(snackbarHostState, msg)
            }
        )
    }

    if (showOpenLinkDialog) {
        OpenBookLinkDialog(
            onDismiss = { showOpenLinkDialog = false },
            onSubmitLink = { rawLink ->
                viewModel.openOrJoinBookLink(rawLink) { bookId ->
                    activeReaderBookId = bookId
                    activeReaderChapter = 0
                }
            }
        )
    }

    pendingImportedPdf?.let { importedMeta ->
        ConfirmPdfUploadDialog(
            meta = importedMeta,
            onDismiss = {
                PdfBookImporter.deleteLocalPdf(importedMeta.localFilePath)
                pendingImportedPdf = null
            },
            onConfirm = { customTitle, customAuthor ->
                val metaToSave = importedMeta
                pendingImportedPdf = null
                viewModel.importPdfBook(
                    meta = metaToSave,
                    customTitle = customTitle,
                    customAuthor = customAuthor
                ) { bookId ->
                    activeReaderBookId = bookId
                    activeReaderChapter = 0
                }
            }
        )
    }

    when (val state = uiState) {
        is UiState.Loading -> {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.statusBars
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Preparing your library…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is UiState.Error -> {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.statusBars
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Could not load your shelf",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                signOutFolio(credentialManager, scope)
                            }
                        ) {
                            Text("Sign Out & Retry")
                        }
                    }
                }
            }
        }

        is UiState.Success -> {
            val data = state.data
            val readerBookItem = activeReaderBookId?.let { id ->
                data.books.firstOrNull { it.book.id == id }
            }

            if (readerBookItem != null) {
                val activeCircle = data.circles.firstOrNull { it.circle.bookId == readerBookItem.book.id }
                Box(modifier = Modifier.fillMaxSize()) {
                    ReaderScreen(
                        bookItem = readerBookItem,
                        initialChapterIndex = activeReaderChapter,
                        preferences = data.preferences,
                        isOnline = data.isOnline,
                        offlineRepository = offlineRepository,
                        currentUserId = currentUserId,
                        circleWithMembers = activeCircle,
                        onSaveChapterProgress = { chapterIdx, minutesDelta, markCompleted ->
                            viewModel.saveChapterProgress(
                                book = readerBookItem.book,
                                chapterIndex = chapterIdx,
                                minutesDelta = minutesDelta,
                                markCompleted = markCompleted
                            )
                        },
                        onSaveBookmark = { chapterIdx, note ->
                            viewModel.saveBookmark(readerBookItem.book, chapterIdx, note)
                        },
                        onShareBook = { book -> viewModel.createOrShareBookLink(book) },
                        onToggleOffline = { book, offline -> viewModel.toggleOfflineBook(book, offline) },
                        onSelectReaderTheme = { theme -> viewModel.setReaderTheme(theme) },
                        onToggleAppNightMode = { viewModel.toggleNightMode() },
                        onAdjustFontSize = { delta -> viewModel.adjustFontSize(delta) },
                        onSetLineSpacing = { lvl -> viewModel.setLineSpacing(lvl) },
                        onSetUseSerif = { serif -> viewModel.setUseSerifFont(serif) },
                        onPostComment = { chapterIdx, body, quoteText, parent ->
                            viewModel.postBookComment(
                                book = readerBookItem.book,
                                chapterIndex = chapterIdx,
                                body = body,
                                quoteText = quoteText,
                                parentComment = parent,
                                preferredCircleId = activeCircle?.circle?.id
                            )
                        },
                        onDeleteComment = { circleId, commentId ->
                            viewModel.deleteBookComment(circleId, commentId)
                        },
                        onMarkCommentsRead = { circleId ->
                            viewModel.markCircleCommentsRead(circleId)
                        },
                        onBack = { activeReaderBookId = null }
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 64.dp)
                    )
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useNavRail = maxWidth >= 600.dp
                    val navigateTab: (String) -> Unit = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = if (useNavRail) WindowInsets.statusBars else WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            if (!useNavRail) {
                                NavigationBar(
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    folioDestinations.forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTab(dest.route) },
                                            icon = {
                                                BadgedBox(
                                                    badge = {
                                                        if (dest.route == ROUTE_CIRCLES && data.totalUnreadComments > 0) {
                                                            Badge { Text("${data.totalUnreadComments}") }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                        contentDescription = stringResource(dest.labelRes)
                                                    )
                                                }
                                            },
                                            label = { Text(stringResource(dest.labelRes)) },
                                            modifier = Modifier.testTag(dest.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useNavRail) {
                                NavigationRail(
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    folioDestinations.forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { navigateTab(dest.route) },
                                            icon = {
                                                BadgedBox(
                                                    badge = {
                                                        if (dest.route == ROUTE_CIRCLES && data.totalUnreadComments > 0) {
                                                            Badge { Text("${data.totalUnreadComments}") }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                        contentDescription = stringResource(dest.labelRes)
                                                    )
                                                }
                                            },
                                            label = { Text(stringResource(dest.labelRes)) },
                                            modifier = Modifier.testTag(dest.testTag)
                                        )
                                    }
                                }
                            }

                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_DASHBOARD,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            ) {
                                composable(ROUTE_DASHBOARD) {
                                    DashboardScreen(
                                        data = data,
                                        displayName = displayName,
                                        currentUserId = currentUserId,
                                        onOpenBook = { book, chapter ->
                                            activeReaderBookId = book.id
                                            activeReaderChapter = chapter
                                        },
                                        onShareBook = { book -> viewModel.createOrShareBookLink(book) },
                                        onOpenBookLinkClick = { showOpenLinkDialog = true },
                                        onToggleNightMode = { viewModel.toggleNightMode() },
                                        onUpdateDailyGoal = { min -> viewModel.updateDailyGoal(min) },
                                        onNavigateToLibrary = { navigateTab(ROUTE_LIBRARY) },
                                        onNavigateToCircles = { navigateTab(ROUTE_CIRCLES) },
                                        onUploadPdfClick = {
                                            if (!isImportingPdf) {
                                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                            }
                                        },
                                        onSignOut = { signOutFolio(credentialManager, scope) }
                                    )
                                }
                                composable(ROUTE_LIBRARY) {
                                    LibraryScreen(
                                        data = data,
                                        onOpenBook = { book, chapter ->
                                            activeReaderBookId = book.id
                                            activeReaderChapter = chapter
                                        },
                                        onToggleOffline = { book, offline ->
                                            viewModel.toggleOfflineBook(book, offline)
                                        },
                                        onDownloadAllOffline = { viewModel.downloadAllOffline() },
                                        onShareBook = { book -> viewModel.createOrShareBookLink(book) },
                                        onUploadPdfClick = {
                                            if (!isImportingPdf) {
                                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                            }
                                        },
                                        onDeleteUploadedPdf = { bookId ->
                                            viewModel.deleteUploadedPdf(bookId)
                                        },
                                        onToggleNightMode = { viewModel.toggleNightMode() }
                                    )
                                }
                                composable(ROUTE_CIRCLES) {
                                    CirclesScreen(
                                        data = data,
                                        currentUserId = currentUserId,
                                        onOpenBook = { book, chapter ->
                                            activeReaderBookId = book.id
                                            activeReaderChapter = chapter
                                        },
                                        onShareBook = { book -> viewModel.createOrShareBookLink(book) },
                                        onJoinBookLink = { raw ->
                                            viewModel.openOrJoinBookLink(raw) { bookId ->
                                                activeReaderBookId = bookId
                                                activeReaderChapter = 0
                                            }
                                        },
                                        onUpdateNote = { circle, note ->
                                            viewModel.updateCircleNote(circle, note)
                                        },
                                        onPostComment = { book, chapterIdx, body, quoteText, parent, circleId ->
                                            viewModel.postBookComment(book, chapterIdx, body, quoteText, parent, circleId)
                                        },
                                        onDeleteComment = { circleId, commentId ->
                                            viewModel.deleteBookComment(circleId, commentId)
                                        },
                                        onMarkCommentsRead = { circleId ->
                                            viewModel.markCircleCommentsRead(circleId)
                                        },
                                        onLeaveCircle = { circleId ->
                                            viewModel.leaveCircle(circleId)
                                        },
                                        onCopiedMessage = { msg ->
                                            scope.launchWithBanner(snackbarHostState, msg)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchWithBanner(
    snackbarHostState: SnackbarHostState,
    message: String
) {
    launch {
        snackbarHostState.showSnackbar(message)
    }
}
