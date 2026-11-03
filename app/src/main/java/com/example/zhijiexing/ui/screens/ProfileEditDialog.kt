package com.example.zhijiexing.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.zhijiexing.AppState
import com.example.zhijiexing.data.AvatarFiles
import com.example.zhijiexing.ui.components.AppTextField
import com.example.zhijiexing.ui.components.Avatar
import com.example.zhijiexing.ui.theme.Line
import com.example.zhijiexing.ui.theme.PrimaryDeep
import com.example.zhijiexing.ui.theme.TextHint
import com.example.zhijiexing.ui.theme.TextMain
import com.example.zhijiexing.ui.theme.TextSub

/**
 * 资料编辑：昵称直接保存到本机；更换头像时先走系统权限弹窗（相册 / 相机），
 * 选中后压缩预览，点保存才归档，避免留下半成品文件。
 */
@Composable
fun ProfileEditDialog(appState: AppState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val username = appState.session?.username ?: return
    var name by remember { mutableStateOf(appState.displayName) }
    var previewPath by remember { mutableStateOf<String?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { AvatarFiles.preparePreview(context, uri) }
                .onSuccess { previewPath = it }
                .onFailure { appState.showMessage("图片读取失败，请换一张试试") }
        }
    }
    val galleryPermissions = remember {
        when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            galleryLauncher.launch("image/*")
        } else {
            appState.showMessage("未获得相册权限，无法选择头像")
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            runCatching { AvatarFiles.preparePreview(context, uri) }
                .onSuccess { previewPath = it }
                .onFailure { appState.showMessage("照片读取失败，请重试") }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            appState.showMessage("未获得相机权限，无法拍照")
            return@rememberLauncherForActivityResult
        }
        runCatching {
            val file = AvatarFiles.newCaptureFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }.onFailure {
            appState.showMessage("相机不可用，请改用相册")
        }
    }

    fun save() {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            appState.showMessage("昵称不能为空")
            return
        }
        var avatarPath: String? = null
        val preview = previewPath
        if (preview != null && preview != appState.avatarPath) {
            avatarPath = runCatching { AvatarFiles.commit(context, username, preview) }.getOrNull()
            if (avatarPath == null) {
                appState.showMessage("头像保存失败，请重试")
                return
            }
        }
        appState.saveProfile(trimmed, avatarPath)
        appState.showMessage("资料已更新")
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.width(302.dp), shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(
                Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("编辑资料", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Box {
                    Avatar(84.dp, previewPath ?: appState.avatarPath)
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrimaryDeep)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("更换头像需授权相册或相机", fontSize = 10.sp, color = TextHint)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SourceButton("从相册选择", Icons.Outlined.PhotoLibrary, Modifier.weight(1f)) {
                        galleryPermissionLauncher.launch(galleryPermissions)
                    }
                    SourceButton("拍照", Icons.Outlined.PhotoCamera, Modifier.weight(1f)) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "昵称",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 11.sp,
                    color = TextSub
                )
                Spacer(Modifier.height(6.dp))
                AppTextField(
                    value = name,
                    onValueChange = { if (it.length <= 16) name = it },
                    placeholder = "给自己起个昵称",
                    leadingIcon = Icons.Outlined.Person
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${name.length}/16",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 10.sp,
                    color = TextHint,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF0F3F9))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("取消", fontSize = 13.sp, color = TextSub)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimaryDeep)
                            .clickable { save() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("保存", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF4F7FC))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PrimaryDeep, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = TextMain)
    }
}
