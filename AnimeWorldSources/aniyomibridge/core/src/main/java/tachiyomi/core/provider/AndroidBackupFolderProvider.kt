package tachiyomi.core.provider

import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import java.io.File

class AndroidBackupFolderProvider(
    private val context: Context,
) : FolderProvider {

    override fun directory(): File {
        return File(
            Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    context.getString(tachiyomi.i18n.R.string.app_name),
            "backup",
        )
    }

    override fun path(): String {
        return directory().toUri().toString()
    }
}
