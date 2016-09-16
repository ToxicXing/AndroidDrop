package com.example.fasttrans;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.fasttrans.SDCardFileExplorer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SDCardFileExplorer extends Activity {

	private TextView tvpath;
	private ListView lvFiles;
	private static final String TAG = "SDCard";
	//private Button btnParent;

	// 记录当前的父文件夹
	File currentParent;

	// 记录当前路径下的所有文件夹的文件数组
	File[] currentFiles;
	
	private Intent OpenFileIntent;
	private final int SECOND_REQUEST_CODE = 11;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sdcardmain);

		lvFiles = (ListView) this.findViewById(R.id.files);

		tvpath = (TextView) this.findViewById(R.id.tvpath);
	

		String text = getIntent().getStringExtra("request_text_for_main");
		Log.i(TAG,text);
		// 获取系统的SDCard的目录
		File root = new File("/mnt/sdcard/AllJoyn");
		// 如果SD卡存在的话
		if (root.exists()) {

			currentParent = root;
			currentFiles = root.listFiles();
			// 使用当前目录下的全部文件、文件夹来填充ListView
			inflateListView(currentFiles);

		}

		lvFiles.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				// 如果用户单击了文件
				if (currentFiles[position].isFile()) {
					// 也可自定义扩展打开这个文件等
					File currentPath = new File(currentFiles[position].getAbsolutePath());	
					//这里改为打开文件！！！！
					OpenFileIntent = new Intent(SDCardFileExplorer.this, OpenSelectedFiles.class);
					OpenFileIntent.putExtra("openedfile",currentPath);
					OpenFileIntent.putExtra("request_text_for_second","从SDcard到OpenSelectedFilesActivity");
					startActivityForResult(OpenFileIntent,SECOND_REQUEST_CODE);
					
					
				
				}else{
				
				// 获取用户点击的文件夹 下的所有文件
				File[] tem = currentFiles[position].listFiles();
				
					// 获取用户单击的列表项对应的文件夹，设为当前的父文件夹
					currentParent = currentFiles[position];
					// 保存当前的父文件夹内的全部文件和文件夹
					currentFiles = tem;
					// 再次更新ListView
					inflateListView(currentFiles);
				
				}
			}
		});

	
		

	}

	
	/**
	 * 根据文件夹填充ListView
	 * 
	 * @param files
	 */
	private void inflateListView(File[] files) {

		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();

		for (int i = 0; i < files.length; i++) {

			Map<String, Object> listItem = new HashMap<String, Object>();

			if (files[i].isDirectory()) {
				// 如果是文件夹就显示的图片为文件夹的图片
				listItem.put("icon", R.drawable.folder);
			} else {
				listItem.put("icon", R.drawable.file);
			}
			// 添加一个文件名称
			listItem.put("filename", files[i].getName());

			

			listItems.add(listItem);

		}

		// 定义一个SimpleAdapter
		SimpleAdapter adapter = new SimpleAdapter(
				SDCardFileExplorer.this,listItems, R.layout.list_item,
				new String[] { "filename", "icon"}, new int[] {
						R.id.file_name, R.id.icon});

		// 填充数据集
		lvFiles.setAdapter(adapter);

		try {
			tvpath.setText("Path:" + currentParent.getCanonicalPath());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
        super.onActivityResult(requestCode, resultCode, data);  
        if(requestCode==SECOND_REQUEST_CODE && resultCode==Activity.RESULT_OK){  
            if(data != null) {  
                System.out.println(data.getStringExtra("request_text_for_third")); 
            }  
        }  
    }  
	
	
	
	
	
	@Override
    public void onBackPressed() 
    {
		// 获取上一级目录
		
				try {

					if (!currentParent.getCanonicalPath().endsWith("AllJoyn") ) {

						
						System.out.println("我在子目录中"+currentParent.getCanonicalPath());
						// 获取上一级目录
						currentParent = currentParent.getParentFile();
						// 列出当前目录下的所有文件
						currentFiles = currentParent.listFiles();
						// 再次更新ListView
						inflateListView(currentFiles);
					}else{
						System.out.println("已经是alljoyn目录，要回main");
						Intent i = new Intent();
						i.putExtra("final_request", "从SDCard回到main");
						setResult(Activity.RESULT_FIRST_USER,i);
						finish();
						}
				} catch (Exception e) {
					// TODO: handle exception
				}
    	
    }	
}