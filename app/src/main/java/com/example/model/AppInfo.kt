package com.example.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isFavorite: Boolean = false,
    val isSystemApp: Boolean = false,
    val icon: Drawable? = null
)
