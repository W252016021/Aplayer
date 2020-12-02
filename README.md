# Aplayer
基于迅雷Aplayer封装

##### 引用方式:

```java
allprojects {
 repositories {
   ...
      maven { url 'https://www.jitpack.io' }
   }
 }
```

```java
dependencies {
 implementation 'com.github.W252016021:Aplayer:1.0.0'
}
```
##### 调用播放:

```java
new FuckPlayer(this).setTitle(name).setUrl(link).setCookies(cookies).setResquestCode(201).start();//201用于回调播放完成事件，可空

@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
 super.onActivityResult(requestCode, resultCode, data);
 Log.e("info", "onActivityResult: requestCode->" + requestCode + "|resultCode->" + resultCode);
 if (requestCode == 201) {
    Toast.makeText(this, "播放完毕", Toast.LENGTH_SHORT).show();
   }
}

```
##### 混淆规则:
```java
-keep class com.aplayer.** {*;}
```

![演示图片](https://raw.githubusercontent.com/W252016021/MyAplayer/master/ScreenShots/Screenshot_20190615-182732.jpg "演示图片")
![演示图片](https://raw.githubusercontent.com/W252016021/MyAplayer/master/ScreenShots/Screenshot_20190615-182748.jpg "演示图片")
![演示图片](https://raw.githubusercontent.com/W252016021/MyAplayer/master/ScreenShots/Screenshot_20190615-182752.jpg "演示图片")


