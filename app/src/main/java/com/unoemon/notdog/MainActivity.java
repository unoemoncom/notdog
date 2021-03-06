package com.unoemon.notdog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.services.rekognition.model.Label;
import com.mlsdev.rximagepicker.RxImageConverters;
import com.mlsdev.rximagepicker.RxImagePicker;
import com.mlsdev.rximagepicker.Sources;
import com.roger.catloadinglibrary.CatLoadingView;
import com.unoemon.notdog.log.Logger;
import com.unoemon.notdog.util.ApiUtil;
import com.unoemon.notdog.util.AppConst;
import com.unoemon.notdog.util.BitmapUtil;
import com.unoemon.notdog.util.DisposableManager;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.unoemon.notdog.util.AppConst.MAXIMUM_SIZE;
import static com.unoemon.notdog.util.AwsConst.IDENTITY_POOL_ID;

/**
 * Main actibity
 * Amazon Rekognition Samples for Android
 */
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.imageview_contents)
    ImageView contentsImage;

    @BindView(R.id.textview_answer)
    TextView answerText;

    @BindView(R.id.textview_lists)
    TextView listsText;

    CatLoadingView loadingView;

    @OnClick(R.id.button_camera)
    void button_camera() {
        getDetectLabels(Sources.CAMERA);
    }

    @OnClick(R.id.button_share)
    void button_share() {
        doShare();

    }

    @OnClick(R.id.button_gallery)
    void button_gallery() {
        getDetectLabels(Sources.GALLERY);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d("");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        loadingView = new CatLoadingView();
    }

    @Override
    protected void onDestroy() {
        Logger.d("");
        DisposableManager.dispose();
        super.onDestroy();
    }


    private void getDetectLabels(Sources sources) {
        Logger.d("");


        if(IDENTITY_POOL_ID.length() == 0){
            Toast.makeText(this, "Please set YOUR_IDENTITY_POOL_ID", Toast.LENGTH_LONG).show();
            return;
        }

        //init
        answerText.setText("");
        answerText.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
        listsText.setText("");

        Disposable disposable =  RxImagePicker.with(MainActivity.this).requestImage(sources)
                .flatMap(new Function<Uri, ObservableSource<Bitmap>>() {
                    @Override
                    public ObservableSource<Bitmap> apply(@NonNull Uri uri) throws Exception {
                        return RxImageConverters.uriToBitmap(MainActivity.this, uri);
                    }
                }).subscribe(orgBitmap -> {

                            if (loadingView != null) {
                                loadingView.show(getSupportFragmentManager(), "");
                            }

                            if (contentsImage != null) {
                                contentsImage.setImageBitmap(orgBitmap);
                            }

                            Bitmap detectBitmap = BitmapUtil.resize(orgBitmap, MAXIMUM_SIZE, MAXIMUM_SIZE);

                            ApiUtil.getDetectLabels(detectBitmap)
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Observer<List<Label>>() {
                                        @Override
                                        public void onCompleted() {
                                            Logger.d("onCompleted!");
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            Logger.d("onError!");
                                        }

                                        @Override
                                        public void onNext(List<Label> labels) {
                                            Logger.d("onNext!");
                                            boolean isFound = false;
                                            listsText.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.transparent2));

                                            for (Label label : labels) {
                                                Logger.d(label.getName() + ": " + label.getConfidence().toString());
                                                listsText.setText(listsText.getText() + label.getName() + "......" + label.getConfidence().toString() + "%" + "\n");

                                                if (label.getName().equals(getString(R.string.string_dog))) {
                                                    isFound = true;
                                                    doFoundDog(label.getConfidence().toString());
                                                }else if(label.getName().equals(getString(R.string.string_hotdog))){
                                                    isFound = true;
                                                    doFoundHotdog(label.getConfidence().toString());
                                                }
                                            }
                                            if (!isFound) {
                                                doNotDog();
                                            }
                                            if (loadingView != null) {
                                                loadingView.dismiss();
                                            }
                                        }
                                    });
                        }
                );

        DisposableManager.add(disposable);

    }

    private void doFoundDog(String confidence) {
        Logger.d("");
        answerText.setText("HIT DOG ! " + confidence + "%");
        answerText.setBackgroundColor(ContextCompat.getColor(this, R.color.green));

    }

    private void doFoundHotdog(String confidence) {
        Logger.d("");
        answerText.setText("HOT DOG ! " + confidence + "%");
        answerText.setBackgroundColor(ContextCompat.getColor(this, R.color.mustard));

    }

    private void doNotDog() {
        Logger.d("");
        answerText.setText("NOT DOG !");
        answerText.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
    }

    private void doShare() {
        Logger.d("");
        Uri uri = Uri.parse(AppConst.URL_OF_SHARE);
        Intent i = new Intent(Intent.ACTION_VIEW,uri);
        startActivity(i);

    }

}


