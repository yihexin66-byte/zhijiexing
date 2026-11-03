package com.example.zhijiexing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhijiexing.AppState
import com.example.zhijiexing.data.ApiException
import com.example.zhijiexing.ui.components.AppCard
import com.example.zhijiexing.ui.components.AppTextField
import com.example.zhijiexing.ui.components.DecoBackground
import com.example.zhijiexing.ui.components.LogoBadge
import com.example.zhijiexing.ui.components.PrimaryButton
import com.example.zhijiexing.ui.theme.Danger
import com.example.zhijiexing.ui.theme.DangerSoft
import com.example.zhijiexing.ui.theme.PrimaryDeep
import com.example.zhijiexing.ui.theme.TextHint
import com.example.zhijiexing.ui.theme.TextMain
import com.example.zhijiexing.ui.theme.TextSub
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(appState: AppState) {
    var registering by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun validate(): String? {
        val name = username.trim()
        if (name.length !in 3..16) return "用户名需为 3-16 位字母、数字或下划线"
        if (!name.all { it.isLetterOrDigit() || it == '_' || it == '.' }) return "用户名只能包含字母、数字、下划线"
        if (password.length !in 6..16) return "密码需为 6-16 位"
        if (registering && confirm != password) return "两次输入的密码不一致"
        return null
    }

    fun submit() {
        val problem = validate()
        if (problem != null) {
            error = problem
            return
        }
        error = null
        loading = true
        scope.launch {
            val result = if (registering) {
                appState.register(username, password, confirm)
            } else {
                appState.login(username, password)
            }
            loading = false
            result.onFailure { throwable ->
                error = when (throwable) {
                    is ApiException -> throwable.message ?: "请求失败"
                    else -> throwable.message ?: "网络连接失败，请稍后重试"
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        DecoBackground()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 26.dp)
        ) {
            Spacer(Modifier.height(58.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoBadge(58.dp, 18.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("智解星", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Text("让刷题更简单", fontSize = 13.sp, color = TextSub)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "刷题、解析与成长记录，\n陪你一步步走到认证通过。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextHint,
                lineHeight = 21.sp
            )
            Spacer(Modifier.height(26.dp))

            AppCard(padding = 20.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF3F6FC))
                        .padding(4.dp)
                ) {
                    listOf("登录" to false, "注册" to true).forEach { (label, mode) ->
                        val selected = registering == mode
                        Box(
                            Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (selected) Color.White else Color.Transparent)
                                .clickable {
                                    if (registering != mode) {
                                        registering = mode
                                        error = null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) PrimaryDeep else TextHint
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("账号", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSub)
                Spacer(Modifier.height(7.dp))
                AppTextField(
                    value = username,
                    onValueChange = { if (it.length <= 20) username = it },
                    placeholder = "请输入用户名",
                    leadingIcon = Icons.Outlined.Person,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(14.dp))
                Text("密码", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSub)
                Spacer(Modifier.height(7.dp))
                AppTextField(
                    value = password,
                    onValueChange = { if (it.length <= 20) password = it },
                    placeholder = if (registering) "设置 6-16 位密码" else "请输入密码",
                    leadingIcon = Icons.Outlined.Lock,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (registering) ImeAction.Next else ImeAction.Done
                    ),
                    trailing = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                tint = TextHint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
                if (registering) {
                    Spacer(Modifier.height(14.dp))
                    Text("确认密码", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSub)
                    Spacer(Modifier.height(7.dp))
                    AppTextField(
                        value = confirm,
                        onValueChange = { if (it.length <= 20) confirm = it },
                        placeholder = "再次输入密码",
                        leadingIcon = Icons.Outlined.Lock,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                    )
                }

                error?.let { message ->
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DangerSoft)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Danger)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(message, fontSize = 12.sp, color = Danger, lineHeight = 17.sp)
                    }
                }

                Spacer(Modifier.height(22.dp))
                PrimaryButton(
                    text = if (registering) "注册并登录" else "登录",
                    onClick = { submit() },
                    loading = loading,
                    enabled = username.isNotBlank() && password.isNotBlank() && (!registering || confirm.isNotBlank())
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (registering) "注册成功后会自动登录并同步学习数据" else "登录后可同步刷题记录、错题与证书",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = TextHint
                )
            }

            Spacer(Modifier.height(26.dp))
            Text(
                "智解星 · 智能刷题与学习成长助手",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                color = TextHint.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

