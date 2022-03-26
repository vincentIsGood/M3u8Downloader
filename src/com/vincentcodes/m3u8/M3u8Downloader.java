package com.vincentcodes.m3u8;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vincentcodes.m3u8.types.KeyTag;
import com.vincentcodes.m3u8.types.Media;
import com.vincentcodes.m3u8.types.Stream;

/**
 * If an m3u8 uses remote resources, it will download 
 * all resources onto your local computer.
 */
public class M3u8Downloader {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.83 Safari/537.36";

    /**
     * Downloads the m3u8 file ONLY. To download required resources
     * inside the master playlist as well, use this method,
     * {@link #master(String, String, int)}. To use this method correctly,
     * <pre>
     * // To access "new/video/master.m3u8" locally
     * master("master.m3u8", "new/video/");
     * </pre>
     * @param url remote or local resource
     */
    public static MasterPlaylist master(String url, String outFolder){
        String baseUrl = "";
        if(isAbsolute(url))
            baseUrl = getBaseUrl(url);
        outFolder = createFolder(outFolder);
        
        String m3u8Content = new String(downloadNoDuplicate(url, baseUrl, outFolder, true));
        return MasterPlaylistParser.parse(m3u8Content);
    }
    /**
     * Download an m3u8 or find an m3u8 locally and download required
     * resources inside an m3u8 file to make local watching possible.
     * To use this method correctly,
     * <pre>
     * // To access "new/video/master.m3u8" locally
     * master("master.m3u8", "new/video/", 1234);
     * </pre>
     * @param url remote or local resource. If local, do not include 
     * folder to the path
     * @param bandwidth choose a bandwidth to download.
     */
    public static void master(String url, String outFolder, int bandwidth){
        String baseUrl = "";
        if(isAbsolute(url))
            baseUrl = getBaseUrl(url);
        outFolder = createFolder(outFolder);

        String m3u8Content = new String(downloadNoDuplicate(url, baseUrl, outFolder, true));
        System.out.println("[*] Parsing m3u8 file...");
        MasterPlaylist masterPlaylist = MasterPlaylistParser.parse(m3u8Content);
        
        // Choose media based on bandwidth 
        List<Stream> streams = masterPlaylist.findStreamsByBandwidth(bandwidth);
        System.out.println("[*] Found " + streams.size() + " streams with bandiwdth " + bandwidth);
        
        int mediaM3u8Count = 0;
        MasterPlaylist newMaster = new MasterPlaylist();
        for(Stream stream : streams){
            Map<String, String> attributes = stream.getAttributes();
            if(stream.isIframeStream()){
                String finalPath = downloadMediaM3u8(stream.getURI(), baseUrl, outFolder, mediaM3u8Count++);
                attributes.put("URI", finalPath);
                newMaster.addStream(new Stream(attributes, null));
            }else{
                String finalPath = downloadMediaM3u8(stream.playlist, baseUrl, outFolder, mediaM3u8Count++);
                newMaster.addStream(new Stream(attributes, finalPath));
                for(Media media : stream.relatedMedia){
                    finalPath = downloadMediaM3u8(media.getURI(), baseUrl, outFolder, mediaM3u8Count++);
                    Map<String, String> mediaAttributes = media.getAttributes();
                    mediaAttributes.put("URI", finalPath);
                    newMaster.addMedia(new Media(mediaAttributes));
                }
            }
        }
        System.out.println("[*] Creating new m3u8 file: '" + outFolder + "local_master.m3u8'");
        writeToLocalFile(outFolder + "local_master.m3u8", PlaylistGenerator.toM3u8(newMaster), null);
    }
    /**
     * @param resourceUrl eg. http://127.0.0.1/media.m3u8 or "media.m3u8"
     * @param baseUrl eg. http://127.0.0.1/ or ""
     * @param outFolder eg. out/
     * @param m3u8foldername eg. playlist0/
     * @returns final path to the downloaded m3u8 file
     */
    private static String downloadMediaM3u8(String resourceUrl, String baseUrl, String outFolder, int mediaM3u8Count){
        String m3u8foldername = "playlist" + mediaM3u8Count + "/";
        String filename = getFilenameFromUrl(resourceUrl);
        if(!filename.endsWith(".m3u8"))
            throw new IllegalArgumentException("[-] Unrecognized url: " + resourceUrl);
        
        System.out.println("[+] Downloading an m3u8 file named '"+ filename +"'");
        if(!isRemote(resourceUrl))
            media(baseUrl + resourceUrl, outFolder + m3u8foldername);
        else media(resourceUrl, outFolder + m3u8foldername);
        return m3u8foldername + "local_media.m3u8";
    }

    /**
     * Downloads the m3u8 and its segment files (eg. ".ts" files)
     * @param url url or local file
     */
    public static void media(String url, String outFolder){
        String baseUrl = "";
        if(isAbsolute(url))
            baseUrl = getBaseUrl(url);
        outFolder = createFolder(outFolder);
        
        String m3u8Content = new String(downloadNoDuplicate(url, baseUrl, outFolder, true));
        System.out.println("[*] Parsing m3u8 file...");
        MediaPlaylist media = MediaPlaylistParser.parse(m3u8Content);
        Map<String, String> urlToLocalPath = new HashMap<>();
        if(media.keys.size() > 0){
            System.out.println("[*] Found " + media.keys.size() + " encryption key files");
            for(KeyTag key : media.keys.values()){
                if(isRemote(key.getURI()))
                    urlToLocalPath.put(key.getURI(), getFilenameFromUrl(key.getURI()));
                downloadNoDuplicate(key.getURI(), baseUrl, outFolder);
            }
        }
        System.out.println("[*] Number of segments to download: " + media.segments.size());
        for(String segmentUrl : media.segments){
            if(isRemote(segmentUrl))
                urlToLocalPath.put(segmentUrl, getFilenameFromUrl(segmentUrl));
            downloadNoDuplicate(segmentUrl, baseUrl, outFolder);
        }
        System.out.println("[*] Creating new m3u8 file: '" + outFolder + "local_media.m3u8'");
        writeToLocalFile(outFolder + "local_media.m3u8", m3u8Content, urlToLocalPath);
    }

    public static byte[] downloadNoDuplicate(String url, String baseUrl, String outFolder){
        return downloadNoDuplicate(url, baseUrl, outFolder, false);
    }
    public static byte[] downloadNoDuplicate(String url, String baseUrl, String outFolder, boolean openLocalFile){
        if(canDownloadRemoteFile(url, outFolder)){
            if(isRemote(url))
                return downloadFile(url, outFolder);
            else if(!isLocalFile(url, outFolder)){
                if(baseUrl.isEmpty()){
                    // Cannot fetch remote resource
                    System.out.println("[-] Cannot download: '" + outFolder + url + "'");
                    throw new IllegalStateException("You need to input an absolute remote url for the master m3u8 file. (eg. http://127.0.0.1/master.m3u8)");
                }
                return downloadFile(baseUrl + url, outFolder);
            }
        }
        System.out.println("[-] File exists locally: " + url);
        if(openLocalFile){
            if(isRemote(url)){
                String filename = outFolder + getFilenameFromUrl(url);
                System.out.println("[*] Proceed to read local file: " + filename);
                return readLocalFile(filename);
            }else if(isLocalFile(url, outFolder)){
                String filename = outFolder + url;
                System.out.println("[*] Proceed to read local file: " + filename);
                return readLocalFile(outFolder + url);
            }
        }
        return null;
    }

    // -------------- Lower Level Utils -------------- //
    /**
     * Do not download large files.
     * @return null if error occured
     */
    public static byte[] downloadFile(String url){
        return downloadFile(url, "");
    }
    public static byte[] downloadFile(String url, String outFolder){
        byte[] fileContent = null;
        try{
            URL resourceUrl = new URL(url);
            String filename = getFilenameFromUrl(resourceUrl);
            System.out.println("[+] Downloading file: '" + outFolder + filename + "'");
            URLConnection conn = resourceUrl.openConnection();
            conn.setRequestProperty("User-Agent", USER_AGENT);
            try(BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            FileOutputStream fis = new FileOutputStream("./"+outFolder + filename)){
                fileContent = in.readAllBytes();
                fis.write(fileContent);
            }
        }catch(MalformedURLException e){
            throw new IllegalArgumentException("Invalid url: " + url, e);
        }catch(IOException ex){
            throw new UncheckedIOException("Cannot download file from " + url, ex);
        }
        return fileContent;
    }

    /**
     * Do not read large files
     * @return null if error occured
     */
    private static byte[] readLocalFile(String file){
        byte[] fileContent = null;
        try(FileInputStream fis = new FileInputStream(file)){
            fileContent = fis.readAllBytes();
        }catch(IOException e){
            throw new UncheckedIOException("Cannot read file: " + file, e);
        }
        return fileContent;
    }

    private static void writeToLocalFile(String file, String content, Map<String, String> replacements){
        try(FileOutputStream fos = new FileOutputStream(file)){
            if(replacements != null){
                for(String key : replacements.keySet()){
                    content = content.replace(key, replacements.get(key));
                }
            }
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }catch(IOException e){
            throw new UncheckedIOException("Cannot read file: " + file, e);
        }
    }

    private static String getBaseUrl(String url){
        return url.substring(0, url.lastIndexOf('/')+1);
    }

    private static String getFilenameFromUrl(String url){
        try{
            if(isRemote(url))
                return getFilenameFromUrl(new URL(url));
            return url;
        }catch(MalformedURLException e){
            throw new IllegalArgumentException("Invalid url: " + url, e);
        }
    }
    private static String getFilenameFromUrl(URL url){
        String path = url.getPath();
        return path.substring(path.lastIndexOf('/')+1);
    }

    /**
     * @param url url or path/to/file
     */
    private static boolean canDownloadRemoteFile(String url, String outFolder){
        if(isRemote(url)){
            String filename = getFilenameFromUrl(url);
            if(isLocalFile(filename, outFolder)){
                return false;
            }
        }
        return true;
    }

    private static String createFolder(String folder){
        if(!folder.isEmpty()){
            if(!folder.endsWith("/"))
                folder += "/";
            if(!isLocalFile(folder))
                new File(folder).mkdirs();
            return folder;
        }
        return folder;
    }

    private static boolean isRemote(String url){
        return url.startsWith("http://") || url.startsWith("https://");
    }
    private static boolean isAbsolute(String url){
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private static boolean isLocalFile(String file){
        return new File("./"+file).exists();
    }
    private static boolean isLocalFile(String file, String outFolder){
        return new File("./"+outFolder+file).exists();
    }
}
