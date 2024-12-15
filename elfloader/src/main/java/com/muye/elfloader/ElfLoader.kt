package com.muye.elfloader

import java.io.File

object ElfLoader {
    fun loadLibrary(libName: String) {
        //1、从APK中找到目标文件
//        ReadElf(File(path)).run {
//            getDynByTag(DT_NEEDED).map {
//                getString(it.d_val)
//            }
//        }.forEach {
//            System.mapLibraryName(it)
//        }
//        System.load(libName)
    }

    private fun unmapLibraryName(mappedLibraryName: String): String{
        return mappedLibraryName.substring(3, mappedLibraryName.length - 3)
    }

    fun load(path: String){
        ReadElf(File(path)).use {elf->
            elf.getDynByTag(DT_NEEDED).map {
                elf.getString(it.d_val)
            }
        }.forEach {
            loadLibrary(unmapLibraryName(it))
        }
        System.load(path)
    }
}