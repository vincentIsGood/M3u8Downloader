package com.vincentcodes.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.vincentcodes.m3u8.MasterDownloader;
import com.vincentcodes.m3u8.MasterPlaylist;
import com.vincentcodes.m3u8.MediaDownloader;

/**
 * FunctionalTest
 */
@TestInstance(Lifecycle.PER_CLASS)
public class MasterM3u8Test {
    @Test
    public void grab_available_bandwidths(){
        MasterDownloader masterDownloader = new MasterDownloader("http://127.0.0.1:1234/master.m3u8", "test_m3u8/result_master_m3u8");
        MasterPlaylist master = masterDownloader.downloadAndParse();
        Set<Integer> availableBandwidths = master.getAvailableBandwidths();
        assertTrue(availableBandwidths.contains(141644));
        assertTrue(availableBandwidths.contains(543144));
        assertTrue(availableBandwidths.contains(691644));
        Set<String> availableGroupIds = master.getAvailableGroupIds();
        assertTrue(availableGroupIds.contains("group_name1"));
        assertTrue(availableGroupIds.contains("group_name2"));
    }
    
    @Test
    public void download_masterM3u8_and_its_files() throws IOException, InterruptedException{
        MasterDownloader masterDownloader = new MasterDownloader("http://127.0.0.1:1234/master.m3u8", "test_m3u8/result_master_m3u8");
        masterDownloader.downloadAndParse();
        List<MediaDownloader> mediaDownloaders = masterDownloader.findResources("group_name1", null, 543144);
        for(MediaDownloader downloader : mediaDownloaders){
            downloader.downloadAndParse();
            downloader.findResources();
            downloader.downloadAllResources();
            downloader.waitUntilAllDownloaded();
            downloader.generateLocalMedia();
            downloader.writeToFile();
        }
        masterDownloader.generateLocalMaster("group_name1", null, 543144);
        masterDownloader.writeToFile();

        ProcessBuilder sha1sum = new ProcessBuilder("sha1sum", "-c", "result_master_m3u8.sha1");
        sha1sum.directory(new File("./test_m3u8"));
        sha1sum.inheritIO();
        Process result = sha1sum.start();
        assertEquals(0, result.waitFor());
    }
}