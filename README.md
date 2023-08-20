# M3u8 Downloader
M3u8 Downloader does not only download m3u8 file. It downloads media m3u8 and its ts files as well.

```sh
$ java -jar m3u8downloader-v3.0.0.jar --help
m3u8downloader: help menu
Usage m3u8downloader [options] <url / file>
    --help         show this help
    --master       The m3u8 file you are inputing in is a master playlist
    --outfolder    Set output folder to this location. If url is a local file, then the m3u8 file MUST locate INSIDE the outfolder
    -o             alias for '--outfolder'
    --bandwidth    Set the bandwidth to start downloading media playlists from master playlist, use ',' to separate multiple bandwidths you wanna select (eg. -b 123,324)
    -b             alias for '--bandwidth'
    --audioid      Use audioId to narrow down the search result of variant streams
    -aid           alias for '--audioid'
    --videoid      Use videoId to narrow down the search result of variant streams
    -vid           alias for '--videoid'
    --progressive  download one ts file from each media at a time.
    --threads      set number of threads to be used, default = 1.
```

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