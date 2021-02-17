package com.game7th.metagame

import java.io.File

interface FileProvider {
    fun getFileContent(name: String): String?
}