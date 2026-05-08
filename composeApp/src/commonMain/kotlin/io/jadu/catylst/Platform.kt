package io.jadu.catylst

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
