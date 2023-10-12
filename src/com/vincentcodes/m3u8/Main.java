package com.vincentcodes.m3u8;

import java.util.List;

import com.vincentcodes.util.commandline.ArgumentObjectMapper;
import com.vincentcodes.util.commandline.Command;
import com.vincentcodes.util.commandline.ObjectMapperParseResult;

public class Main {
    public static void main(String[] args) {
        handleArgs(args);
        MediaDownloader.stopExecutor();
    }

    private static void handleArgs(String[] args){
        ObjectMapperParseResult<CmdLineOptions> parseResult = ArgumentObjectMapper.parseToObject(args, CmdLineOptions.class);
        CmdLineOptions cmdOptions = parseResult.result;
        Command cmd = parseResult.command;

        if(cmdOptions.help || cmd.getParameters().size() == 0){
            parseResult.simplePrintHelp("Usage m3u8downloader [options] <url / file>");
            return;
        }

        String url = cmd.getParameter(0);

        MediaDownloader.UNIQUE_TS_NAMES = cmdOptions.unique;
        MediaDownloader.NUM_THREADS = cmdOptions.threads;
        
        if(cmdOptions.master){
            handleMasterM3u8(cmdOptions, url);
            return;
        }
        handleMediaM3u8(cmdOptions, url);
    }

    private static void handleMediaM3u8(CmdLineOptions cmdOptions, String url){
        MediaDownloader downloader = new MediaDownloader(url, cmdOptions.outfolder);
        downloader.downloadAndParse();
        downloader.findResources();
        downloader.downloadAllResources();
        downloader.waitUntilAllDownloaded();
        downloader.writeToFile(downloader.generateLocalMedia());
        System.out.println("[*] Use ffmpeg to combine the ts files: ffmpeg -i local_media.m3u8 -c copy output.mp4");
    }

    private static void handleMasterM3u8(CmdLineOptions cmdOptions, String url){
        MasterDownloader masterDownloader = new MasterDownloader(url, cmdOptions.outfolder);
        MasterPlaylist master = masterDownloader.downloadAndParse();
        int[] bandwidth = cmdOptions.bandwidth;
        String audioId = cmdOptions.audioid;
        String videoId = cmdOptions.videoid;
        if(bandwidth == null){
            System.out.println("Available bandwidths: " + master.getAvailableBandwidths());
            System.out.println("Available GroupIds: " + master.getAvailableGroupIds());
            return;
        }
        masterDownloader.generateLocalMaster(audioId, videoId, bandwidth);
        masterDownloader.writeToFile();
        List<MediaDownloader> downloaders = masterDownloader.findResources(audioId, videoId, bandwidth);
        if(cmdOptions.progressive){
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
        System.out.println("[*] Use ffmpeg to combine the ts files: ffmpeg -i local_master.m3u8 -c copy output.mp4");
    }
}
