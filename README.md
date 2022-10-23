# M3u8 Downloader
M3u8 Downloader does not only download m3u8 file. It downloads media m3u8 and its ts files as well.

## Manually Compile
1. Compile the code using the bat file `DEV_compile.bat`
2. After running `DEV_GetJar.bat`, a jar file named `m3u8downloader-vX.Y.Z.jar` is created
3. Then you are required to de-compress libraries (not source jars) inside `lib/`
4. Add the de-compressed folders (except `META-INF/`) into the newly created `m3u8downloader-vX.Y.Z.jar`

## How to use it
### Examples
To find available bandwidths from master.m3u8:
```sh
m3u8downloader-vX.Y.Z.jar --outfolder output/ --master http://127.0.0.1/master.m3u8
```

To download *all* media m3u8 files and its related segments with a provided bandwidth:
```sh
m3u8downloader-vX.Y.Z.jar --outfolder output/ --master http://127.0.0.1/master.m3u8 --bandwidth 12345
```

To download a media m3u8 with its segment files:
```sh
m3u8downloader-vX.Y.Z.jar --outfolder output/ http://127.0.0.1/media.m3u8
```

## Fixed bug
If you wanted to use `outfolder` option and you would like to use a local m3u8 file to download its remote segment files. You were required to create the `outfolder` yourself and move the m3u8 to the `outfolder`.

This bug is fixed.