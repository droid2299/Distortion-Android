package com.music.spotui.data.engine

import app.myspoti.api.LibraryFilter
import app.myspoti.api.LibraryItem
import app.myspoti.api.LibrarySort
import app.myspoti.api.Page
import app.myspoti.api.SavedTrack
import kotlinx.coroutines.flow.Flow

interface DistortionLibrary {
    val changes: Flow<Unit>

    suspend fun contains(uris: List<String>): List<Boolean>
    suspend fun add(uris: List<String>)
    suspend fun remove(uris: List<String>)
    suspend fun items(
        filter: LibraryFilter = LibraryFilter.ALL,
        sort: LibrarySort = LibrarySort.RECENTS,
        folderUri: String? = null,
        offset: Int = 0,
        limit: Int = 50,
    ): Page<LibraryItem>
    suspend fun likedTracks(offset: Int = 0, limit: Int = 50): Page<SavedTrack>
    suspend fun pin(uri: String)
    suspend fun unpin(uri: String)
    suspend fun createFolder(name: String, parentFolderUri: String? = null): String
    suspend fun renameFolder(folderUri: String, name: String)
    suspend fun deleteFolder(folderUri: String)
    suspend fun moveToFolder(uri: String, folderUri: String?)
}
