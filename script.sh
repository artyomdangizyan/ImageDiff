javac -classpath ~/android/platforms/android-25/android.jar:lib/commons-cli-1.4.jar Main.java
java -jar r8.jar --min-api 16  --release --output out.zip  --lib ~/android/platforms/android-25/android.jar lib/commons-cli-1.4.jar  *.class
adb  push out.zip /sdcard/
adb shell
cd sdcard/
export CLASSPATH=/sdcard/out.zip
exec app_process $/system/bin Main -sourceImage /sdcard/Pictures/first.jpg -destImage /sdcard/Pictures/second.jpg -t 100 -minp 30 -maxp 80
