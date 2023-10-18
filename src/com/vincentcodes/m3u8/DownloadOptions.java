package com.vincentcodes.m3u8;

import java.util.HashMap;
import java.util.Map;

public class DownloadOptions{
    public Map<String, String> queries = new HashMap<>();
    public Map<String, String> headers = new HashMap<>();
    
    public String toString(){
        return String.format("{DownloadOptions queries: %s, headers: %s}", queries, headers);
    }
}
