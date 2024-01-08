package com.vincentcodes.m3u8;

import com.vincentcodes.util.commandline.annotations.CmdOption;

public class CmdLineOptions {
    @CmdOption(shortForm = "h", description = "Show this help")
    public boolean help;

    @CmdOption(description = "The m3u8 file you are inputing in is a master playlist")
    public boolean master;

    @CmdOption(shortForm = "o", description = "Set output folder to this location")
    public String outfolder = "./";

    @CmdOption(shortForm = "b", description = "Set the bandwidth to start downloading media playlists from master playlist, use ',' to separate multiple bandwidths you wanna select (eg. -b 123,324)")
    public int[] bandwidth;

    @CmdOption(shortForm = "aid", description = "Use audioId to narrow down the search result of variant streams")
    public String audioid;

    @CmdOption(shortForm = "vid", description = "Use videoid to narrow down the search result of variant streams")
    public String videoid;

    @CmdOption(description = "Download one ts file from each media at a time")
    public boolean progressive;

    @CmdOption(shortForm = "t", description = "Set number of threads to be used (default = 1)")
    public int threads = 1;

    @CmdOption(description = "Unique names for ts files")
    public boolean unique;

    @CmdOption(shortForm = "options", parameterDescription = "file.json", description = "Custom options in json")
    public String optionsfile;

    @CmdOption(value = "normalize", shortForm = "norm", description = "Normalize paths (eg. a/b/c/d/../e -> a/b/c/e, ./a -> a)")
    public boolean normalizePath;

    // @CmdOption(description = "Live download (this involves downloading the same m3u8 media file over and over again)")
    // public boolean live;
}
