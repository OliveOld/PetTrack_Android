package org.olive.pets.Profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.olive.pets.DB.DogProfile;
import org.olive.pets.MainActivity;
import org.olive.pets.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import io.realm.Realm;
import io.realm.RealmResults;

import static java.lang.Integer.parseInt;

public class DogProfileEditActivity extends Activity implements View.OnClickListener{
    private static final int PICK_FROM_CAMERA = 0;
    private static final int PICK_FROM_ALBUM = 1;
    private static final int CROP_FROM_IMAGE = 2;

    private Uri mImageCaptureUri;
    private ImageView iv_DogPhoto;
    private EditText et_DogName;
    private EditText et_DogAge;
    private EditText et_DogSex;
    private int id_view;
    private String absolutePath;
    private int DOG_ID;
    Toast toast;
    private Realm mRealm;
    String dir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dog_edit_info);

        // 전 intent 에 담긴 정보 받기
        Intent intent = getIntent();
        DOG_ID = intent.getIntExtra("DOG_ID", 0);
        if(DOG_ID==0)
            toast = Toast.makeText(DogProfileEditActivity.this, "넘어온 정보가 없습니다.", Toast.LENGTH_SHORT);

        iv_DogPhoto = (ImageView) this.findViewById(R.id.dog_image_modify_e);

        // EditText를 한줄만 입력 가능하도록 설정
        et_DogName = (EditText) this.findViewById(R.id.dog_name_e);
        et_DogAge = (EditText) this.findViewById(R.id.dog_age_e);
        et_DogSex = (EditText) this.findViewById(R.id.dog_sex_e);
        et_DogName.setSingleLine(true);
        et_DogSex.setSingleLine(true);

        // 만들어진 Realm 설정 사용..
        mRealm = Realm.getDefaultInstance();

        // 각 부분에 Realm 정보 불러오기
        final RealmResults<DogProfile> puppies = mRealm.where(DogProfile.class).equalTo("dog_id", DOG_ID).findAll();

        //첫번째로등록 된 놈 보이기
        DogProfile myDog = puppies.first();
        Toast.makeText(this, myDog.getDogId() + ":id", Toast.LENGTH_SHORT).show();

        et_DogName.setText(myDog.getDogName());
        et_DogAge.setText(myDog.getDogAge()+"");
        et_DogSex.setText(myDog.getDogSex());
        dir = myDog.getDogPhoto();

        // 사진 설정
        File imgFile = null;
        if (dir != null)
            imgFile = new File(dir);

        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            iv_DogPhoto.setImageBitmap(myBitmap);
        }
    }

    // 카메라에서 사진 촬영하기 => 촬영 후 이미지 가져옴
    public void doTakePhotoAction() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // 임시 사용할 파일 경로 생성
        String url = "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg";
        mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), url));

        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
        startActivityForResult(intent, PICK_FROM_CAMERA);
    }

    // 앨범에서 가져오기
    public void doTakeAlbumAction() {
        // 앨범을 호출
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, PICK_FROM_ALBUM);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode != RESULT_OK)
            return;

        switch(requestCode) {
            case PICK_FROM_ALBUM: {
                mImageCaptureUri = data.getData();
                Log.d("PetsAndroid", mImageCaptureUri.getPath().toString());
            }
            case PICK_FROM_CAMERA: {
                // 이미지를 가져온 이후 리사이즈할 이미지 크기 결정
                // 이미지 크롭 어플리케이션을 호출
                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(mImageCaptureUri, "image/*");

                // 크롭할 이미지 사이즈 200 * 200으로 저장
                intent.putExtra("outputX", 200);
                intent.putExtra("outputY", 200);
                intent.putExtra("aspectX", 1);
                intent.putExtra("aspectY", 1);
                intent.putExtra("scale", true);
                intent.putExtra("return-data", true);
                startActivityForResult(intent, CROP_FROM_IMAGE);

                break;
            }
            case CROP_FROM_IMAGE: {
                // 크롭 된 이미지 넘겨받음
                // 이미지뷰에 이미지를 보여주거나 부가 작업 이후 임시파일 삭제
                if (resultCode != RESULT_OK) {
                    return;
                }

                final Bundle extras = data.getExtras();

                // 크롭된 이미지의 저장 경로
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/PetsAndroid/" + System.currentTimeMillis() + ".jpg";

                absolutePath="null";
                if (extras != null) {
                    // 크롭된 이미지를 비트맵 photo로 저장
                    Bitmap photo = extras.getParcelable("data");
                    iv_DogPhoto.setImageBitmap(photo);  // 레이아웃 이미지 칸에 crop된 이미지 보여줌

                    storeCropImage(photo, filePath);    // 크롭된 이미지를 외부저장소, 앨범에 저장

                    absolutePath = filePath;
                    break;
                }

                // 임시 파일 삭제
                File f = new File(mImageCaptureUri.getPath());
                if (f.exists()) {
                    f.delete();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        id_view = v.getId();
        if(v.getId() == R.id.btn_submit_profile_e) {
            // 프로필 갱신
            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    DogProfile myDog = realm.where(DogProfile.class).equalTo("dog_id", DOG_ID).findFirst();
                    myDog.setDogName(et_DogName.getText().toString());
                    myDog.setDogAge(parseInt(et_DogAge.getText().toString()));
                    myDog.setDogSex(et_DogSex.getText().toString());
                    if(absolutePath==null)
                        //원래 있던 사진으로 설정
                        myDog.setDogPhoto(dir);
                    else
                        myDog.setDogPhoto(absolutePath);
                }
            });
            mRealm.close();

           // Intent mainIntent = new Intent(DogProfileEditActivity.this, MainActivity.class);
           // DogProfileEditActivity.this.startActivity(mainIntent);
            DogProfileEditActivity.this.finish();

            Toast.makeText(this, "강아지 프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show();
        }else if(v.getId() == R.id.btn_UploadDogPic_e) {
            DialogInterface.OnClickListener cameraListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    doTakePhotoAction();
                }
            };

            DialogInterface.OnClickListener albumListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    doTakeAlbumAction();
                }
            };

            DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            };

            new AlertDialog.Builder(this)
                    .setTitle("업로드할 이미지 선택")
                    .setPositiveButton("사진 촬영", cameraListener)
                    .setNeutralButton("앨범 선택", albumListener)
                    .setNegativeButton("취 소", cancelListener)
                    .show();
        }
    }

    // 비트맵을 저장하는 부분
    private void storeCropImage(Bitmap bitmap, String filePath) {
        // PetsAndroid 폴더를 생성하여 이미지 저장
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PetsAndroid";
        File directory_PetsAndroid = new File(dirPath);

        if(!directory_PetsAndroid.exists()) // 새로 이미지 저장
            directory_PetsAndroid.mkdir();

        File copyFile = new File(filePath);
        BufferedOutputStream out = null;

        try {
            copyFile.createNewFile();
            out = new BufferedOutputStream(new FileOutputStream(copyFile));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

            // 크롭 한 사진이 앨범에 보이도록 갱신
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(copyFile)));

            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
