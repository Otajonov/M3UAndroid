package com.m3u.features.foryou.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.itemsIndexed
import com.m3u.data.database.model.Playlist
import com.m3u.data.database.model.PlaylistWithCount
import com.m3u.i18n.R.string
import com.m3u.material.ktx.plus
import com.m3u.material.model.LocalSpacing
import com.m3u.ui.UiMode
import com.m3u.ui.currentUiMode
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun PlaylistGallery(
    rowCount: Int,
    playlistCounts: ImmutableList<PlaylistWithCount>,
    onClick: (Playlist) -> Unit,
    onLongClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    header: (@Composable () -> Unit)? = null
) {
    when (currentUiMode()) {
        UiMode.Default -> {
            PlaylistGalleryImpl(
                rowCount = rowCount,
                playlistCounts = playlistCounts,
                onClick = onClick,
                onLongClick = onLongClick,
                contentPadding = contentPadding,
                modifier = modifier,
                header = header
            )
        }

        UiMode.Television -> {
            TvPlaylistGalleryImpl(
                rowCount = rowCount,
                playlistCounts = playlistCounts,
                onClick = onClick,
                onLongClick = onLongClick,
                contentPadding = contentPadding,
                modifier = modifier,
                header = header
            )
        }

        UiMode.Compat -> {
            CompactPlaylistGalleryImpl(
                rowCount = rowCount,
                playlistCounts = playlistCounts,
                navigateToPlaylist = onClick,
                onMenu = onLongClick,
                contentPadding = contentPadding,
                modifier = modifier,
                header = header
            )
        }
    }
}

@Composable
private fun PlaylistGalleryImpl(
    rowCount: Int,
    playlistCounts: ImmutableList<PlaylistWithCount>,
    onClick: (Playlist) -> Unit,
    onLongClick: (Playlist) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(rowCount),
        contentPadding = PaddingValues(vertical = spacing.medium) + contentPadding,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        modifier = modifier
    ) {
        if (header != null) {
            item(span = { GridItemSpan(rowCount) }) {
                header()
            }
        }
        itemsIndexed(
            items = playlistCounts,
            key = { _, playlistCount -> playlistCount.playlist.url },
            contentType = { _, _ -> }
        ) { index, playlistCount ->
            PlaylistItem(
                label = PlaylistGalleryDefaults.calculateUiTitle(
                    title = playlistCount.playlist.title,
                    fromLocal = playlistCount.playlist.fromLocal
                ),
                type = playlistCount.playlist.type,
                typeWithSource = playlistCount.playlist.typeWithSource,
                count = playlistCount.count,
                local = playlistCount.playlist.fromLocal,
                onClick = { onClick(playlistCount.playlist) },
                onLongClick = { onLongClick(playlistCount.playlist) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        PlaylistGalleryDefaults.calculateItemHorizontalPadding(
                            rowCount = rowCount,
                            index = index
                        )
                    )
            )
        }
    }
}

@Composable
private fun TvPlaylistGalleryImpl(
    rowCount: Int,
    playlistCounts: ImmutableList<PlaylistWithCount>,
    onClick: (Playlist) -> Unit,
    onLongClick: (Playlist) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    TvLazyVerticalGrid(
        columns = TvGridCells.Fixed(rowCount),
        contentPadding = PaddingValues(vertical = spacing.medium) + contentPadding,
        verticalArrangement = Arrangement.spacedBy(spacing.large),
        horizontalArrangement = Arrangement.spacedBy(spacing.large),
        modifier = modifier
    ) {
        if (header != null) {
            item(span = { TvGridItemSpan(rowCount) }) {
                header()
            }
        }
        itemsIndexed(
            items = playlistCounts,
            key = { _, it -> it.playlist.url }
        ) { index, playlistCount ->
            PlaylistItem(
                label = PlaylistGalleryDefaults.calculateUiTitle(
                    title = playlistCount.playlist.title,
                    fromLocal = playlistCount.playlist.fromLocal
                ),
                type = playlistCount.playlist.type,
                typeWithSource = playlistCount.playlist.typeWithSource,
                count = playlistCount.count,
                local = playlistCount.playlist.fromLocal,
                onClick = { onClick(playlistCount.playlist) },
                onLongClick = { onLongClick(playlistCount.playlist) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        PlaylistGalleryDefaults.calculateItemHorizontalPadding(
                            rowCount = rowCount,
                            index = index,
                            padding = spacing.large
                        )
                    )
            )
        }
    }
}

@Composable
private fun CompactPlaylistGalleryImpl(
    rowCount: Int,
    playlistCounts: ImmutableList<PlaylistWithCount>,
    navigateToPlaylist: (Playlist) -> Unit,
    onMenu: (Playlist) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(rowCount),
        contentPadding = contentPadding,
        modifier = modifier
    ) {
        if (header != null) {
            item(span = { GridItemSpan(rowCount) }) {
                header()
            }
        }
        items(
            items = playlistCounts,
            key = { it.playlist.url },
            contentType = {}
        ) { detail ->
            PlaylistItem(
                label = PlaylistGalleryDefaults.calculateUiTitle(
                    title = detail.playlist.title,
                    fromLocal = detail.playlist.fromLocal
                ),
                type = detail.playlist.type,
                typeWithSource = detail.playlist.typeWithSource,
                count = detail.count,
                local = detail.playlist.fromLocal,
                onClick = { navigateToPlaylist(detail.playlist) },
                onLongClick = { onMenu(detail.playlist) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private object PlaylistGalleryDefaults {
    @Composable
    fun calculateUiTitle(title: String, fromLocal: Boolean): String {
        val actual = title.ifEmpty {
            if (fromLocal) stringResource(string.feat_foryou_imported_playlist_title)
            else ""
        }
        return actual.uppercase()
    }

    @Composable
    fun calculateItemHorizontalPadding(
        rowCount: Int,
        index: Int,
        padding: Dp = LocalSpacing.current.medium
    ): PaddingValues {
        return PaddingValues(
            start = if (index % rowCount == 0) padding else 0.dp,
            end = if (index % rowCount == rowCount - 1) padding else 0.dp
        )
    }
}