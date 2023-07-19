package tachiyomi.core.provider

import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import eu.kanade.tachiyomi.core.R
import java.io.File

class AndroidDownloadFolderProvider(
    val context: Context,
) : FolderProvider {

    override fun directory(): File {
        return File(
            Environment.getExternalStorageDirectory().absolutePath + File.separator +
                "",
            "downloads",
        )
    }

    override fun path(): String {
        return directory().toUri().toString()
    }
}
