package com.mrdev.tashtech.ui.news

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrdev.tashtech.domain.model.NewsArticle
import com.mrdev.tashtech.ui.components.GlassSurface

/**
 * THE one place where your SwipeableCards plugs in. NewsCard is the card face and has no swipe
 * logic. Until SwipeableCards is wired here, a plain pager keeps the section working.
 */

internal fun reconcileStack(current: List<String>, incoming: List<String>, touched: Boolean): List<String> {
    val wanted = incoming.reversed() // last = top card
    if (!touched) return wanted
    val incomingSet = incoming.toHashSet()
    val kept = current.filter { it in incomingSet }
    val keptSet = kept.toHashSet()
    return wanted.filter { it !in keptSet } + kept
}
private val StackLift = 6.dp
@Composable
fun NewsCardStack(
    articles: List<NewsArticle>,
    onOpen: (NewsArticle) -> Unit,
    onToggleSaved: (NewsArticle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ids = remember(articles) { articles.map { it.id }.distinct() }
    val byId = remember(articles) { articles.associateBy { it.id } } // always the latest article objects
    var order by remember { mutableStateOf(ids.reversed()) }
    var touched by remember { mutableStateOf(false) }

    LaunchedEffect(ids) { order = reconcileStack(order, ids, touched) }

    val shown = order.filter { it in byId } // a removed article never renders, even for the frame before reconcile
    val stackExtra = if (shown.size > 1) StackLift * (shown.size - 1) else 0.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(NewsCardHeight + stackExtra)
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        shown.forEachIndexed { index, id ->
            key(id) {
                val article = byId.getValue(id)
                val isTop = index == shown.size - 1

                    SwipeableCard(
                        order = index,
                        totalCount = shown.size,
                        enabled = true,
                        onMoveToBack = {
                            touched = true
                            order =
                                listOf(id) + (order - id) // reads the current state, never a captured copy
                        },
                    ) {
                        GlassSurface(){
                        NewsCard(
                            article = article,
                            onClick = { onOpen(article) }, // the article for this key, even after a refresh
                            onToggleSaved = { onToggleSaved(article) },
                            loadImage = index >= shown.size - 2,
                        )
                    }
                }
            }
        }
    }
}