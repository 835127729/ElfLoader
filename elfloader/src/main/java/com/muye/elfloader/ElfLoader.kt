package com.muye.elfloader

import android.annotation.SuppressLint
import dalvik.system.BaseDexClassLoader
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

object ElfLoader {
    private val installedDir = CopyOnWriteArrayList<File>()

    fun install(folder: File): Boolean {
        if (installedDir.contains(folder)) {
            return true
        }
        if (javaClass.classLoader is BaseDexClassLoader) {
            if ((javaClass.classLoader as BaseDexClassLoader).appendLibraryDirectory(folder)) {
                installedDir.add(folder)
                return true
            }
            return false
        }
        return false
    }

    private fun unmapLibraryName(mappedLibraryName: String): String {
        return mappedLibraryName.substring(3, mappedLibraryName.length - 3)
    }

    fun loadLibrary(libName: String): Boolean = runCatching {
        //1、try system load first
        System.loadLibrary(libName)
        true
    }.onFailure {
        installedDir.forEach { dir ->
            //2. Perhaps you can find the corresponding so in the newly installed directory
            if (load(File(dir, "lib${libName}.so"))) {
                return true
            }
        }
        return false
    }.getOrDefault(false)

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun load(file: File, autoInstall: Boolean = true): Boolean = kotlin.runCatching {
        //1、check if the file exists
        if (!file.exists()) {
            return false
        }
        if (runCatching {
                //2、try system load first
                System.load(file.absolutePath)
                true
            }.getOrDefault(false)) {
            return true
        }

        //3、try install first
        if (autoInstall) {
            file.parentFile?.apply {
                install(this)
            }
        }

        //4、parse dependencies
        ReadElf(file).use { elf ->
            elf.getDynByTag(DT_NEEDED).map {
                elf.getString(it.d_val)
            }
        }.forEach {
            //5、load dependencies
            loadLibrary(unmapLibraryName(it))
        }
        //6、try system load again
        System.load(file.absolutePath)
        true
    }.getOrDefault(false)
}