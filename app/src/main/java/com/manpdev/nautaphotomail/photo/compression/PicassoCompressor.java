package com.manpdev.nautaphotomail.photo.compression;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;

import com.manpdev.nautaphotomail.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by novoa.pro@gmail.com on 2/13/16
 */
public class PicassoCompressor implements CompressionContract.Compressor {

    private static final String TAG = "PicassoCompressor";

    private CompressionContract.CompressionView view;

    private Picasso compressor;
    private static final int MAX_SIZE = 600;
    private static final int COMPRESSION = 70;
    private final Context context;
    private final List<Uri> outputUriList;
    private final File outputFolder;

    public PicassoCompressor(Context context, CompressionContract.CompressionView view) {
        this.view = view;
        this.context = context;
        this.compressor = Picasso.with(this.context);
        this.outputUriList = new ArrayList<>();

        this.outputFolder = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    @Override
    public void compressPhotos(final List<Uri> photoUriList) {

        view.showCompressionProcessDialog(context.getResources().getString(R.string.compression_dialog_message,
                0, photoUriList.size()));

        Observable.create(new Observable.OnSubscribe<Uri>() {

            @Override
            public void call(Subscriber<? super Uri> subscriber) {
                cleanPreviousImages();

                Bitmap bitmap;
                for (Uri image : photoUriList) {
                    try {
                        bitmap = compressor.load(image)
                                .resize(MAX_SIZE, MAX_SIZE)
                                .centerInside()
                                .onlyScaleDown()
                                .get();

                        subscriber.onNext(saveBitmap(bitmap, image.getLastPathSegment()));
                        bitmap.recycle();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        subscriber.onError(e);
                    }
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getCompressorObserver(photoUriList));
    }

    @NonNull
    private Observer<Uri> getCompressorObserver(final List<Uri> photoUriList) {
        return new Observer<Uri>() {
            private int i = 0;

            @Override
            public void onCompleted() {
                view.onCompression(outputUriList);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: ", e);
            }

            @Override
            public void onNext(Uri uri) {
                view.showCompressionProcessDialog(context.getResources().getString(R.string.compression_dialog_message,
                        ++i, photoUriList.size()));

                if (uri != null)
                    outputUriList.add(uri);
            }
        };
    }

    public void cleanPreviousImages() {
        if(!outputFolder.exists())
            return;

        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        String selection = MediaStore.Images.Media.DATA + " like ? ";
        String[] selectionArg = {"%"+outputFolder.getAbsolutePath()+"%"};

        Cursor cursor = MediaStore.Images.Media.query(context.getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, filePathColumn,
                selection, selectionArg, null);

        if(!cursor.moveToFirst()) {
            cursor.close();
            return;
        }

        File item;
        while (!cursor.isAfterLast()){
            item = new File(cursor.getString(0));

            if(item.exists())
                item.delete();

            cursor.moveToNext();
        }

        cursor.close();

        context.getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                selection, selectionArg);
    }

    public Uri saveBitmap(Bitmap bitmap, String fileName) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            if(outputFolder.exists())
                values.put(MediaStore.Images.Media.DATA, outputFolder.getAbsolutePath() + File.separator + fileName + ".jpg");

            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                OutputStream stream = context.getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION, stream);

                if (stream != null)
                    stream.close();

                return uri;
            }
        } catch (Exception e) {
            Log.e(TAG, "onBitmapLoaded: ", e);
            return null;
        }

        return null;
    }

}
