/**
 * 
 */
package com.sample.map.tile;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

final class TileDownloadable implements Downloadable<Tile, Bitmap>{
	
	@Override
	public Bitmap download(Tile key) throws InterruptedException {
		return downloadBitmap(key);
	}

    public Bitmap downloadBitmap(Tile tile){
        final DefaultHttpClient client = new DefaultHttpClient();
        final HttpGet getRequest = new HttpGet(new Uri.Builder()
        	.scheme("http")
        	.authority("vec.maps.yandex.net")
        	.appendPath("tiles")
        	.appendQueryParameter("l", "map")
        	.appendQueryParameter("v", "2.21.0")
        	.appendQueryParameter("x", tile.col+630+"")
        	.appendQueryParameter("y", tile.row+320+"")
        	.appendQueryParameter("z", 10+"")
        	.build().toString());

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w("ImageDownloader", "Error " + statusCode +
                        " while retrieving bitmap from " + getRequest.getRequestLine().getUri());
                return null;
            }
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = new FlushedInputStream(entity.getContent());
                    final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    Log.d("ImageDownloader", bitmap + "");
                    return bitmap;
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (IOException e) {
            getRequest.abort();
            Log.w(TileDownloader.LOG_TAG, "I/O error while retrieving bitmap for " + tile, e);
        } catch (IllegalStateException e) {
            getRequest.abort();
            Log.w(TileDownloader.LOG_TAG, "Incorrect URL: " + getRequest.getRequestLine().getUri());
        } catch (Exception e) {
            getRequest.abort();
            Log.w(TileDownloader.LOG_TAG, "Error while retrieving bitmap from " + getRequest.getRequestLine().getUri(), e);
        } finally {
            if (client != null) {
            	//FIXME client.close()?
            }
        }
        return null;
    }
	static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                      int b = read();
                      if (b < 0) {
                          break;  // we reached EOF
                      } else {
                          bytesSkipped = 1; // we read one byte
                      }
               }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }
}