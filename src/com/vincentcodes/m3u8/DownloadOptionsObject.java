package com.vincentcodes.m3u8;

public class DownloadOptionsObject {
    public DownloadOptions ts_options;
    public DownloadOptions media_options;
    public DownloadOptions master_options;

    public String toString(){
        return String.format("{DownloadOptionsObject ts_options: %s, media_options: %s, master_options: %s}", ts_options, media_options, master_options);
    }
}