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
import java.util.Map;

public class DownloadUtils {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.83 Safari/537.36";

    public static void downloadNoDuplicateNoReturn(String url, String baseUrl, String outFolder){
        downloadNoDuplicateNoReturn(url, baseUrl, outFolder, false);
    }
    public static void downloadNoDuplicateNoReturn(String url, String baseUrl, String outFolder, boolean openLocalFile){
        if(isRemoteAndNotLocal(url, outFolder)){
            if(isRemote(url)){
                downloadFileNoReturn(url, outFolder);
                return;
            }else if(!isLocalFile(url, outFolder)){
                if(baseUrl.isEmpty()){
                    // Cannot fetch remote resource
                    System.out.println("[-] Cannot download: '" + outFolder + url + "'");
                    throw new IllegalStateException("You need to input an absolute remote url for the master m3u8 file. (eg. http://127.0.0.1/master.m3u8)");
                }
                downloadFileNoReturn(baseUrl + url, outFolder);
                return;
            }
        }
        System.out.println("[-] File exists locally: " + url);
    }

    public static byte[] downloadNoDuplicate(String url, String baseUrl, String outFolder){
        return downloadNoDuplicate(url, baseUrl, outFolder, false);
    }
    /**
     * @param url can be a local file
     * @param baseUrl base url is the full url path used to download a remote m3u8 file (comes from command line)
     */
    public static byte[] downloadNoDuplicate(String url, String baseUrl, String outFolder, boolean openLocalFile){
        if(isRemoteAndNotLocal(url, outFolder)){
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
    public static void downloadFileNoReturn(String url){
        downloadFile(url, "");
    }
    public static void downloadFileNoReturn(String url, String outFolder){
        try{
            byte[] fileContent = null;
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
    }

    /**
     * Do not read large files
     * @return null if error occured
     */
    public static byte[] readLocalFile(String file){
        byte[] fileContent = null;
        try(FileInputStream fis = new FileInputStream(file)){
            fileContent = fis.readAllBytes();
        }catch(IOException e){
            throw new UncheckedIOException("Cannot read file: " + file, e);
        }
        return fileContent;
    }

    public static void writeToLocalFile(String file, String content, Map<String, String> replacements){
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

    public static String getBaseUrl(String url){
        if(url.contains("?"))
            return url.substring(0, url.lastIndexOf('/', url.indexOf('?'))+1);
        return url.substring(0, url.lastIndexOf('/')+1);
    }

    public static String getFilenameFromUrl(String url){
        try{
            if(isRemote(url))
                return getFilenameFromUrl(new URL(url));
            return url;
        }catch(MalformedURLException e){
            throw new IllegalArgumentException("Invalid url: " + url, e);
        }
    }
    public static String getFilenameFromUrl(URL url){
        String path = url.getPath();
        return path.substring(path.lastIndexOf('/')+1);
    }
    public static String getFilenameFromPath(String path){
        if(path.lastIndexOf('/') == -1){
            return path;
        }
        if(path.contains("?"))
            return path.substring(path.lastIndexOf('/', path.indexOf('?'))+1, path.lastIndexOf('?'));
        return path.substring(path.lastIndexOf('/')+1);
    }

    /**
     * @param url url or path/to/file
     */
    public static boolean isRemoteAndNotLocal(String url, String outFolder){
        if(isRemote(url)){
            String filename = getFilenameFromUrl(url);
            if(isLocalFile(filename, outFolder)){
                return false;
            }
        }
        return true;
    }

    /**
     * @return folder path
     */
    public static String createFolder(String folder){
        if(!folder.isEmpty()){
            if(!folder.endsWith("/"))
                folder += "/";
            if(!isLocalFile(folder))
                new File(folder).mkdirs();
            return folder;
        }
        return folder;
    }

    public static String toRemote(String path, String baseUrl){
        if(isRemote(path))
            return path;
        if(!baseUrl.isEmpty() && !baseUrl.endsWith("/")) baseUrl += "/";
        return baseUrl + path;
    }

    public static boolean isRemote(String url){
        return url.startsWith("http://") || url.startsWith("https://");
    }
    public static boolean isAbsolute(String url){
        return url.startsWith("http://") || url.startsWith("https://");
    }

    public static boolean isLocalFile(String file){
        return new File("./"+file).exists();
    }
    public static boolean isLocalFile(String file, String outFolder){
        return new File("./"+outFolder+file).exists();
    }
}