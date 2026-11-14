package com.music.spotui.data.engine

import app.myspoti.api.Library
import app.myspoti.api.LibraryFilter
import app.myspoti.api.LibrarySort

internal class ExistingLibrary(private val current: () -> Library) : DistortionLibrary {
    override val changes get() = current().changes

    override suspend fun contains(uris: List<String>) = current().contains(uris)
    override suspend fun add(uris: List<String>) = current().add(uris)
    override suspend fun remove(uris: List<String>) = current().remove(uris)
    override suspend fun items(
        filter: LibraryFilter,
        sort: LibrarySort,
        folderUri: String?,
        offset: Int,
        limit: Int,
    ) = current().items(filter, sort, folderUri, offset, limit)

    override suspend fun likedTracks(offset: Int, limit: Int) = current().likedTracks(offset, limit)
    override suspend fun pin(uri: String) = current().pin(uri)
    override suspend fun unpin(uri: String) = current().unpin(uri)
    override suspend fun createFolder(name: String, parentFolderUri: String?) = current().createFolder(name, parentFolderUri)
    override suspend fun renameFolder(folderUri: String, name: String) = current().renameFolder(folderUri, name)
    override suspend fun deleteFolder(folderUri: String) = current().deleteFolder(folderUri)
    override suspend fun moveToFolder(uri: String, folderUri: String?) = current().moveToFolder(uri, folderUri)
}
