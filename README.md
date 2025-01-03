# Junkyard

## TL;DR

This is a collection of some junk tools and codes with GUI. Just for fun and learning purpose.

Some tools are based on: https://github.com/retheviper/PythonTools

## Features

- Archive subdirectories to zip in a directory
- Convert RAR to ZIP
- Change file extension
- Convert image format
- Create thumbnail (resize) image
- Resync subtitle
- Dark mode
- Multiple language support (English, Japanese, Korean)

## Target Platform

- Windows
- Linux (has text encoding issue)
- macOS

## Screenshot

<details>
<summary>Click to expand screenshots</summary>

![archive.png](misc/archive.png)
![rar_to_zip.png](misc/rar_to_zip.png)
![change_extension.png](misc/change_extension.png)
![convert_format.png](misc/convert_format.png)
![create_thumbnail.png](misc/create_thumbnail.png)
![resync_subtitle.png](misc/resync_subtitle.png)

</details>

## Used Libraries

- [Compose for Desktop](https://www.jetbrains.com/lp/compose/)
- [FileKit](https://github.com/vinceglb/FileKit)
- [Junrar](https://github.com/junrar/junrar)
- [Srimage](https://github.com/sksamuel/scrimage)

## to Run

### macOS, Linux

```bash
./gradlew run
```

### Windows

```bash
gradlew.bat run
```

## to Build

### just runnable

#### macOS, Linux

```bash
./gradlew createDistributable
```

#### Windows

```bash
gradlew.bat createDistributable
```

### with installer

#### macOS, Linux

```bash
./gradlew package
```

#### Windows

```bash
gradlew.bat package
```