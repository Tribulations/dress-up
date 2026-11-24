package com.dressup.core.data.backup

import androidx.room.withTransaction
import com.dressup.core.data.DressUpData
import com.dressup.core.data.cleanName
import com.dressup.core.data.db.CategoryEntity
import com.dressup.core.data.db.DiaryEntryGarmentEntity
import com.dressup.core.data.db.GarmentCategoryEntity
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Reads an export into a fresh, empty profile's [DressUpData] (ARCHITECTURE.md §7.8). Blocking: call off the main thread.
 *
 * The zip is untrusted: only `profile.yaml` and plain file names in `images/` are read, each with a size limit, and
 * every picture is stored under a new name of our own, so no entry can write outside the profile's photo folder.
 * Anything in the YAML that doesn't fit (an unknown category, a rating out of range, a missing picture) is dropped or
 * clamped rather than failing the whole import.
 */
internal object ProfileImporter {
    sealed interface Outcome {
        data class Read(val profileName: String, val garments: Int, val outfits: Int, val diaryEntries: Int) : Outcome
        data object NotAnExport : Outcome
        data object TooNew : Outcome
    }

    suspend fun import(input: InputStream, data: DressUpData, now: Long): Outcome {
        var yamlText: String? = null
        val pictures = HashMap<String, String>() // path in the zip (images/…) → our file name
        var entries = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++entries > MAX_ENTRIES) break
                val name = entry.name
                when {
                    entry.isDirectory -> Unit
                    name == ProfileExporter.YAML_ENTRY -> yamlText = readLimited(zip, MAX_YAML_BYTES)?.toString(Charsets.UTF_8)
                    name.startsWith(ProfileExporter.IMAGES_DIR) -> {
                        val fileName = name.removePrefix(ProfileExporter.IMAGES_DIR)
                        if (SAFE_NAME.matches(fileName) && name !in pictures) {
                            data.photoFiles.saveImported(zip, fileName, MAX_PICTURE_BYTES)?.let { pictures[name] = it }
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        val text = yamlText ?: return Outcome.NotAnExport
        val header = runCatching { ProfileYaml.header(text) }.getOrNull()
        if (header?.format != ProfileFile.FORMAT || header.version == null) return Outcome.NotAnExport
        if (header.version > ProfileFile.VERSION) return Outcome.TooNew
        val file = ProfileYaml.decode(text)
        return fill(data, file, pictures, now)
    }

    /**
     * Adds everything in [file] to the profile in [data], in one transaction. [pictures] maps a picture's path in the
     * zip (`images/…`) to the file it was stored as. Also how the test user is filled
     * ([com.dressup.core.data.testuser.TestWardrobe]).
     */
    suspend fun fill(data: DressUpData, file: ProfileFile, pictures: Map<String, String>, now: Long): Outcome.Read {
        val db = data.database
        val garmentDao = db.garmentDao()
        val outfitDao = db.outfitDao()
        val diaryDao = db.diaryDao()
        val picture = { path: String -> pictures[picturePath(path)] }
        return db.withTransaction {
            val categoryIds = importCategories(data, file.categories)
            val fallbackCategory = db.categoryDao().getAll().let { all -> all.firstOrNull { it.builtInKey == FALLBACK_CATEGORY } ?: all.first() }.id

            // The file's ids only link things inside it: each gets a new one here.
            val garmentIds = HashMap<Long, Long>()
            file.garments.forEach { g ->
                val id = garmentDao.insert(g.toEntity(picture, now))
                if (g.id !in garmentIds) garmentIds[g.id] = id
                val categories = g.categories.mapNotNull(categoryIds::get).distinct().ifEmpty { listOf(fallbackCategory) }
                garmentDao.insertCategories(categories.mapIndexed { position, c -> GarmentCategoryEntity(id, c, position) })
                g.aspectEntities(id).forEach { garmentDao.upsertAspectRating(it) }
            }

            val outfitIds = HashMap<Long, Long>()
            file.outfits.forEach { o ->
                val id = outfitDao.insert(o.toEntity(picture, now))
                if (o.id !in outfitIds) outfitIds[o.id] = id
                val items = o.items.mapNotNull { item -> garmentIds[item.garment]?.let { item to it } }.distinctBy { it.second }
                outfitDao.insertItems(items.mapIndexed { position, (item, garmentId) -> item.toEntity(id, garmentId, position) })
                outfitDao.upsertAspectRatings(o.aspectEntities(id))
            }

            var diaryEntries = 0
            file.diary.forEach { e ->
                val outfitId = e.outfit?.let(outfitIds::get)
                val worn = e.garments.mapNotNull(garmentIds::get).distinct()
                if (outfitId == null && worn.isEmpty()) return@forEach
                val entry = e.toEntity(outfitId, now) ?: return@forEach
                val id = diaryDao.insert(entry)
                diaryDao.insertGarments(worn.mapIndexed { position, garmentId -> DiaryEntryGarmentEntity(id, garmentId, position) })
                diaryEntries++
            }
            Outcome.Read(file.profile.name, garmentIds.size, outfitIds.size, diaryEntries)
        }
    }

    /** Built-in categories are matched by key (the fresh profile already has them); the user's own are added. */
    private suspend fun importCategories(data: DressUpData, categories: List<CategoryFile>): Map<Long, Long> {
        val dao = data.database.categoryDao()
        val builtIn = dao.getAll().filter { it.builtInKey != null }.associateBy { it.builtInKey }
        val ownByName = HashMap<String, Long>()
        val ids = HashMap<Long, Long>()
        categories.forEach { c ->
            val existing = c.builtIn?.let(builtIn::get)
            val id = if (existing != null) {
                dao.setHidden(existing.id, c.hidden)
                existing.id
            } else {
                val name = cleanName(c.name).ifEmpty { return@forEach }
                ownByName.getOrPut(name.lowercase()) {
                    dao.insert(CategoryEntity(name = name, builtInKey = null, hidden = c.hidden, sortOrder = 0))
                }
            }
            if (c.id !in ids) ids[c.id] = id
        }
        return ids
    }

    /** `images/a.jpg`, also for a hand-edited file that says just `a.jpg`. */
    private fun picturePath(path: String): String =
        if (path.startsWith(ProfileExporter.IMAGES_DIR)) path else ProfileExporter.IMAGES_DIR + path

    /** At most [limit] bytes of the current zip entry, or null when it is bigger. */
    private fun readLimited(input: InputStream, limit: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) return out.toByteArray()
            if (out.size() + read > limit) return null
            out.write(buffer, 0, read)
        }
    }

    private val SAFE_NAME = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,99}")
    private const val FALLBACK_CATEGORY = "accessory"
    private const val MAX_ENTRIES = 20_000
    private const val MAX_YAML_BYTES = 32 * 1024 * 1024
    private const val MAX_PICTURE_BYTES = 30L * 1024 * 1024
    private const val BUFFER_BYTES = 64 * 1024
}
