package app.ij.mlwithtensorflowlite;


import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.lite.DataType;

import app.ij.mlwithtensorflowlite.ml.Model;



import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends Activity {//AppCompatActivity

    TextView result, confidence;
    ImageView imageView;
    Button camera, gallery;
    int imageSize = 640;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = findViewById(R.id.result);
        confidence = findViewById(R.id.confidence);
        imageView = findViewById(R.id.imageView);
        camera = findViewById(R.id.button);
        gallery = findViewById(R.id.button2);

        camera.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                // Launch camera if we have permission
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//camera dönmesini engelle
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 1);
                } else {
                    //Request camera permission if we don't have it.
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(cameraIntent, 1);
            }
        });
    }


    public void classifyImage(Bitmap image){
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            //TensorImage image = TensorImage.fromBitmap(imageBit);//?
            TensorBuffer inputFeature0=TensorBuffer.createFixedSize(new int[]{1,640,640,3}, DataType.FLOAT32);
            ByteBuffer byteBuffer= ByteBuffer.allocateDirect(4*imageSize*imageSize*3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int [] intValues=new int[imageSize*imageSize];
            image.getPixels(intValues,0,image.getWidth(),0,0,image.getWidth(),image.getHeight());
            int pixel=0;
            for(int i=0;i<imageSize;i++){
                for(int j=0;j<imageSize;j++){
                    int val=intValues[pixel++];
                    byteBuffer.putFloat(((val>>16)&0xFF)*(1.f / 1));//1 || /255.f
                    byteBuffer.putFloat(((val>>8)&0xFF)*(1.f / 1));
                    byteBuffer.putFloat((val&0xFF)*(1.f / 1));

                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0= outputs.getOutputAsTensorBuffer();//

            float[] confidences=outputFeature0.getFloatArray();
            int maxPos=0;
            float maxConfidence=0;
            for(int i=0;i<confidences.length;i++){
                if(confidences[i]>maxConfidence){
                    maxConfidence=confidences[i];
                    maxPos=i;
                }
            }
            String[] classes= {"headphone","mouse","papia"};
            result.setText(classes[maxPos]);

            //String s="";
            //for(int i=0;i< classes.length;i++){
             //  s += String.format("s%: %.1f%%\n",classes[i],confidences[i]*100);
            //}

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }


    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            Bitmap image = (Bitmap) data.getExtras().get("data");
            int dimension= Math.min(image.getWidth(),image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image, dimension,dimension);
            imageView.setImageBitmap(image);

            image= Bitmap.createScaledBitmap(image,imageSize,imageSize,false);
            classifyImage(image);
        }else{
            Uri dat = data.getData();
            Bitmap image = null;
            try {
                image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
            } catch (IOException e) {
                e.printStackTrace();
            }
            imageView.setImageBitmap(image);

            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
            classifyImage(image);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}