package razek.fuchoquiniela.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import razek.fuchoquiniela.R
import razek.fuchoquiniela.data.ApiError
import razek.fuchoquiniela.data.AppLink
import razek.fuchoquiniela.data.AppLocale
import razek.fuchoquiniela.data.DateText
import razek.fuchoquiniela.data.FuchoApi
import razek.fuchoquiniela.data.Post
import razek.fuchoquiniela.data.SITE_URL
import razek.fuchoquiniela.ui.components.AlertHost
import razek.fuchoquiniela.ui.components.AlertState
import razek.fuchoquiniela.ui.components.EmptyState
import razek.fuchoquiniela.ui.components.FuchoCard
import razek.fuchoquiniela.ui.components.FuchoTopBar
import razek.fuchoquiniela.ui.components.gutter
import razek.fuchoquiniela.ui.components.LoadingBox
import razek.fuchoquiniela.ui.components.openWeb
import razek.fuchoquiniela.ui.rememberLoader
import razek.fuchoquiniela.ui.theme.FuchoColors
import razek.fuchoquiniela.ui.theme.Spacing

/**
 * "Noticias" tab: the site's blog (the web's /blog), loaded a page at a time; a post opens in a
 * native screen that renders its HTML body in the app's colors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(onOpenPost: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alerts = remember { AlertState() }
    val title = stringResource(R.string.tab_news)
    val posts = remember { mutableStateListOf<Post>() }
    var page by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var loadedOnce by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    fun loadNextPage(replacing: Boolean = false) {
        if (loading || (!replacing && !hasMore)) return
        loading = true
        scope.launch {
            try {
                val response = FuchoApi.posts(if (replacing) 1 else page + 1)
                if (replacing) posts.clear()
                val known = posts.map { it.id }.toSet()
                posts.addAll(response.posts.filter { it.id !in known })
                page = response.page
                hasMore = response.hasMore
                loadedOnce = true
            } catch (e: ApiError) {
                alerts.show(context, e, title)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { if (!loadedOnce) loadNextPage(replacing = true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to listState.layoutInfo.totalItemsCount }
            .collect { (last, total) -> if (last != null && total > 0 && last >= total - 3) loadNextPage() }
    }

    Scaffold(containerColor = FuchoColors.background, topBar = { FuchoTopBar(title) }) { padding ->
        PullToRefreshBox(
            isRefreshing = loading && loadedOnce && page <= 1,
            onRefresh = { loadNextPage(replacing = true) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                !loadedOnce && posts.isEmpty() -> if (loading) LoadingBox()
                posts.isEmpty() -> EmptyState(
                    Icons.Outlined.Newspaper,
                    stringResource(R.string.no_news),
                    stringResource(R.string.no_news_message),
                )
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = gutter, vertical = Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Spacing.l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(posts, key = { it.id }) { post -> NewsCard(post) { onOpenPost(post.id) } }
                }
            }
        }
    }
    AlertHost(alerts)
}

/** Image on top (16:9), date, title and excerpt, like `.news-card` on the web. */
@Composable
private fun NewsCard(post: Post, onClick: () -> Unit) {
    FuchoCard(Modifier.widthIn(max = 680.dp), onClick = onClick) {
        val image = post.imageUrl ?: post.thumbnailUrl
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(FuchoColors.inset),
            )
        }
        Column(Modifier.padding(Spacing.l)) {
            post.published?.let {
                Text(
                    DateText.long(it).uppercase(AppLocale.locale()),
                    color = FuchoColors.lime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(Spacing.xs))
            }
            Text(post.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (post.excerpt.isNotBlank()) {
                Spacer(Modifier.height(Spacing.s))
                Text(post.excerpt, color = FuchoColors.textBody, fontSize = 15.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A post rendered from its HTML body. Links open in a Custom Tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailScreen(id: Int, onBack: () -> Unit, onAppLink: (AppLink, String) -> Unit) {
    val context = LocalContext.current
    val alerts = remember { AlertState() }
    val title = stringResource(R.string.tab_news)
    val loader = rememberLoader(id, onError = { alerts.show(context, it, title) }) { FuchoApi.post(id).post }
    LaunchedEffect(id) { loader.load() }
    val post = loader.value

    Scaffold(
        containerColor = FuchoColors.background,
        topBar = {
            FuchoTopBar(post?.title ?: "", onBack = onBack) {
                if (post != null && post.url.isNotEmpty()) {
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, post.title)
                            putExtra(Intent.EXTRA_TEXT, "${post.title} ${post.url}")
                        }
                        context.startActivity(Intent.createChooser(send, null))
                    }) {
                        Icon(Icons.Filled.Share, stringResource(R.string.share))
                    }
                }
            }
        },
    ) { padding ->
        if (post == null) {
            if (loader.loading) LoadingBox(Modifier.padding(padding))
        } else {
            val html = remember(post) { render(context, post) }
            PostWebView(html, Modifier.fillMaxSize().padding(padding), onAppLink)
        }
    }
    AlertHost(alerts)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PostWebView(html: String, modifier: Modifier, onAppLink: (AppLink, String) -> Unit) {
    val currentOnAppLink by rememberUpdatedState(onAppLink)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#1E2024"))
                // Embedded videos and tweets in posts need scripts.
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    // The page itself loads in place. Tapped links to pages the app has (the pool
                    // list, a pool, Partidos, Tabla, another post…) open those screens; the rest
                    // open in a Custom Tab.
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (!request.hasGesture()) return false
                        val url = request.url
                        val appLink = AppLink.parse(url.toString())
                        if (appLink != null) {
                            currentOnAppLink(appLink, url.toString())
                        } else if (url.scheme == "http" || url.scheme == "https") {
                            openWeb(view.context, url.toString())
                        } else {
                            runCatching { view.context.startActivity(Intent(Intent.ACTION_VIEW, url)) }
                        }
                        return true
                    }
                }
            }
        },
        update = { it.loadDataWithBaseURL(SITE_URL, html, "text/html", "utf-8", null) },
    )
}

private fun escape(text: String) = text
    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

private fun render(context: android.content.Context, post: Post): String {
    val image = post.imageUrl?.let { "<img class=\"hero\" src=\"${escape(it)}\" alt=\"\">" } ?: ""
    val meta = listOfNotNull(post.published?.let(DateText::long), post.author?.let { context.getString(R.string.by_author, it) })
        .joinToString(" · ")
    val link = post.link?.takeIf { it.isNotEmpty() }
        ?.let { "<p><a class=\"button\" href=\"${escape(it)}\">${escape(context.getString(R.string.read_full_story))}</a></p>" } ?: ""
    return """
        <!doctype html><html lang="${AppLocale.languageCode()}"><head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
        :root { color-scheme: dark; }
        body { margin: 0; padding: 0 20px 40px; background: #1e2024; color: #c5c8cc;
               font-family: sans-serif; font-size: 17px; line-height: 1.6; }
        .hero { display: block; width: calc(100% + 40px); margin: 0 -20px 20px; aspect-ratio: 16 / 9; object-fit: cover; background: #31404b; }
        .meta { color: #c2ff1f; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: .04em; margin-top: 20px; }
        h1 { color: #fff; font-size: 28px; font-weight: 800; line-height: 1.2; margin: 6px 0 16px; }
        h2, h3 { color: #fff; line-height: 1.3; }
        a { color: #38a9ff; }
        img { max-width: 100%; height: auto; border-radius: 8px; }
        iframe { max-width: 100%; }
        blockquote { border-left: 3px solid #38a9ff; margin: 16px 0; padding: 2px 14px; color: #9a9da2; }
        table { border-collapse: collapse; width: 100%; } td, th { border-bottom: 1px solid rgba(255,255,255,.08); padding: 6px; }
        .button { display: inline-block; margin-top: 8px; padding: 12px 18px; border-radius: 10px; background: #38a9ff;
                  color: #fff; font-weight: 700; text-decoration: none; }
        </style></head><body>
        $image
        <div class="meta">${escape(meta)}</div>
        <h1>${escape(post.title)}</h1>
        ${post.bodyHtml ?: ""}
        $link
        </body></html>
    """.trimIndent()
}

@Suppress("unused")
private fun Uri.isWeb() = scheme == "http" || scheme == "https"
