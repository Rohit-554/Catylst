package io.jadu.catylst.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.jadu.catylst.ui.viewmodel.HomeUiState
import io.jadu.catylst.ui.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (Long, String) -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catylst") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val currentState = state) {
                is HomeUiState.Loading -> {
                    Text("Loading...", style = MaterialTheme.typography.bodyLarge)
                }
                is HomeUiState.Success -> {
                    Text(
                        text = currentState.message,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Button(
                        onClick = { onNavigateToDetail(1, "Sample Detail") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Go to Detail")
                    }

                    OutlinedButton(
                        onClick = onNavigateToPermissions,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Permissions Demo")
                    }

                    OutlinedButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Notifications Demo")
                    }

                    OutlinedButton(
                        onClick = onNavigateToPreferences,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Preferences Demo")
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(currentState.items) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            ) {
                                Text(
                                    text = item,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
                is HomeUiState.Error -> {
                    Text(
                        text = currentState.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
