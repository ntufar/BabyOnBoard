# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Room entities
-keep class io.github.ntufar.babyonboard.data.model.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
