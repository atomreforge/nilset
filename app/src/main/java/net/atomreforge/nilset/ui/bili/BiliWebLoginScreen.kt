package net.atomreforge.nilset.ui.bili

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.atomreforge.nilset.R
import net.atomreforge.nilset.bili.auth.BiliLoginLevel
import net.atomreforge.nilset.ui.theme.themeContainerBorderColor
import net.atomreforge.nilset.ui.theme.themeContainerColor

private const val BILI_LOGIN_URL = "https://passport.bilibili.com/login"

@Composable
fun BiliWebLoginScreen(
    onNavigateBack: () -> Unit,
    viewModel: BiliVideoViewModel = hiltViewModel(),
) {
    val loginState by viewModel.biliLoginState.collectAsStateWithLifecycle()
    val avatar by viewModel.biliAvatar.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var isClosingWithImport by remember { mutableStateOf(false) }

    fun closeWithImport() {
        if (isClosingWithImport) {
            return
        }
        isClosingWithImport = true
        coroutineScope.launch {
            viewModel.importBiliWebViewCookies()
            onNavigateBack()
        }
    }

    BackHandler {
        closeWithImport()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = ::closeWithImport,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = stringResource(R.string.bili_nil_login_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (loginState.isValidating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, themeContainerBorderColor()),
            color = themeContainerColor(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = when (loginState.level) {
                        BiliLoginLevel.LOGGED_OUT -> stringResource(R.string.bili_nil_login_logged_out)
                        BiliLoginLevel.NORMAL_USER -> stringResource(R.string.bili_nil_login_normal_user)
                        BiliLoginLevel.VIP_MEMBER -> stringResource(R.string.bili_nil_login_vip_member)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                loginState.nickname?.let { nickname ->
                    Text(
                        text = nickname,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (avatar != null) {
                    BiliAvatarImage(avatar = avatar!!)
                }
            }
        }
        BiliLoginWebView(
            modifier = Modifier.weight(1f),
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BiliLoginWebView(
    modifier: Modifier = Modifier,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.destroy()
        }
    }

    AndroidView(
        factory = { context ->
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.allowFileAccess = false
            webView.settings.allowContentAccess = false
            webView.settings.userAgentString = webView.settings.userAgentString.replace("; wv", "")
            webView.webViewClient = WebViewClient()
            webView.webChromeClient = WebChromeClient()
            val cookieManager = android.webkit.CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
            webView.loadUrl(BILI_LOGIN_URL)
            webView.also { webViewRef = it }
        },
        modifier = modifier,
    )
}

@Composable
private fun BiliAvatarImage(avatar: androidx.compose.ui.graphics.ImageBitmap) {
    Image(
        bitmap = avatar,
        contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = Modifier
            .size(44.dp),
    )
}
