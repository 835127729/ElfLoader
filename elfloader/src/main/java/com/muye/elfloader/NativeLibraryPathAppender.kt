package com.muye.elfloader

import android.os.Build
import dalvik.system.BaseDexClassLoader
import java.io.File
import java.io.IOException

private interface ILibraryPathAppender {
    fun append(classLoader: BaseDexClassLoader, folder: File): Boolean
}

private object NativeLibraryPathAppenderApi21 : ILibraryPathAppender {
    override fun append(classLoader: BaseDexClassLoader, folder: File): Boolean = runCatching {
        val pathListField = BaseDexClassLoader::class.java.getDeclaredField("pathList").apply {
            isAccessible = true
        }
        val pathList = pathListField.get(classLoader)
        val nativeLibraryDirectories =
            pathList::class.java.getDeclaredField("nativeLibraryDirectories").run {
                isAccessible = true
                get(pathList) as Array<File>
            }

        val newNativeLibraryDirectories = mutableListOf(folder).apply {
            for (dir in nativeLibraryDirectories) {
                if (dir != folder) {
                    add(dir)
                }
            }
        }
        pathListField.set(classLoader, newNativeLibraryDirectories.toTypedArray())
        true
    }.getOrDefault(false)
}

private object NativeLibraryPathAppenderApi23 : ILibraryPathAppender {
    override fun append(classLoader: BaseDexClassLoader, folder: File): Boolean = runCatching {
        val pathListField = BaseDexClassLoader::class.java.getDeclaredField("pathList").apply {
            isAccessible = true
        }
        val pathList = pathListField.get(this)
        val nativeLibraryDirectoriesField =
            pathList::class.java.getDeclaredField("nativeLibraryDirectories").apply {
                isAccessible = true
            }
        val nativeLibraryDirectories: ArrayList<File> =
            (nativeLibraryDirectoriesField.get(pathList) as ArrayList<File>?
                ?: ArrayList(2)).apply {
                removeAll {
                    it == folder
                }
                add(0, folder)
            }

        val systemNativeLibraryDirectories =
            pathList::class.java.getDeclaredField("systemNativeLibraryDirectories").run {
                isAccessible = true
                get(pathList) as ArrayList<File>? ?: ArrayList<File>(2)
            }
        val newLibDirs = systemNativeLibraryDirectories + nativeLibraryDirectories

        val dexElements = pathList::class.java.getDeclaredMethod(
            "makePathElements",
            List::class.java,
            File::class.java,
            List::class.java
        ).run {
            isAccessible = true
            invoke(null, newLibDirs, null, ArrayList<IOException>()) as Array<Any>
        }

        dexElements::class.java.getDeclaredField("nativeLibraryPathElements").apply {
            isAccessible = true
            set(pathList, dexElements)
        }
        true
    }.getOrDefault(false)
}

private object NativeLibraryPathAppenderApi25 : ILibraryPathAppender {
    override fun append(classLoader: BaseDexClassLoader, folder: File): Boolean = runCatching {
        val pathListField = BaseDexClassLoader::class.java.getDeclaredField("pathList").apply {
            isAccessible = true
        }
        val pathList = pathListField.get(this)
        val nativeLibraryDirectoriesField =
            pathList::class.java.getDeclaredField("nativeLibraryDirectories").apply {
                isAccessible = true
            }
        val nativeLibraryDirectories: ArrayList<File> =
            (nativeLibraryDirectoriesField.get(pathList) as ArrayList<File>?
                ?: ArrayList(2)).apply {
                removeAll {
                    it == folder
                }
                add(0, folder)
            }

        val systemNativeLibraryDirectories =
            pathList::class.java.getDeclaredField("systemNativeLibraryDirectories").run {
                isAccessible = true
                get(pathList) as ArrayList<File>? ?: ArrayList<File>(2)
            }
        val newLibDirs = systemNativeLibraryDirectories + nativeLibraryDirectories

        val dexElements = pathList::class.java.getDeclaredMethod(
            "makePathElements",
            List::class.java,
        ).run {
            isAccessible = true
            invoke(null, newLibDirs) as Array<Any>
        }

        pathList::class.java.getDeclaredField("nativeLibraryPathElements").apply {
            isAccessible = true
            set(pathList, dexElements)
        }
        true
    }.getOrDefault(false)
}

fun BaseDexClassLoader.pathList() = kotlin.runCatching {
    val pathListField = BaseDexClassLoader::class.java.getDeclaredField("pathList").apply {
        isAccessible = true
    }
    pathListField.get(this)
}.getOrDefault(null)

fun BaseDexClassLoader.nativeLibraryDirectories(): ArrayList<File> = kotlin.runCatching {
    val pathList = pathList() ?: return@runCatching ArrayList<File>(2)
    val nativeLibraryDirectoriesField =
        pathList::class.java.getDeclaredField("nativeLibraryDirectories").apply {
            isAccessible = true
        }
    val nativeLibraryDirectories: ArrayList<File> =
        (nativeLibraryDirectoriesField.get(pathList) as ArrayList<File>?
            ?: ArrayList(2))
    return nativeLibraryDirectories
}.getOrDefault(ArrayList(2))

fun BaseDexClassLoader.appendLibraryDirectory(folder: File): Boolean {
    if (!folder.exists() || !folder.isDirectory) {
        return false
    }
    if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1 && Build.VERSION.PREVIEW_SDK_INT != 0)
        || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    ) {
        if (NativeLibraryPathAppenderApi25.append(this, folder)) {
            return true
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (NativeLibraryPathAppenderApi23.append(this, folder)) {
            return true
        }
    }

    return NativeLibraryPathAppenderApi21.append(this, folder)
}