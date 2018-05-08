package com.beyondsw.palette;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.beyondsw.palette.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, PaletteView.Callback, Handler.Callback {

    private ProgressDialog mSaveProgressDlg;

    private static final int MSG_SAVE_SUCCESS = 1;
    private static final int MSG_SAVE_FAILED = 2;
    private Handler mHandler;
    private int penSize = 3;
    private int eraserSize = 30;

    private int penSizeMax = 10;
    private int eraserSizeMax = 40;

    private boolean isPenSelected = true;
    private ActivityMainBinding binding;
    private ArrayList<CheckBox> cbs = new ArrayList<>();
    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view instanceof CheckBox && ((CheckBox) view).isChecked()) {
                return;
            } else if (view instanceof CheckBox) {
                ((CheckBox) view).setChecked(true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        cbs.add(binding.cbWhite);
        cbs.add(binding.cbBlack);
        cbs.add(binding.cbRed);
        cbs.add(binding.cbGreen);
        cbs.add(binding.cbBlue);
        cbs.add(binding.cbYellow);
        for (CheckBox cb : cbs) {
            cb.setOnCheckedChangeListener(listener);
            cb.setOnClickListener(clickListener);
        }
        binding.cbWhite.setChecked(true);
        binding.pen.setSelected(true);
        binding.palette.setPenColor(ContextCompat.getColor(this, R.color.white));
        binding.palette.setCallback(this);
        binding.palette.setSelected(true);
        binding.seekBar.setProgress(penSize);
        binding.seekBar.setMax(penSizeMax);
        binding.tvProgress.setText(String.valueOf(penSize));

        binding.undo.setOnClickListener(this);
        binding.redo.setOnClickListener(this);
        binding.pen.setOnClickListener(this);
        binding.eraser.setOnClickListener(this);
        binding.clear.setOnClickListener(this);
        binding.zoom.setOnClickListener(this);

        binding.undo.setEnabled(false);
        binding.redo.setEnabled(false);

        mHandler = new Handler(this);
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (!b) {
                    return;
                }
                binding.tvProgress.setText(String.valueOf(i));
                if (isPenSelected) {
                    penSize = i;
                    binding.palette.setPenRawSize(DimenUtils.dp2pxInt(penSize));
                } else {
                    eraserSize = i;
                    binding.palette.setEraserSize(DimenUtils.dp2pxInt(eraserSize));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    private CheckBox.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if (!b) {
                return;
            }
            for (CheckBox cb : cbs) {
                cb.setChecked(false);
            }
            compoundButton.setChecked(true);
            int color = ContextCompat.getColor(MainActivity.this, R.color.white);
            switch (compoundButton.getId()) {
                case R.id.cb_white:
                    color = ContextCompat.getColor(MainActivity.this, R.color.white);
                    break;
                case R.id.cb_black:
                    color = ContextCompat.getColor(MainActivity.this, R.color.black);
                    break;
                case R.id.cb_red:
                    color = ContextCompat.getColor(MainActivity.this, R.color.red);
                    break;
                case R.id.cb_green:
                    color = ContextCompat.getColor(MainActivity.this, R.color.green);
                    break;
                case R.id.cb_blue:
                    color = ContextCompat.getColor(MainActivity.this, R.color.blue);
                    break;
                case R.id.cb_yellow:
                    color = ContextCompat.getColor(MainActivity.this, R.color.yellow);
                    break;
            }
            binding.palette.setPenColor(color);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MSG_SAVE_FAILED);
        mHandler.removeMessages(MSG_SAVE_SUCCESS);
    }

    private void initSaveProgressDlg() {
        mSaveProgressDlg = new ProgressDialog(this);
        mSaveProgressDlg.setMessage("正在保存,请稍候...");
        mSaveProgressDlg.setCancelable(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SAVE_FAILED:
                mSaveProgressDlg.dismiss();
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                break;
            case MSG_SAVE_SUCCESS:
                mSaveProgressDlg.dismiss();
                Toast.makeText(this, "画板已保存", Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    }

    private static void scanFile(Context context, String filePath) {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(new File(filePath)));
        context.sendBroadcast(scanIntent);
    }

    private String saveImage(Bitmap bmp, int quality) {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder adb=new AlertDialog.Builder(MainActivity.this);
                    adb.setMessage("没有读写SD卡权限！");
                    adb.setNegativeButton("取消",null);
                    adb.setPositiveButton("去打开", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);//或者直接start
                        }
                    });
                    adb.show();
                }
            });
            return null;
        }
        if (bmp == null) {
            return null;
        }
        File appDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (appDir == null) {
            return null;
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            fos.flush();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                if (mSaveProgressDlg == null) {
                    initSaveProgressDlg();
                }
                mSaveProgressDlg.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bm = binding.palette.buildBitmap();
                        String savedFile = saveImage(bm, 100);
                        if (savedFile != null) {
                            scanFile(MainActivity.this, savedFile);
                            mHandler.obtainMessage(MSG_SAVE_SUCCESS).sendToTarget();
                        } else {
                            mHandler.obtainMessage(MSG_SAVE_FAILED).sendToTarget();
                        }
                    }
                }).start();
                break;
        }
        return true;
    }

    @Override
    public void onUndoRedoStatusChanged() {
        binding.undo.setEnabled(binding.palette.canUndo());
        binding.redo.setEnabled(binding.palette.canRedo());
    }

    @Override
    public void onClick(View v) {
        binding.preView.setZoom(false);
        switch (v.getId()) {
            case R.id.undo:
                binding.llSeek.setVisibility(View.GONE);
                binding.llSeekColor.setVisibility(View.GONE);
                binding.palette.undo();
                break;
            case R.id.redo:
                binding.llSeek.setVisibility(View.GONE);
                binding.llSeekColor.setVisibility(View.GONE);
                binding.palette.redo();
                break;
            case R.id.pen:
                isPenSelected = true;
                binding.llSeek.setVisibility(View.VISIBLE);
                binding.llSeekColor.setVisibility(View.VISIBLE);
                binding.seekBar.setMax(penSizeMax);
                binding.seekBar.setProgress(penSize);
                binding.tvProgress.setText(String.valueOf(penSize));
                v.setSelected(true);
                binding.zoom.setSelected(false);
                binding.eraser.setSelected(false);
                binding.palette.setMode(PaletteView.Mode.DRAW);
                break;
            case R.id.eraser:
                isPenSelected = false;
                binding.zoom.setSelected(false);
                binding.llSeek.setVisibility(View.VISIBLE);
                binding.llSeekColor.setVisibility(View.GONE);
                binding.seekBar.setMax(eraserSizeMax);
                binding.seekBar.setProgress(eraserSize);
                binding.tvProgress.setText(String.valueOf(eraserSize));
                v.setSelected(true);
                binding.pen.setSelected(false);
                binding.palette.setMode(PaletteView.Mode.ERASER);
                break;
            case R.id.clear:
                binding.palette.clear();
                break;
            case R.id.zoom:
                binding.llSeek.setVisibility(View.GONE);
                binding.llSeekColor.setVisibility(View.GONE);
                binding.preView.setZoom(!binding.preView.isZoom());
                binding.zoom.setSelected(true);
                binding.pen.setSelected(false);
                break;
        }
    }
}
