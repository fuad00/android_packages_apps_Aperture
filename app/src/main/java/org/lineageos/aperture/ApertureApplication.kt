/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.CameraCompatibilityFilter
import androidx.camera.core.CameraXConfig
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.stabilization.StabilizationMode
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.MainScope
import org.lineageos.aperture.repositories.CameraRepository
import org.lineageos.aperture.repositories.MediaRepository
import org.lineageos.aperture.repositories.OverlaysRepository
import org.lineageos.aperture.repositories.PreferencesRepository

class ApertureApplication : Application(), CameraXConfig.Provider {
    private val coroutineScope = MainScope()

    val cameraRepository by lazy { CameraRepository(this, coroutineScope, overlaysRepository) }
    val mediaRepository by lazy { MediaRepository(this) }
    val overlaysRepository by lazy { OverlaysRepository(this) }
    val preferencesRepository by lazy { PreferencesRepository(this, coroutineScope) }

    @SuppressLint("RestrictedApi")
    override fun getCameraXConfig(): CameraXConfig {
        val defaultConfig = Camera2Config.defaultConfig()
        if (Build.DEVICE != "metroid") {
            return defaultConfig
        }

        val defaultFactoryProvider = checkNotNull(
            defaultConfig.getUseCaseConfigFactoryProvider(null)
        )

        return CameraXConfig.Builder.fromConfig(defaultConfig)
            .setUseCaseConfigFactoryProvider { context, flag ->
                // CameraX 1.7 adds a boolean to Provider.newInstance(); pass it through
                val defaultFactory = defaultFactoryProvider.newInstance(context, flag)

                UseCaseConfigFactory { captureType, captureMode ->
                    val useCaseConfig = defaultFactory.getConfig(captureType, captureMode)
                        ?: return@UseCaseConfigFactory null
                    MutableOptionsBundle.from(
                        useCaseConfig
                    ).apply {
                        insertOption(
                            UseCaseConfig.OPTION_VIDEO_STABILIZATION_MODE,
                            StabilizationMode.OFF,
                        )
                    }
                }
            }
            .build()
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()

        // Observe dynamic colors changes
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Set backward compatible camera ids
        CameraCompatibilityFilter.setBackwardCompatibleCameraIds(
            overlaysRepository.backwardCompatibleCameraIds.asList()
        )
    }
}
