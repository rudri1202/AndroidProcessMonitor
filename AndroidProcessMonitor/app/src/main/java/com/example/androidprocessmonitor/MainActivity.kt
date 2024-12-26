package com.example.androidprocessmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidprocessmonitor.ui.theme.AndroidProcessMonitorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class ProcessInfo(
    val pid: String,
    val name: String,
    val cpuUsage: String,
    val memoryUsage: String
)

data class SystemStats(
    val totalCpuUsage: Float,
    val totalMemoryUsage: Float,
    val cacheMemoryUsage: Float
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidProcessMonitorTheme {
                Surface {
                    ProcessMonitorScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessMonitorScreen() {
    var processList by remember { mutableStateOf(emptyList<ProcessInfo>()) }
    var systemStats by remember { mutableStateOf(SystemStats(0f, 0f,0f)) }
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            processList = getProcessList()
            systemStats = getSystemStats()

            delay(5000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Process Monitor", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SystemStatsCard(systemStats)
            Spacer(modifier = Modifier.height(16.dp))
            SearchBar(searchQuery) { newQuery ->
                searchQuery = newQuery
            }
            Spacer(modifier = Modifier.height(16.dp))
            ProcessList(
                processList = processList.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                },
                onKillProcess = { pid ->
                    coroutineScope.launch {
                        killProcess(pid)
                        processList = processList.filter { it.pid != pid }
                    }
                }
            )
        }
    }
}

@Composable
fun SystemStatsCard(stats: SystemStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                "System Resources",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "CPU Usage",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "${String.format("%.1f", stats.totalCpuUsage)}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Memory Usage",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "${String.format("%.1f", stats.totalMemoryUsage)}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Cache Usage",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "${String.format("%.1f", stats.cacheMemoryUsage)}MB",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search Processes") },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        colors = TextFieldDefaults.textFieldColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun ProcessList(processList: List<ProcessInfo>, onKillProcess: (String) -> Unit) {
    if (processList.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                "No processes found or root access denied.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(processList) { process ->
                ProcessItem(process, onKillProcess)
            }
        }
    }
}

@Composable
fun ProcessItem(process: ProcessInfo, onKillProcess: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = process.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "PID: ${process.pid}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "CPU: ${process.cpuUsage}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Memory: ${process.memoryUsage}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            FilledTonalButton(
                onClick = { onKillProcess(process.pid) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Kill")
            }
        }
    }
}


private suspend fun readCpuStats(): List<Long> = withContext(Dispatchers.IO) {
    try {
        val statReader = Runtime.getRuntime().exec("su -c cat /proc/stat").inputStream.bufferedReader()
        val cpuLine = statReader.readLine()
        statReader.close()

        cpuLine.split("\\s+".toRegex())
            .drop(1) // Drop "cpu" prefix
            .mapNotNull { it.toLongOrNull() }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

private fun calculateCpuUsage(first: List<Long>, second: List<Long>): Float {
    if (first.size < 4 || second.size < 4) return 0f

    // Calculate deltas for each value
    val idle = second[3] - first[3]
    val total = second.sum() - first.sum()

    if (total == 0L) return 0f

    // Calculate CPU usage percentage
    return ((total - idle).toFloat() / total.toFloat() * 100).coerceIn(0f, 100f)
}
suspend fun getSystemStats(): SystemStats = withContext(Dispatchers.IO) {
    var totalCpuUsage = 0f
    var totalMemoryUsage = 0f
    var cacheMemoryUsage = 0f

    try {
        // Get CPU usage by reading /proc/stat
        val firstSnapshot = readCpuStats()
        delay(1000) // Wait for 1 second to get a meaningful difference
        val secondSnapshot = readCpuStats()

        totalCpuUsage = calculateCpuUsage(firstSnapshot, secondSnapshot)

        // Get memory usage
        val memProcess = Runtime.getRuntime().exec("su -c free")
        val memReader = BufferedReader(InputStreamReader(memProcess.inputStream))
        memReader.readLine() // Skip header
        val memLine = memReader.readLine()
        val memInfo = memLine?.split("\\s+".toRegex())
        if (memInfo != null && memInfo.size >= 3) {
            val total = memInfo[1].toFloatOrNull() ?: 0f
            val used = memInfo[2].toFloatOrNull() ?: 0f
            if (total > 0) {
                totalMemoryUsage = (used / total) * 100
            }
        }

        memReader.close()

        // Get cache usage
        cacheMemoryUsage = getCacheStats()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    SystemStats(totalCpuUsage, totalMemoryUsage, cacheMemoryUsage)
}

suspend fun getCacheStats(): Float = withContext(Dispatchers.IO) {
    try {
        // Execute the cat /proc/meminfo command
        val process = Runtime.getRuntime().exec("su -c cat /proc/meminfo")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val memInfoLines = reader.readLines()
        reader.close()

        // Extract cache-related stats
        val cacheStats = memInfoLines.firstOrNull { it.startsWith("Cached:") }
            ?.split("\\s+".toRegex()) // Split line by whitespace
            ?.getOrNull(1) // Get the second element, which is the value
            ?.toLongOrNull() // Convert to Long (kilobytes)

        // Convert to MB as Float
        cacheStats?.let { it / 1024f } ?: 0f
    } catch (e: Exception) {
        e.printStackTrace()
        0f
    }
}

suspend fun getProcessList(): List<ProcessInfo> = withContext(Dispatchers.IO) {
    val processList = mutableListOf<ProcessInfo>()
    try {
        val process = Runtime.getRuntime().exec("su -c ps")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        reader.readLine() // Skip the header
        reader.forEachLine { line ->
            val columns = line.trim().split("\\s+".toRegex())
            if (columns.size >= 9) {
                val pid = columns[1]
                val name = columns.last()
                val cpuUsage = columns[2]
                val memoryUsage = columns[3]
                processList.add(ProcessInfo(pid, name, cpuUsage, memoryUsage))
            }
        }
        reader.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    processList
}

suspend fun killProcess(pid: String) = withContext(Dispatchers.IO) {
    try {
        Runtime.getRuntime().exec("su -c kill $pid")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}