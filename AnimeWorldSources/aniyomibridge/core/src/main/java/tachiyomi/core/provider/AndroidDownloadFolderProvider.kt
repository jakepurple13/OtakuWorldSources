package tachiyomi.core.provider

import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import java.io.File

class AndroidDownloadFolderProvider(
    val context: Context,
) : FolderProvider {

    override fun directory(): File {
        return File(
            Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    context.getString(android.R.string.dialog_alert_title),
            "downloads",
        )
    }

    override fun path(): String {
        return directory().toUri().toString()
    }
}
