package com.izambasiron.free.linktofile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class FilesDbAdapter {
	public static final String KEY_ICON = "icon";
	public static final String KEY_TITLE = "title";
    public static final String KEY_PATH = "path";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_MIME = "mime";

    private static final String TAG = "FilesDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
        "create table files (_id integer primary key autoincrement, "
        + "title text not null, path text not null, icon integer not null, mime text);";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "files";
    private static final int DATABASE_VERSION = 4;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS files");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public FilesDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the files database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public FilesDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new file link using the title and path provided. If the link is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param title the title of the note
     * @param body the body of the note
     * @return rowId or -1 if failed
     */
    public long createFile(String title, String path, int icon, String mime) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_PATH, path);
        initialValues.put(KEY_ICON, icon);
        initialValues.put(KEY_MIME, mime);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Delete the file link with the given rowId
     * 
     * @param rowId id of file to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteFile(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all files in the database
     * 
     * @return Cursor over all files
     */
    public Cursor fetchAllFiles() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE,
                KEY_PATH, KEY_ICON, KEY_MIME}, null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the File that matches the given rowId
     * 
     * @param rowId id of file to retrieve
     * @return Cursor positioned to matching file, if found
     * @throws SQLException if file could not be found/retrieved
     */
    public Cursor fetchFile(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_TITLE, KEY_PATH, KEY_ICON, KEY_MIME}, KEY_ROWID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the file using the details provided. The file to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of file to update
     * @param title value to set file title to
     * @param path value to set file body to
     * @return true if the file was successfully updated, false otherwise
     */
    public boolean updateFile(long rowId, String title, String path, int icon, String mime) {
        ContentValues args = new ContentValues();
        args.put(KEY_TITLE, title);
        args.put(KEY_PATH, path);
        args.put(KEY_ICON, icon);
        args.put(KEY_MIME, mime);

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}
