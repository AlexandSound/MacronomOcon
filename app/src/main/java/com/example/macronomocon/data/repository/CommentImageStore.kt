package com.example.macronomocon.data.repository

/** Uploads a photo the user picked for a comment. */
interface CommentImageStore {
    /** Uploads the image at [localUri] (a `content://` URI) and returns its public URL. */
    suspend fun upload(localUri: String): String
}
