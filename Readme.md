# ElfLoader

**ElfLoader**是一个Android中用于绕过Android系统限制，加载**任意位置**动态库(.so)的库。

这项技术通常是为了避免将一些so库打入APK中，从而**减少APK体积**，在运行时才根据需要去下载这些so库并且加载到内存运行。





## 一、特征

- 兼容Android API21 - API35。
- 提供与原生`System.load()`和`System.loadLibrary()`类似的接口，但是可以加载任意位置动态库。
- 使用 MIT 许可证授权。



## 二、实现原理

[Android Hook - 动态加载so库](https://juejin.cn/post/7451505838344912948#heading-8)



## 三、快速开始

你可以参考[app](https://github.com/835127729/ElfLoader/tree/main/app)中的示例。

### 1、在 build.gradle.kts 中增加依赖

```Kotlin
android {
    buildFeatures {
        //1、声明可以进行原生依赖，具体参考https://developer.android.com/build/native-dependencies
        prefab = true
    }
}

dependencies {
    //2、依赖最新版本
    implementation("com.github.835127729:ElfLoader:1.0.2")
}
```



### 2、使用

```C
//.so库所在的目录
val elfDir = File(filesDir, "lib/${Build.SUPPORTED_ABIS[0]}").apply {
            mkdirs()
        }

//1、使用load()方法加载指定路径的.so库，功能和System.load()类似
ElfLoader.load(File(elfDir, "libtestso3.so")

//2、安装目标.so库所在的目录，如果要使用ElfLoader.loadLibrary()方法加载.so库，则需要先指定目录
ElfLoader.install(elfDir)
  
//3、使用loadLibrary()方法加载指定名称的.so库，功能和System.loadLibrary()类似，前提是需要先调用install()方法
ElfLoader.loadLibrary("testso3")
```





## 四、许可证

MapsVisitor 使用 [MIT 许可证](https://github.com/bytedance/bhook/blob/main/LICENSE) 授权。