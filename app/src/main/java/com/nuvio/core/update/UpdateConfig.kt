package com.nuvio.core.update

object UpdateConfig {
    const val GITHUB_OWNER = "ShawkyMH"
    const val GITHUB_REPO = "Nuvio"

    val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    val ALL_RELEASES_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"
}
