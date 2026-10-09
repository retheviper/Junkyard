-keep class com.twelvemonkeys.** extends javax.imageio.spi.IIOServiceProvider {
    *;
}

-keep class com.github.ustc_zzzz.imageio.avif.** {
    *;
}

# FileKit, as documented at https://filekit.mintlify.app/dialogs/setup (ProGuard Configuration).
# Its native dialogs use JNA, which native code calls back into (e.g. Native.dispose) and which reflects on
# Library interfaces and Structure fields.
-keep class com.sun.jna.** {
    *;
}
-keep class * implements com.sun.jna.** {
    *;
}

# Linux dialogs go through the XDG desktop portal over D-Bus.
-keep class org.freedesktop.dbus.** {
    *;
}
-keep class io.github.vinceglb.filekit.dialogs.platform.xdg.** {
    *;
}
-keepattributes Signature,InnerClasses,RuntimeVisibleAnnotations

# slf4j-simple is only discovered through ServiceLoader.
-keep class org.slf4j.simple.SimpleServiceProvider {
    *;
}
