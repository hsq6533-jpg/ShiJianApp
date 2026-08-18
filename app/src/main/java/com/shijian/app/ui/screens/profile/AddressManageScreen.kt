package com.shijian.app.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijian.app.AppContainer
import com.shijian.app.data.db.entity.SearchAddressEntity
import com.shijian.app.ui.components.EmptyState
import com.shijian.app.ui.components.SjCard
import com.shijian.app.ui.components.SubPageTopBar
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.Danger500
import com.shijian.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/** 地址管理（设计稿：地址管理 + PRD 5.4） */
@Composable
fun AddressManageScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val addresses by container.addressRepo.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())

    var editing by remember { mutableStateOf<SearchAddressEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<SearchAddressEntity?>(null) }

    val toast: (String) -> Unit = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }

    Column(modifier = Modifier.fillMaxWidth()) {
        SubPageTopBar(
            title = "地址管理",
            onBack = onBack,
            actions = {
                IconButton(onClick = { adding = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "新增地址", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            if (addresses.isEmpty()) {
                EmptyState(
                    emoji = "📍",
                    title = "还没有常用地址",
                    subtitle = "点击右上角 + 添加家、公司等常用地址，用于美食搜索"
                )
            } else {
                SjCard(modifier = Modifier.fillMaxWidth()) {
                    addresses.forEachIndexed { i, a ->
                        if (i > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                        AddressItem(
                            item = a,
                            onSetDefault = {
                                scope.launch {
                                    container.addressRepo.setDefault(a)
                                    toast("已将「${a.name}」设为默认地址")
                                }
                            },
                            onEdit = { editing = a },
                            onDelete = { deleting = a }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(13.dp))
                        .clickable { adding = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("＋ 新增地址", style = MaterialTheme.typography.labelLarge, color = Brand500)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "时笺手机版 · 纯本地运行",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (adding) {
        EditAddressDialog(
            title = "新增地址",
            initial = null,
            onDismiss = { adding = false },
            onSave = { name, addr ->
                scope.launch {
                    container.addressRepo.add(name, addr, null, null)
                    toast("地址已添加")
                }
                adding = false
            }
        )
    }

    editing?.let { a ->
        EditAddressDialog(
            title = "编辑地址",
            initial = a,
            onDismiss = { editing = null },
            onSave = { name, addr ->
                scope.launch { container.addressRepo.update(a.copy(name = name, address = addr)) }
                editing = null
            }
        )
    }

    deleting?.let { a ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除地址？") },
            text = { Text("确定删除「${a.name}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { container.addressRepo.delete(a) }
                    deleting = null
                }) { Text("删除", color = Danger500) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun AddressItem(
    item: SearchAddressEntity,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (item.isDefault) {
                Text(
                    text = "默认",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Brand500, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.address,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            if (!item.isDefault) {
                SmallAction("设为默认", primary = true, onClick = onSetDefault)
            }
            SmallAction("编辑", primary = false, onClick = onEdit)
            SmallAction("删除", primary = false, danger = true, onClick = onDelete)
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun SmallAction(text: String, primary: Boolean, onClick: () -> Unit, danger: Boolean = false) {
    Box(
        modifier = Modifier
            .background(
                if (primary) MaterialTheme.colorScheme.primary
                else if (danger) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 12.sp,
            color = when {
                primary -> Color.White
                danger -> Danger500
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun EditAddressDialog(
    title: String,
    initial: SearchAddressEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(10) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("名称，如：家 / 公司") },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("详细地址") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && address.isNotBlank()) onSave(name.trim(), address.trim())
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
