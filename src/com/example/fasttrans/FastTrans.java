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
    //�Ҷ����
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
					//��ʾToast��Ϣ������һ����ǰ�����Ļ���������getApplicationConteext()
					//��������Ҫ��ʾ���ַ���
					//����������ʾ����ʱ���������֣�LENGTH_SHORT��ʾ����ʱ��̡�LENGTH_LONG��ʾ����ʱ�䳤
					Toast toast = Toast.makeText(getApplicationContext(), (String) message.obj, Toast.LENGTH_SHORT);
					//��ʾTOAST��Ϣ
					toast.show();
					break;
					//����obj����Ӧ���Ǹ�����������һ���ǰ�ť���Ƿ����á�
					//����ť�Ƿ��ǻ�ť�������ǿ��԰��İ�ť
				case ENABLE_VIEW:
					View viewToEnable = (View) message.obj;
					viewToEnable.setEnabled(true);
					break;
					//����obj����Ӧ���Ǹ�����������һ���ǰ�ť���Ƿ����á�
					//����ť�Ƿ��ǻ�ť�������ǿ��԰��İ�ť
				case DISABLE_VIEW:
					View viewToDisable = (View) message.obj;
					viewToDisable.setEnabled(false);
					break;	
					//���������գ�
				case UPDATE_RECEIVE_PROGRESS:
					ProgressBar receiveProgressBar = (ProgressBar) findViewById(R.id.receiveProgressBar);	
					//���ý�����ɰٷֱ�
					receiveProgressBar.setProgress( (Integer) message.obj);
					break;
					//������������
				case UPDATE_SEND_PROGRESS:
					ProgressBar sendProgressBar = (ProgressBar) findViewById(R.id.sendProgressBar);	
					//���ý�����ɰٷֱ�
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
     * ��ʼ��ͼ�ν��棬�����˸��ְ�ť
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
	 * ��ʼ���ļ�����ģ�ͺ����ļ�����
	 */
	private void initializeFileTransferModule()
	{
		//Construct the module using the Bus Attachment and Session Id created by the AllJoyn Manager
		//����AllJoyn Manager������Bus Attachment��Session Id������file transfer module
		//�ļ������Component(���߸������ỰID)
		ftModule = new FileTransferComponent(ajManager.getBusAttachment(), ajManager.getSessionId());
		
		//Register announcement listener - create a toast when a file announcement is received
		//ע��annoucement listener  ���յ�file announcementʱ����toast
		ftModule.setFileAnnouncementReceivedListener(new FileAnnouncementReceivedListener()
		{
			//???������ʲô��˼����ΪfileIdResponse����ֵΪtrue��false����ʲô����
			public void receivedAnnouncement(FileDescriptor[] fileList, boolean isFileIdResponse)
			{		
				if (!isFileIdResponse)
				{
					//����Message��������Toast�������ǡ���С��鷢���ļ�����
					//obtainMessage������������һ��������what���������ֵ������return message�е�
					//what field��ʲô������������toast���ڶ���ֵ��Obj��������return message��obj��ֵ��ʲô����toastҪ��ʾʲô��
					handler.sendMessage(handler.obtainMessage(TOAST, "File accessable..."));
				}
				else
				{
					handler.sendMessage(handler.obtainMessage(TOAST, "File ID Response Announcement Received!"));
				}
			}			
		});
		
		//Register file completed listener - disable buttons if the completion was triggered by a cancel
		//���洢�ļ���ɡ��ļ������� ״̬��Ϊcancelledʱ������ص���ͣ���պ�ȡ�����հ�ť��������Ǵ������
		ftModule.setFileCompletedListener(new FileCompletedListener()
		{
			public void fileCompleted(String filename, int statusCode)
			{
				if (statusCode == StatusCode.CANCELLED)
				{
					//��ǰ���switch�У���������what����ΪDISABLE_VIEW����setEnable=false
					//Ҳ����˵�����״̬����CANCELLED����ôpauseReceivedButton��CancelRecievedButton
					//���ǻ�ť
					handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, pauseReceiveButton));
					handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, cancelReceiveButton));
				}
				//�ļ�������ɣ���ʾtoast��ʾ��
				handler.sendMessage(handler.obtainMessage(TOAST, "Transfer finished�� " + filename));				
			}			
		});
		
		//Register offer file listener - always accept offers (return true), and monitor the receiving progress
		//ע��offer file listener ���ǽ����ṩ���ļ������Ҽ�ؽ��յĹ��̣�������ͣ���պ�ȡ������
		
		//�˴�Ӧ��Ҫ�޸ģ�������֤����
		//�������ǽ��ܹ���
		ftModule.setOfferReceivedListener(new OfferReceivedListener()
		{			
			public boolean acceptOfferedFile(FileDescriptor file, String peer)
			{	//����߳�û��busy����ִ��ReceiveProgress�ļ�ع���
				//�����ع��̻��UI Handler���ź������½�����
				if (monitorReceiveThread == null)
				{
					monitorReceiveProgress();
				}				
				//����������ť��
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, pauseReceiveButton));
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, cancelReceiveButton));
				return true;
			}
		});
		
		
		//Register unannounced File Listener - always allow files to be requested by path, 
		//even if they aren't shared. Required for use of requestFileId feature
		//���������ļ������󣬼�ʹ����û�б�����
		
		//Ĭ���ǲ�����ģ���Ҫ�������������������������������ļ�
		ftModule.setUnannouncedFileRequestListener(new UnannouncedFileRequestListener()
		{			
			public boolean allowUnannouncedFileRequests(String filePath)
			{
				return true;
			}
		});
		
		//register request data listener - monitor sending progress when a file is requested
		//�������ݼ�����   ���ļ���������ط��͵Ĺ���
		//�������Ƿ��͹���
		ftModule.setRequestDataReceivedListener(new RequestDataReceivedListener()
		{
			public void fileRequestReceived(String fileName)
			{
				//����͹��̵�ȡ����ť
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
	 * һ��AllJoyn���ӱ������������û����棬
	 * ��AllJoyn Manager����
	 */
	
	//�������������״̬
	public void ConnectionChanged(ConnectionState connectionState)
	{
		if (connectionState == ConnectionState.CONNECTED)
		{
			handler.sendMessage(handler.obtainMessage(TOAST, "Successfully Connected��"));
			
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
	 * ��ʼ���ֲ��㲥һ��AllJoyn�ļ�����Ự
	 */
	private void onHostButtonClicked()
	{
		hostButton.setEnabled(false);
		joinButton.setEnabled(true);
		
		
		//�����Ի��������û����ûỰ�����ƺ�����
		hostButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	//��������ں�����ʵ�֡��������Ự��������
                showSessionConfigDialog();
        	}
        });
		
		
		
		handler.sendMessage(handler.obtainMessage(TOAST, "Hosting Session"));	
		
		ajManager.createSession();
	}
	
	/*
	 * Attempt to join an AllJoyn File Transfer Session. Another device must be hosting
	 * before a join will complete successfully
	 * ���Լ���Ự����ҪҪ���������豸�����˻Ự���ܳɹ�����
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
	//��Ҫֹͣ�����ļ�ʱ�������ťʱִ�е�����д������
	private void OnUnshareButtonClicked()
	{
		//get list of files currently being shared
		//�õ��������ڱ�������ļ��б�
		final ArrayList<FileDescriptor> announcedFiles = ftModule.getAnnouncedLocalFiles();
		
		//create an array of filenames to display to the user
		//����һ���ļ��������������û�չʾ
		String[] filenames = new String[announcedFiles.size()];		
		for (int i = 0; i < announcedFiles.size(); i++)
		{
			filenames[i] = announcedFiles.get(i).filename;
		}
		
		//create the click listener - when a file is selected, share it
		//���ļ���ѡ��ʱ���ͷ����������ע��ò�Ʋ��԰�������
		//DialogInterface.onClickListener��Ϊ�Ի���׼���ĵ��������
		DialogInterface.OnClickListener onFileClicked = new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				//which����ָ����ʲô��which��ʲô��
				//�����Ƿ�����һ���ļ�������Ƿ���һ���ļ��Ļ����Ϳ϶���Ҫ������ļ�
				FileDescriptor selected = announcedFiles.get(which);//���ص���which������
				
				ArrayList<String> path = new ArrayList<String>();
				//�½�һ���ļ�������ļ��ļ����õ����յ��ļ����ļ������ļ�·���õ���sharedPath����֪��ʲô·����
				//����path�ַ��������м������ļ��ľ���·����
				path.add(new File(selected.sharedPath, selected.filename).getAbsolutePath());				
				
				//stopAnnounce��ֹͣ�㲥���������Ϊʲô����Ҫ�ڵ���Ժ�ֹͣ�㲥�أ��������ʲô�أ�
				ftModule.stopAnnounce(path);
			}			
		};
		
		//show the file picker dialog
		//��ʾ�ļ�ѡ��Ի������������������������һ�����ļ������ڶ����ǵ����������
		//��������ں�����ʵ��
		showFilePickerDialog(filenames, onFileClicked);
	}

	/*
	 * Request a known file. Files must have been announced, or requested by file file path
	 * before they can be transfered.	 
	 * ����һ����֪���ļ������ļ����Ա�����֮ǰ���ļ������Ѿ���announced��������ͨ���ļ�·��������
	 */
	//��ȡ�ļ���ť�����ʱִ�е�����
	private void onRequestButtonClicked()
	{
		//get list of files available for transfer
		//FD���顣��������availableFiles
		final ArrayList<FileDescriptor> availableFiles = ftModule.getAvailableRemoteFiles();
		
		//create an array of filenames to display to the user
		//����һ���ļ��������������û�չʾ
		String[] filenames = new String[availableFiles.size()];		
		for (int i = 0; i < availableFiles.size(); i++)
		{
			filenames[i] = availableFiles.get(i).filename;
		}
		
		//create the click listener - when a file is selected, request it and monitor its
		//receiving progress
		//�����������������һ���ļ���ѡ�У����������Ҽ�����Ľ��ܹ���
		//�����Ի�����������������onFileClicked ���ܣ�
		DialogInterface.OnClickListener onFileClicked = new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				
				//��ȡwhich�������õ������ļ�����
				FileDescriptor selected = availableFiles.get(which);
				//requestFile������������������һ�����ַ���peer��������������ǲ��Ǵ�������ͨ��˫���е���һ���أ�
				//�ڶ���������byte���飬�������ļ�ID����ô�ļ�ID��ʲô������
				//�������������ļ�����
				ftModule.requestFile(selected.owner, selected.fileID, selected.filename);
				//������չ����е���ͣ��ȡ����ť
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, pauseReceiveButton));
				handler.sendMessage(handler.obtainMessage(ENABLE_VIEW, cancelReceiveButton));
				//����ļ������̲߳�busy����ִ�н��չ��̵ļ�ع��̡�
				//����ִ�н��ܹ��̡������ع��̻��Handler���ź������½�����
				if (monitorReceiveThread == null)
				{
					monitorReceiveProgress();
				}				
			}			
		};
		//��ʾ�ļ�ѡ��Ի���
		//��������ں�����ʵ��
		showFilePickerDialog(filenames, onFileClicked);	
	}
	
	/*
	 * Start the thread responsible for monitoring receiving progress. The thread then signals the
	 * UI handler to update the receiving progress bar
	 * ��ʼ�����ؽ��ܹ��̵��̡߳�����̻߳��UI handler���ź������½�����
	 */
	//��ؽ��ܹ���
	private void monitorReceiveProgress()
	{
		//�½�һ���̣߳�Override Run����
		monitorReceiveThread = new Thread(new Runnable()
		{
			public void run()
			{
				//get list of files being received
				//�½�һ��ProgressDescriptor����
				ArrayList<ProgressDescriptor> receiveList = ftModule.getReceiveProgressList();
				
				while (receiveList.size() > 0)
				{
					//ȡ�����鵹���ڶ���Ԫ��Ϊdescriptor
					ProgressDescriptor descriptor = receiveList.get(receiveList.size() - 1);
					//��descriptorת��Ϊbyte�ͳ������ļ���С����100��ת��Ϊ�ٷ���
					int progress = (int) (((float)descriptor.bytesTransferred)/descriptor.fileSize * 100);
					
					//signal the UI handler to update the progress bar
					//������ٷ�������UI handler������progress bar�Ľ�����
					handler.sendMessage(handler.obtainMessage(UPDATE_RECEIVE_PROGRESS, progress));
					
					//sleep before checking progress again
					try
					{
						Thread.sleep(100);
					} catch (InterruptedException e) { }
					
					receiveList = ftModule.getReceiveProgressList();					
				}				
				
				//no more files being received - update progress bar to 100%
				//�������κ��ļ�����������Ϊ100
				handler.sendMessage(handler.obtainMessage(UPDATE_RECEIVE_PROGRESS, 100));
				
				//disable pause and cancel buttons
				//������ͣ��ȡ����ť
				handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, pauseReceiveButton));
				handler.sendMessage(handler.obtainMessage(DISABLE_VIEW, cancelReceiveButton));
				//���thread
				monitorReceiveThread = null;
			}				
		});
		//�����߳�
		monitorReceiveThread.start();
	}

	/*
	 * Start the thread responsible for monitoring sending progress. The thread then signals the
	 * UI handler to update the sending progress bar
	 * ��ʼ�����ط��͹��̵��̡߳�����̻߳��UI handler���ź������½�����
	 */
	//��ط��͹���
	private void monitorSendProgress()
	{
		monitorSendThread = new Thread(new Runnable()
		{
			public void run()
			{
				//get list of files being sent
				//��֮ǰ��һ����ֻ�����ﲻһ��
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
	 * ò���������û�а�������������
	 * ��û�������Ĺ���   
	 */
	//�����ͣ�����ա���ť��ִ�е�����
	private void onPauseReceiveButtonClicked()
	{
		ArrayList<ProgressDescriptor> receiveList = ftModule.getReceiveProgressList();
		
		ProgressDescriptor descriptor = receiveList.get(receiveList.size() - 1);
		//pauseFile�ں���ֻ��Ҫ��дfileID����
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
	//�����ͣ�����͡���ť���Ч��
	private void onPauseSendButtonClicked()
	{
		ArrayList<ProgressDescriptor> sendList = ftModule.getSendingProgressList();
		
		ProgressDescriptor descriptor = sendList.get(sendList.size() - 1);
		
		ftModule.cancelSendingFile(descriptor.fileID);		
	}
	
	/*
	 * Offer a file. Launches the FileExplorer activity allowing the user to browse the file system.
	 * See the OFFER_SELECTED_FILE portion of the onActivityResult function
	 * ����ṩ�ļ��������File Explorer��ʵ�ֹ���
	 */
	private void onOfferButtonClicked()
	{
		//�����Ի���������ť��ѡͼƬ����ѡ�����ļ�
		final AlertDialog.Builder builder = new AlertDialog.Builder(
                FastTrans.this);
		builder.setTitle("Type of File?");

		builder.setPositiveButton("Images",
                new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface

                            dialog, int which) {  
		                // TODO Auto-generated method stub 
						//������������ͼƬ���ܣ����½����ģ���Intent
						//����startActivityForResult�������ģ�飬������ص�������
						//��������:intent��request code��request code�ǻش�ʱ�ı�ǡ�
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
	 * �о��������ûʲô�ã�׼��������~~~
	 */
	//�鿴�ҵ��ļ�
	private void OnReceivedFilesClicked()
	{
	
		Intent ReceivedFileIntent = new Intent(FastTrans.this,SDCardFileExplorer.class);
		ReceivedFileIntent.putExtra("request_text_for_main", "��main��sdCardactivity");
		startActivityForResult(ReceivedFileIntent,FIRST_REQUEST_CODE);
		
	}    	



	/* 
	 * Called when the FileExplorer exits. Retrieves the selected file
	 * and executes a command based on the requestCode
	 * ��File Explorer���Activity��������á��ظ���ѡ�е��ļ�������requestCode����һ������
	 */
	@Override  
	public void onActivityResult(int requestCode, int resultCode, final Intent intent)
	{		
		//the intent is null if the FileExplorer was exited 
		//without a selection being made
		//���û��ѡ���ļ��͹ر���FileExplorer�Ļ���intent��Ϊnull
		if (intent == null)
		{
			return;
		}
		
		switch (requestCode)
		{
			case SHARE_SELECTED_FILE:
			{
				//retrieve selected file from FileExplorer intent
				//���뺯����intent�������Ķ�����Ϣ�������ϢΪһ����Ϊfile�Ķ����ں����putExtras����ġ�
				//��һ�����ǽ���ͬactivity֮�����Ϣ���д���
				File selected = (File) intent.getExtras().get("file"); 
					
				//��file�ľ���·����������filePath��
				ArrayList<String> filePath = new ArrayList<String>();
				filePath.add(selected.getAbsolutePath());
					
				//announce selected file
				ftModule.announce(filePath);
				break;
			}
			
			//�õ�����request code�������final_request�����ݡ�
			//����request��ʲô����ã���
			case FIRST_REQUEST_CODE:
			{
				if( resultCode == Activity.RESULT_FIRST_USER){
					System.out.println(intent.getStringExtra("final_request"));
				}
				break;
			}
			//����Ƿ���ͼ���request code
			//��sd���л�ȡ�����Ƭ
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
				//����������ֵ��ȡͼƬ·��
				String img_path = actualimagecursor
						.getString(actual_image_column_index);
				File file = new File(img_path);
				
				ArrayList<String> filePath = new ArrayList<String>();
				filePath.add(file.getAbsolutePath());
					
				//announce selected file
				//IMAGE_SHARE�ǹ㲥��Ҫ���͵�ͼƬ��ַ��Ȼ�����յĶ����հɡ�
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
					//��������ں�����ʵ��
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
				//��������ں�����ʵ��
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
	
	
	
	//���ûỰ�����ƺ�����
	@SuppressWarnings("static-access")
	public Dialog showSessionConfigDialog()
	{
		//send an INFO log message
		Log.i(TAG, "showSessionConfigDialog()");
		
    	final Dialog dialog = new Dialog(this);
    	//���ư�׿������ʾ ���ô�����չ����
    	//������1.DEFAULT_FEATURES��ϵͳĬ��״̬��һ�㲻��Ҫָ��
    	//2.FEATURE_CONTEXT_MENU������ContextMenu��Ĭ�ϸ��������ã�һ������ָ��
    	//3.FEATURE_CUSTOM_TITLE���Զ�����⡣����Ҫ�Զ������ʱ����ָ�����磺������һ����ťʱ
    	//4.FEATURE_INDETERMINATE_PROGRESS����ȷ���Ľ���
    	//5.FEATURE_LEFT_ICON������������ͼ��
    	//6.FEATURE_NO_TITLE�������
    	//7.FEATURE_OPTIONS_PANEL�����á�ѡ����塱���ܣ�Ĭ�������á�
    	//8.FEATURE_PROGRESS������ָʾ������
    	//9.FEATURE_RIGHT_ICON:�������Ҳ��ͼ��
    	dialog.requestWindowFeature(dialog.getWindow().FEATURE_NO_TITLE);
    	dialog.setContentView(R.layout.sessionconfigdialog);
    	//����N��P�Ǵ�д
        final EditText sessionName = (EditText)dialog.findViewById(R.id.sessionName);
        final EditText sessionPsw = (EditText)dialog.findViewById(R.id.sessionPsw);
        
        //���ı�����ӱ༭�����¼�
        //�½�һ��textview�µ�OnEditorActionListener��Ķ���
        //��β����Ǻ����ף�������
        //����sessionname��sessionpsw����Сд
        sessionName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                	String sessionname = view.getText().toString();
    				//��������
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
    				//��������
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
            		//���sessionName��sessionPsw���ı����ݲ�ת��Ϊ�ַ���
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
	 * �����ļ���ʹ��FileExplorer activity�������û�����ļ�ϵͳ
	 * ��������֤���ܵĹ��̣���Ҫ�Լ��޸�
	 */
	private void onShareButtonClicked()
	{
		//��ת������ѡ��ͼƬ�����ļ��ĶԻ����activity

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
	//ִ�з��ع��ܻ��ߵ�����ذ�ť��
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
