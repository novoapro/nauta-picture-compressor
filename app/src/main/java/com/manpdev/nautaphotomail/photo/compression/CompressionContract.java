package com.manpdev.nautaphotomail.photo.compression;

import android.net.Uri;

import java.util.List;

/**
 * Created by novoa.pro@gmail.com on 2/13/16
 */
public interface CompressionContract {

    interface CompressionView{
        void showCompressionProcessDialog(String message);
        void onCompression(List<Uri> compressedPhotoUriList);
    }

    interface Compressor{
        void compressPhotos(List<Uri> photoUriList);
    }
}
