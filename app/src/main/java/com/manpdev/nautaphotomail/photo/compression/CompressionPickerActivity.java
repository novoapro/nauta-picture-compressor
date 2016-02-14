package com.manpdev.nautaphotomail.photo.compression;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.manpdev.nautaphotomail.R;

import java.util.ArrayList;
import java.util.List;

public class CompressionPickerActivity extends AppCompatActivity implements CompressionContract.CompressionView {

    private static final String TAG = "CompressionPicker";
    public static final int REQUEST_CODE = 231;
    private ProgressDialog dialog;
    private CompressionContract.Compressor compressor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "created activity");

        this.compressor = new PicassoCompressor(getApplicationContext(), this);

        requestPermission();
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)){

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);

            }else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        }else{
            handleSendImages(getIntent());
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CODE){
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                handleSendImages(getIntent());

            } else {
                Toast.makeText(CompressionPickerActivity.this, R.string.permission_denied_message, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void handleSendImages(Intent intent) {
        List<Uri> list = new ArrayList<>();
        if (Intent.ACTION_SEND.equals(intent.getAction()))
            list.add((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));

        else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))
            list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

        compressor.compressPhotos(list);

    }

    @Override
    public void showCompressionProcessDialog(String message) {
        if (dialog == null) {
            this.dialog = new ProgressDialog(CompressionPickerActivity.this);
            this.dialog.setIndeterminate(true);
        }

        dialog.setMessage(message);

        if (!dialog.isShowing())
            dialog.show();
    }

    @Override
    public void onCompression(List<Uri> compressedPhotoUriList) {
        if (dialog != null && dialog.isShowing())
            dialog.hide();

        shareContent(compressedPhotoUriList);
    }

    private void shareContent(List<Uri> photos) {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("message/rfc822");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, (ArrayList<Uri>) photos);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


}
