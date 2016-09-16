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

        private ImageView img; // ͼƬ
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
                 * ��������� ���߲�ѡ��ͼƬ���� ��ִ���κβ���
                 */

                if (data != null) {
                        /**
                         * ��Ϊ���ַ�ʽ���õ���startActivityForResult�������������ִ����󶼻�ִ��onActivityResult����
                         * �� ����Ϊ�����𵽵�ѡ�����Ǹ���ʽ��ȡͼƬҪ�����ж�
                         * �������requestCode��startActivityForResult����ڶ���������Ӧ 1== ��� 2 ==���
                         */
                      
                	if (requestCode == 1) {

                                try {
                                	    Log.i(TAG, "���");
                                	    
                                        // ���ͼƬ��uri
                                       originalUri = data.getData();
                                     
                                        // ��ͼƬ���ݽ������ֽ�����
                                        mContent = readStream(resolver.openInputStream(Uri
                                                        .parse(originalUri.toString())));
                                                                
                                       // ���ֽ�����ת��ΪImageView�ɵ��õ�Bitmap����
                                        myBitmap = getPicFromBytes(mContent, null);
                                        // //�ѵõ���ͼƬ���ڿؼ�����ʾ
                                        img.setImageBitmap(myBitmap);
                                     
       
                                        
                                } catch (Exception e) {
                                       // System.out.println(e.getMessage());
                                        Toast toast = Toast.makeText(getApplicationContext(), (String)e.getMessage() , Toast.LENGTH_SHORT);
                    					toast.show();
                                }

                        } else if (requestCode == 0) {

                        		Log.i(TAG, "����");
                        	
                        		
                                
                             
                        		String sdStatus = Environment.getExternalStorageState();
                                if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) { // ���sd�Ƿ����
                                        return;
                                }
                                Bundle bundle = data.getExtras();
                                Bitmap bitmap = (Bitmap) bundle.get("data");// ��ȡ������ص����ݣ���ת��ΪBitmapͼƬ��ʽ
                                FileOutputStream b = null;
                                File file = new File( "/sdcard/AllJoyn/send/");
                                file.mkdirs();// �����ļ��У�����ΪAllJoynImage
                                if (data.getData() != null)  
                     	        {  
                     	            originalUri = data.getData();  
                     	            Log.i(TAG,"��uri");
                     	        }  
                     	        else  
                     	        {  
                     	            originalUri  = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, null,null));      
                     	        }  
                                // ��Ƭ��������Ŀ���ļ����£��Ե�ǰʱ�����ִ�Ϊ���ƣ�����ȷ��ÿ����Ƭ���Ʋ���ͬ��
                                String str = null;
                                Date date = null;
                                SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");// ��ȡ��ǰʱ�䣬��һ��ת��Ϊ�ַ���
                                date = new Date();
                                str = format.format(date);
                                String fileName =  "/sdcard/AllJoyn/Send/" + str + ".jpg";
                              
                                try {
                                        b = new FileOutputStream(fileName);
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, b);// ������д���ļ�
                                    
                                 		 
                                  		
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
                                  
                                     System.out.println("�ɹ�======" + cameraBitmap.getWidth()
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

        // ��ͼƬ���ݽ������ֽ�����
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
        		   //�����Ի�ѡ���Ƿ�Ҫ�������ͼƬ
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

      
       	Log.i(TAG,"���selecteImageIntent�Ѿ�ѡ����ͼƬ");
       	selectedUri = originalUri;
        Intent selectImageIntent = new Intent();
        selectImageIntent.putExtra("imageUri", selectedUri);
       	
	    Log.i(TAG,"finishimageselection()");
		setResult(RESULT_OK, selectImageIntent);
		finish();
       	
        }
        

}