package com.example.fasttrans;


import java.io.File;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class OpenSelectedFiles extends Activity{
	
    
	 public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
        Intent OpenFileIntent = getIntent();
        String text = OpenFileIntent.getStringExtra("request_text_for_second");
     	System.out.print(text);
        File currentPath = (File) OpenFileIntent.getExtras().get("openedfile");
     	
     	if(currentPath!=null&&currentPath.isFile())
    {
        String fileName = currentPath.toString();
        Intent intent;
        if(checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingImage))){
           intent = OpenFiles.getImageFileIntent(currentPath);
        	startActivity(intent);
               
         
        }else if(checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingWebText))){
            intent = OpenFiles.getHtmlFileIntent(currentPath);
            startActivity(intent);
            
        }else if(checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingPackage))){
            intent = OpenFiles.getApkFileIntent(currentPath);
            startActivity(intent);
           
        }else if(checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingAudio))){
            intent = OpenFiles.getAudioFileIntent(currentPath);
            startActivity(intent);
          
        }else if(checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingVideo))){
            intent = OpenFiles.getVideoFileIntent(currentPath);
            startActivity(intent);
        }else if(checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingText))){
            intent = OpenFiles.getTextFileIntent(currentPath);
            startActivity(intent);
        }else if(checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingPdf))){
            intent = OpenFiles.getPdfFileIntent(currentPath);
            startActivity(intent);
        }else if(checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingWord))){
            intent = OpenFiles.getWordFileIntent(currentPath);
            startActivity(intent);
        }else if(checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingExcel))){
            intent = OpenFiles.getExcelFileIntent(currentPath);
            startActivity(intent);
        }else if(checkEndsWithInStringArray(fileName, getResources().
                getStringArray(R.array.fileEndingPPT))){
            intent = OpenFiles.getPPTFileIntent(currentPath);
            startActivity(intent);
            
        }else
        {
            showMessage("无法打开，请安装相应的软件！");
        }
    }else
    {
        showMessage("对不起，这不是文件！");
    }
    
     	finish();    
	 }

//定义用于检查要打开的文件的后缀是否在遍历后缀数组中
private boolean checkEndsWithInStringArray(String checkItsEnd,
                String[] fileEndings){
    for(String aEnd : fileEndings){
        if(checkItsEnd.endsWith(aEnd))
            return true;
    }
    return false;
}


public void showMessage(String message){
Toast toast = Toast.makeText(OpenSelectedFiles.this,(String)message,Toast.LENGTH_SHORT);
toast.show();
}

@Override
public void onBackPressed() 
{
	
	
			try {
				Intent i = new Intent();
				i.putExtra("request_text_for_third","从OpenSelectedFile到SDcard");
						
				setResult(Activity.RESULT_OK,i);
				finish();
			} catch (Exception e) {
				// TODO: handle exception
			}
	
}	



}
      
     
       




