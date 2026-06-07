package com.katha.mep.mep_ia_poc

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform