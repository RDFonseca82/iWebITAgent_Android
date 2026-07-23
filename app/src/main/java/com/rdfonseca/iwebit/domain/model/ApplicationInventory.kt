package com.rdfonseca.iwebit.domain.model

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.annotation.Keep
import java.time.Instant

@Keep
data class AdvancedApplicationInfo(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val firstInstallAt: String,
    val lastUpdateAt: String,
    val targetSdk: Int,
    val applicationType: String,
    val category: String,
    val enabled: Boolean,
    val stopped: Boolean,
    val suspended: Boolean,
    val installerPackage: String?,
    val dangerousPermissionsRequested: List<String>,
    val dangerousPermissionsGranted: List<String>
)

@Keep
data class ApplicationInventorySummary(
    val total: Int,
    val system: Int,
    val user: Int,
    val disabled: Int,
    val suspended: Int,
    val installedOrUpdatedLast30Days: Int
)

data class ApplicationInventoryReport(
    val applications: List<AdvancedApplicationInfo>,
    val summary: ApplicationInventorySummary
) {
    fun toLegacyMap(): MutableMap<String, Map<String, String>> =
        applications.mapIndexed { index, app ->
            index.toString() to mapOf(
                "Name" to app.packageName,
                "InstallDate" to app.firstInstallAt.replace('T', ' ').take(19),
                "Caption" to app.applicationType
            )
        }.toMap(mutableMapOf())
}

object ApplicationInventoryCollector {
    private const val RECENT_WINDOW_MILLIS = 30L * 24L * 60L * 60L * 1000L

    @Suppress("DEPRECATION")
    fun collect(packageManager: PackageManager): ApplicationInventoryReport {
        val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val applications = packages.map { packageInfo ->
            packageInfo.toAdvancedInfo(packageManager)
        }.sortedBy { it.packageName.lowercase() }

        val recentThreshold = System.currentTimeMillis() - RECENT_WINDOW_MILLIS
        return ApplicationInventoryReport(
            applications = applications,
            summary = ApplicationInventorySummary(
                total = applications.size,
                system = applications.count { it.applicationType == "System" },
                user = applications.count { it.applicationType == "User" },
                disabled = applications.count { !it.enabled },
                suspended = applications.count { it.suspended },
                installedOrUpdatedLast30Days = packages.count {
                    maxOf(it.firstInstallTime, it.lastUpdateTime) >= recentThreshold
                }
            )
        )
    }

    @Suppress("DEPRECATION")
    private fun PackageInfo.toAdvancedInfo(
        packageManager: PackageManager
    ): AdvancedApplicationInfo {
        val appInfo = applicationInfo
        val appFlags = appInfo?.flags ?: 0
        val requested = requestedPermissions.orEmpty()
        val requestedFlags = requestedPermissionsFlags ?: IntArray(0)
        val dangerousPermissions = requested.mapIndexedNotNull { index, permission ->
            if (!packageManager.isDangerousPermission(permission)) return@mapIndexedNotNull null
            val granted = requestedFlags.getOrNull(index)?.let { flags: Int ->
                flags and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
            } ?: false
            permission to granted
        }
        val isSystem = appFlags and ApplicationInfo.FLAG_SYSTEM != 0 ||
            appFlags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

        return AdvancedApplicationInfo(
            packageName = packageName,
            label = appInfo?.loadLabel(packageManager)?.toString().orEmpty(),
            versionName = versionName.orEmpty(),
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                longVersionCode
            } else {
                versionCode.toLong()
            },
            firstInstallAt = Instant.ofEpochMilli(firstInstallTime).toString(),
            lastUpdateAt = Instant.ofEpochMilli(lastUpdateTime).toString(),
            targetSdk = appInfo?.targetSdkVersion ?: 0,
            applicationType = if (isSystem) "System" else "User",
            category = appInfo?.category.toApplicationCategory(),
            enabled = appInfo?.enabled ?: false,
            stopped = appFlags and ApplicationInfo.FLAG_STOPPED != 0,
            suspended = appFlags and ApplicationInfo.FLAG_SUSPENDED != 0,
            installerPackage = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    packageManager.getInstallSourceInfo(packageName).installingPackageName
                } else {
                    packageManager.getInstallerPackageName(packageName)
                }
            }.getOrNull(),
            dangerousPermissionsRequested = dangerousPermissions.map { it.first }.sorted(),
            dangerousPermissionsGranted = dangerousPermissions
                .filter { it.second }
                .map { it.first }
                .sorted()
        )
    }

    @Suppress("DEPRECATION")
    private fun PackageManager.isDangerousPermission(permission: String): Boolean =
        runCatching {
            val info = getPermissionInfo(permission, 0)
            info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE ==
                PermissionInfo.PROTECTION_DANGEROUS
        }.getOrDefault(false)

    private fun Int?.toApplicationCategory(): String = when (this) {
        ApplicationInfo.CATEGORY_GAME -> "GAME"
        ApplicationInfo.CATEGORY_AUDIO -> "AUDIO"
        ApplicationInfo.CATEGORY_VIDEO -> "VIDEO"
        ApplicationInfo.CATEGORY_IMAGE -> "IMAGE"
        ApplicationInfo.CATEGORY_SOCIAL -> "SOCIAL"
        ApplicationInfo.CATEGORY_NEWS -> "NEWS"
        ApplicationInfo.CATEGORY_MAPS -> "MAPS"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "PRODUCTIVITY"
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> "ACCESSIBILITY"
        else -> "UNDEFINED"
    }
}
