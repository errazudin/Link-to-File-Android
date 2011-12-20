package com.izambasiron.free.linktofile;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class CreateFileDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
	private static final String TAG = "CreateFileDialogFragment";

	public static CreateFileDialogFragment newInstance(String dialogTitle, String fileName, 
			String path, String mime) {
		CreateFileDialogFragment frag = new CreateFileDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", dialogTitle);
        args.putString("path", path);
        args.putString("name", fileName);
        args.putString("mime", mime);
        frag.setArguments(args);
        return frag;
    }
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString("title");
        View view = createView(getActivity().getLayoutInflater(), (ViewGroup) getView());

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Add", this)
                .setNegativeButton("Cancel", this)
                .create();
    }
    
    public View createView(LayoutInflater inflater, ViewGroup container) {
    	String name = getArguments().getString("name");
    	String path = getArguments().getString("path");
    	String mime = getArguments().getString("mime");
		View layout = inflater.inflate(R.layout.create_link_dialog, container, true);
		
		EditText nameText = (EditText) layout.findViewById(R.id.name);
		nameText.setText(name);
		
		//change icon based on mime type
		int icon = getIconForType(mime);
		
		ImageView image = (ImageView) layout.findViewById(R.id.icon);
		image.setImageResource(icon);
		getArguments().putInt("icon", icon);
		TextView uriText = (TextView) layout.findViewById(R.id.uri);
		uriText.setText(path);
		TextView mimeText = (TextView) layout.findViewById(R.id.mime);
		mimeText.setText("Type: "+mime);
		
		return layout;
    }

	private int getIconForType(String mime) {
		if (mime == null)
			return R.drawable.document;
		
		String[] type = mime.split("/");
		Log.d(TAG, type[0] + " == " + type[1]);
		if (type[0].equalsIgnoreCase("image")) {
			return R.drawable.image_any;
		} else if (type[0].equalsIgnoreCase("audio")) {
			return R.drawable.audio_any;
		} else if (type[0].equalsIgnoreCase("video")) {
			return R.drawable.video_any;
		} else if (type[0].equalsIgnoreCase("text")) {
			return R.drawable.text_any;
		} else if (type[0].equalsIgnoreCase("application")) {
			if (type[1].equalsIgnoreCase("pdf")) {
				return R.drawable.pdf;
			} else if (type[1].equalsIgnoreCase("zip") || type[1].equalsIgnoreCase("x-rar-compressed")) {
				return R.drawable.archive;
			} else if (type[1].equalsIgnoreCase("msword") || 
					type[1].equalsIgnoreCase("vnd.openxmlformats-officedocument.wordprocessingml.document") ||
					type[1].equalsIgnoreCase("vnd.oasis.opendocument.text")) {
				return R.drawable.word;
			} else if (type[1].equalsIgnoreCase("vnd.ms-excel") || 
					type[1].equalsIgnoreCase("vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
					type[1].equalsIgnoreCase("vnd.oasis.opendocument.spreadsheet")) {
				return R.drawable.spreadsheet;
			} else if (type[1].equalsIgnoreCase("vnd.ms-powerpoint") || 
					type[1].equalsIgnoreCase("vnd.openxmlformats-officedocument.presentationml.presentation") ||
					type[1].equalsIgnoreCase("vnd.oasis.opendocument.presentation")) {
				return R.drawable.presentation;
			} else {
				return R.drawable.application_any;
			}
		} else {
			return R.drawable.document;
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		// TODO Auto-generated method stub
		Log.d(TAG, "Which button: " + which);
		switch (which) {
		case -1:
			//getDialog().getWindow().findViewById(id)
			//EditText name = (EditText) getView().findViewById(R.id.name);
			EditText name = (EditText) getDialog().getWindow().findViewById(R.id.name);
        	Bundle args = getArguments();
        	args.putString("name", name.getText().toString());
        	
        	CheckBox checkBox = (CheckBox) getDialog().getWindow().findViewById(R.id.addShortcut);
        	args.putBoolean("addShortcut", checkBox.isChecked());
        	
            ((LinkToFileActivity) getActivity()).doPositiveClick(args);
			break;
		case -2:
			((LinkToFileActivity) getActivity()).doNegativeClick();
			break;
		}
	}
}
