package com.izambasiron.free.linktofile;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.android.actionbarcompat.ActionBarActivity;
import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdRequest.ErrorCode;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class LinkToFileActivity extends ActionBarActivity implements OnItemClickListener, 
OnItemLongClickListener, AdListener {
	private static final String TAG = "LinkToFileActivity";
	private static final int PICK_A_FILE = 1;
	private int adSizeId = 0;
	private final AdSize[] adSize = new AdSize[] {AdSize.IAB_LEADERBOARD, AdSize.IAB_BANNER, AdSize.BANNER};
	private AdView mAdView;
	
	private FilesAdapter filesAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        filesAdapter = new FilesAdapter(this);

        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(filesAdapter);
        gridview.setOnItemClickListener(this);
        gridview.setOnItemLongClickListener(this);
        
        // Decide which type of ad to display
        LinearLayout layout = (LinearLayout)findViewById(R.id.main);
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = (int) (metrics.widthPixels / metrics.density);
        
        createAdView(getAdSize(width-layout.getPaddingLeft()-layout.getPaddingRight()));
    }
    
    private void createAdView(AdSize adSize) {
    	if (mAdView != null) 
    		mAdView.destroy();
    	
    	mAdView = new AdView(this, 
        		adSize, 
        		getResources().getString(R.string.admob_id));
		mAdView.setAdListener(this);
	    mAdView.loadAd(getAdRequest());
	    mAdView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
	    
	    LinearLayout layout = (LinearLayout)findViewById(R.id.main);
	    layout.addView(mAdView);
    }
    
    private AdSize getAdSize(int width) {
    	Log.d(TAG, "width - " + width);
		if (width >= 728) {
			adSizeId = 0;
			return AdSize.IAB_LEADERBOARD;
		} else if (width >= 468) {
			adSizeId = 1;
			return AdSize.IAB_BANNER;
		} else {
			adSizeId = 2;
			return AdSize.BANNER;
		}
	}

	private AdRequest getAdRequest() {
    	AdRequest adRequest = new AdRequest();
        Set<String> set = new HashSet<String>();
        set.add("Video");
        set.add("Audio");
        set.add("Image");
        set.add("Document");
        set.add("Multimedia");
        set.add("Download");
        adRequest.setKeywords(set);
        adRequest.addTestDevice(AdRequest.TEST_EMULATOR);
        adRequest.addTestDevice("CDC7E419D08AB3C1FDC54A441E862DB6"); // My test phone
        adRequest.addTestDevice("E3954F9764F6B841A2C0E66E6BE8B8AE"); // My test tablet
        return adRequest;
	}

	@Override
    protected void onDestroy() {
    	filesAdapter.close();
    	super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);

        // Calling super after populating the menu is necessary here to ensure that the
        // action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //Toast.makeText(this, "Tapped home", Toast.LENGTH_SHORT).show();
                break;

            case R.id.menu_refresh:
                Toast.makeText(this, "Fake refreshing...", Toast.LENGTH_SHORT).show();
                getActionBarHelper().setRefreshActionItemState(true);
                getWindow().getDecorView().postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                getActionBarHelper().setRefreshActionItemState(false);
                            }
                        }, 1000);
                break;

            case R.id.menu_add:
            	// show: pick a file
            	Intent action = new Intent(Intent.ACTION_GET_CONTENT);  
            	action = action.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
            	action = action.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            	startActivityForResult(Intent.createChooser(action, "Add new file"), PICK_A_FILE);
            	
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
    	if (data != null) {
	    	switch (requestCode) {
		    	case PICK_A_FILE:
		    		new DisplayAddNewDialogTask().execute(data);
		    		break;
	    	}
	    	Log.d(TAG, data.getDataString());
    	}
    }
    
	private class DisplayAddNewDialogTask extends AsyncTask<Intent, Void, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			if (!result) {
				Log.d(TAG, "Failed to resolve file type");
				Toast.makeText(getApplicationContext(), "Failed to resolve file type", 3000).show();
			}
		}
		
		@Override
		protected Boolean doInBackground(Intent... arg0) {
			
			// Resolve file path
			Intent intent = arg0[0];
			Uri uri = intent.getData();
			String scheme = uri.getScheme();
			String path = null;
			String fileName;
			if (scheme.equals("file")) {
				path = uri.getPath();
				fileName = uri.getLastPathSegment();
			} else if(scheme.equals("content")) {
				path = getRealPathFromURI(uri);
				fileName = path.substring(path.lastIndexOf("/") + 1);
			} else {
				return false;
			}
			
			String mime = intent.resolveType(getApplicationContext());
			if (mime == null) {
				// try something else
				String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
				mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
			}
			
			if (mime == null) {
				// Give up
				return false;
			}
			
			Log.d(TAG, path + " --- " + fileName);
			
		    // DialogFragment.show() will take care of adding the fragment
		    // in a transaction.  We also want to remove any currently showing
		    // dialog, so make our own transaction and take care of that here.
			removeCreateNewDialog();
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		    // Create and show the dialog.
		    DialogFragment newFragment = CreateFileDialogFragment.newInstance("Add file", 
		    		fileName, path,
		    		mime);
		    newFragment.show(ft, "dialog");
			return true;
		}
    }
    
	public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.MediaColumns.DATA };
        //Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        //Cursor cursor = (Cursor) new CursorLoader(this, contentUri,
        //		proj, null, null, null);
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
	
	public void doPositiveClick(Bundle args) {
	    String name = args.getString("name");
	    String path = args.getString("path");
	    int icon = args.getInt("icon");
	    String mime = args.getString("mime");
	    Boolean addShortcut = args.getBoolean("addShortcut");
		Boolean added = filesAdapter.addItem(name, path, icon, mime);
		if (added) {
			if (addShortcut) {
				makeShortcutForFile(name, path, icon, mime);
			}
		} else {
			Toast.makeText(this, "Failed to add \"" + name + "\"", Toast.LENGTH_SHORT).show();
		}
		
		removeCreateNewDialog();
	}

	private void makeShortcutForFile(String name, String path, int icon,
			String mime) {
		// TODO: move to file adapter
		Uri uri = Uri.fromFile(new File(path));
		final Intent shortcutIntent = new Intent(Intent.ACTION_VIEW);
		shortcutIntent.setDataAndType(uri, mime);
		
		final Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		// Sets the custom shortcut's title
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
		// Set the custom shortcut icon
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, icon));
		// add the shortcut
		intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		sendBroadcast(intent);
	}

	public void doNegativeClick() {
		removeCreateNewDialog();
	}

	private void removeCreateNewDialog() {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		String path = ((FilesAdapter.FileItem) filesAdapter.getItem(position)).PATH;
		String mime = ((FilesAdapter.FileItem) filesAdapter.getItem(position)).MIME;
		Uri uri = Uri.fromFile(new File(path));
		
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(uri, mime);
		
		Log.d(TAG, mime + " " + path + " " + uri);
		try {
			startActivity(intent);
		} catch( ActivityNotFoundException e ) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		// Show remove dialog
		final int pos = position;
		String name = ((FilesAdapter.FileItem) filesAdapter.getItem(position)).NAME;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to delete '" + name + "'?")
		       .setCancelable(true)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                // Remove from db
		        	   filesAdapter.removeItem(pos);
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
		return false;
	}

	@Override
	public void onDismissScreen(Ad arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFailedToReceiveAd(Ad ad, ErrorCode code) {
		Log.d(TAG, "Error code - " + code + adSizeId);
		if (adSizeId < adSize.length) {
			createAdView(adSize[adSizeId]);
		    adSizeId++;
		}
	}

	@Override
	public void onLeaveApplication(Ad arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPresentScreen(Ad arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReceiveAd(Ad ad) {
	}
}