package com.m3u.features.main

sealed interface MainEvent {
    data class UnsubscribeFeedByUrl(val url: String) : MainEvent
    data class Rename(val feedUrl: String, val target: String) : MainEvent
}