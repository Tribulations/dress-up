# ML Kit's subject segmentation (16.0.0-beta1) ships no R8 rules of its own. Minified, it crashes: first with a
# NullPointerException inside ML Kit, and when only its own classes are kept, at app start because ML Kit's component
# registry can't find a component of its common library (both found on the emulator, 2026-11-19). ML Kit finds its parts
# by reflection, so all of ML Kit is kept as it is.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
