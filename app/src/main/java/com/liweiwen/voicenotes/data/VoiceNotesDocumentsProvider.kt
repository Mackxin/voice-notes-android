package com.liweiwen.voicenotes.data

import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import com.liweiwen.voicenotes.R
import java.io.File
import java.io.FileNotFoundException

/**
 * 把 App 私有 filesDir 暴露给系统「文件」应用，
 * 这样设置页点击路径时能直接进入「随手说本机数据」目录，而不是停在 Downloads。
 */
class VoiceNotesDocumentsProvider : DocumentsProvider() {
    private lateinit var filesDir: File
    private val recordingsDir: File get() = File(filesDir, "recordings")
    private val notesFile: File get() = File(filesDir, "notes.json")

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        filesDir = ctx.filesDir
        return true
    }

    override fun queryRoots(projection: Array<String>?): Cursor =
        MatrixCursor(ROOT_COLUMNS).apply {
            newRow().apply {
                add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID)
                add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_ID)
                add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD)
                add(DocumentsContract.Root.COLUMN_TITLE, "随手说本机数据")
                add(DocumentsContract.Root.COLUMN_SUMMARY, "记录文件与语音目录")
                add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_launcher)
                add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
                add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, filesDir.freeSpace)
            }
        }

    override fun queryDocument(documentId: String?, projection: Array<String>?): Cursor =
        MatrixCursor(DOCUMENT_COLUMNS).apply {
            val file = fileFor(documentId ?: ROOT_ID) ?: throw FileNotFoundException(documentId)
            addFileRow(this, documentId ?: ROOT_ID, file)
        }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<String>?,
        sortOrder: String?,
    ): Cursor = MatrixCursor(DOCUMENT_COLUMNS).apply {
        when (parentDocumentId) {
            ROOT_ID -> {
                if (notesFile.exists()) addFileRow(this, DOC_NOTES, notesFile)
                if (recordingsDir.exists()) addFileRow(this, DOC_RECORDINGS, recordingsDir)
            }
            DOC_RECORDINGS -> {
                recordingsDir.listFiles { f -> f.isFile && f.name.endsWith(".wav", true) }
                    ?.sortedByDescending { it.lastModified() }
                    ?.forEach { addFileRow(this, "$DOC_AUDIO${it.name}", it) }
            }
        }
    }

    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        val file = fileFor(documentId ?: ROOT_ID) ?: throw FileNotFoundException(documentId)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun openDocumentThumbnail(
        documentId: String?,
        sizeHint: Point?,
        signal: CancellationSignal?,
    ): AssetFileDescriptor? = null

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        val parent = parentDocumentId ?: return false
        val child = documentId ?: return false
        return (parent == ROOT_ID && (child == DOC_NOTES || child == DOC_RECORDINGS)) ||
            (parent == DOC_RECORDINGS && child.startsWith(DOC_AUDIO))
    }

    override fun getDocumentType(documentId: String?): String = when {
        documentId == DOC_NOTES -> "text/plain"
        documentId == DOC_RECORDINGS || documentId == ROOT_ID -> DocumentsContract.Document.MIME_TYPE_DIR
        documentId?.startsWith(DOC_AUDIO) == true -> "audio/wav"
        else -> "*/*"
    }

    private fun fileFor(documentId: String): File? = when {
        documentId == ROOT_ID -> filesDir
        documentId == DOC_NOTES -> notesFile
        documentId == DOC_RECORDINGS -> recordingsDir
        documentId.startsWith(DOC_AUDIO) -> File(recordingsDir, documentId.removePrefix(DOC_AUDIO))
        else -> null
    }

    private fun addFileRow(cursor: MatrixCursor, documentId: String, file: File) {
        val isDir = file.isDirectory
        cursor.newRow().apply {
            add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, documentId)
            add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, if (documentId == DOC_NOTES) "notes.json" else file.name)
            add(DocumentsContract.Document.COLUMN_MIME_TYPE, getDocumentType(documentId))
            add(DocumentsContract.Document.COLUMN_FLAGS, 0)
            add(DocumentsContract.Document.COLUMN_SIZE, if (isDir) 0L else file.length())
            add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified())
        }
    }

    companion object {
        const val AUTHORITY = "com.liweiwen.voicenotes.documents"
        const val ROOT_ID = "root"
        const val DOC_NOTES = "notes"
        const val DOC_RECORDINGS = "recordings"
        const val DOC_AUDIO = "audio:"

        private val ROOT_COLUMNS = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,
        )

        private val DOCUMENT_COLUMNS = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    }
}
