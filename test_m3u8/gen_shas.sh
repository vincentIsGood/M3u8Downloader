for file in result_media_m3u8/*; do sha1sum $file; done > result_media_m3u8.sha1
cp result_media_m3u8.sha1 result_media_m3u8_threaded.sha1

for file in $(find result_master_m3u8/); do sha1sum $file; done > result_master_m3u8.sha1