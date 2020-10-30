package com.edt.ut3.compatibility

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.preferences.PreferencesManager

object CompatibilityManager {

    val PackageInfo.minorVersion: Int
        get() {
            return if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
                versionCode
            } else{
                (longVersionCode and 0xFFFF).toInt()
            }
        }

    val Context.packageInfo: PackageInfo
        get() = packageManager.getPackageInfo(packageName, 0)

    private lateinit var preferencesManager: PreferencesManager

    fun ensureCompatibility(context: Context) {
        preferencesManager = PreferencesManager.getInstance(context)

        val oldVersion = preferencesManager.codeVersion
        val newVersion = context.packageInfo.minorVersion

        when (oldVersion to newVersion) {
            0 to 19,
            0 to 20 -> from0To19()

            else -> {
                Log.d(
                    "CompatibilityManager",
                    "Versions are the same or " +
                            "compatibility cannot be done: " +
                            "old=$oldVersion new=$newVersion"
                )
            }
        }

        updateVersionCode(newVersion)
    }

    private fun updateVersionCode(newVersion: Int) {
        preferencesManager.codeVersion = newVersion
    }

    private fun from0To19() {
        preferencesManager.run {
            if (link?.isNotEmpty() == true) {
                link = School.default.info.first().toJSON().toString()
            } else if (link.isNullOrBlank() && groups?.isNotEmpty() == true) {
                link = School.default.info.first().toJSON().toString()
            }
        }
    }

}