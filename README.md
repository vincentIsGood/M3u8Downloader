# M3u8 Downloader
M3u8 Downloader does not only download m3u8 file. It downloads media m3u8 and its ts files as well.

```sh
$ java -jar m3u8downloader-v3.1.2.jar --help
m3u8downloader: help menu
Usage m3u8downloader [options] <url / file>
    --help                  Show this help
    -h
    --master                The m3u8 file you are inputing in is a master playlist
    --outfolder             Set output folder to this location
    -o
    --bandwidth             Set the bandwidth to start downloading media playlists from master playlist, use ',' to separate multiple bandwidths you wanna select (eg. -b 123,324)
    -b
    --audioid               Use audioId to narrow down the search result of variant streams
    -aid
    --videoid               Use videoid to narrow down the search result of variant streams
    -vid
    --progressive           Download one ts file from each media at a time
    --threads               Set number of threads to be used (default = 1)
    -t
    --unique                Unique names for ts files
    --optionsfile file.json Custom options in json
    -options
```

## Options File: Custom headers and query params
Define json file to customize params and headers you send to remote server.

Example [`test_m3u8/download_options.json`](test_m3u8/download_options.json)
```json
{
    "ts_options": {
        "queries": {
            "asd": "help",
            "key": "value"
        },
        "headers": {
            "X-Test-You": "and",
            "X-Test-Me": "nice"
        }
    },
    "media_options": {
        "queries": {
            "asd": "help",
            "key": "value"
        }
    },
    "master_options": {
        "headers": {
            "X-Test-You": "and",
            "X-Test-Me": "nice"
        }
    }
}
```

Run jar with the options file:
```sh
java -jar m3u8downloader-vX.Y.Z.jar -o dl -options test_m3u8/download_options.json http://127.0.0.1/media.m3u8
```

Requests to be sent according to the above option file:
```
GET /media.m3u8?asd=help&key=value HTTP/1.1
Host: 127.0.0.1
User-Agent: example
Date: ...

GET /example_001.ts?asd=help&key=value HTTP/1.1
Host: 127.0.0.1
User-Agent: example
Date: ...
X-Test-You: and
X-Test-Me: nice
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
java -jar m3u8downloader-vX.Y.Z.jar --outfolder output/ --master http://127.0.0.1/master.m3u8
```

To download *all* media m3u8 files and its related segments with a provided bandwidth:
```sh
java -jar m3u8downloader-vX.Y.Z.jar --outfolder output/ --master http://127.0.0.1/master.m3u8 --bandwidth 12345
```

To download a media m3u8 with its segment files:
```sh
java -jar m3u8downloader-vX.Y.Z.jar --outfolder output/ http://127.0.0.1/media.m3u8
```