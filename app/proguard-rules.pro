# Adicionar ao proguard-rules.pro existente
-keep class com.luisborges.limpadorandroid.model.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { *; }
-keep class androidx.work.** { *; }
