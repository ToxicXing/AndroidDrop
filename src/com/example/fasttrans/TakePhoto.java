package com.example.fasttrans;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.R.string;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class TakePhoto extends Activity {
        /** Called when the activity is first created. */

        private ImageView img; // 图片
        Bitmap myBitmap;
        private byte[] mContent;
        private File selected;
        Bitmap selectedBitmap;
   
        
       
        Uri selectedUri;
        Uri originalUri;
        String img_path;
        private static final String TAG = "FileTrans.TakePhoto";
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.photo);
                Log.i(TAG, "onCreate()");
              
                img = (ImageView) findViewById(R.id.img);
              
                      
                                final AlertDialog.Builder builder = new AlertDialog.Builder(
                                                TakePhoto.this);
                                builder.setTitle("Choose Image");

                                builder.setPositiveButton("Camera",
                                                new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface

                                                        dialog, int which) {
                                                                Intent intent = new Intent(
                                                                                "android.media.action.IMAGE_CAPTURE");
                                                                startActivityForResult(intent, 0);

                                                        }
                                                });
                                builder.setNegativeButton("Album",
                                                new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface

                                                        dialog, int which) {
                                                                Intent intent = new Intent(
                                                                                Intent.ACTION_PICK,
                                                                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                                                startActivityForResult(intent, 1);

                                                        }
                                                });
                                AlertDialog alert = builder.create();
                                alert.show();

                        }
                

        

        protected void onActivityResult(int requestCode, int resultCode, Intent data) {

                ContentResolver resolver = getContentResolver();
                /**
                 * 如果不拍照 或者不选择图片返回 不执行任何操作
                 */

                if (data != null) {
                        /**
                         * 因为两种方式都用到了startActivityForResult方法，这个方法执行完后都会执行onActivityResult方法
                         * ， 所以为了区别到底选择了那个方式获取图片要进行判断
                         * ，这里的requestCode跟startActivityForResult里面第二个参数对应 1== 相册 2 ==相机
                         */
                      
                	if (requestCode == 1) {

                                try {
                                	    Log.i(TAG, "相册");
                                	    
                                        // 获得图片的uri
                                       originalUri = data.getData();
                                     
                                        // 将图片内容解析成字节数组
                                        mContent = readStream(resolver.openInputStream(Uri
                                                        .parse(originalUri.toString())));
                                                                
                                       // 将字节数组转换为ImageView可调用的Bitmap对象
                                        myBitmap = getPicFromBytes(mContent, null);
                                        // //把得到的图片绑定在控件上显示
                                        img.setImageBitmap(myBitmap);
                                     
       
                                        
                                } catch (Exception e) {
                                       // System.out.println(e.getMessage());
                                        Toast toast = Toast.makeText(getApplicationContext(), (String)e.getMessage() , Toast.LENGTH_SHORT);
                    					toast.show();
                                }

                        } else if (requestCode == 0) {

                        		Log.i(TAG, "拍照");
                        	
                        		
                                
                             
                        		String sdStatus = Environment.getExternalStorageState();
                                if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) { // 检测sd是否可用
                                        return;
                                }
                                Bundle bundle = data.getExtras();
                                Bitmap bitmap = (Bitmap) bundle.get("data");// 获取相机返回的数据，并转换为Bitmap图片格式
                                FileOutputStream b = null;
                                File file = new File( "/sdcard/AllJoyn/send/");
                                file.mkdirs();// 创建文件夹，名称为AllJoynImage
                                if (data.getData() != null)  
                     	        {  
                     	            originalUri = data.getData();  
                     	            Log.i(TAG,"有uri");
                     	        }  
                     	        else  
                     	        {  
                     	            originalUri  = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, null,null));      
                     	        }  
                                // 照片的命名，目标文件夹下，以当前时间数字串为名称，即可确保每张照片名称不相同。
                                String str = null;
                                Date date = null;
                                SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");// 获取当前时间，进一步转化为字符串
                                date = new Date();
                                str = format.format(date);
                                String fileName =  "/sdcard/AllJoyn/Send/" + str + ".jpg";
                              
                                try {
                                        b = new FileOutputStream(fileName);
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, b);// 把数据写入文件
                                    
                                 		 
                                  		
                                //        b.write(buffer);
                                        
                                } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                } finally {
                                        try {
                                                b.flush();
                                                b.close();
                                        } catch (IOException e) {
                                                e.printStackTrace();
                                        }
                                        if (data != null) {
                                        	 Bitmap cameraBitmap = (Bitmap) data.getExtras().get("data");
            //                           
                                        	 
                                     System.out.println("fdf================="+ data.getDataString());
                                     img.setImageBitmap(cameraBitmap);
                                  
                                     System.out.println("成功======" + cameraBitmap.getWidth()
                                                     + cameraBitmap.getHeight());
                                     
                                     
                                    
                                        }

                                }
                        }
                              
                }
        }
        
        public static Bitmap getPicFromBytes(byte[] bytes,
                        BitmapFactory.Options opts) {
                if (bytes != null)
                        if (opts != null)
                                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length,
                                                opts);
                        else
                                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                return null;
        }

        // 将图片内容解析成字节数组
        public static byte[] readStream(InputStream inStream) throws Exception {
                byte[] buffer = new byte[1024];
                int len = -1;
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                while ((len = inStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, len);
                }
                byte[] data = outStream.toByteArray();
                
                outStream.close();
                inStream.close();
                return data;

        }
        

        
        
        
        
   
        
        
        
        public void ImgClickListener(View v){
        	switch(v.getId()){
        		case R.id.img:
        		   //弹出对话选择是否要发送这个图片
        			final AlertDialog.Builder builder = new AlertDialog.Builder(
                            TakePhoto.this);
            builder.setTitle("Double Comfirm");

            builder.setPositiveButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface

                                    dialog, int which) {
                                    dialog.dismiss();
                                    finish();
                                    }
                            });
            builder.setNegativeButton("OK",
                            new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface

                                    dialog, int which) {
                                    
                                   finishimageselection();
                                   finish();
                                    }
                            });
            AlertDialog alert = builder.create();
            alert.show();
        			break;
        	}
        }
        
        private void finishimageselection(){

      
       	Log.i(TAG,"完成selecteImageIntent已经选择了图片");
       	selectedUri = originalUri;
        Intent selectImageIntent = new Intent();
        selectImageIntent.putExtra("imageUri", selectedUri);
       	
	    Log.i(TAG,"finishimageselection()");
		setResult(RESULT_OK, selectImageIntent);
		finish();
       	
        }
        

}