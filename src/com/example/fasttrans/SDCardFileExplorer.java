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

	// ��¼��ǰ�ĸ��ļ���
	File currentParent;

	// ��¼��ǰ·���µ������ļ��е��ļ�����
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
		// ��ȡϵͳ��SDCard��Ŀ¼
		File root = new File("/mnt/sdcard/AllJoyn");
		// ���SD�����ڵĻ�
		if (root.exists()) {

			currentParent = root;
			currentFiles = root.listFiles();
			// ʹ�õ�ǰĿ¼�µ�ȫ���ļ����ļ��������ListView
			inflateListView(currentFiles);

		}

		lvFiles.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				// ����û��������ļ�
				if (currentFiles[position].isFile()) {
					// Ҳ���Զ�����չ������ļ���
					File currentPath = new File(currentFiles[position].getAbsolutePath());	
					//�����Ϊ���ļ���������
					OpenFileIntent = new Intent(SDCardFileExplorer.this, OpenSelectedFiles.class);
					OpenFileIntent.putExtra("openedfile",currentPath);
					OpenFileIntent.putExtra("request_text_for_second","��SDcard��OpenSelectedFilesActivity");
					startActivityForResult(OpenFileIntent,SECOND_REQUEST_CODE);
					
					
				
				}else{
				
				// ��ȡ�û�������ļ��� �µ������ļ�
				File[] tem = currentFiles[position].listFiles();
				
					// ��ȡ�û��������б����Ӧ���ļ��У���Ϊ��ǰ�ĸ��ļ���
					currentParent = currentFiles[position];
					// ���浱ǰ�ĸ��ļ����ڵ�ȫ���ļ����ļ���
					currentFiles = tem;
					// �ٴθ���ListView
					inflateListView(currentFiles);
				
				}
			}
		});

	
		

	}

	
	/**
	 * �����ļ������ListView
	 * 
	 * @param files
	 */
	private void inflateListView(File[] files) {

		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();

		for (int i = 0; i < files.length; i++) {

			Map<String, Object> listItem = new HashMap<String, Object>();

			if (files[i].isDirectory()) {
				// ������ļ��о���ʾ��ͼƬΪ�ļ��е�ͼƬ
				listItem.put("icon", R.drawable.folder);
			} else {
				listItem.put("icon", R.drawable.file);
			}
			// ���һ���ļ�����
			listItem.put("filename", files[i].getName());

			

			listItems.add(listItem);

		}

		// ����һ��SimpleAdapter
		SimpleAdapter adapter = new SimpleAdapter(
				SDCardFileExplorer.this,listItems, R.layout.list_item,
				new String[] { "filename", "icon"}, new int[] {
						R.id.file_name, R.id.icon});

		// ������ݼ�
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
		// ��ȡ��һ��Ŀ¼
		
				try {

					if (!currentParent.getCanonicalPath().endsWith("AllJoyn") ) {

						
						System.out.println("������Ŀ¼��"+currentParent.getCanonicalPath());
						// ��ȡ��һ��Ŀ¼
						currentParent = currentParent.getParentFile();
						// �г���ǰĿ¼�µ������ļ�
						currentFiles = currentParent.listFiles();
						// �ٴθ���ListView
						inflateListView(currentFiles);
					}else{
						System.out.println("�Ѿ���alljoynĿ¼��Ҫ��main");
						Intent i = new Intent();
						i.putExtra("final_request", "��SDCard�ص�main");
						setResult(Activity.RESULT_FIRST_USER,i);
						finish();
						}
				} catch (Exception e) {
					// TODO: handle exception
				}
    	
    }	
}