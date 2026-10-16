package com.moviehub.movie.domain

import org.springframework.data.domain.Sort

/** The grid's sort choices (E6-S1). Title ignores case so "alien" does not sort after "Zodiac"; year ties break on title. */
enum class MovieSort(val order: Sort) {
    TITLE_ASC(Sort.by(Sort.Order.asc("title").ignoreCase())),
    TITLE_DESC(Sort.by(Sort.Order.desc("title").ignoreCase())),
    YEAR_ASC(Sort.by(Sort.Order.asc("releaseYear"), Sort.Order.asc("title").ignoreCase())),
    YEAR_DESC(Sort.by(Sort.Order.desc("releaseYear"), Sort.Order.asc("title").ignoreCase())),
}
