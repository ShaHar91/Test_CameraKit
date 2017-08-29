package be.appwise.test_camerakit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.flurgle.camerakit.CameraKit;
import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private CameraView camera;
    private ImageButton capturePhoto, toggleCamera;
    private boolean isTaken = true;


    //TODO: add permission request for external storage

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = findViewById(R.id.camera);
        capturePhoto = findViewById(R.id.capturePhoto);
        toggleCamera = findViewById(R.id.toggleCamera);

        capturePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                camera.setCameraListener(new CameraListener() {
                    @Override
                    public void onPictureTaken(byte[] jpeg) {
                        super.onPictureTaken(jpeg);
                        OutputStream fOut = null;
                        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);

                        String path = Environment.getExternalStorageDirectory().toString();
                        String ts = String.valueOf(System.currentTimeMillis() / 1000);

                        File fileMk = new File(path + "/Pictures/");
                        if (!fileMk.exists()) {
                            fileMk.mkdirs();
                        }

                        File file = new File(path + "/Pictures/", ts + ".jpeg");
                        try {
                            fOut = new FileOutputStream(file);

                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
                            fOut.flush();
                            fOut.close();

                            Log.v("PICTURE_DETAIL", file.getAbsolutePath());
                            Log.v("PICTURE_DETAIL", file.getName());
                            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

                            isTaken = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            isTaken = false;
                        }
                    }
                });
                camera.captureImage();

                if (isTaken) {
                    Toast.makeText(MainActivity.this, "Picture has been taken", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "ooh boy", Toast.LENGTH_SHORT).show();
                }
            }
        });

        toggleCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (camera.toggleFacing()) {
                    case CameraKit.Constants.FACING_BACK:
                        Toast.makeText(MainActivity.this, "Switched to back camera!", Toast.LENGTH_SHORT).show();
                        break;

                    case CameraKit.Constants.FACING_FRONT:
                        Toast.makeText(MainActivity.this, "Switched to front camera!", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        camera.start();
    }

    @Override
    protected void onPause() {
        camera.stop();
        super.onPause();
    }
}