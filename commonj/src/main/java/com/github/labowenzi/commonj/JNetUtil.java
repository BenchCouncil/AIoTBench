package com.github.labowenzi.commonj;

import com.github.labowenzi.commonj.annotation.NotNull;
import com.github.labowenzi.commonj.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by alanubu on 20-11-16.
 */
public class JNetUtil {

    public static final int BUFFER_SIZE = 8192;

    @Nullable
    public static URL resoveUrl(@NotNull String url) {
        URL finalUrl;
        try {
            finalUrl = new URL(url);
            return resoveUrl(finalUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }
    @Nullable
    public static URL resoveUrl(@NotNull URL url) {
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        connection.setInstanceFollowRedirects(false);
        int code;
        try {
            code = connection.getResponseCode();
        } catch ( IOException e ) {
            e.printStackTrace();
            connection.disconnect();
            return null;
        }
//        int length = connection.getContentLength();
//        if (length <= 0) {
//            Log.e(TAG, "connection content length: " + length);
//            return null;
//        }

        // permanent redirection
        if ( code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP
                || code == HttpURLConnection.HTTP_SEE_OTHER ) {
            String newLocation = connection.getHeaderField( "Location" );
            connection.disconnect();
            URL nextUrl;
            try {
                nextUrl = new URL(newLocation);
                return resoveUrl(nextUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
        }

        connection.disconnect();
        return url;
    }

    public static long getLength(@NotNull URL url) {
        URL finalUrl = resoveUrl(url);
        if (finalUrl == null) return -1;
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) finalUrl.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

        connection.setInstanceFollowRedirects(false);
        int length = connection.getContentLength();
        connection.disconnect();
        return length;
    }

    @Nullable
    public static InputStream openRemoteInputStream(@NotNull URL url) {
        URL finalUrl = resoveUrl(url);

        if (finalUrl != null) {
            try {
                return (InputStream) finalUrl.getContent();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
