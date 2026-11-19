package com.dressup.core.model

/**
 * Whose wardrobe the app shows (ARCHITECTURE.md §5.1). Every profile has its own garments, categories and photos.
 *
 * The [isTestUser] profile is the built-in test user: a made-up, prefilled wardrobe for trying the app out. Its
 * [name] is not shown; the UI uses its own "Test user" text.
 */
data class Profile(
    val id: String,
    val name: String,
    val isTestUser: Boolean,
)

/** All profiles and which one is active. [activeId] is null before the first start's "What's your name?". */
data class ProfilesState(
    val profiles: List<Profile> = emptyList(),
    val activeId: String? = null,
) {
    val active: Profile? get() = profiles.firstOrNull { it.id == activeId }
    val own: List<Profile> get() = profiles.filterNot { it.isTestUser }
    val testUser: Profile? get() = profiles.firstOrNull { it.isTestUser }
}
