package com.vincentcodes.m3u8;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

// TODO: fix the dirty code
public class DownloadUtils {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.83 Safari/537.36";

    public DownloadOptions downloadOptions;

    public DownloadUtils(DownloadOptions downloadOptions){
        this.downloadOptions = downloadOptions;
    }

    public void downloadNoDuplicateNoReturn(String url, String baseUrl, String outFolder, boolean openLocalFile){
        downloadNoDuplicateNoReturn(url, baseUrl, outFolder, null, openLocalFile);
    }
    public void downloadNoDuplicateNoReturn(String url, String baseUrl, String outFolder, String givenFilename){
        downloadNoDuplicateNoReturn(url, baseUrl, outFolder, givenFilename, false);
    }
    public void downloadNoDuplicateNoReturn(String url, String baseUrl, String outFolder, String givenFilename, boolean openLocalFile){
        downloadNoDuplicate(url, baseUrl, outFolder, givenFilename, openLocalFile);
    }

    public byte[] downloadNoDuplicate(String url, String baseUrl, String givenFilename, String outFolder){
        return downloadNoDuplicate(url, baseUrl, outFolder, givenFilename, false);
    }
    public byte[] downloadNoDuplicate(String url, String baseUrl, String outFolder, boolean openLocalFile){
        return downloadNoDuplicate(url, baseUrl, outFolder, null, openLocalFile);
    }
    /**
     * @param url can be a local file 
     * OR full remote path "http://127.0.0.1:1234/subdir/master.m3u8"
     * OR partial path "asdasd.ts"
     * OR partial abs path "/asdasd.ts"
     * @param baseUrl (eg. "http://127.0.0.1:1234/subdir/master.m3u8" => base url "http://127.0.0.1:1234/")
     */
    public byte[] downloadNoDuplicate(String url, String baseUrl, String outFolder, String givenFilename, boolean openLocalFile){
        if(isRemoteAndNotLocal(url, baseUrl, outFolder, givenFilename)){
            String localFilename = givenFilename != null? givenFilename : getFilenameFromUrl(url);
            if(isRemote(url))
                return downloadFile(url, outFolder, givenFilename);
            else if(!isLocalFile(localFilename, outFolder)){
                if(baseUrl == null || baseUrl.isEmpty()){
                    // Cannot fetch remote resource
                    System.out.println("[-] Cannot download: '" + outFolder + url + "'");
                    throw new IllegalStateException("You need to input an absolute remote url for the master m3u8 file. (eg. http://127.0.0.1/master.m3u8)");
                }
                return downloadFile(baseUrl + url, outFolder, givenFilename);
            }
        }
        System.out.println("[-] File exists locally: " + url);
        if(openLocalFile){
            if(isRemote(url)){
                String filename = outFolder + (givenFilename == null? getFilenameFromUrl(url) : givenFilename);
                System.out.println("[*] Proceed to read local file: " + filename);
                return readLocalFile(filename);
            }else if(isLocalFile(url, outFolder)){
                String filename = outFolder + (givenFilename == null? url : givenFilename);
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
    public byte[] downloadFile(String url){
        return downloadFile(url, "", null);
    }
    public byte[] downloadFile(String url, String outFolder){
        return downloadFile(url, outFolder, null);
    }
    public byte[] downloadFile(String url, String outFolder, String givenFilename){
        byte[] fileContent = null;
        try{
            if(downloadOptions != null && downloadOptions.queries != null){
                url = UrlUtils.addQueryParamsToURL(url, downloadOptions.queries);
            }
            URL resourceUrl = new URL(url);
            String filename = getFilenameFromUrl(resourceUrl);
            if(givenFilename != null) filename = givenFilename;

            File outputFile = new File(outFolder, filename);
            System.out.println("[+] Downloading file: '" + outFolder + filename + "' (from "+ url +")");
            HttpURLConnection conn = (HttpURLConnection)resourceUrl.openConnection();
            conn.setRequestProperty("User-Agent", USER_AGENT);

            if(downloadOptions != null && downloadOptions.headers != null){
                for(Map.Entry<String, String> header : downloadOptions.headers.entrySet()){
                    conn.addRequestProperty(header.getKey(), header.getKey());
                }
            }

            if(!outputFile.exists()){
                outputFile.createNewFile();
            }

            if(conn.getResponseCode() != 200){
                throw new IOException("Bad response code: " + conn.getResponseCode());
            }

            try(BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            FileOutputStream fis = new FileOutputStream(outputFile)){
                fileContent = in.readAllBytes();
                fis.write(fileContent);
                fis.flush();
            }
        }catch(MalformedURLException e){
            throw new IllegalArgumentException("Invalid url: " + url, e);
        }catch(IOException ex){
            ex.printStackTrace();
            throw new UncheckedIOException("Cannot download file from " + url, ex);
        }
        return fileContent;
    }
    public void downloadFileNoReturn(String url){
        downloadFile(url, "");
    }
    public void downloadFileNoReturn(String url, String outFolder){
        downloadFile(url, outFolder, null);
    }
    public void downloadFileNoReturn(String url, String outFolder, String givenFilename){
        downloadFile(url, outFolder, givenFilename);
    }


    // -------------- Pure Utils -------------- //
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
        if(!url.startsWith("https://") && !url.startsWith("http://")){
            return "";
        }
        return url.substring(0, url.indexOf('/', url.indexOf("//")+3)+1);
    }
    /**
     * asd/dsa.txt => "asd/"
     * asd => ""
     */
    public static String getPathDir(String urlToFile){
        if(!isAbsolute(urlToFile)) return "";
        File file = new File(urlToFile.substring(urlToFile.indexOf('/', urlToFile.indexOf("//")+3)+1));
        String parent = file.getParent() + "/";
        return parent == null ? "" : parent;
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
        return getFilenameFromPath(url.getPath());
    }
    public static String getFilenameFromPath(String path){
        if(path.lastIndexOf('/') == -1){
            if(path.contains("?"))
                return path.substring(0, path.indexOf('?'));
            return path;
        }

        if(path.contains("?"))
            return path.substring(path.lastIndexOf('/', path.indexOf('?'))+1, path.indexOf('?'));
        return path.substring(path.lastIndexOf('/')+1);
    }

    /**
     * "https://dsadsa/asd" => "https://dsadsa"
     * "https://dsadsa/asd/dsa/dsa/dsad" => "https://dsadsa/asd/dsa/dsa"
     */
    public static String getPathExcludeName(String path){
        if(path.lastIndexOf('/') == -1){
            if(path.contains("?"))
                return path.substring(0, path.indexOf('?'));
            return path;
        }
        if(path.contains("?"))
            return path.substring(0, path.lastIndexOf('/', path.indexOf('?')));
        return path.substring(0, path.lastIndexOf('/'));
    }

    /**
     * @param url url or path/to/file
     */
    public static boolean isRemoteAndNotLocal(String url, String outFolder){
        return isRemoteAndNotLocal(url, "", outFolder, null);
    }
    public static boolean isRemoteAndNotLocal(String url, String baseUrl, String outFolder, String givenFilename){
        if(isRemote(url) || isRemote(baseUrl + url)){
            String filename = givenFilename != null? givenFilename : getFilenameFromUrl(url);
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

    public static String toRemote(String path, String baseUrl, String pathDirUrl){
        if(isRemote(path))
            return path;
        if(!baseUrl.isEmpty() && !baseUrl.endsWith("/")) baseUrl += "/";
        return baseUrl + pathDirUrl + path;
    }

    public static boolean isRemote(String url){
        return url.contains("://");
    }
    public static boolean isAbsolute(String url){
        return isRemote(url);
    }

    public static boolean isLocalFile(String file){
        return new File(file).exists();
    }
    public static boolean isLocalFile(String file, String outFolder){
        return new File(outFolder, file).exists();
    }
}
