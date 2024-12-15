package com.muye.elfloader

import java.io.File
import java.io.RandomAccessFile
import kotlin.experimental.and
import kotlin.time.times

typealias Addr = ULong
typealias Half = UShort
typealias SHalf = Short
typealias Off = ULong
typealias Sword = Int
typealias Word = UInt
typealias Xword = ULong
typealias Sxword = Long

const val EI_MAG0 = 0
const val EI_MAG1 = 1
const val EI_MAG2 = 2
const val EI_MAG3 = 3
const val EI_CLASS = 4
const val EI_DATA = 5
const val EI_VERSION = 6
const val EI_OSABI = 7
const val EI_PAD = 8

const val ELFCLASSNONE = 0
const val ELFCLASS32 = 1
const val ELFCLASS64 = 2
const val ELFCLASNUM = 3


const val EI_NIDENT = 16
val ELFMAG = byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())

private const val ELFDATA2LSB = 1
private const val ELFDATA2MSB = 2

const val SHT_NULL:Word = 0u
const val SHT_PROGBITS:Word = 1u
const val SHT_SYMTAB:Word = 2u
const val SHT_STRTAB:Word = 3u
const val SHT_RELA = 4
const val SHT_HASH = 5
const val SHT_DYNAMIC = 6
const val SHT_NOTE = 7
const val SHT_NOBITS = 8
const val SHT_REL = 9
const val SHT_SHLIB = 10
const val SHT_DYNSYM = 11
const val SHT_NUM = 12

const val PT_NULL = 0
const val PT_LOAD = 1
const val PT_DYNAMIC:Word = 2u
const val PT_INTERP = 3
const val PT_NOTE = 4
const val PT_SHLIB = 5
const val PT_PHDR = 6
const val PT_TLS = 7


const val DT_NULL:Sxword = 0
const val DT_NEEDED:Sxword = 1
const val DT_PLTRELSZ = 2
const val DT_PLTGOT = 3
const val DT_HASH = 4
const val DT_STRTAB = 5
const val DT_SYMTAB = 6
const val DT_RELA = 7
const val DT_RELASZ = 8
const val DT_RELAENT = 9
const val DT_STRSZ = 10
const val DT_SYMENT = 11
const val DT_INIT = 12
const val DT_FINI = 13
const val DT_SONAME = 14
const val DT_RPATH = 15
const val DT_SYMBOLIC = 16
const val DT_REL = 17
const val DT_RELSZ = 18
const val DT_RELENT = 19
const val DT_PLTREL = 20
const val DT_DEBUG = 21
const val DT_TEXTREL = 22
const val DT_JMPREL = 23
const val DT_ENCODING = 32

/**
 * typedef struct {
 *   Elf64_Sxword d_tag;		/* entry tag value */
 *   union {
 *     Elf64_Xword d_val;
 *     Elf64_Addr d_ptr;
 *   } d_un;
 * } Elf64_Dyn;
 */
data class Dyn(
    val d_tag: Sxword,
    val d_val: Xword
)

/**
 * typedef struct elf64_phdr {
 *   Elf64_Word p_type;
 *   Elf64_Word p_flags;
 *   Elf64_Off p_offset;		/* Segment file offset */
 *   Elf64_Addr p_vaddr;		/* Segment virtual address */
 *   Elf64_Addr p_paddr;		/* Segment physical address */
 *   Elf64_Xword p_filesz;		/* Segment size in file */
 *   Elf64_Xword p_memsz;		/* Segment size in memory */
 *   Elf64_Xword p_align;		/* Segment alignment, file & memory */
 * } Elf64_Phdr;
 */
data class Phdr(
    val p_type: Word,
    val p_flags: Word,
    val p_offset: Off,
    val p_vaddr: Addr,
    val p_paddr: Addr,
    val p_filesz: Xword,
    val p_memsz: Xword,
    val p_align: Xword
)

data class Shdr(
    val sh_name: Word,
    val sh_type: Word,
    val sh_flags: Xword,
    val sh_addr: Addr,
    val sh_offset: Off,
    val sh_size: Xword,
    val sh_link: Word,
    val sh_info: Word,
    val sh_addralign: Xword,
    val sh_entsize: Xword
)

data class Ehdr(
    val e_ident: ByteArray,
    val e_type: Half,
    val e_machine: Half,
    val e_version: Word,
    val e_entry: Addr,
    val e_phoff: Off,
    val e_shoff: Off,
    val e_flags: Word,
    val e_ehsize: Half,
    val e_phentsize: Half,
    val e_phnum: Half,
    val e_shentsize: Half,
    val e_shnum: Half,
    val e_shstrndx: Half
)

class ReadElf(file: File) :AutoCloseable{
    private val randomAccessFile = RandomAccessFile(file, "r")
    private val bytes = ByteArray(512)
    private var endian = ELFDATA2LSB
    private var addressSize = 4
    private lateinit var ehdr:Ehdr

    private val shstr:Lazy<Shdr> = lazy {
        randomAccessFile.seek((ehdr.e_shoff + ehdr.e_shstrndx * ehdr.e_shentsize).toLong())
        Shdr(
            sh_name = readWord(),
            sh_type = readWord(),
            sh_flags = readXword(),
            sh_addr = readAddress(),
            sh_offset = readOffset(),
            sh_size = readXword(),
            sh_link = readWord(),
            sh_info = readWord(),
            sh_addralign = readXword(),
            sh_entsize = readXword()
        )
    }

    private val shdrs:Lazy<List<Shdr>> = lazy {
        val list = mutableListOf<Shdr>()
        for(i in 0..< ehdr.e_shnum.toInt()) {
            randomAccessFile.seek((ehdr.e_shoff + (i * ehdr.e_shentsize.toInt()).toULong()).toLong())
            list.add(
                Shdr(
                    sh_name = readWord(),
                    sh_type = readWord(),
                    sh_flags = readXword(),
                    sh_addr = readAddress(),
                    sh_offset = readOffset(),
                    sh_size = readXword(),
                    sh_link = readWord(),
                    sh_info = readWord(),
                    sh_addralign = readXword(),
                    sh_entsize = readXword()
                )
            )
        }
        list
    }

    private val phdrs :Lazy<List<Phdr>> = lazy {
        val list = mutableListOf<Phdr>()
        for(i in 0..< ehdr.e_phnum.toInt()){
            randomAccessFile.seek((ehdr.e_phoff + (i * ehdr.e_phentsize.toInt()).toULong()).toLong())
            val phdr = Phdr(
                p_type = readWord(),
                p_flags = readWord(),
                p_offset = readOffset(),
                p_vaddr = readAddress(),
                p_paddr = readAddress(),
                p_filesz = readXword(),
                p_memsz = readXword(),
                p_align = readXword(
                ))
            list.add(phdr)
        }
        list
    }

    private val dynPhdr: Lazy<Phdr> = lazy {
        phdrs.value.first {
            it.p_type == PT_DYNAMIC
        }
    }

    private val strShdr: Lazy<Shdr> = lazy {
        shdrs.value.first {
            it.sh_type == SHT_STRTAB
        }
    }

    /**
     * typedef struct {
     *   Elf64_Sxword d_tag;		/* entry tag value */
     *   union {
     *     Elf64_Xword d_val;
     *     Elf64_Addr d_ptr;
     *   } d_un;
     * } Elf64_Dyn;
     */
    private val dyns : Lazy<List<Dyn>> = lazy {
        randomAccessFile.seek(dynPhdr.value.p_offset.toLong())
        val list = mutableListOf<Dyn>()
        var dyn: Dyn
        do{
            dyn = Dyn(
                d_tag = readSxword(),
                d_val = readXword(),
            )
            list.add(dyn)
        }while (dyn.d_tag != DT_NULL)
        list
    }

    init {
        readHeader()
    }

    /**
     * typedef struct elf64_hdr {
     *   unsigned char	e_ident[EI_NIDENT];	/* ELF "magic number" */
     *   Elf64_Half e_type;
     *   Elf64_Half e_machine;
     *   Elf64_Word e_version;
     *   Elf64_Addr e_entry;		/* Entry point virtual address */
     *   Elf64_Off e_phoff;		/* Program header table file offset */
     *   Elf64_Off e_shoff;		/* Section header table file offset */
     *   Elf64_Word e_flags;
     *   Elf64_Half e_ehsize;
     *   Elf64_Half e_phentsize;
     *   Elf64_Half e_phnum;
     *   Elf64_Half e_shentsize;
     *   Elf64_Half e_shnum;
     *   Elf64_Half e_shstrndx;
     * } Elf64_Ehdr;
     */
    private fun readHeader(){
        randomAccessFile.seek(0)
        randomAccessFile.readFully(bytes, 0, EI_NIDENT)
        if(bytes[EI_MAG0] != ELFMAG[EI_MAG0] || bytes[EI_MAG1] != ELFMAG[EI_MAG1]
            || bytes[EI_MAG2] != ELFMAG[EI_MAG2] || bytes[EI_MAG3] != ELFMAG[EI_MAG3]){
            return
        }
        //位数
        val elfClass = bytes[EI_CLASS].toInt()
        if(elfClass == ELFCLASS32){
            addressSize = 4
        }else if(elfClass == ELFCLASS64){
            addressSize = 8
        }else{
            return
        }

        //字节序
        endian = bytes[EI_DATA].toInt()
        if(endian != ELFDATA2LSB && endian != ELFDATA2MSB){
            return
        }

        ehdr = Ehdr(
            e_ident = bytes,
            e_type = readHalf(),
            e_machine = readHalf(),
            e_version = readWord(),
            e_entry = readAddress(),
            e_phoff = readOffset(),
            e_shoff = readOffset(),
            e_flags = readWord(),
            e_ehsize = readHalf(),
            e_phentsize = readHalf(),
            e_phnum = readHalf(),
            e_shentsize = readHalf(),
            e_shnum = readHalf(),
            e_shstrndx = readHalf()
        )
    }

    fun getDynByTag(tag: Sxword): List<Dyn>{
        return dyns.value.filter {
            it.d_tag == tag
        }
    }

    fun getString(offset: Off): String{
        randomAccessFile.seek((strShdr.value.sh_offset + offset).toLong())
        randomAccessFile.read(bytes)
        for(i in bytes.indices){
            if(bytes[i] == 0.toByte()) {
                return String(bytes, 0, i)
            }
        }
        return ""
    }

    private fun readHalf():Half = readX(2).toUShort()
    private fun readWord():Word = readX(4).toUInt()
    private fun readOffset():Off = readX(addressSize).toULong()
    private fun readAddress():Addr = readX(addressSize).toULong()
    private fun readXword():Xword = readX(addressSize).toULong()
    private fun readSxword():Sxword = readX(addressSize)

    private fun readX(byteCount: Int): Long{
        randomAccessFile.readFully(bytes, 0, byteCount)
        var res = 0L
        if(endian == ELFDATA2LSB){
            for(i in (byteCount-1)  downTo   0) {
                res = (res shl 8) or (bytes[i].toLong() and 0xff)
            }
        }else{
            for(i in 0  ..< byteCount) {
                res = (res shl 8) or (bytes[i].toLong() and 0xff)
            }
        }
        return res
    }

    override fun close(){
        runCatching { randomAccessFile.close() }
    }
}