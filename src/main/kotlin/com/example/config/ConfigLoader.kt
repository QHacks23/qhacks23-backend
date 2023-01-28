package com.example.config

interface ConfigLoader {
    fun loadConfig(configPath: String): Config
}