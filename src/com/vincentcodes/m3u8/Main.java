package com.vincentcodes.m3u8;

import java.util.List;
import java.util.stream.Stream;

import com.vincentcodes.util.commandline.Command;
import com.vincentcodes.util.commandline.CommandLineParser;
import com.vincentcodes.util.commandline.ParserConfig;

public class Main {
    // public static void main(String[] args) {
    //     MediaDownloader mediaDownloader = new MediaDownloader("http://127.0.0.1:1234/media0/media.m3u8", "others/tests");
    //     mediaDownloader.downloadAndParse();
    //     mediaDownloader.findResources();
    //     mediaDownloader.downloadAllResources();
    //     mediaDownloader.generateLocalMedia();
    //     mediaDownloader.writeToFile();
    //     // MasterDownloader masterDownloader = new MasterDownloader("http://127.0.0.1:1234/master.m3u8", "others/tests");
    //     // // MasterDownloader masterDownloader = new MasterDownloader("others/tests/master.m3u8", "others/tests");
    //     // MasterPlaylist master = masterDownloader.downloadAndParse();
    //     // System.out.println(master.getAvailableBandwidths());
    //     // System.out.println(master.getAvailableGroupIds());
    //     // List<MediaDownloader> mediaDownloaders = masterDownloader.findResources("group_name1", null, 141226, 542726);
    //     // System.out.println(mediaDownloaders);
    //     // for(MediaDownloader downloader : mediaDownloaders){
    //     //     downloader.downloadAndParse();
    //     //     downloader.findResources();
    //     //     downloader.downloadOne();
    //     //     // downloader.downloadAllResources();
    //     //     downloader.generateLocalMedia();
    //     //     // downloader.writeToFile();
    //     // }
    //     // masterDownloader.generateLocalMaster("group_name1", null, 141226, 542726);
    //     // // masterDownloader.writeToFile();
    // }
    public static void main(String[] args) {
        handleArgs(args);
        MediaDownloader.stopExecutor();
    }

    private static void handleArgs(String[] args){
        ParserConfig config = new ParserConfig();
        config.addOption("--help", true, "show this help");
        config.addOption("--master", true, "The m3u8 file you are inputing in is a master playlist");
        config.addOption("--outfolder", false, "Set output folder to this location. If url is a local file, then the m3u8 file MUST locate INSIDE the outfolder");
        config.addOption("-o", false, "alias for '--outfolder'");
        config.addOption("--bandwidth", false, "Set the bandwidth to start downloading media playlists from master playlist, use ',' to separate multiple bandwidths you wanna select (eg. -b 123,324)");
        config.addOption("-b", false, "alias for '--bandwidth'");
        config.addOption("--audioid", false, "Use audioId to narrow down the search result of variant streams");
        config.addOption("-aid", false, "alias for '--audioid'");
        config.addOption("--videoid", false, "Use videoId to narrow down the search result of variant streams");
        config.addOption("-vid", false, "alias for '--videoid'");
        config.addOption("--progressive", true, "download one ts file from each media at a time.");
        config.addOption("--threads", false, "set number of threads to be used, default = 1.");
        config.addOption("--unique", true, "unique names for ts files");

        CommandLineParser parser = new CommandLineParser(config);
        Command cmd = parser.parse(args);

        if(cmd.hasOption("--help") || cmd.getParameters().size() == 0){
            printhelp(config);
            return;
        }

        String url = cmd.getParameter(0);
        String outfolder;
        int[] bandwidth = null;
        String audioId;
        String videoId;

        if((outfolder = cmd.getOptionValue("--outfolder")) != null
        || (outfolder = cmd.getOptionValue("-o")) != null);
        if(outfolder == null) outfolder = "./";
        
        String optionValue;
        if((optionValue = cmd.getOptionValue("--bandwidth")) != null
        || (optionValue = cmd.getOptionValue("-b")) != null){
            if(optionValue.contains(","))
                bandwidth = Stream.of(optionValue.split(",")).mapToInt(Integer::parseInt).toArray();
            else bandwidth = new int[]{Integer.parseInt(optionValue)};
        }
        
        if((audioId = cmd.getOptionValue("--audioid")) != null
        || (audioId = cmd.getOptionValue("-aid")) != null);
        
        if((videoId = cmd.getOptionValue("--videoid")) != null
        || (videoId = cmd.getOptionValue("-vid")) != null);

        if(cmd.getOptionValue("--unique") != null)
            MediaDownloader.UNIQUE_TS_NAMES = true;

        if(cmd.hasOption("--threads")) 
            MediaDownloader.NUM_THREADS = Integer.parseInt(cmd.getOptionValue("--threads"));
        
        if(cmd.hasOption("--master")){
            MasterDownloader masterDownloader = new MasterDownloader(url, outfolder);
            MasterPlaylist master = masterDownloader.downloadAndParse();
            if(bandwidth == null){
                System.out.println("Available bandwidths: " + master.getAvailableBandwidths());
                System.out.println("Available GroupIds: " + master.getAvailableGroupIds());
                return;
            }
            masterDownloader.generateLocalMaster(audioId, videoId, bandwidth);
            masterDownloader.writeToFile();
            List<MediaDownloader> downloaders = masterDownloader.findResources(audioId, videoId, bandwidth);
            if(cmd.hasOption("--progressive")){
                for(MediaDownloader downloader : downloaders){
                    downloader.downloadAndParse();
                    downloader.findResources();
                }
                while(downloaders.stream().anyMatch(downloader -> downloader.hasFilesInQueue())){
                    downloaders.forEach(downloader -> downloader.downloadOne());
                }
                downloaders.forEach(media -> media.waitUntilAllDownloaded());
                for(MediaDownloader downloader : downloaders){
                    downloader.writeToFile(downloader.generateLocalMedia());
                }
                return;
            }
            for(MediaDownloader downloader : downloaders){
                downloader.downloadAndParse();
                downloader.findResources();
                downloader.downloadAllResources();
                downloader.waitUntilAllDownloaded();
                downloader.writeToFile(downloader.generateLocalMedia());
            }
            System.out.println("[*] Use ffmpeg to combine the ts files: ffmpeg -i local_media.m3u8 -c copy output.mp4");
            return;
        }
        MediaDownloader downloader = new MediaDownloader(url, outfolder);
        downloader.downloadAndParse();
        downloader.findResources();
        downloader.downloadAllResources();
        downloader.waitUntilAllDownloaded();
        downloader.writeToFile(downloader.generateLocalMedia());
        System.out.println("[*] Use ffmpeg to combine the ts files: ffmpeg -i local_media.m3u8 -c copy output.mp4");
    }

    private static void printhelp(ParserConfig config){
        System.out.println("m3u8downloader: help menu");
        System.out.println("Usage m3u8downloader [options] <url / file>");
        System.out.println(config.getOptionsHelpString());
        System.exit(0);
    }
}
