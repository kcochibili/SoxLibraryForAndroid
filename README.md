# SoxLibraryForAndroid
Easy to use Sox library for android, with all the classic sound processing kungfu included.

First of all, this project is based on [**SoxLibInAndroid**][1] for the .aar file sources, and the java helper class with easy to use functions are based on  [**android-ffmpeg-java**][2]



[1]:  https://github.com/pxhbug123/SoxLibInAndroid
[2]:  https://github.com/guardianproject/android-ffmpeg-java/tree/master/src/net/sourceforge/sox

### Installation
1. Download the ``soxcommandlibrary-release.aar`` file onto your computer
2. Then in your app module build.gradle file, add the path to the downloaded file like the example below

```gradle
dependencies {
  implementation files('/Users/Bob/path/to/file/soxcommandlibrary-release.aar')
}
```
3. Sync the project


**Example usage:**
```Java

    import com.vtech.audio.helper.SoxCommandLib;

    // Simple combiner
    // sox file[0] file[1] ... file[n] <outFile>

    String soxCommand = "sox -m in1.mp3 in2.mp3 in3.mp3 out.mp3"
    int exitVal = SoxCommandLib.executeCommand(soxCommand);
 
    if(exitVal == 0){
      Log.v("SOX", "Sox finished successfully");
    }else{
      Log.v("SOX", "Sox failed");
    }
 
```


**Example usage with helper methods:**
```Java

    Coming soon..

```
