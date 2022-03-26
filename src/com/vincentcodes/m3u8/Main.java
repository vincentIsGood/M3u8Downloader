package com.vincentcodes.m3u8;

import java.util.List;
import java.util.stream.Collectors;

import com.vincentcodes.util.commandline.Command;
import com.vincentcodes.util.commandline.CommandLineParser;
import com.vincentcodes.util.commandline.ParserConfig;

public class Main {
    public static void main(String[] args) {
        // TODO: outfolder bug (involving local files)

        ParserConfig config = new ParserConfig();
        config.addOption("--help", true, "show this help");
        config.addOption("--master", true, "The m3u8 file you are inputing in is a master playlist");
        config.addOption("--outfolder", false, "Set output folder to this location. If url is a local file, then the m3u8 file MUST locate INSIDE the outfolder");
        config.addOption("--bandwidth", false, "Set the bandwidth to start downloading media playlists from master playlist");
        config.addOption("-b", false, "alias for '--bandwidth'");
        CommandLineParser parser = new CommandLineParser(config);
        Command cmd = parser.parse(args);
        
        if(cmd.hasOption("--help") || cmd.getParameters().size() == 0){
            printhelp(config);
        }

        String url = cmd.getParameter(0);
        String outfolder = cmd.getOptionValue("--outfolder");
        int bandwidth = -1;
        
        if(cmd.getOptionValue("--bandwidth") != null)
            bandwidth = Integer.parseInt(cmd.getOptionValue("--bandwidth"));
        else if(cmd.getOptionValue("-b") != null)
            bandwidth = Integer.parseInt(cmd.getOptionValue("-b"));
        
        if(outfolder == null)
            outfolder = "";
        if(cmd.hasOption("--master")){
            if(bandwidth == -1){
                MasterPlaylist master = M3u8Downloader.master(url, outfolder);
                List<Integer> sorted = master.getAvailableBandwidths().stream().sorted().collect(Collectors.toList());
                System.out.println("Available bandwidths: " + sorted);
                return;
            }
            M3u8Downloader.master(url, outfolder, bandwidth);
            return;
        }
        M3u8Downloader.media(url, outfolder);
    }

    private static void printhelp(ParserConfig config){
        System.out.println("m3u8downloader: help menu");
        System.out.println(config.getOptionsHelpString());
        System.exit(0);
    }
}
