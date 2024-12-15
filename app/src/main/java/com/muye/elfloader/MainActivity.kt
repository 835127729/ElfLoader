package com.muye.elfloader

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.muye.elfloader.ui.theme.ElfLoaderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ElfLoaderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Row(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Button({
                            lifecycleScope.launch(Dispatchers.IO) {
                                //copy
                                val elfFile = File(cacheDir, "libart34.so")
                                assets.open("libart34.so").use {_in->
                                    elfFile.outputStream().use {_out->
                                        _in.copyTo(_out)
                                    }
                                }
                                val dependencies = ReadElf(elfFile).run {
                                    getDynByTag(DT_NEEDED).map {
                                        getString(it.d_val)
                                    }
                                }
                                Log.d("cky", dependencies.toString())
                            }
                        }) {
                            Text("加载")
                        }
                    }
                }
            }
        }
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