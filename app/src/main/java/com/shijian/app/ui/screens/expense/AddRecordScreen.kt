package com.shijian.app.ui.screens.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijian.app.AppContainer
import com.shijian.app.data.db.entity.TransactionEntity
import com.shijian.app.ui.components.SubPageTopBar
import com.shijian.app.ui.components.SwitchRow
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.Danger500
import com.shijian.app.ui.theme.MilkTeaGradientEnd
import com.shijian.app.ui.theme.MilkTeaGradientStart
import com.shijian.app.ui.theme.Success500
import com.shijian.app.ui.theme.TextSecondary
import com.shijian.app.util.DateUtils
import com.shijian.app.util.FormatUtils
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val TYPE_CHIPS = listOf("支出", "收入", "待报销")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreen(
    container: AppContainer,
    editId: Long?,
    initialType: String,
    initialDate: String = "",
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val defaultDate = remember {
        initialDate.takeIf { it.isNotBlank() }
            ?.let { runCatching { DateUtils.parseYmd(it) }.getOrNull() }
            ?: today
    }

    var typeChip by rememberSaveableCompat(initialType.ifEmpty { "支出" })
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var amountText by rememberSaveableCompat("")
    var remark by rememberSaveableCompat("")
    var merchant by rememberSaveableCompat("")
    var reimbursable by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var date by remember { mutableStateOf(defaultDate) }
    var time by remember { mutableStateOf(LocalTime.now()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    // 分类（Flow 容错：数据库异常不崩）
    val dbType = if (typeChip == "收入") "INCOME" else "EXPENSE"
    val categories by remember(dbType) {
        container.database.categoryDao().observeByType(dbType)
            .catch { emit(emptyList()) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    if (selectedCategory == null && categories.isNotEmpty()) {
        selectedCategory = categories.first().name
    }

    // 编辑模式回填（任何异常不崩，最多不回填）
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    if (editId != null && editing == null) {
        LaunchedEffect(editId) {
            runCatching {
                val entity = container.transactionRepo.byId(editId).firstOrNull()
                if (entity != null && editing == null) {
                    typeChip = if (entity.isReimbursable) "待报销" else if (entity.type == "INCOME") "收入" else "支出"
                    selectedCategory = entity.category
                    amountText = FormatUtils.amount(entity.amount)
                    remark = entity.remark
                    merchant = entity.merchant
                    reimbursable = entity.isReimbursable
                    date = runCatching { DateUtils.parseYmd(entity.date) }.getOrDefault(defaultDate)
                    time = runCatching { LocalTime.parse(entity.time) }.getOrDefault(LocalTime.now())
                    editing = entity
                }
            }
        }
    }

    // 奶茶杯数提示（容错）
    var milkTeaCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedCategory, date) {
        runCatching {
            if (selectedCategory == "奶茶") {
                milkTeaCount = container.transactionRepo.milkTeaCountOn(DateUtils.ymd(date)) + 1
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SubPageTopBar(
            title = if (editId != null) "编辑记录" else "记一笔",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // 类型 chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                TYPE_CHIPS.forEach { chip ->
                    val sel = chip == typeChip
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (sel) MaterialTheme.colorScheme.surface else Color.Transparent,
                                RoundedCornerShape(9.dp)
                            )
                            .clickable {
                                typeChip = chip
                                selectedCategory = null
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelLarge,
                            color = when {
                                sel && chip == "收入" -> Success500
                                sel && chip == "待报销" -> Brand500
                                sel -> Danger500
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            // 金额
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() || it == '.' }
                    if (clean.count { it == '.' } <= 1 && clean.length <= 10) {
                        amountText = clean
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                placeholder = { Text("0.00", fontSize = 30.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Text("¥", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Brand500,
                    cursorColor = Brand500
                ),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))
            // 分类网格 4 列
            Text(text = "分类", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyRow {
                items(chunkedCategories(categories)) { rowItems ->
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { c ->
                            val sel = c.name == selectedCategory
                            Column(
                                modifier = Modifier
                                    .background(
                                        if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedCategory = c.name }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = c.icon, fontSize = 22.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = c.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (sel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 奶茶提示
            if (selectedCategory == "奶茶") {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(MilkTeaGradientStart.copy(alpha = 0.15f), MilkTeaGradientEnd.copy(alpha = 0.15f))
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "☕", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "已标记为今日第 $milkTeaCount 杯奶茶",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            // 时间 / 备注 / 商家
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .clickable { showDatePicker = true }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "时间", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${DateUtils.dateLabel(DateUtils.ymd(date))}  ${String.format("%02d:%02d", time.hour, time.minute)}  ›",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = remark,
                onValueChange = { remark = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") },
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("商家 / 来源") },
                singleLine = true
            )

            // 待报销开关（仅支出）
            if (typeChip != "收入") {
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    icon = "🧾",
                    label = "这笔是待报销",
                    subtitle = "将计入首页与开销页的待报销统计",
                    checked = reimbursable || typeChip == "待报销",
                    onCheckedChange = { reimbursable = it }
                )
            }

            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(
                        if (saving) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = !saving) {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount <= 0 || selectedCategory == null) {
                            showError = true
                            return@clickable
                        }
                        saving = true
                        scope.launch {
                            val type = if (typeChip == "收入") "INCOME" else "EXPENSE"
                            val reimbursable = typeChip == "待报销" || reimbursable
                            val entity = TransactionEntity(
                                id = editId ?: editing?.id ?: 0,
                                type = type,
                                amount = amount,
                                category = selectedCategory ?: "其他",
                                remark = remark.trim(),
                                merchant = merchant.trim(),
                                date = DateUtils.ymd(date),
                                time = String.format("%02d:%02d", time.hour, time.minute),
                                isReimbursable = reimbursable,
                                // 编辑已报销记录时保留报销状态；不再是待报销则重置
                                isReimbursed = if (reimbursable) editing?.isReimbursed == true else false,
                                isMilkTea = selectedCategory == "奶茶"
                            )
                            if (entity.id == 0L) container.transactionRepo.insert(entity)
                            else container.transactionRepo.update(entity)
                            saving = false
                            onBack()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "保 存",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 17.sp
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        date = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        SjTimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                time = LocalTime.of(timeState.hour, timeState.minute)
                showTimePicker = false
            }
        ) {
            TimePicker(state = timeState)
        }
    }

    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("无法保存") },
            text = { Text("请先输入金额并选择分类") },
            confirmButton = {
                TextButton(onClick = { showError = false }) { Text("好的") }
            }
        )
    }
}

private fun chunkedCategories(categories: List<com.shijian.app.data.db.entity.CategoryEntity>): List<List<com.shijian.app.data.db.entity.CategoryEntity>> =
    categories.chunked(4)

@Composable
private fun rememberSaveableCompat(initial: String) =
    androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(initial) }

@Composable
private fun SjTimePickerDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("选择时间") },
        text = { content() }
    )
}
