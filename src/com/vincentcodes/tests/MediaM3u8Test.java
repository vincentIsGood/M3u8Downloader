package com.vincentcodes.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.vincentcodes.m3u8.MediaDownloader;

/**
 * FunctionalTest
 */
@TestInstance(Lifecycle.PER_CLASS)
public class MediaM3u8Test {
    @BeforeEach
    public void setup(){
        MediaDownloader.NUM_THREADS = 1;
    }

    @AfterEach
    public void cleanup(){
        MediaDownloader.stopExecutor();
    }

    @Test
    public void download_from_mediaM3u8_no_errors() throws IOException, InterruptedException{
        MediaDownloader mediaDownloader = new MediaDownloader("http://127.0.0.1:1234/video1/media.m3u8", "test_m3u8/result_media_m3u8");
        mediaDownloader.downloadAndParse();
        mediaDownloader.findResources();
        mediaDownloader.downloadAllResources();
        mediaDownloader.waitUntilAllDownloaded();
        mediaDownloader.generateLocalMedia();
        mediaDownloader.writeToFile();
        
        ProcessBuilder sha1sum = new ProcessBuilder("sha1sum", "-c", "result_media_m3u8.sha1");
        sha1sum.directory(new File("./test_m3u8"));
        sha1sum.inheritIO();
        Process result = sha1sum.start();
        assertEquals(0, result.waitFor());
    }
    
    @Test
    public void download_from_mediaM3u8_with_threads() throws IOException, InterruptedException{
        MediaDownloader.NUM_THREADS = 4;
        MediaDownloader mediaDownloader = new MediaDownloader("http://127.0.0.1:1234/video1/media.m3u8", "test_m3u8/result_media_m3u8_threaded");
        mediaDownloader.downloadAndParse();
        mediaDownloader.findResources();
        mediaDownloader.downloadAllResources();
        mediaDownloader.waitUntilAllDownloaded();
        mediaDownloader.generateLocalMedia();
        mediaDownloader.writeToFile();
        
        ProcessBuilder sha1sum = new ProcessBuilder("sha1sum", "-c", "result_media_m3u8_threaded.sha1");
        sha1sum.directory(new File("./test_m3u8"));
        sha1sum.inheritIO();
        Process result = sha1sum.start();
        assertEquals(0, result.waitFor());
    }
}