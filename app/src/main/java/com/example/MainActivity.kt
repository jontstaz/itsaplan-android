package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ConnectionStore
import com.example.data.remote.ApiClientManager
import com.example.data.repository.ItsAPlanRepository
import com.example.ui.screens.ConnectScreen
import com.example.ui.screens.MainShell
import com.example.ui.theme.ItsAPlanTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.CyclesViewModel
import com.example.ui.viewmodel.DocsViewModel
import com.example.ui.viewmodel.InboxViewModel
import com.example.ui.viewmodel.InitiativesViewModel
import com.example.ui.viewmodel.IssueDetailViewModel
import com.example.ui.viewmodel.MyIssuesViewModel
import com.example.ui.layout.toAppWindowSize

class MainActivity : ComponentActivity() {

    private lateinit var repository: ItsAPlanRepository

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val connectionStore = ConnectionStore(applicationContext)
        val apiClientManager = ApiClientManager()
        repository = ItsAPlanRepository(
            context = applicationContext,
            store = connectionStore,
            clientManager = apiClientManager
        )

        val vmFactory = AppViewModelFactory(repository, applicationContext)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val appWindowSize = windowSizeClass.widthSizeClass.toAppWindowSize()

            val appViewModel: AppViewModel = viewModel(factory = vmFactory)
            val issueDetailViewModel: IssueDetailViewModel = viewModel(factory = vmFactory)
            val inboxViewModel: InboxViewModel = viewModel(factory = vmFactory)
            val cyclesViewModel: CyclesViewModel = viewModel(factory = vmFactory)
            val initiativesViewModel: InitiativesViewModel = viewModel(factory = vmFactory)
            val myIssuesViewModel: MyIssuesViewModel = viewModel(factory = vmFactory)
            val docsViewModel: DocsViewModel = viewModel(factory = vmFactory)

            val appState by appViewModel.uiState.collectAsStateWithLifecycle()

            ItsAPlanTheme(themeMode = appState.connection.themeMode) {
                when (val auth = appState.authState) {
                    is AuthState.Checking -> {
                        SplashLoadingView()
                    }

                    is AuthState.Unauthenticated -> {
                        ConnectScreen(
                            savedServerUrl = appState.connection.serverUrl,
                            probeResult = appState.probeResult,
                            isProbing = appState.isProbing,
                            isAuthenticating = appState.isAuthenticating,
                            authError = appState.authError,
                            onProbeServer = { appViewModel.probeServer(it) },
                            onSignInApiKey = { url, key -> appViewModel.signInWithApiKey(url, key) },
                            onSignInPassword = { url, id, pw -> appViewModel.signInWithPassword(url, id, pw) }
                        )
                    }

                    is AuthState.Authenticated -> {
                        MainShell(
                            user = auth.user,
                            appState = appState,
                            appViewModel = appViewModel,
                            issueDetailViewModel = issueDetailViewModel,
                            inboxViewModel = inboxViewModel,
                            cyclesViewModel = cyclesViewModel,
                            initiativesViewModel = initiativesViewModel,
                            myIssuesViewModel = myIssuesViewModel,
                            docsViewModel = docsViewModel,
                            windowSize = appWindowSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashLoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(34.dp), strokeWidth = 3.dp)
            Text(
                text = "Connecting to your deployment…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private class AppViewModelFactory(
    private val repository: ItsAPlanRepository,
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AppViewModel::class.java) -> AppViewModel(repository) as T
            modelClass.isAssignableFrom(IssueDetailViewModel::class.java) -> IssueDetailViewModel(repository) as T
            modelClass.isAssignableFrom(InboxViewModel::class.java) -> InboxViewModel(repository) as T
            modelClass.isAssignableFrom(CyclesViewModel::class.java) -> CyclesViewModel(repository) as T
            modelClass.isAssignableFrom(MyIssuesViewModel::class.java) -> MyIssuesViewModel(repository, context.applicationContext as android.app.Application) as T
            modelClass.isAssignableFrom(InitiativesViewModel::class.java) -> InitiativesViewModel(repository) as T
            modelClass.isAssignableFrom(DocsViewModel::class.java) -> DocsViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
