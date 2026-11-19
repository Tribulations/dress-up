package com.dressup.core.data.backup

import com.charleskorn.kaml.MultiLineStringStyle
import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.Serializable

/** `profile.yaml` as text. Unknown keys are skipped, so a file from a newer version with extra fields still imports. */
internal object ProfileYaml {
    private val yaml = Yaml(
        configuration = YamlConfiguration(
            encodeDefaults = false, // leave out empty fields, so the file stays short and readable
            strictMode = false,
            singleLineStringStyle = SingleLineStringStyle.PlainExceptAmbiguous, // quotes only where YAML needs them
            multiLineStringStyle = MultiLineStringStyle.Literal, // reviews and notes keep their line breaks
        ),
    )

    fun encode(file: ProfileFile): String = yaml.encodeToString(ProfileFile.serializer(), file)

    /** The [Header] alone, to check the format and version before reading the rest. */
    fun header(text: String): Header = yaml.decodeFromString(Header.serializer(), text)

    fun decode(text: String): ProfileFile = yaml.decodeFromString(ProfileFile.serializer(), text)

    @Serializable
    data class Header(val format: String? = null, val version: Int? = null)
}
