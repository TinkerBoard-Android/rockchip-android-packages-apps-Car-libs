# required for replacing elements in PreferenceFragment
-keep class com.android.car.ui.preference.CarUiDropDownPreference {*;}
-keep class com.android.car.ui.preference.CarUiListPreference {*;}
-keep class com.android.car.ui.preference.CarUiMultiSelectListPreference {*;}
-keep class com.android.car.ui.preference.CarUiEditTextPreference {*;}
-keep class com.android.car.ui.preference.CarUiSwitchPreference {*;}
-keep class com.android.car.ui.preference.CarUiPreference {*;}
-keep class com.android.car.ui.preference.** extends com.android.car.ui.preference.CarUiPreference {*;}

# required for default scrollbar implementation.
-keep class com.android.car.ui.recyclerview.DefaultScrollBar {*;}

# required for MenuItem click listeners
-keepclasseswithmembers class * extends android.app.Activity {
  public void * (com.android.car.ui.toolbar.MenuItem);
}

-dontwarn com.android.car.ui.sharedlibrary.oemapis.**

# requried for accessing oem apis
-keep class com.android.car.ui.sharedlibrarysupport.OemApiUtil {*;}


# Required for AppCompat instantiating our layout inflater factory,
# Otherwise it will be obfuscated and the reference to it in xml won't match
-keep class com.android.car.ui.CarUiLayoutInflaterFactory {*;}
