# required for reading Parcelable stored in Bundle
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
