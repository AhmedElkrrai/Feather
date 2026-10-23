package com.elkrrai.feather.domain.model

/**
 * A label on tasks. A tag can sit under one other tag ([parentId]), so "bugfix" can extend "work"
 * and "work" can extend "life". Searching a tag finds the tasks of the tag and of every tag under it.
 * Tags never loop: a tag cannot be put under itself or under anything that sits below it
 * (see [wouldLoop]).
 */
data class Tag(
    val id: String,
    val name: String,
    val parentId: String? = null,
)
