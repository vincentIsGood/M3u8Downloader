package com.vincentcodes.m3u8;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Vanilla solution. May introduce bugs
 */
public class UrlUtils {
    public static String addQueryParamsToURL(String url, Map<String, String> queryParams){
        Map<String, String> finalQueryParams = new HashMap<>(queryParams);
        finalQueryParams.putAll(extractQueryParams(url));

        if(url.contains("?"))
            url = url.substring(0, url.indexOf('?'));

        StringBuilder queryParamsStr = new StringBuilder();
        for(Map.Entry<String, String> entry : finalQueryParams.entrySet()){
            queryParamsStr.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .append('&');
        }

        // cut trailing '&'
        if(queryParamsStr.length() > 0 && queryParamsStr.charAt(queryParamsStr.length()-1) == '&'){
            queryParamsStr.setLength(queryParamsStr.length()-1);
        }

        if(queryParamsStr.length() > 0)
            url += "?" + queryParamsStr.toString();
        
        return url;
    }

    public static Map<String, String> extractQueryParams(String url){
        Map<String, String> queryParams = new HashMap<>();
        if(url.contains("?")){
            String[] params = url.substring(url.indexOf('?')+1).split("&");
            for(String param : params){
                if(param.trim().isEmpty()) continue;
                param = URLDecoder.decode(param, StandardCharsets.UTF_8);
                if(param.contains("=")){
                    String[] keyval = param.split("=");
                    queryParams.put(keyval[0], keyval.length == 1? "" : keyval[1]);
                }else{
                    queryParams.put(param, "");
                }
            }
        }
        return queryParams;
    }
}
