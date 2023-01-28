package com.example.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import kotlin.io.path.Path

class ConfigLoaderImpl: ConfigLoader {
    override fun loadConfig(configPath: String): Config {
        return ConfigLoaderBuilder
            .default()
            .addSource(PropertySource.path(Path(configPath))).build().loadConfigOrThrow()
    }
}