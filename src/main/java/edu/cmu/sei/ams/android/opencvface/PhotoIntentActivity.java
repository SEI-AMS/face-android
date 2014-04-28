/**
 * See : <http://developer.android.com/training/camera/cameradirect.html>
 */
package edu.cmu.sei.ams.android.opencvface;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;
import edu.cmu.sei.ams.cloudlet.Cloudlet;
import edu.cmu.sei.ams.cloudlet.ServiceVM;
import edu.cmu.sei.ams.cloudlet.android.CloudletCallback;
import edu.cmu.sei.ams.cloudlet.android.FindCloudletByRankAsyncTask;
import edu.cmu.sei.ams.cloudlet.android.StartServiceAsyncTask;
import edu.cmu.sei.ams.cloudlet.rank.CpuBasedRanker;


public class PhotoIntentActivity extends Activity {

	private static final int ACTION_TAKE_PHOTO_S = 2;
	
	private static final String BITMAP_STORAGE_KEY = "viewbitmap";
	private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
	private ImageView mImageView;
	private Bitmap mImageBitmap;
	
	private TextView mTextView;
	private String mText;
	private static final String TEXTVIEW_VISIBILITY_STORAGE_KEY = "textviewvisibility";
	
	private String mCurrentPhotoPath;

	private static final String JPEG_FILE_PREFIX = "IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";

	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
	private ClientThread clientTalker = null;

	private String ipAddress;
	private int portNumber;
	
	public static final String INTENT_EXTRA_APP_SERVER_IP_ADDRESS = "edu.cmu.sei.cloudlet.appServerIp";
	public static final String INTENT_EXTRA_APP_SERVER_PORT ="edu.cmu.sei.cloudlet.appServerPort";	
	
	/* Photo album for this application */
	private String getAlbumName() {
		return getString(R.string.album_name);
	}

	
	private File getAlbumDir() {
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			
			storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());

			if (storageDir != null) {
				if (! storageDir.mkdirs()) {
					if (! storageDir.exists()) {
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}
			
		} else {
			Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
		}
		
		return storageDir;
	}

	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
//		File albumF = getAlbumDir();
		File albumF = new File("storage/sdcard0/cloudlet/faces/");
//		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
		File imageF = new File(albumF, imageFileName+JPEG_FILE_SUFFIX);
		
		return imageF;
	}

	private File setUpPhotoFile() throws IOException {
		
		File f = createImageFile();
		mCurrentPhotoPath = f.getAbsolutePath();
		Log.v("Photo", "Current path: " + mCurrentPhotoPath);
		return f;
	}

	private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
		int targetW = mImageView.getWidth();
		int targetH = mImageView.getHeight();

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;
		
		/* Figure out which way needs to be reduced less */
		int scaleFactor = 1;
		if ((targetW > 0) || (targetH > 0)) {
			scaleFactor = Math.min(photoW/targetW, photoH/targetH);	
		}

		/* Set bitmap options to scale the image decode target */
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		
		/* Associate the Bitmap to the ImageView */
		mImageView.setImageBitmap(bitmap);
		mTextView.setText(mText);
		mTextView.setVisibility(View.VISIBLE);
		mImageView.setVisibility(View.VISIBLE);
	}

	private void galleryAddPic() {
	    Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		File f = new File(mCurrentPhotoPath);
	    Uri contentUri = Uri.fromFile(f);
	    mediaScanIntent.setData(contentUri);
	    this.sendBroadcast(mediaScanIntent);
	}

	private void dispatchTakePictureIntent(int actionCode) {

		Log.v("Photo", "dispatchTakePictureIntent");
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		startActivityForResult(takePictureIntent, actionCode);
	}

	private void handleSmallCameraPhoto(Intent intent) {

		Log.v("Photo", "handleSmallCameraPhoto");
		
		Bundle extras = intent.getExtras();
		mImageBitmap = (Bitmap) extras.get("data");
		mImageView.setImageBitmap(mImageBitmap);

		mImageView.setVisibility(View.VISIBLE);
		
		sendImage(mImageBitmap);
		Log.v("Photo", "sent image");
		
		mTextView.setText(mText);
		mTextView.setVisibility(View.VISIBLE);
	}

	private static byte[] codec(Bitmap src, Bitmap.CompressFormat format, int quality) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		src.compress(format, quality, os);
 
		byte[] array = os.toByteArray();
		return array;
	}
	
	private void sendImage(Bitmap bitmap) {
		
		
		Log.v("Photo", "sendImage");
	    String sentence;
	    String modifiedSentence;
	    Socket clientSocket;
		
	    try {
	    	// Open the socket.
	    	Log.v("Photo", "Connecting to " + ipAddress + ":" + portNumber);
			clientSocket = new Socket(ipAddress, portNumber);
		    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		    // Read the handshake message.
		    modifiedSentence = inFromServer.readLine();
	    	Log.v("Photo", "FROM SERVER 1 : " + modifiedSentence);
		    
	    	// Encode he image into bytes.
	    	byte[] imgBytes = codec(bitmap, Bitmap.CompressFormat.JPEG, 85);
	    	
	    	// Write the image.
		    outToServer.writeInt(imgBytes.length);
		    outToServer.write(imgBytes);
		    
		    // Read the name.
		    modifiedSentence = inFromServer.readLine();
	    	Log.v("Photo", "FROM SERVER 2 : " + modifiedSentence);
	    	mText = modifiedSentence;
	    	
		    outToServer.flush();
		    outToServer.close();
		    clientSocket.close();

	    } catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	Button.OnClickListener mTakePicSOnClickListener = 
		new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_S);
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mTextView = (TextView) findViewById(R.id.textView1);
		mImageView = (ImageView) findViewById(R.id.imageView1);
		mImageBitmap = null;

		Button picSBtn = (Button) findViewById(R.id.btnIntendS);
		setBtnListenerOrDisable( 
				picSBtn, 
				mTakePicSOnClickListener,
				MediaStore.ACTION_IMAGE_CAPTURE
		);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 


		if (storeServerConnectionInfo(getIntent()))
        {
            loadPreferences();
        }
        else
        {
            new FindCloudletByRankAsyncTask(this, "edu.cmu.sei.ams.face_rec_service_opencv", new CpuBasedRanker(), new CloudletCallback<Cloudlet>()
            {
                @Override
                public void handle(Cloudlet result)
                {
                    try
                    {
                        new StartServiceAsyncTask(result.getServiceById("edu.cmu.sei.ams.face_rec_service_opencv"), PhotoIntentActivity.this, new CloudletCallback<ServiceVM>()
                        {
                            @Override
                            public void handle(ServiceVM result)
                            {
                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PhotoIntentActivity.this);
                                SharedPreferences.Editor prefsEditor = prefs.edit();
                                prefsEditor.putString(PhotoIntentActivity.this.getString(R.string.pref_ipaddress), result.getAddress().getHostAddress());
                                prefsEditor.putString(PhotoIntentActivity.this.getString(R.string.pref_portnumber), "" + result.getPort());
                                prefsEditor.commit();
                                loadPreferences();
                            }
                        }).execute();
                    }
                    catch (Exception e)
                    {
                        Log.v("FACE", "Error Starting Service", e);
                    }
                }
            }).execute();
        }
		//loadPreferences();
	}
	
	public void loadPreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
		this.ipAddress = prefs.getString(getString(R.string.pref_ipaddress), getString(R.string.default_ipaddress));
		this.portNumber = Integer.parseInt( prefs.getString(getString( R.string.pref_portnumber), getString(R.string.default_portnumber)) );
	}
	
	private boolean storeServerConnectionInfo(Intent intent)
	{
		Log.v("Photo", "storeServerConnectionInfo");
		if(intent == null)
			return false;
		
		Bundle extras = intent.getExtras();

        if(extras != null)
        {
        	// Get the values from the intent.
        	String serverIPAddress = extras.getString(INTENT_EXTRA_APP_SERVER_IP_ADDRESS);
        	int cloudletPort = extras.getInt(INTENT_EXTRA_APP_SERVER_PORT); 
        	
        	// Only store the IP and port in the preferences file if they are valid.
        	boolean validExtras = serverIPAddress != null && cloudletPort != 0;
        	if(validExtras)
        	{
        		Log.v("Photo", "IP:   " + serverIPAddress);
        		Log.v("Photo", "Port: " + cloudletPort);
        		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        		SharedPreferences.Editor prefsEditor = prefs.edit();
        		prefsEditor.putString(getString(R.string.pref_ipaddress), serverIPAddress);
        		prefsEditor.putString(getString(R.string.pref_portnumber), String.valueOf(cloudletPort));
        		prefsEditor.commit();
        		
            	// Remove the extras so that they won't be used again each time we jump back to this activity.
            	intent.removeExtra(INTENT_EXTRA_APP_SERVER_IP_ADDRESS);
            	intent.removeExtra(INTENT_EXTRA_APP_SERVER_PORT);
                return true;
        	}        	
        }
        else
        {
			System.err.println("Server IP address and Port not received from Intent. Values from preferences will not be changed.");
        }
        return false;
	}	

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {

		case ACTION_TAKE_PHOTO_S: {
			if (resultCode == RESULT_OK) {
				handleSmallCameraPhoto(data);
			}
			break;
		} // ACTION_TAKE_PHOTO_S

		} // switch
	}

	// Some lifecycle callbacks so that the image can survive orientation change
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
		outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null) );
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
		mImageView.setImageBitmap(mImageBitmap);
		mImageView.setVisibility(
				savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ? 
						ImageView.VISIBLE : ImageView.INVISIBLE
		);
	}

	/**
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 *
	 * @param context The application's environment.
	 * @param action The Intent action to check for availability.
	 *
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
			packageManager.queryIntentActivities(intent,
					PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	private void setBtnListenerOrDisable( 
			Button btn, 
			Button.OnClickListener onClickListener,
			String intentName
	) {
		if (isIntentAvailable(this, intentName)) {
			btn.setOnClickListener(onClickListener);        	
		} else {
			btn.setText( 
				getText(R.string.cannot).toString() + " " + btn.getText());
			btn.setClickable(false);
		}
	}

}