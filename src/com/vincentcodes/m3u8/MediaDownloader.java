package com.vincentcodes.m3u8;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.vincentcodes.m3u8.types.KeyTag;

public class MediaDownloader {
    public static DownloadOptions TS_DOWNLOAD_OPTIONS;
    public static DownloadOptions DOWNLOAD_OPTIONS;

    public static int NUM_THREADS = 1;
    public static boolean UNIQUE_TS_NAMES = false;
    public static int MAX_RETRIES = 5;

    public static boolean normalizePath;

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
        } finally {
            executorService = null;
        }
    }

    private final Lock lock;
    private final Condition doneDownloadNotif;

    private String baseUrl = "";
    private String url;
    private String urlDirPath; // "example/file.txt" -> "example/"
    private String outfolder;
    private MediaPlaylist media;
    private MediaPlaylist localMedia;

    /**
     * finalPath is a term used to denote "remote download path / link"
     */
    private Deque<String> finalPaths; // queue used to enable pause, resume download
    private Map<String, Integer> pathRetries;
    private Map<String, String> finalPathsToFilename;
    private int totalFilesNeeded = 0;
    private int noFilesDownloaded = 0;
    private int tasksSubmitted = 0;

    private DownloadUtils downloader;
    private DownloadUtils tsDownloader;
    
    /**
     * @param url remote or local resource (note: if it is a local resource, 
     * please make sure the file exists locally and the m3u8 uses absolute url format.)
     * @param outfolder files output location
     */
    public MediaDownloader(String url, String outfolder) {
        initExecutor();

        this.url = url;
        this.urlDirPath = DownloadUtils.getPathExcludeName(this.url) + "/";
        if(DownloadUtils.isAbsolute(url)) baseUrl = DownloadUtils.getBaseUrl(url);
        this.outfolder = DownloadUtils.createFolder(outfolder);
        finalPaths = new ArrayDeque<>();
        pathRetries = new HashMap<>();
        finalPathsToFilename = new HashMap<>();
        downloader = new DownloadUtils(DOWNLOAD_OPTIONS);
        tsDownloader = new DownloadUtils(TS_DOWNLOAD_OPTIONS);

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
        }else m3u8Content = new String(downloader.downloadNoDuplicate(url, baseUrl, outfolder,  null, true));
        System.out.println("[*] Parsing m3u8 file...");
        media = MediaPlaylistParser.parse(m3u8Content);
        if(media.segments.size() == 0){
            System.out.println("[!] No segments found in this media m3u8 file. Is this a master file? (use '--master' if needed)");
        }
        return media;
    }

    /**
     * {@link #downloadAndParse()} before finding resources
     */
    public void findResources(){
        // Expect abs path when baseUrl is empty
        int counter = 0;
        System.out.println("[*] Number of keys to download: " + media.keys.size());
        for(KeyTag key : media.keys.values()){
            if(key.getMethod().trim().toLowerCase().equals("none")) continue;
            String finalPath = DownloadUtils.isRemote(key.getURI())? key.getURI() : DownloadUtils.getFilenameFromUrl(key.getURI());
            finalPaths.add(finalPath);
        }
        System.out.println("[*] Number of segments to download: " + media.segments.size());
        for(String seg : media.segments){
            // eg. /segment.ts == example.com/segment.ts
            // eg.  segment.ts == example.com/media/segment.ts
            String finalPath = resolveFinalPath(urlDirPath, seg);
            if(finalPaths.size() > 0 && finalPaths.peekLast().equals(finalPath)){
                // Skip duplicating ts files
                continue;
            }
            if(normalizePath && DownloadUtils.isRemote(finalPath)){
                try {
                    finalPath = new URL(finalPath).toURI().normalize().toString();
                } catch (MalformedURLException | URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            finalPaths.add(finalPath);
            if(UNIQUE_TS_NAMES)
                finalPathsToFilename.put(finalPath, (counter++) + "_" + DownloadUtils.getFilenameFromPath(seg));
            else finalPathsToFilename.put(finalPath, DownloadUtils.getFilenameFromPath(seg));
        }
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

            boolean isRetry = pathRetries.containsKey(finalPath);
            boolean overRetryLimit = isRetry && pathRetries.get(finalPath) > MAX_RETRIES;
            if(!overRetryLimit){
                if(isRetry){
                    System.out.println("[*] The next download is attempt: " + pathRetries.get(finalPath));
                }

                try{
                    if(DownloadUtils.isRemote(finalPath)){
                        tsDownloader.downloadFile(finalPath, outfolder, finalPathsToFilename.get(finalPath));
                    }else tsDownloader.downloadNoDuplicateNoReturn(finalPath, baseUrl, outfolder, finalPathsToFilename.get(finalPath));
                }catch(UncheckedIOException e){
                    System.out.println("[-] Download Error: " + e.getMessage());
                    synchronized(finalPaths){
                        finalPaths.add(finalPath);
                        pathRetries.put(finalPath, pathRetries.getOrDefault(finalPath, 0)+1);
                    }
                    return; // do not count it as downloaded
                }
                pathRetries.remove(finalPath);
            }else{
                System.out.println("[-] Over max retry limit for: " + finalPath);
            }

            lock.lock();
            try{
                noFilesDownloaded++;
                if(noFilesDownloaded >= totalFilesNeeded){
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
        }finally{
            lock.unlock();
        }
        if(pathRetries.size() > 0){
            noFilesDownloaded -= pathRetries.size();
            System.out.println("[!] These files cannot be downloaded: " + pathRetries.keySet());
        }
        System.out.println("[*] For '"+ getOutPath() +"', progress: "+ noFilesDownloaded +"/"+ totalFilesNeeded + " ("+ (noFilesDownloaded/(double)totalFilesNeeded)*100 +"%)");
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
        String previousPath = "";
        for(int i = 0; i < media.segments.size(); i++){
            String seg = media.segments.get(i);
            String finalPath = resolveFinalPath(urlDirPath, seg);
            
            if(normalizePath && DownloadUtils.isRemote(finalPath)){
                try {
                    finalPath = new URL(finalPath).toURI().normalize().toString();
                } catch (MalformedURLException | URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            finalPaths.add(finalPath);

            if(previousPath.equals(finalPath)){
                // encounter duplicate ts file -> combine the duration ('cause they maybe bounded by BYTERANGE)
                int lastIndex = newMedia.segDurations.size()-1;
                newMedia.segDurations.set(lastIndex, newMedia.segDurations.get(lastIndex) + media.segDurations.get(i));
            }else
                newMedia.addSegment(finalPathsToFilename.get(finalPath), media.segDurations.get(i));
            previousPath = finalPath;
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

    /**
     * @param urlDirPath eg. ("path/to/dir/", "https://example.com/dir/")
     * @param seg eg. ("path/to/asd.ts", "/remote/fullpath/to/asd.ts", "../asd.ts")
     * @return If it's a local file, it stays that way. 
     * If it's a remote file, absolute path is returned
     */
    private static String resolveFinalPath(String urlDirPath, String seg){
        // localfile
        String finalPath = urlDirPath + seg;

        // remote files
        if(DownloadUtils.isRemote(seg)){
            finalPath = seg;
        }else if(seg.startsWith("/")){
            finalPath = seg.substring(1);
        }
        return finalPath;
    }
}
