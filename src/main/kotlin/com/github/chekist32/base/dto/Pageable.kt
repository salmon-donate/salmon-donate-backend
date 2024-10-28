package com.github.chekist32.base.dto

interface Pageable<T> {
    val page: Int
    val limit: Int
    val totalCount: Int
    val data: Collection<T>
}

