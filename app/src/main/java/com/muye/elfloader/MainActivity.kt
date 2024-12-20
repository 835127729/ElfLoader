package com.muye.elfloader

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.muye.elfloader.ui.theme.ElfLoaderTheme
import com.muye.testso.NativeLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var elfDir: File;

    private fun toast(text: String) = runOnUiThread {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        elfDir = File(filesDir, "lib/${Build.SUPPORTED_ABIS[0]}").apply {
            mkdirs()
        }
        lifecycleScope.launch(Dispatchers.IO) {
            //copy so from asset to local dir
            for (i in 1..3) {
                val elfFile = File(elfDir, "libtestso${i}.so")
                assets.open("lib/${Build.SUPPORTED_ABIS[0]}/libtestso${i}.so")
                    .use { _in ->
                        elfFile.outputStream().use { _out ->
                            _in.copyTo(_out)
                        }
                    }
            }
        }

        enableEdgeToEdge()
        setContent {
            ElfLoaderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            24.dp,
                            Alignment.CenterVertically
                        )
                    ) {
                        Text(
                            text = "libtestso3依赖libtestso2、libtestso1\nlibtestso2依赖libtestso1",
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Button(
                            {
                                for (i in 1..3) {
                                    val s1 = ReadElf(File(elfDir, "libtestso${i}.so")).use { elf ->
                                        elf.getDynByTag(DT_NEEDED)
                                            .joinToString {
                                                elf.getString(it.d_val)
                                            }
                                    }
                                    Log.d(TAG, "libtestso${i}.so依赖: $s1")
                                }
                            }
                        ) {
                            Text("1、Logcat打印依赖关系")
                        }
                        Button(
                            {
                                if (ElfLoader.install(elfDir)) {
                                    toast("添加成功")
                                } else {
                                    toast("添加失败")
                                }
                            }
                        ) {
                            Text("2、添加目录到ClassLoader")
                        }
                        Button({
                            if (ElfLoader.loadLibrary("testso3")) {
                                toast("加载成功")
                            } else {
                                toast("加载失败")
                            }
                        }) {
                            Text("3.1、loadLibrary()加载testso3")
                        }
                        Button({
                            if (ElfLoader.load(File(elfDir, "libtestso3.so"))) {
                                toast("加载成功")
                            } else {
                                toast("加载失败")
                            }
                        }) {
                            Text("3.2、load()加载testso3")
                        }
                        Button(
                            {
                                Toast.makeText(
                                    this@MainActivity,
                                    "load success ${NativeLib().test3()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Text("4、调用来自libtestso3的Native方法")
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ElfLoaderTheme {
        Greeting("Android")
    }
}