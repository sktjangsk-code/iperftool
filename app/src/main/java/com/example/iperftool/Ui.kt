package com.example.iperftool

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private val BgDark = Color(0xFF303030)
private val Panel = Color(0xFF3A3A3A)
private val Track = Color(0xFF4D4D4D)
private val Accent = Color(0xFF0A8FB5)
private val Green = Color(0xFF43A047)
private val Red = Color(0xFFE53935)
private val Muted = Color(0xFFAAAAAA)

@Composable
fun IperfScreen(vm: IperfViewModel = viewModel()) {
    val view = LocalView.current
    DisposableEffect(vm.running) {
        view.keepScreenOn = vm.running
        onDispose { view.keepScreenOn = false }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgDark)
            .systemBarsPadding()
            .imePadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF3F51B5))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text("iPerf Tool", color = Color.White, fontSize = 18.sp)
        }
        TopControls(vm)
        CommandInput(vm)
        TpPanel(vm.tp, parseDuration(vm.args))
        LogView(vm.logs, Modifier.weight(1f).fillMaxWidth())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopControls(vm: IperfViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
            listOf("iPerf2", "iPerf3").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = vm.version == label,
                    onClick = { vm.version = label },
                    enabled = !vm.running,
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Accent,
                        activeContentColor = Color.White,
                        inactiveContainerColor = Color(0xFF4A4A4A),
                        inactiveContentColor = Color(0xFFCCCCCC)
                    )
                ) { Text(label) }
            }
        }
        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
            SegmentedButton(
                selected = !vm.running,
                onClick = { vm.stop() },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Red,
                    activeContentColor = Color.White,
                    inactiveContainerColor = Color(0xFF4A4A4A),
                    inactiveContentColor = Color(0xFFCCCCCC)
                )
            ) { Text("Stop") }
            SegmentedButton(
                selected = vm.running,
                onClick = { vm.start() },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Green,
                    activeContentColor = Color.White,
                    inactiveContainerColor = Color(0xFF4A4A4A),
                    inactiveContentColor = Color(0xFFCCCCCC)
                )
            ) { Text("Start") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandInput(vm: IperfViewModel) {
    var showHistory by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = vm.args,
            onValueChange = { vm.args = it },
            enabled = !vm.running,
            minLines = 2,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Muted,
                focusedBorderColor = Accent,
                unfocusedBorderColor = Color(0xFF666666),
                disabledBorderColor = Color(0xFF555555),
                cursorColor = Accent
            ),
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .size(width = 60.dp, height = 56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Panel)
                .border(0.5.dp, Color(0xFF777777), RoundedCornerShape(6.dp))
                .clickable(
                    enabled = !vm.running,
                    role = Role.Button,
                    onClickLabel = "이전 명령어 보기"
                ) { showHistory = true },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val tint = if (vm.running) Color(0xFF777777) else Color.White
            Icon(Icons.Default.History, contentDescription = "이전 명령어 보기", tint = tint)
            Text("iPerf", color = tint, fontSize = 11.sp)
        }
    }

    if (showHistory) {
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            sheetState = sheetState
        ) {
            HistorySheetContent(
                history = vm.history,
                onSelect = { cmd ->
                    vm.selectHistory(cmd)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showHistory = false }
                },
                onDelete = { vm.deleteHistory(it) },
                onClearAll = { vm.clearHistory() }
            )
        }
    }
}

@Composable
fun HistorySheetContent(
    history: List<String>,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "이전 명령어",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (history.isNotEmpty()) TextButton(onClick = onClearAll) { Text("전체 삭제") }
        }
        HorizontalDivider()

        if (history.isEmpty()) {
            Text(
                "아직 실행한 명령어가 없어요.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn {
                items(history, key = { it }) { cmd ->
                    ListItem(
                        headlineContent = {
                            Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        },
                        trailingContent = {
                            IconButton(onClick = { onDelete(cmd) }) {
                                Icon(Icons.Default.Close, contentDescription = "삭제")
                            }
                        },
                        modifier = Modifier.clickable { onSelect(cmd) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun TpPanel(tp: TpState, totalSec: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .background(Panel, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                if (tp.omitting) "워밍업 중" else "실시간 SUM",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Text("%.0f".format(tp.liveSum), color = Color.White, fontSize = 26.sp)
            Text(" Mbits/sec", color = Muted, fontSize = 13.sp)
        }

        tp.streams.toSortedMap(compareBy<String> { it.toIntOrNull() ?: 0 }).forEach { (id, mbps) ->
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("[$id]", color = Color(0xFFBBBBBB), fontSize = 12.sp, modifier = Modifier.width(34.dp))
                LinearProgressIndicator(
                    progress = { (mbps / 320.0).coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Accent,
                    trackColor = Track
                )
                Text(
                    "%.0f M".format(mbps),
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(62.dp)
                )
            }
        }

        if (totalSec > 0) {
            LinearProgressIndicator(
                progress = { (tp.elapsed / totalSec.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(4.dp),
                color = Green,
                trackColor = Track
            )
        }
    }

    tp.finalSum?.let { sum ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .background(Color(0xFF1B3A22), RoundedCornerShape(8.dp))
                .border(0.5.dp, Green, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text("테스트 완료 - TP SUM", color = Color(0xFF9FD8A8), fontSize = 12.sp)
            Text("%.0f Mbits/sec".format(sum), color = Color.White, fontSize = 24.sp)
        }
    }
}

@Composable
fun LogView(logs: List<String>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val atBottom by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            last == null || last.index >= logs.lastIndex
        }
    }
    var autoFollow by remember { mutableStateOf(true) }

    LaunchedEffect(listState.isScrollInProgress, atBottom) {
        if (listState.isScrollInProgress) autoFollow = atBottom
    }
    LaunchedEffect(logs.size) {
        if (autoFollow && logs.isNotEmpty()) listState.scrollToItem(logs.lastIndex)
    }

    Box(modifier.padding(top = 10.dp)) {
        SelectionContainer {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                items(logs) { line ->
                    Text(
                        text = line,
                        color = if (line.startsWith("[SUM]")) Color(0xFF7FD0EA) else Color(0xFFDDDDDD),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontFamily = FontFamily.Monospace,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }

        if (!autoFollow) {
            SmallFloatingActionButton(
                onClick = {
                    autoFollow = true
                    scope.launch { listState.animateScrollToItem(logs.lastIndex.coerceAtLeast(0)) }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                containerColor = Accent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "맨 아래로")
            }
        }
    }
}
