package be.appwise.test_camerakit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.flurgle.camerakit.CameraKit;
import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;
import com.vinaygaba.rubberstamp.RubberStamp;
import com.vinaygaba.rubberstamp.RubberStampConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    private CameraView camera;
    private ImageButton capturePhoto, toggleCamera;
    private boolean isTaken = true;
    private RelativeLayout pictureLayout;
    private RubberStamp mRubberStamp;


    //TODO: add permission request for external storage

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = findViewById(R.id.camera);
        capturePhoto = findViewById(R.id.capturePhoto);
        toggleCamera = findViewById(R.id.toggleCamera);
        pictureLayout = findViewById(R.id.pictureLayout);

        mRubberStamp = new RubberStamp(this);

//        final BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inMutable = true;

        capturePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                camera.setCameraListener(new CameraListener() {
                    @Override
                    public void onPictureTaken(byte[] jpeg) {
                        super.onPictureTaken(jpeg);

                        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        options.inMutable = true;

//                        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, options);

                        Bitmap overlay = BitmapFactory.decodeResource(getResources(), R.drawable.bross_template_square);

                        OutputStream fOut = null;

                        String path = Environment.getExternalStorageDirectory().toString();
                        String ts = String.valueOf(System.currentTimeMillis() / 1000);

                        File fileMk = new File(path + "/Pictures/");
                        if (!fileMk.exists()) {
                            fileMk.mkdirs();
                        }

                        File file = new File(path + "/Pictures/", ts + ".jpeg");
                        try {
                            fOut = new FileOutputStream(file);

                            overlay(bitmap, overlay).compress(Bitmap.CompressFormat.JPEG, 85, fOut);
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

    private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmCanvas = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());

        Bitmap bmOverlay = Bitmap.createScaledBitmap(bmp2, bmp1.getWidth(), bmp1.getHeight(), false);
        Canvas canvas = new Canvas(bmCanvas);

        canvas.drawBitmap(bmp1, new Matrix(), null);

        canvas.drawBitmap(bmOverlay, new Matrix(), null);

        return bmCanvas;
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

    public Observable<Bitmap> getBitmap(final RubberStamp rubberStamp,
                                        final RubberStampConfig config) {
        return Observable.defer(new Func0<Observable<Bitmap>>() {
            @Override
            public Observable<Bitmap> call() {
                return Observable.just(rubberStamp.addStamp(config));
            }
        });
    }
}