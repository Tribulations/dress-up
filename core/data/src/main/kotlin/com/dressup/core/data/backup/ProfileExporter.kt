package com.dressup.core.data.backup

import androidx.room.withTransaction
import com.dressup.core.data.DressUpData
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Writes one profile as `profile.yaml` plus `images/` in a zip (ARCHITECTURE.md §7.8). Blocking: call off the main thread. */
internal object ProfileExporter {
    const val YAML_ENTRY = "profile.yaml"
    const val IMAGES_DIR = "images/"

    suspend fun export(data: DressUpData, profileName: String, appVersion: String?, out: OutputStream, now: Instant): ExportResult.Done {
        val file = read(data, profileName, appVersion, now)
        val pictures = (file.garments.flatMap { listOfNotNull(it.photo, it.cutout) } + file.outfits.mapNotNull { it.backgroundImage })
            .distinct() // paths in the zip
        var written = 0
        val zip = ZipOutputStream(out)
        zip.putNextEntry(ZipEntry(YAML_ENTRY))
        zip.write(ProfileYaml.encode(file).toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        pictures.forEach { name ->
            val source = data.photoFiles.file(name.removePrefix(IMAGES_DIR))
            if (!source.isFile) return@forEach
            zip.putNextEntry(ZipEntry(name))
            source.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
            written++
        }
        zip.finish()
        zip.flush()
        return ExportResult.Done(garments = file.garments.size, pictures = written)
    }

    /** One consistent snapshot of the profile. A picture is named by its path in the zip, `images/<file>`. */
    private suspend fun read(data: DressUpData, profileName: String, appVersion: String?, now: Instant): ProfileFile {
        val db = data.database
        return db.withTransaction {
            val categories = db.categoryDao().getAll().sortedBy { it.id }
            val garments = db.garmentDao().getAll()
            val outfits = db.outfitDao().getAll()
            val diary = db.diaryDao().getAll()
            ProfileFile(
                format = ProfileFile.FORMAT,
                version = ProfileFile.VERSION,
                app = appVersion,
                exportedAt = now.toString(),
                profile = ProfileInfo(profileName),
                categories = categories.map { it.toFile() },
                garments = garments.map { it.toFile() },
                outfits = outfits.map { it.toFile() },
                diary = diary.map { it.toFile() },
            )
        }
    }
}
