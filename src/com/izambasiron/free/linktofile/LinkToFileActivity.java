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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.actionbarcompat.ActionBarActivity;
import com.google.ads.AdRequest;
import com.google.ads.AdView;

public class LinkToFileActivity extends ActionBarActivity implements OnItemClickListener, OnItemLongClickListener {
	private static final String TAG = "LinkToFileActivity";
	private static final int PICK_A_FILE = 1;
	
	private FilesAdapter filesAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        filesAdapter = new FilesAdapter(this);

        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(filesAdapter);
        gridview.setOnItemClickListener(this);
        gridview.setOnItemLongClickListener(this);
        
        AdView adView = (AdView) findViewById(R.id.ad);
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
        adRequest.addTestDevice("3832CBF156DF00EC"); // My test phone
        adView.loadAd(adRequest);
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
    
	private class DisplayAddNewDialogTask extends AsyncTask<Intent, Void, Void> {

		@Override
		protected Void doInBackground(Intent... arg0) {
			showCreateNewDialog(arg0[0]);
			return null;
		}
    }
    
	private void showCreateNewDialog(Intent intent) {

		// Resolve file path
		Uri uri = intent.getData();
		String scheme = uri.getScheme();
		String path;
		String fileName;
		if (scheme.equals("file")) {
			path = uri.getPath();
			fileName = uri.getLastPathSegment();
		} else if(scheme.equals("content")) {
			path = getRealPathFromURI(uri);
			fileName = path.substring(path.lastIndexOf("/") + 1);
		} else {
			Log.d(TAG, "Failed to resolve file path");
			Toast.makeText(this, "Failed to resolve file path", 3000);
			return;
		}
		
		String mime = intent.resolveType(this);
		if (mime == null) {
			// try something else
			String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
			mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
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
}