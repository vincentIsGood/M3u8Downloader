package com.vincentcodes.m3u8;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.vincentcodes.m3u8.types.KeyTag;

public class MediaDownloader {
    public static int NUM_THREADS = 1;
    private static ExecutorService executorService;

    public static void initExecutor(){
        if(executorService == null)
            executorService = Executors.newFixedThreadPool(NUM_THREADS);
    }
    public static void stopExecutor(){
        if(executorService == null) return;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("[-] Executor did not terminate");
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private final Lock lock;
    private final Condition doneDownloadNotif;

    private String baseUrl = "";
    private String url;
    private String outfolder;
    private MediaPlaylist media;
    private MediaPlaylist localMedia;

    private Deque<String> finalPaths; // queue used to enable pause, resume download
    private int totalFilesNeeded = 0;
    private int noFilesDownloaded = 0;
    private int tasksSubmitted = 0;
    
    /**
     * @param url remote or local resource (note: if it is a local resource, 
     * please make sure the file exists locally and the m3u8 uses absolute url format.)
     * @param outfolder files output location
     */
    public MediaDownloader(String url, String outfolder) {
        initExecutor();

        this.url = url;
        if(DownloadUtils.isAbsolute(url)) baseUrl = DownloadUtils.getBaseUrl(url);
        this.outfolder = DownloadUtils.createFolder(outfolder);
        finalPaths = new ArrayDeque<>();

        lock = new ReentrantLock();
        doneDownloadNotif = lock.newCondition();
    }

    /**
     * Download the m3u8 file
     */
    public MediaPlaylist downloadAndParse(){
        String m3u8Content;
        if(DownloadUtils.isLocalFile(url)){
            m3u8Content = new String(DownloadUtils.readLocalFile(url));
        }else m3u8Content = new String(DownloadUtils.downloadNoDuplicate(url, baseUrl, outfolder, true));
        System.out.println("[*] Parsing m3u8 file...");
        media = MediaPlaylistParser.parse(m3u8Content);
        return media;
    }

    /**
     * {@link #downloadAndParse()} before finding resources
     */
    public void findResources(){
        // Expect abs path when baseUrl is empty
        System.out.println("[*] Number of keys to download: " + media.keys.size());
        for(KeyTag key : media.keys.values())
            finalPaths.add(DownloadUtils.isRemote(key.getURI())? key.getURI() : DownloadUtils.getFilenameFromUrl(key.getURI()));
        System.out.println("[*] Number of segments to download: " + media.segments.size());
        for(String seg : media.segments)
            finalPaths.add(DownloadUtils.isRemote(seg)? seg : DownloadUtils.getFilenameFromPath(seg));
        totalFilesNeeded = finalPaths.size();
    }

    /**
     * Run {@link #downloadAndParse()} and {@link #findResources()}
     * before running this function
     */
    public void downloadAllResources(){
        while(tasksSubmitted < totalFilesNeeded)
            downloadOne();
    }

    /**
     * Run {@link #downloadAndParse()} and {@link #findResources()}
     * before running this function
     */
    public void downloadOne(){
        if(tasksSubmitted < totalFilesNeeded){
            downloadOneUnsafe();
            tasksSubmitted++;
        }
    }

    /**
     * Will not check whether the queue is empty or not.
     */
    private void downloadOneUnsafe(){
        // Submit tasks asynchonously
        executorService.submit(()->{
            String finalPath;
            synchronized(finalPaths){
                finalPath = finalPaths.pop();
            }
            if(DownloadUtils.isRemote(finalPath)){
                DownloadUtils.downloadFile(finalPath, outfolder);
            }else DownloadUtils.downloadNoDuplicateNoReturn(finalPath, baseUrl, outfolder);

            lock.lock();
            try{
                noFilesDownloaded++;
                if(noFilesDownloaded == totalFilesNeeded){
                    doneDownloadNotif.signal();
                }
            }finally{
                lock.unlock();
            }
        });
    }

    public boolean hasFilesInQueue(){
        return tasksSubmitted < totalFilesNeeded;
    }

    /**
     * Should call {@link #downloadOne()} or {@link #downloadAllResources()} 
     * first before this function.
     */
    public void waitUntilAllDownloaded(){
        lock.lock();
        try{
            while(noFilesDownloaded < totalFilesNeeded){
                try{
                    doneDownloadNotif.await();
                }catch(InterruptedException e){}
            }
            System.out.println("[*] For '"+ getOutPath() +"', progress: "+ noFilesDownloaded +"/"+ totalFilesNeeded + " ("+ (noFilesDownloaded/(double)totalFilesNeeded)*100 +"%)");
        }finally{
            lock.unlock();
        }
        return;
    }

    public MediaPlaylist generateLocalMedia(){
        MediaPlaylist newMedia = new MediaPlaylist();
        newMedia.version = media.version;
        newMedia.type = media.type;
        newMedia.iframesOnly = media.iframesOnly;
        newMedia.targetDuration = media.targetDuration;
        newMedia.sequence = media.sequence;
        newMedia.disSequence = media.disSequence;
        newMedia.doesPlaylistEnds = media.doesPlaylistEnds;
        for(KeyTag key : media.keys.values()){
            Map<String, String> attributes = key.getAttributes();
            attributes.put("URI", DownloadUtils.getFilenameFromPath(key.getURI()));
            KeyTag newKey = new KeyTag(attributes);
            newMedia.addKey(newKey);
        }
        for(int i = 0; i < media.segments.size(); i++){
            String filename = DownloadUtils.getFilenameFromPath(media.segments.get(i));
            newMedia.addSegment(filename, media.segDurations.get(i));
        }
        this.localMedia = newMedia;
        return newMedia;
    }

    /**
     * Writes media to its own output folder (eg. playlist0/local_media.m3u8)
     * Note: {@link #getLocalMedia()} must not return null.
     */
    public void writeToFile(){
        writeToFile(getLocalMedia());
    }
    public void writeToFile(MediaPlaylist media){
        if(media == null) 
            throw new NullPointerException("localMedia cannot be null");
        try(FileOutputStream fos = new FileOutputStream(getOutPath())){
            String m3u8Content = PlaylistGenerator.toM3u8(media);
            fos.write(m3u8Content.getBytes());
        }catch(IOException e){
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Call {@link #downloadAndParse()} before using this function
     */
    public MediaPlaylist getMedia(){
        return media;
    }
    /**
     * Call {@link #generateLocalMedia()} before using this function
     */
    public MediaPlaylist getLocalMedia(){
        return localMedia;
    }

    public String getUrl() {
        return url;
    }

    private String getOutPath() {
        return outfolder + "local_media.m3u8";
    }

    public String toString(){
        return String.format("{url: %s, outpath: %s}", getUrl(), getOutPath());
    }
}
