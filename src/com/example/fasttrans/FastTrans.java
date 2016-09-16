package com.example.fasttrans;

import java.io.File;
import java.util.ArrayList;



import org.alljoyn.cops.filetransfer.FileTransferComponent;
import org.alljoyn.cops.filetransfer.data.FileDescriptor;
import org.alljoyn.cops.filetransfer.data.ProgressDescriptor;
import org.alljoyn.cops.filetransfer.data.StatusCode;
import org.alljoyn.cops.filetransfer.listener.FileAnnouncementReceivedListener;
import org.alljoyn.cops.filetransfer.listener.FileCompletedListener;
import org.alljoyn.cops.filetransfer.listener.OfferReceivedListener;
import org.alljoyn.cops.filetransfer.listener.RequestDataReceivedListener;
import org.alljoyn.cops.filetransfer.listener.UnannouncedFileRequestListener;

import com.example.fasttrans.AlljoynManager;
import com.example.fasttrans.ConnectionListener;
import com.example.fasttrans.FileExplorer;
import com.example.fasttrans.FastTrans;
import com.example.fasttrans.R;
import com.example.fasttrans.SDCardFileExplorer;
import com.example.fasttrans.TakePhoto;
import com.example.fasttrans.AlljoynManager.ConnectionState;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class FastTrans extends Activity implements ConnectionListener{
	private static final String TAG = "FileTransferActivity";
	//private String sessionname;
	//private String sessionpsw;
	private int CONFIG_FLAG_NAME = 0;
	private int CONFIG_FLAG_PASSWORD = 0;
	int SELECT_IMAGEORFILE_FLAG;
	
	
	/* UI Handler Codes */
	private static final int TOAST = 0;
	private static final int ENABLE_VIEW = 1;
	private static final int DISABLE_VIEW = 2;
	private static final int UPDATE_RECEIVE_PROGRESS = 3;
	private static final int UPDATE_SEND_PROGRESS = 4;
	
	/* File Explorer Activity Result Codes */
	private static final int SHARE_SELECTED_FILE = 5;
	private static final int OFFER_SELECTED_FILE = 6;
    //我定义的
	private static final int IMAGE_SHARE = 7;
	private static final int IMAGE_OFFER = 8;
	//private static final int SELECTED_FILES = 9;
	private static final int FIRST_REQUEST_CODE = 10;
	
	
	/* UI Buttons */
	private Button hostButton;
	private Button joinButton;
	private Button shareButton;
	private Button unshareButton;
	private Button requestButton;
	private Button offerButton;
	private Button receivedFilesButton;
	private Button pauseReceiveButton;
	private Button cancelReceiveButton;
	private Button cancelSendButton;
	//private Button requestImageButton;
	
	/* Creates and manages AllJoyn communication  */
	private AlljoynManager ajManager;	
	/* Facilitates sharing and transferring files */
	///private org.alljoyn.cops.filetransfer.FileTransferModule ftModule;	
	public static FileTransferComponent ftModule;
	/* Background thread used to monitor the progress of files being received */
	private Thread monitorReceiveThread;
	/* Background thread used to monitor the progress of files being sent */
	private Thread monitorSendThread;
	
	/* UI Handler. Ensures UI operations are performed on the UI thread */
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message message)
		{
			switch (message.what)
			{
				case TOAST:			
					//显示Toast消息。参数一：当前上下文环境，可用getApplicationConteext()
					//参数二：要显示的字符串
					//参数三：显示持续时长，有两种，LENGTH_SHORT表示持续时间短。LENGTH_LONG表示持续时间长
					Toast toast = Toast.makeText(getApplicationContext(), (String) message.obj, Toast.LENGTH_SHORT);
					//显示TOAST消息
					toast.show();
					break;
					//设置obj所对应的那个东西（这里一般是按钮）是否启用。
					//即按钮是否是灰钮，或者是可以按的按钮
				case ENABLE_VIEW:
					View viewToEnable = (View) message.obj;
					viewToEnable.setEnabled(true);
					break;
					//设置obj所对应的那个东西（这里一般是按钮）是否启用。
					//即按钮是否是灰钮，或者是可以按的按钮
				case DISABLE_VIEW:
					View viewToDisable = (View) message.obj;
					viewToDisable.setEnabled(false);
					break;	
					//进度条（收）
				case UPDATE_RECEIVE_PROGRESS:
					ProgressBar receiveProgressBar = (ProgressBar) findViewById(R.id.receiveProgressBar);	
					//设置进度完成百分比
					receiveProgressBar.setProgress( (Integer) message.obj);
					break;
					//进度条（发）
				case UPDATE_SEND_PROGRESS:
					ProgressBar sendProgressBar = (ProgressBar) findViewById(R.id.sendProgressBar);	
					//设置进度完成百分比
					sendProgressBar.setProgress( (Integer) message.obj);
					break;		
			}
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        ajManager = new AlljoynManager();  
        ajManager.setConnectionListener(this);
        
        initializeGuiModules();  
    }
    
    /* Initialize and assign listeners to UI Modules
     * 初始化图形界面，定义了各种按钮
     *  */
	private void initializeGuiModules()
	{
		hostButton = (Button) findViewById(R.id.hostButton);
        hostButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onHostButtonClicked();
			}    	
        });        
        
        joinButton = (Button) findViewById(R.id.joinButton);
        joinButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onJoinButtonClicked();
			}    	
        });
        
        shareButton = (Button) findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onShareButtonClicked();
			}    	
        });
        
        unshareButton = (Button) findViewById(R.id.stopShareButton);
        unshareButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				OnUnshareButtonClicked();
			}    	
        });
        
        //getFiles Button
        requestButton = (Button) findViewById(R.id.requestButton);
        requestButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onRequestButtonClicked();
			}    	
        });
        
        offerButton = (Button) findViewById(R.id.offerButton);
        offerButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onOfferButtonClicked();
			}    	
        });
        
        //My Files button
        receivedFilesButton = (Button) findViewById(R.id.requestFileIdButton);
        receivedFilesButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				OnReceivedFilesClicked();
			}
        });
        
        pauseReceiveButton = (Button) findViewById(R.id.pauseReceiveButton);
        pauseReceiveButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onPauseReceiveButtonClicked();
			}    	
        });
        
        cancelReceiveButton = (Button) findViewById(R.id.cancelButton);
        cancelReceiveButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onCancelReceiveButtonClicked();
			}    	
        });
        
        cancelSendButton = (Button) findViewById(R.id.cancelSendButton);
        cancelSendButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				onPauseSendButtonClicked();
			}    	
        });
        

	}
	
	
	/*
	 * Initialize the File Transfer Module and its listeners
	 * 初始化文件传输模型和它的监听器
	 */
	private void initializeFileTransferModule()
	{
		//Construct the module using the Bus Attachment and Session Id created by the AllJoyn Manager
		//利用AllJoyn Manager创建的Bus Attachment和Session Id来构建file transfer module
		//文件传输的Component(总线附件，会话ID)
		ftModule = new FileTransferComponent(ajManager.getBusAttachment(), ajManager.getSessionId());
		
		//Register announcement listener - create a toast when a file announcement is received
		//注册annoucement listener  当收到file announcement时创建toast
		ftModule.setFileAnnouncementReceivedListener(new FileAnnouncementReceivedListener()
		{
			//???参数是什么意思？何为fileIdResponse。该值为true和false各有什么作用
			public void receivedAnnouncement(FileDescriptor[] fileList, boolean isFileIdResponse)
			{		
				if (!isFileIdResponse)
				{
					//发送Message，类型是Toast，内容是“有小伙伴发送文件啦”
					//obtainMessage两个参数。第一个参数是what，这个参数值决定了return message中的
					//what field填什么。这里明显是toast。第二个值是Obj。决定了return message中obj的值是什么。即toast要显示什么。
					handler.sendMessage(handler.obtainMessage(TOAST, "File accessable..."));
				}
				else
				{
					handler.sendMessage(handler.obtainMessage(TOAST, "File ID Response Announcement Received!"));
				}
			}			
		});
		
		//Register file completed listener - disable buttons if the completion was triggered by a cancel
		//“存储文件完成”的监听方法 状态码为cancelled时隐藏相关的暂停接收和取消接收按钮，否则就是传输完成
		ftModule.setFileCompletedListener(new FileCompletedListener()
		{
			public void fileCompleted(String filename, int statusCode)
			{
				if (statusCode == StatusCode.CANCELLED)
				{
					//在前面的switch中，如果传入的what参数为DISABLE_VIEW。就setEnable=false
					//也就是说。如果状态码是CANCELLED，那么pauseReceivedButton和CancelRecievedButton
					//就是灰钮
					handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, pauseReceiveButton));
					handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, cancelReceiveButton));
				}
				//文件传输完成，显示toast提示。
				handler.sendMessage(handler.obtainMessage(TOAST, "Transfer finished： " + filename));				
			}			
		});
		
		//Register offer file listener - always accept offers (return true), and monitor the receiving progress
		//注册offer file listener 总是接收提供的文件，并且监控接收的过程，可以暂停接收和取消接收
		
		//此处应该要修改，增加验证过程
		//监听的是接受过程
		ftModule.setOfferReceivedListener(new OfferReceivedListener()
		{			
			public boolean acceptOfferedFile(FileDescriptor file, String peer)
			{	//如果线程没有busy，就执行ReceiveProgress的监控过程
				//这个监控过程会给UI Handler发信号来更新进度条
				if (monitorReceiveThread == null)
				{
					monitorReceiveProgress();
				}				
				//激活两个灰钮。
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, pauseReceiveButton));
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, cancelReceiveButton));
				return true;
			}
		});
		
		
		//Register unannounced File Listener - always allow files to be requested by path, 
		//even if they aren't shared. Required for use of requestFileId feature
		//总是允许文件被请求，即使他们没有被共享。
		
		//默认是不允许的，需要被请求者主动设置允许其他人请求文件
		ftModule.setUnannouncedFileRequestListener(new UnannouncedFileRequestListener()
		{			
			public boolean allowUnannouncedFileRequests(String filePath)
			{
				return true;
			}
		});
		
		//register request data listener - monitor sending progress when a file is requested
		//请求数据监听器   当文件被请求后监控发送的过程
		//监听的是发送过程
		ftModule.setRequestDataReceivedListener(new RequestDataReceivedListener()
		{
			public void fileRequestReceived(String fileName)
			{
				//激活发送过程的取消按钮
				//
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, cancelSendButton));
				
				if (monitorSendThread == null)
				{
					monitorSendProgress();
				}
			}			
		});
	}	
	
	/* 
	 * Activates the UI once an AllJoyn connection has been established.
	 * Triggered by the AllJoyn Manager. 
	 * 一旦AllJoyn连接被建立，激活用户界面，
	 * 由AllJoyn Manager触发
	 */
	
	//传入参数：连接状态
	public void ConnectionChanged(ConnectionState connectionState)
	{
		if (connectionState == ConnectionState.CONNECTED)
		{
			handler.sendMessage(handler.obtainMessage(TOAST, "Successfully Connected！"));
			
			handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, hostButton));
			handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, joinButton));
			
			handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, shareButton));
			handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, unshareButton));
			handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, requestButton));
			handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, offerButton));
		//	handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, requestByPathButton));
			
            new Thread()
            {
                public void run()
                {
                    initializeFileTransferModule();     
                }
            }.start();                      
		}
	}
    
	/*
	 * Start hosting and advertising an AllJoyn File Transfer Session
	 * 开始主持并广播一个AllJoyn文件传输会话
	 */
	private void onHostButtonClicked()
	{
		hostButton.setEnabled(false);
		joinButton.setEnabled(true);
		
		
		//弹出对话框，允许用户设置会话的名称和密码
		hostButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	//这个函数在后面有实现。用来给会话设置密码
                showSessionConfigDialog();
        	}
        });
		
		
		
		handler.sendMessage(handler.obtainMessage(TOAST, "Hosting Session"));	
		
		ajManager.createSession();
	}
	
	/*
	 * Attempt to join an AllJoyn File Transfer Session. Another device must be hosting
	 * before a join will complete successfully
	 * 尝试加入会话，必要要先有其他设备发起了会话才能成功加入
	 */
	private void onJoinButtonClicked()
	{
		joinButton.setEnabled(false);
		hostButton.setEnabled(true);
		
		handler.sendMessage(handler.obtainMessage(TOAST, "Session creating"));		
		
		ajManager.joinSession();	
	}

	
	/*
	 * Stop sharing a file. 
	 */
	//我要停止分享文件时，点击按钮时执行的内容写在下面
	private void OnUnshareButtonClicked()
	{
		//get list of files currently being shared
		//得到现在正在被分享的文件列表
		final ArrayList<FileDescriptor> announcedFiles = ftModule.getAnnouncedLocalFiles();
		
		//create an array of filenames to display to the user
		//建立一个文件名的数组来向用户展示
		String[] filenames = new String[announcedFiles.size()];		
		for (int i = 0; i < announcedFiles.size(); i++)
		{
			filenames[i] = announcedFiles.get(i).filename;
		}
		
		//create the click listener - when a file is selected, share it
		//当文件被选择时，就分享它（这句注释貌似不对啊？？）
		//DialogInterface.onClickListener是为对话框准备的点击监听器
		DialogInterface.OnClickListener onFileClicked = new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				//which索引指的是什么？which是什么？
				//好像是返回了一个文件，如果是返回一个文件的话，就肯定是要传输的文件
				FileDescriptor selected = announcedFiles.get(which);//返回的是which的索引
				
				ArrayList<String> path = new ArrayList<String>();
				//新建一个文件，这个文件文件名用的是收到文件的文件名？文件路径用的是sharedPath。不知是什么路径？
				//并在path字符串数组中加入新文件的绝对路径。
				path.add(new File(selected.sharedPath, selected.filename).getAbsolutePath());				
				
				//stopAnnounce是停止广播的命令。但是为什么这里要在点击以后停止广播呢？点击的是什么呢？
				ftModule.stopAnnounce(path);
			}			
		};
		
		//show the file picker dialog
		//显示文件选择对话框，这个函数有两个参数。第一个是文件名。第二个是点击监听器。
		//这个函数在后面有实现
		showFilePickerDialog(filenames, onFileClicked);
	}

	/*
	 * Request a known file. Files must have been announced, or requested by file file path
	 * before they can be transfered.	 
	 * 请求一个已知的文件。在文件可以被传输之前，文件必须已经被announced，或者是通过文件路径被请求。
	 */
	//获取文件按钮被点击时执行的内容
	private void onRequestButtonClicked()
	{
		//get list of files available for transfer
		//FD数组。数组名：availableFiles
		final ArrayList<FileDescriptor> availableFiles = ftModule.getAvailableRemoteFiles();
		
		//create an array of filenames to display to the user
		//创建一个文件名的数组来向用户展示
		String[] filenames = new String[availableFiles.size()];		
		for (int i = 0; i < availableFiles.size(); i++)
		{
			filenames[i] = availableFiles.get(i).filename;
		}
		
		//create the click listener - when a file is selected, request it and monitor its
		//receiving progress
		//建立点击监听器，当一个文件被选中，请求它并且监控它的接受过程
		//建立对话框点击监听器，名：onFileClicked 功能：
		DialogInterface.OnClickListener onFileClicked = new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				
				//获取which索引，得到的是文件？？
				FileDescriptor selected = availableFiles.get(which);
				//requestFile函数有三个参数：第一个：字符串peer参数，这个参数是不是代表着是通信双方中的哪一端呢？
				//第二个参数是byte数组，内容是文件ID，那么文件ID是什么？？？
				//第三个参数是文件名。
				ftModule.requestFile(selected.owner, selected.fileID, selected.filename);
				//激活接收过程中的暂停和取消按钮
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, pauseReceiveButton));
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, cancelReceiveButton));
				//如果文件接收线程不busy，则执行接收过程的监控过程。
				//并非执行接受过程。这个监控过程会给Handler发信号来更新进度条
				if (monitorReceiveThread == null)
				{
					monitorReceiveProgress();
				}				
			}			
		};
		//显示文件选择对话框
		//这个函数在后面有实现
		showFilePickerDialog(filenames, onFileClicked);	
	}
	
	/*
	 * Start the thread responsible for monitoring receiving progress. The thread then signals the
	 * UI handler to update the receiving progress bar
	 * 开始负责监控接受过程的线程。这个线程会给UI handler发信号来更新进度条
	 */
	//监控接受过程
	private void monitorReceiveProgress()
	{
		//新建一个线程，Override Run方法
		monitorReceiveThread = new Thread(new Runnable()
		{
			public void run()
			{
				//get list of files being received
				//新建一个ProgressDescriptor数组
				ArrayList<ProgressDescriptor> receiveList = ftModule.getReceiveProgressList();
				
				while (receiveList.size() > 0)
				{
					//取出数组倒数第二个元素为descriptor
					ProgressDescriptor descriptor = receiveList.get(receiveList.size() - 1);
					//将descriptor转换为byte型除以总文件大小乘以100，转换为百分数
					int progress = (int) (((float)descriptor.bytesTransferred)/descriptor.fileSize * 100);
					
					//signal the UI handler to update the progress bar
					//将这个百分数发给UI handler来更新progress bar的进度条
					handler.sendMessage(handler.obtainMessage(UPDATE_RECEIVE_PROGRESS, progress));
					
					//sleep before checking progress again
					try
					{
						Thread.sleep(100);
					} catch (InterruptedException e) { }
					
					receiveList = ftModule.getReceiveProgressList();					
				}				
				
				//no more files being received - update progress bar to 100%
				//不再收任何文件，进度条变为100
				handler.sendMessage(handler.obtainMessage(UPDATE_RECEIVE_PROGRESS, 100));
				
				//disable pause and cancel buttons
				//禁用暂停和取消按钮
				handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, pauseReceiveButton));
				handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, cancelReceiveButton));
				//清空thread
				monitorReceiveThread = null;
			}				
		});
		//驱动线程
		monitorReceiveThread.start();
	}

	/*
	 * Start the thread responsible for monitoring sending progress. The thread then signals the
	 * UI handler to update the sending progress bar
	 * 开始负责监控发送过程的线程。这个线程会给UI handler发信号来更新进度条
	 */
	//监控发送过程
	private void monitorSendProgress()
	{
		monitorSendThread = new Thread(new Runnable()
		{
			public void run()
			{
				//get list of files being sent
				//和之前的一样。只有这里不一样
				ArrayList<ProgressDescriptor> sendList = ftModule.getSendingProgressList();
				
				while (sendList.size() > 0)
				{
					ProgressDescriptor descriptor = sendList.get(0);
					
					int progress = (int) (((float)descriptor.bytesTransferred)/descriptor.fileSize * 100);
					
					handler.sendMessage(handler.obtainMessage(UPDATE_SEND_PROGRESS, progress));
					
					try
					{
						Thread.sleep(100);
					} catch (InterruptedException e) { }
					
					sendList = ftModule.getSendingProgressList();					
				}				
				
				handler.sendMessage(handler.obtainMessage(UPDATE_SEND_PROGRESS, 100));
				handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, cancelSendButton));
				
				monitorSendThread = null;
			}				
		});
		monitorSendThread.start();
	}

	/*
	 * Pause the file currently being received	
	 * 貌似这个功能没有啊。。。。。。
	 * 又没有重启的功能   
	 */
	//点击暂停“接收”按钮后执行的内容
	private void onPauseReceiveButtonClicked()
	{
		ArrayList<ProgressDescriptor> receiveList = ftModule.getReceiveProgressList();
		
		ProgressDescriptor descriptor = receiveList.get(receiveList.size() - 1);
		//pauseFile内函数只需要填写fileID即可
		ftModule.pauseFile(descriptor.fileID);
		
		handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, pauseReceiveButton));
	}

	/*
	 * Cancel the file currently being received	   
	 */
	private void onCancelReceiveButtonClicked()
	{
		ArrayList<ProgressDescriptor> receiveList = ftModule.getReceiveProgressList();
		
		ProgressDescriptor descriptor = receiveList.get(receiveList.size() - 1);
		
		ftModule.cancelReceivingFile(descriptor.fileID);		
	}
	
	/*
	 * Pause the file currently being sent	   
	 */
	//点击暂停“发送”按钮后的效果
	private void onPauseSendButtonClicked()
	{
		ArrayList<ProgressDescriptor> sendList = ftModule.getSendingProgressList();
		
		ProgressDescriptor descriptor = sendList.get(sendList.size() - 1);
		
		ftModule.cancelSendingFile(descriptor.fileID);		
	}
	
	/*
	 * Offer a file. Launches the FileExplorer activity allowing the user to browse the file system.
	 * See the OFFER_SELECTED_FILE portion of the onActivityResult function
	 * 点击提供文件，会调用File Explorer来实现功能
	 */
	private void onOfferButtonClicked()
	{
		//弹出对话框，两个按钮。选图片还是选其他文件
		final AlertDialog.Builder builder = new AlertDialog.Builder(
                FastTrans.this);
		builder.setTitle("Type of File?");

		builder.setPositiveButton("Images",
                new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface

                            dialog, int which) {  
		                // TODO Auto-generated method stub 
						//想从主界面调用图片功能，就新建这个模块的Intent
						//并用startActivityForResult启动这个模块，结束后回到主界面
						//两个参数:intent和request code。request code是回传时的标记。
						Intent selectImageIntent = new Intent(FastTrans.this,TakePhoto.class);
						startActivityForResult(selectImageIntent,IMAGE_OFFER); 

						
		            }  
});
	

            
		builder.setNegativeButton("Other Files",
                new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface

                        dialog, int which) {                                                        	                        	
                        	Intent selectedFileIntent = new Intent( FastTrans.this, FileExplorer.class);	
                    		startActivityForResult(selectedFileIntent, OFFER_SELECTED_FILE); 
                        		
                        }
                });
		AlertDialog alert = builder.create();
		alert.show();
	
	}

	/*
	 * Request a file announcement by path. Allows a transfer to be started if the absolute path is known
	 * on the remote device (and their UnannouncedFileListener returns true). 
	 * 感觉这个功能没什么用！准备做了它~~~
	 */
	//查看我的文件
	private void OnReceivedFilesClicked()
	{
	
		Intent ReceivedFileIntent = new Intent(FastTrans.this,SDCardFileExplorer.class);
		ReceivedFileIntent.putExtra("request_text_for_main", "从main到sdCardactivity");
		startActivityForResult(ReceivedFileIntent,FIRST_REQUEST_CODE);
		
	}    	



	/* 
	 * Called when the FileExplorer exits. Retrieves the selected file
	 * and executes a command based on the requestCode
	 * 当File Explorer这个Activity结束后调用。回复被选中的文件并根据requestCode产生一个命令
	 */
	@Override  
	public void onActivityResult(int requestCode, int resultCode, final Intent intent)
	{		
		//the intent is null if the FileExplorer was exited 
		//without a selection being made
		//如果没有选中文件就关闭了FileExplorer的话，intent就为null
		if (intent == null)
		{
			return;
		}
		
		switch (requestCode)
		{
			case SHARE_SELECTED_FILE:
			{
				//retrieve selected file from FileExplorer intent
				//输入函数的intent所包含的额外信息。这个信息为一个名为file的对象。在后面的putExtras里给的。
				//这一部分是将不同activity之间的信息进行传递
				File selected = (File) intent.getExtras().get("file"); 
					
				//将file的绝对路径存在数组filePath中
				ArrayList<String> filePath = new ArrayList<String>();
				filePath.add(selected.getAbsolutePath());
					
				//announce selected file
				ftModule.announce(filePath);
				break;
			}
			
			//得到这种request code，就输出final_request的内容。
			//这种request是什么情况用？？
			case FIRST_REQUEST_CODE:
			{
				if( resultCode == Activity.RESULT_FIRST_USER){
					System.out.println(intent.getStringExtra("final_request"));
				}
				break;
			}
			//如果是分享图像的request code
			//从sd卡中获取相册照片
			case IMAGE_SHARE:
			{
				if (intent !=null){
				Uri uri = (Uri) intent.getExtras().get("imageUri");
				String[] proj = { MediaStore.Images.Media.DATA };
				Cursor actualimagecursor = managedQuery(uri, proj, null,
						null, null);
				int actual_image_column_index = actualimagecursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				actualimagecursor.moveToFirst();
				//最后根据索引值获取图片路径
				String img_path = actualimagecursor
						.getString(actual_image_column_index);
				File file = new File(img_path);
				
				ArrayList<String> filePath = new ArrayList<String>();
				filePath.add(file.getAbsolutePath());
					
				//announce selected file
				//IMAGE_SHARE是广播我要发送的图片地址。然后想收的都能收吧。
				ftModule.announce(filePath);
				}
				break;
				
			}
			case IMAGE_OFFER:
			{
														
					final String[] peers = ajManager.getPeers();
					
					//create the click listener - when a peer is selected, offer them the file
					DialogInterface.OnClickListener onPeerClicked = new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int which)
						{
							String peer = peers[which];
							
							Uri uri = (Uri) intent.getExtras().get("imageUri");
							String[] proj = { MediaStore.Images.Media.DATA };
							Cursor actualimagecursor = managedQuery(uri, proj, null,
									null, null);
							int actual_image_column_index = actualimagecursor
									.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
							actualimagecursor.moveToFirst();
							String img_path = actualimagecursor
									.getString(actual_image_column_index);
							File file = new File(img_path);
													
							ftModule.offerFileToPeer(peer, file.getAbsolutePath(), 1000);						
						}
					};
					//这个函数在后面有实现
					showPeerPickerDialog(onPeerClicked);
				break;
			}
			case OFFER_SELECTED_FILE:
			{							
				final String[] peers = ajManager.getPeers();
				
				//create the click listener - when a peer is selected, offer them the file
				DialogInterface.OnClickListener onPeerClicked = new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						String peer = peers[which];
						File file = (File) intent.getExtras().get("file"); 
						
						ftModule.offerFileToPeer(peer, file.getAbsolutePath(), 1000);						
					}
				};
				//这个函数在后面有实现
				showPeerPickerDialog(onPeerClicked);
				break;
			}
			
		}
	}
	
	/*
	 * Build the file list dialog and execute the clickListener when a file is selected	 
	 */
	private void showFilePickerDialog(String[] filenames, DialogInterface.OnClickListener clickListener)
	{
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose File");
		builder.setItems(filenames, clickListener);
		builder.create().show();
	}

	/*
	 * Build the peer list dialog and execute the clickListener when a file is selected	 
	 */
	public void showPeerPickerDialog(DialogInterface.OnClickListener clickListener)
	{
		final String[] peers = ajManager.getPeers();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose Member");
	    builder.setItems(peers, clickListener);
		builder.create().show();				
	}
	
	
	
	//设置会话的名称和密码
	@SuppressWarnings("static-access")
	public Dialog showSessionConfigDialog()
	{
		//send an INFO log message
		Log.i(TAG, "showSessionConfigDialog()");
		
    	final Dialog dialog = new Dialog(this);
    	//控制安卓窗体显示 启用窗体扩展特性
    	//参数含1.DEFAULT_FEATURES：系统默认状态，一般不需要指定
    	//2.FEATURE_CONTEXT_MENU：启用ContextMenu，默认该项已启用，一般无需指定
    	//3.FEATURE_CUSTOM_TITLE：自定义标题。当需要自定义标题时必须指定。如：标题是一个按钮时
    	//4.FEATURE_INDETERMINATE_PROGRESS：不确定的进度
    	//5.FEATURE_LEFT_ICON：标题栏左侧的图标
    	//6.FEATURE_NO_TITLE：吴标题
    	//7.FEATURE_OPTIONS_PANEL：启用“选项面板”功能，默认已启用。
    	//8.FEATURE_PROGRESS：进度指示器功能
    	//9.FEATURE_RIGHT_ICON:标题栏右侧的图标
    	dialog.requestWindowFeature(dialog.getWindow().FEATURE_NO_TITLE);
    	dialog.setContentView(R.layout.sessionconfigdialog);
    	//这里N和P是大写
        final EditText sessionName = (EditText)dialog.findViewById(R.id.sessionName);
        final EditText sessionPsw = (EditText)dialog.findViewById(R.id.sessionPsw);
        
        //给文本框添加编辑监听事件
        //新建一个textview下的OnEditorActionListener类的对象
        //这段并不是很明白？？？？
        //这里sessionname和sessionpsw都是小写
        sessionName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                	String sessionname = view.getText().toString();
    				//传递名称
    				CONFIG_FLAG_NAME = 1;
    				   				
    				
        			//dialog.cancel();
                }
                return true;
            }
        });
        
        sessionPsw.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                	String sessionpsw = view.getText().toString();
    				//传递密码
                	CONFIG_FLAG_PASSWORD = 1;
                	
                			
        			
    				
    				
    				//dialog.cancel();
                }
                return true;
            }
        });
        
        Button okay = (Button)dialog.findViewById(R.id.configOK);
        okay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	if(CONFIG_FLAG_NAME==1 && CONFIG_FLAG_PASSWORD == 1) {
            		//获得sessionName和sessionPsw的文本内容并转换为字符串
            		String sessionname = sessionName.getText().toString();
            		String sessionpsw = sessionPsw.getText().toString();
				
    			dialog.cancel();
            	}
            }
        });
        
        Button cancel = (Button)dialog.findViewById(R.id.configCancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        
     return dialog;
    		 
	}
	
	

	
	
	/*
	 * Share a file. Launches the FileExplorer activity allowing the user to browse the file system.
	 * See the SHARE_SELECTED_FILE portion of the onActivityResult function
	 * 分享文件，使用FileExplorer activity来允许用户浏览文件系统
	 * 依旧是验证加密的过程，需要自己修改
	 */
	private void onShareButtonClicked()
	{
		//跳转到弹出选择图片或者文件的对话框的activity

		final AlertDialog.Builder builder = new AlertDialog.Builder(
                FastTrans.this);
builder.setTitle("Type of File?");

builder.setPositiveButton("Images",
                new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface

                            dialog, int which) {  
		                // TODO Auto-generated method stub 
						
						Intent selectImageIntent = new Intent(FastTrans.this,TakePhoto.class);
						startActivityForResult(selectImageIntent,IMAGE_SHARE); 

						
		            }  
});
	

            
builder.setNegativeButton("Other Files",
                new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface

                        dialog, int which) {
                                
                        	
                        	
                        	Intent selectedFileIntent = new Intent( FastTrans.this, FileExplorer.class);	
                    		startActivityForResult(selectedFileIntent, SHARE_SELECTED_FILE); 
                        		
                        }
                });
AlertDialog alert = builder.create();
alert.show();
	

	}

	
	
	
	@Override
	//执行返回功能或者点击返回按钮后
    public void onBackPressed() 
    {
	    if (ftModule != null)
	    {
	    	ftModule.destroy();
	    }
    	ajManager.disconnect();  	
    	super.onBackPressed();
    }	
}
