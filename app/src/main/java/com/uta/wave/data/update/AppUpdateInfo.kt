package com.uta.wave.data.update

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val title: String,
    val changelog: String,
    val downloadUrl: String,
    val isMandatory: Boolean = false
)
