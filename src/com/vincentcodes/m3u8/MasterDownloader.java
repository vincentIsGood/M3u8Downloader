package com.vincentcodes.m3u8;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vincentcodes.m3u8.types.Media;
import com.vincentcodes.m3u8.types.Stream;

public class MasterDownloader {
    public static DownloadOptions DOWNLOAD_OPTIONS;

    private String baseUrl = "";
    private String pathDirUrl = "";

    private String url;
    private String outfolder;
    private MasterPlaylist master;
    private MasterPlaylist localMaster;
    private int mediaM3u8Count;

    private DownloadUtils downloader;

    /**
     * @param url remote or local resource (note: if it is a local resource, 
     * please make sure the file exists locally and the m3u8 uses absolute url format.)
     * @param outfolder files output location
     */
    public MasterDownloader(String url, String outfolder){
        this.url = url;
        if(DownloadUtils.isAbsolute(url)){
            baseUrl = DownloadUtils.getBaseUrl(url);
            pathDirUrl = DownloadUtils.getPathDir(url);
        }
        this.outfolder = DownloadUtils.createFolder(outfolder);
        downloader = new DownloadUtils(DOWNLOAD_OPTIONS);
    }

    public MasterPlaylist downloadAndParse(){
        String m3u8Content;
        if(DownloadUtils.isLocalFile(url)){
            m3u8Content = new String(DownloadUtils.readLocalFile(url));
        }else m3u8Content = new String(downloader.downloadNoDuplicate(url, baseUrl, outfolder, true));
        System.out.println("[*] Parsing m3u8 file...");
        master = MasterPlaylistParser.parse(m3u8Content);
        return master;
    }
    
    /**
     * {@link #downloadAndParse()} before finding resources
     * @param bandwidth choose a bandwidth to download.
     * @return a map of local path to the paths to remote / local m3u8 files
     * <p>
     * For example: "playlist0/local.m3u8" to "http://remote.com/media.m3u8"
     * <p>
     * Note: duplicated m3u8 files are not included in the list.
     */
    public List<MediaDownloader> findResources(String audioId, String videoId, int... bandwidths){
        if(bandwidths == null)
            throw new NullPointerException("bandwidths cannot be null");
        resetMediaCount();
        List<Stream> streams;
        if(audioId == null) 
            streams = master.findStreamsByBandwidth(bandwidths);
        else streams = master.findStreamsByBandwidthAndIds(audioId, videoId, bandwidths);
        System.out.println("[*] Found " + streams.size() + " streams with bandiwdth " + Arrays.toString(bandwidths));

        // remotePath to downloader map
        Map<String, MediaDownloader> foundPlaylist = new HashMap<>();
        Set<String> relatedGroupIds = new HashSet<>();
        String remotePath;
        for(Stream stream : streams){
            if(stream.isIframeStream()){
                remotePath = DownloadUtils.toRemote(stream.getURI(), baseUrl, pathDirUrl);
                if(!foundPlaylist.containsKey(remotePath))
                    foundPlaylist.put(remotePath, new MediaDownloader(remotePath, outfolder + getNextMediaFolderPath()));
                continue;
            }
            if(stream.getAudioId() != null) relatedGroupIds.add(stream.getAudioId());
            if(stream.getVideoId() != null) relatedGroupIds.add(stream.getVideoId());
            remotePath = DownloadUtils.toRemote(stream.playlist, baseUrl, pathDirUrl);
            if(!foundPlaylist.containsKey(remotePath))
                foundPlaylist.put(remotePath, new MediaDownloader(remotePath, outfolder + getNextMediaFolderPath()));
        }
        for(Media media : master.allMedia){
            remotePath = DownloadUtils.toRemote(media.getURI(), baseUrl, pathDirUrl);
            if(!relatedGroupIds.contains(media.getGroupId()) || foundPlaylist.containsKey(remotePath))
                continue;
            foundPlaylist.put(remotePath, new MediaDownloader(remotePath, outfolder + getNextMediaFolderPath()));
        }
        return new ArrayList<>(foundPlaylist.values());
    }

    /**
     * May use {@link PlaylistGenerator#toM3u8(MasterPlaylist)} to 
     * generate the m3u8 file from a master playlist object.
     * 
     * Local Master M3u8 means the master file will never reference
     * external resources, allowing conversion of m3u8 to video format
     * locally.
     * @return new local master m3u8 file
     */
    public MasterPlaylist generateLocalMaster(String audioId, String videoId, int... bandwidths){
        resetMediaCount();
        List<Stream> streams;
        if(audioId == null) 
            streams = master.findStreamsByBandwidth(bandwidths);
        else streams = master.findStreamsByBandwidthAndIds(audioId, videoId, bandwidths);

        Map<String, String> foundPathToOutFileLoc = new HashMap<>();
        Set<String> relatedGroupIds = new HashSet<>();
        MasterPlaylist newMaster = new MasterPlaylist();
        for(Stream stream : streams){
            Map<String, String> attributes = new HashMap<>(stream.getAttributes());
            if(stream.isIframeStream()){
                attributes.put("URI", putIfAbscentGetOtherwise(foundPathToOutFileLoc, DownloadUtils.toRemote(stream.getURI(), baseUrl, pathDirUrl), getNextMediaFolderPath() + "local_media.m3u8"));
                newMaster.addStream(new Stream(attributes, null));
                continue;
            }
            if(stream.getAudioId() != null) relatedGroupIds.add(stream.getAudioId());
            if(stream.getVideoId() != null) relatedGroupIds.add(stream.getVideoId());
            newMaster.addStream(new Stream(attributes, putIfAbscentGetOtherwise(foundPathToOutFileLoc, DownloadUtils.toRemote(stream.playlist, baseUrl, pathDirUrl), getNextMediaFolderPath() + "local_media.m3u8")));
        }
        for(Media media : master.allMedia){
            if(!relatedGroupIds.contains(media.getGroupId()))
                continue;
            Map<String, String> mediaAttributes = new HashMap<>(media.getAttributes());
            mediaAttributes.put("URI", putIfAbscentGetOtherwise(foundPathToOutFileLoc, DownloadUtils.toRemote(media.getURI(), baseUrl, pathDirUrl), getNextMediaFolderPath() + "local_media.m3u8"));
            newMaster.addMedia(new Media(mediaAttributes));
        }
        this.localMaster = newMaster;
        return newMaster;
    }
    private String putIfAbscentGetOtherwise(Map<String, String> map, String key, String value){
        if(!map.containsKey(key)){
            map.put(key, value);
            return value;
        }
        return map.get(key);
    }

    /**
     * Note: {@link #getLocalMaster()} must not return null
     */
    public void writeToFile(){
        writeToFile(getLocalMaster());
    }
    public void writeToFile(MasterPlaylist master){
        if(master == null) 
            throw new NullPointerException("localMaster cannot be null");
        try(FileOutputStream fos = new FileOutputStream(outfolder + "local_master.m3u8")){
            String m3u8Content = PlaylistGenerator.toM3u8(master);
            fos.write(m3u8Content.getBytes());
        }catch(IOException e){
            throw new UncheckedIOException(e);
        }
    }

    public String getUrl(){
        return url;
    }

    /**
     * Call {@link #downloadAndParse()} before using this function
     */
    public MasterPlaylist getMaster(){
        return master;
    }

    /**
     * Call {@link #generateLocalMaster(String, String, int...)} 
     * before using this function
     */
    public MasterPlaylist getLocalMaster(){
        return localMaster;
    }

    private void resetMediaCount(){
        mediaM3u8Count = 0;
    }

    private String getNextMediaFolderPath(){
        return String.format("playlist%d/", mediaM3u8Count++);
    }
}
