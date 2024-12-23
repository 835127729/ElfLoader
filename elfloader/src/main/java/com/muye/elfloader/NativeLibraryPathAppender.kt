package com.muye.elfloader

import android.os.Build
import dalvik.system.BaseDexClassLoader
import java.io.File
import java.io.IOException

private interface ILibraryPathAppender {
    fun append(classLoader: BaseDexClassLoader, folder: File): Boolean
}

private val pathListField = lazy {
    BaseDexClassLoader::class.java.getDeclaredField("pathList").apply {
        isAccessible = true
    }
}

private object NativeLibraryPathAppenderApi21 : ILibraryPathAppender {
    override fun append(classLoader: BaseDexClassLoader, folder: File): Boolean = runCatching {
        val pathList = pathListField.value.get(classLoader)
        val nativeLibraryDirectoriesField =
            pathList::class.java.getDeclaredField("nativeLibraryDirectories").apply {
                isAccessible = true
            }
        val nativeLibraryDirectories = nativeLibraryDirectoriesField.get(pathList) as Array<File>
        val newNativeLibraryDirectories = mutableListOf(folder).apply {
            for (dir in nativeLibraryDirectories) {
                if (dir != folder) {
                    add(dir)
                }
            }
            add(0, folder)
        }
        nativeLibraryDirectoriesField.set(pathList, newNativeLibraryDirectories.toTypedArray())
        true
    }.onFailure {
        it.printStackTrace()
    }.getOrDefault(false)
}

private object NativeLibraryPathAppenderApi23 : ILibraryPathAppender {
    override fun append(classLoader: BaseDexClassLoader, folder: File): Boolean = runCatching {
        val pathList = pathListField.value.get(classLoader)
        val nativeLibraryDirectoriesField =
            pathList::class.java.getDeclaredField("nativeLibraryDirectories").apply {
                isAccessible = true
            }
        val nativeLibraryDirectories: ArrayList<File> =
            ArrayList(
                nativeLibraryDirectoriesField.get(pathList) as ArrayList<File>?
                    ?: ArrayList(2)
            ).apply {
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

        //这里是和NativeLibraryPathAppenderApi25的区别
        val dexElements = pathList::class.java.getDeclaredMethod(
            "makePathElements",
            List::class.java,
            File::class.java,
            List::class.java
        ).run {
            isAccessible = true
            invoke(null, newLibDirs, null, ArrayList<IOException>()) as Array<Any>
        }

        nativeLibraryDirectoriesField.set(pathList, nativeLibraryDirectories)
        dexElements::class.java.getDeclaredField("nativeLibraryPathElements").apply {
            isAccessible = true
            set(pathList, dexElements)
        }
        true
    }.onFailure {
        it.printStackTrace()
    }.getOrDefault(false)
}

private object NativeLibraryPathAppenderApi25 : ILibraryPathAppender {
    override fun append(classLoader: BaseDexClassLoader, folder: File): Boolean = runCatching {
        //1、反射获取pathList
        val pathList = pathListField.value.get(classLoader)
        val nativeLibraryDirectoriesField =
            pathList::class.java.getDeclaredField("nativeLibraryDirectories").apply {
                isAccessible = true
            }
        //2、复制一份nativeLibraryDirectories，并将so所在的目录添加到其中
        val nativeLibraryDirectories: ArrayList<File> =
            ArrayList(
                nativeLibraryDirectoriesField.get(pathList) as ArrayList<File>?
                    ?: ArrayList(2)
            ).apply {
                removeAll {
                    it == folder
                }
                add(0, folder)
            }

        //3、复制一份systemNativeLibraryDirectories
        val systemNativeLibraryDirectories =
            pathList::class.java.getDeclaredField("systemNativeLibraryDirectories").run {
                isAccessible = true
                ArrayList(get(pathList) as ArrayList<File>? ?: ArrayList(2))
            }
        //4、等价于调用getAllNativeLibraryDirectories()
        val newLibDirs = systemNativeLibraryDirectories + nativeLibraryDirectories

        //5、调用makePathElements生成nativeLibraryPathElements
        val dexElements = pathList::class.java.getDeclaredMethod(
            "makePathElements",
            List::class.java,
        ).run {
            isAccessible = true
            invoke(null, newLibDirs) as Array<Any>
        }

        //6、修改nativeLibraryDirectories、nativeLibraryPathElements
        nativeLibraryDirectoriesField.set(pathList, nativeLibraryDirectories)
        pathList::class.java.getDeclaredField("nativeLibraryPathElements").apply {
            isAccessible = true
            set(pathList, dexElements)
        }
        true
    }.onFailure {
        it.printStackTrace()
    }.getOrDefault(false)
}

fun BaseDexClassLoader.appendLibraryDirectory(folder: File): Boolean {
    if (!folder.exists() || !folder.isDirectory) {
        return false
    }
    synchronized(this) {
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
}