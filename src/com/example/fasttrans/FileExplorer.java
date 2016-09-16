package com.example.fasttrans;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.example.fasttrans.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FileExplorer extends Activity 
{
	private List<String> fileList = new ArrayList<String>();
	private ListView listView;
	private Intent selectedFileIntent;
	private File root;
	private File selected;

	/*
	 * Displays the file system to the user and returns the
	 * selected file to the calling activity using the putExtras()
	 * method 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		//加载explorer资源，这里缺少
		setContentView(R.layout.explorer);

		selectedFileIntent = new Intent();
        
		//这里改动：新建了个explorer.xml文件，并写了个listview在里面
		listView = (ListView) findViewById(R.id.listView);			
		listView.setOnItemClickListener(new OnItemClickListener() 
		{
			public void onItemClick(AdapterView<?> listView, View v, int position, long id) 
			{				
				selected = new File(fileList.get(position));				
				
				if(selected.isDirectory()) 
				{
					listDir();
				}
				else 
				{
					finishSelection();
				}				
			}
		});		
		registerForContextMenu(listView);

		root = new File(Environment.getExternalStorageDirectory().getAbsolutePath());	
		selected = root;
		listDir(); 
	}		

	private void finishSelection() 
	{
		selectedFileIntent.putExtra("file", selected);
		setResult(RESULT_OK, selectedFileIntent);
		finish();
	}

	private void listDir() 
	{
		File[] files = selected.listFiles();

		fileList.clear();
		for (File file : files) 
		{
			fileList.add(file.getPath()); 
		}
//配置ListView，使用系统自带的simple_list_item_1格式，格式内容为只有一个TextView，其id是android.R.id.text1
		//fileList是向格式中填充的数据。
		ArrayAdapter<String> directoryList = new ArrayAdapter<String>(this, 
				android.R.layout.simple_list_item_1, fileList);
		listView.setAdapter(directoryList);
	} 

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{		   
		//这里改动：新建了个explorer.xml文件，并写了个listview在里面
		if (v.getId() == R.id.listView) 
		{
			menu.setHeaderTitle("Choose Directory");
			String[] menuItems = new String[] { "是", "否" }; 

			for (int i = 0; i < menuItems.length; i++) 
			{
				menu.add(Menu.NONE, i, i, menuItems[i]);
			}			   			   			   
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)	
	{
		String selectedString = item.getTitle().toString();		
		
		if (selectedString == "是")
		{
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();			
			String path = fileList.get(info.position);
			selected = new File(path);
			
			finishSelection();		
		}		
		return true;				
	}
	
	@Override
    public void onBackPressed() 
    {
		if (!selected.getAbsolutePath().equals(root.getAbsolutePath()))
		{
			selected = selected.getParentFile();
			listDir();
			return;
		}
    	super.onBackPressed();
    }	
}

