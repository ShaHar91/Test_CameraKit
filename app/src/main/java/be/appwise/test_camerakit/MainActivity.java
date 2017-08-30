package be.appwise.test_camerakit;

import android.Manifest;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.canelmas.let.AskPermission;
import com.canelmas.let.DeniedPermission;
import com.canelmas.let.Let;
import com.canelmas.let.RuntimePermissionListener;
import com.canelmas.let.RuntimePermissionRequest;
import com.flurgle.camerakit.CameraKit;
import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements RuntimePermissionListener {

    private CameraView camera;
    private ImageButton capturePhoto, toggleCamera;
    private boolean isTaken = true;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private int rotation;

    //TODO: (2) : add rotate camera functionality => cameraview needs to stay fixed, only buttons need to turn
    //TODO: (4) : add landscape mode for the other frames
    //TODO: (5) : add flash functionality, only works when camera is in normal capture mode (don't use still)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        camera = findViewById(R.id.camera);

        toggleCamera = findViewById(R.id.toggleCamera);

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

        setViews();
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

    //Had to put this in another method for AskPermission to work, otherwise an Exception was called
    @AskPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void setViews() {
        capturePhoto = findViewById(R.id.capturePhoto);

        capturePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capturePhoto.setEnabled(false);
                capturePhoto.setClickable(false);

                camera.setCameraListener(new CameraListener() {
                    @Override
                    public void onPictureTaken(byte[] jpeg) {
                        super.onPictureTaken(jpeg);

                        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        options.inMutable = true;

                        int page = mViewPager.getCurrentItem();
                        Bitmap overlay;
                        switch (page) {
                            case 0:
                                overlay = setOverlay(R.drawable.bross_template);
                                break;
                            case 1:
                                overlay = setOverlay(R.drawable.bross_template_square);
                                break;
                            case 2:
                                overlay = setOverlay(R.drawable.signmania_frame);
                                break;
                            case 3:
                                overlay = setOverlay(R.drawable.signmania_frame_square);
                                break;
                            default:
                                overlay = setOverlay(R.drawable.bross_template_square);
                                break;
                        }

                        Log.v("PICTURE_DETAIL", mViewPager.getCurrentItem() + "");

                        OutputStream fOut;

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

                            MediaScannerConnection.scanFile(MainActivity.this, new String[]{file.getPath()}, new String[]{"image/jpeg"}, null);
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
                    Toast.makeText(MainActivity.this, "Error has occurred", Toast.LENGTH_SHORT).show();
                }

                capturePhoto.setEnabled(true);
                capturePhoto.setClickable(true);
            }
        });
    }

    private Bitmap setOverlay(int drawable) {
        Bitmap temp = null;

        try {
            temp = Glide.with(MainActivity.this).asBitmap().load(drawable).into(camera.getWidth(), camera.getHeight()).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return temp;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Let.handle(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onShowPermissionRationale(List<String> permissionList, RuntimePermissionRequest permissionRequest) {
        permissionRequest.retry();
    }

    //TODO: (6) : test this by deniying access to permission
    @Override
    public void onPermissionDenied(List<DeniedPermission> deniedPermissionList) {
//        String denied = "";
        for (DeniedPermission deniedPermission : deniedPermissionList) {
//            denied += deniedPermission.getPermission() + "\n";
            if (deniedPermission.isNeverAskAgainChecked()) {
                //show dialog to go to settings & manually allow the permission
                MaterialDialog permissionDialog = new MaterialDialog.Builder(this)
                        .title(R.string.permission_dialog_title)
                        .content(R.string.permission_dialog_body)
                        .positiveText(R.string.permission_go_to_settings)
                        .negativeText(R.string.cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                DialogManager.startInstalledAppDetailsActivity(MainActivity.this);
                                dialog.dismiss();
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .build();
                if (!permissionDialog.isShowing()) {
                    permissionDialog.show();
                }
            }
        }
    }

    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";
        private ImageView imageView;

        public PlaceholderFragment() {
        }

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_tabbed, container, false);

            imageView = rootView.findViewById(R.id.overlayIv);

            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 1:
                    Glide.with(this).load(R.drawable.bross_template)
                            .into(imageView);
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                    break;
                case 2:
                    Glide.with(this).load(R.drawable.bross_template_square)
                            .into(imageView);
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                    break;
                case 3:
                    Glide.with(this).load(R.drawable.signmania_frame)
                            .into(imageView);
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                    break;
                case 4:
                    Glide.with(this).load(R.drawable.signmania_frame_square)
                            .into(imageView);
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                    break;
                default:
                    Glide.with(this).load(R.drawable.bross_template_square)
                            .into(imageView);
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                    break;
            }
            return rootView;
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return 4;
        }
    }
}