package com.izambasiron.free.linktofile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObservable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FilesAdapter extends BaseAdapter {
	private static final String TAG = "FilesAdapter";
	private List<FileItem> files;
	private final Context mCtx;
	
	private FilesDbAdapter dbAdapter;
	private LayoutInflater li;
	
	public FilesAdapter(Context ctx) {
		mCtx = ctx;
		
		files = new ArrayList<FileItem>();
		// Connect to db
		dbAdapter = new FilesDbAdapter(ctx);
		dbAdapter.open();
		
		Cursor filesCursor = dbAdapter.fetchAllFiles();
		for (boolean hasItem = filesCursor.moveToFirst(); hasItem; hasItem = filesCursor.moveToNext()) {
			//int pathColId = filesCursor.getColumnIndex(FilesDbAdapter.KEY_PATH);
			int nameColId = filesCursor.getColumnIndex(FilesDbAdapter.KEY_TITLE);
			int rowIdColId = filesCursor.getColumnIndex(FilesDbAdapter.KEY_ROWID);
			int iconColId = filesCursor.getColumnIndex(FilesDbAdapter.KEY_ICON);
			int pathColId = filesCursor.getColumnIndex(FilesDbAdapter.KEY_PATH);
			int mimeColId = filesCursor.getColumnIndex(FilesDbAdapter.KEY_MIME);
		    files.add(new FileItem(filesCursor.getInt(rowIdColId), filesCursor.getString(nameColId),
		    		filesCursor.getInt(iconColId), filesCursor.getString(pathColId), 
		    		filesCursor.getString(mimeColId), null));
		}
		
		li =  (LayoutInflater)mCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return files.size();
	}

	@Override
	public Object getItem(int position) {
		return files.get(position);
	}

	@Override
	public long getItemId(int position) {
		return files.get(position)._ID;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View itemView = convertView;
		if (convertView == null) {
			itemView = li.inflate(R.layout.grid_item, parent, false);
		}
		
		TextView text = (TextView)itemView.findViewById(R.id.grid_item_text);
		text.setText(files.get(position).NAME);
		ImageView icon = (ImageView)itemView.findViewById(R.id.grid_item_icon);
		icon.setImageResource(files.get(position).ICON);
		
		return itemView;
	}
	
	public Boolean addItem(String name, String path, int icon, String mime) {
		long rowId = dbAdapter.createFile(name, path, icon, mime);
		if (rowId > 0) {
			files.add(new FileItem(rowId, name, icon, path, mime, null));
			notifyDataSetChanged();
			return true;
		} else {
			//Toast.makeText(mCtx, "Failed to add " + name, Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Failed to add " + name);
			return false;
		}
	}
	
	public Boolean removeItem(int position) {
		FileItem file = files.get(position);
		if (dbAdapter.deleteFile(file._ID)) {
			removeShortcut(position);
			files.remove(position);
			notifyDataSetChanged();
			
			return true;
		}
		return false;
	}
	
	private void removeShortcut(int position) {
		FileItem file = files.get(position);
		Uri uri = Uri.fromFile(new File(file.PATH));
		final Intent shortcutIntent = new Intent(Intent.ACTION_VIEW);
		shortcutIntent.setDataAndType(uri, file.MIME);
		
		Intent removeIntent = new Intent();
		removeIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		removeIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, file.NAME);
		removeIntent.putExtra("duplicate", false);
		removeIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
		 
		mCtx.sendBroadcast(removeIntent);
	}

	public final class FileItem extends DataSetObservable{
		public long _ID;
		public String NAME;
		public int ICON;
		public String PATH;
		public String SHORTCUT;
		public String MIME;
		
		public FileItem(long id, String name, int icon, String path, String mime, String shortcut) {
			_ID = id;
			NAME = name;
			ICON = icon;
			PATH = path;
			SHORTCUT = shortcut;
			MIME = mime;
		}
		
		public FileItem(long id, String name, int icon) {
			_ID = id;
			NAME = name;
			ICON = icon;
		}
		
		public void setValues(String name, int icon, String path, String mime, String shortcut) {
			NAME = name;
			ICON = icon;
			PATH = path;
			SHORTCUT = shortcut;
			MIME = mime;
			notifyChanged();
		}
	}

	public void close() {
		dbAdapter.close();
	}
}
