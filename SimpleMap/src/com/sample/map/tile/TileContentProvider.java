/**
 * 
 */
package com.sample.map.tile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

public class TileContentProvider extends ContentProvider {
	
	static final String AUTHORITY = "com.sample.map.tileprovider";
	
	private static final int TILES_ALL = 1;
	private static final int TILE_ID = 2;
	private static final int TILES_OLDEST = 3;
	
	private static final int SAVE_TILES_COUNT = 250;

	private static final UriMatcher sMatcher = new UriMatcher(0);
	
	private DbHelper mDbHelper;
	
	@Override
	public boolean onCreate() {
		mDbHelper = new DbHelper(getContext());
		return true;
	}
	
	@Override
	public String getType(Uri uri) {
		switch (sMatcher.match(uri)){
		case TILES_ALL:
		case TILES_OLDEST:
			return Tile.CONTENT_TYPE;
		case TILE_ID:
			return Tile.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown Uri " + uri);
		}
	}
		
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (sMatcher.match(uri) != TILES_ALL){
			throw new IllegalArgumentException("Cant insert to the uri " + uri);
		}
		if (values == null){
			throw new IllegalArgumentException("Content Values can't be null");
		}
		//copy values if exists
		ContentValues v = new ContentValues(values);
		if (!v.containsKey(Tile.KEY_ROW) || !v.containsKey(Tile.KEY_COL)){
			throw new IllegalArgumentException("Content Values for tile should " +
					"containt both: " + Tile.KEY_ROW + " and " + Tile.KEY_COL + " keys");
		}
		v.put(Tile.LAST_ACCESS, System.currentTimeMillis());
		v.put(Tile.DATA, getAbsoluteFile(getTileRow(v), getTileCol(v)));
		//everything is fine, save to the database now
		SQLiteDatabase db = mDbHelper.getWritableDatabase();		
		long id = db.insert(Tile.TABLE_NAME, "", v);
		if (id > 0){
			getContext().getContentResolver().notifyChange(Tile.CONTENT_URI, null);
			Uri tileUri = getTileUri(v);
			return tileUri;
		}
		throw new SQLException("Error while inserting " + uri +
				" with content values " + v);
	}
	
	private Uri getTileUri(ContentValues values){
		return Tile.getUri(values.getAsInteger(Tile.KEY_ROW), 
				values.getAsInteger(Tile.KEY_COL));
	}
	
	private int getTileRow(ContentValues values){
		return values.getAsInteger(Tile.KEY_ROW);		
	}
	
	private int getTileCol(ContentValues values){
		return values.getAsInteger(Tile.KEY_COL);
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(Tile.TABLE_NAME);
		switch (sMatcher.match(uri)){
		case TILE_ID:
			int row = getTileItemRow(uri), col = getTileItemCol(uri);
			qb.appendWhere(Tile.KEY_ROW + "=" + row);
			qb.appendWhere(" AND " + Tile.KEY_COL + "=" + col);
			break;
		case TILES_ALL:
			//TODO add other uri types support;
			break;
		default:
			throw new IllegalArgumentException("Unknown uri " + uri);
		}
		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);		
		return c;
	}
	
	private int getTileItemRow(Uri uri){
		//we always have last two path segments reserved for row and column 
		//like this content://com.sample.map.tileprovide/tiles/$row/$col
		List<String> pathSegments = uri.getPathSegments();
		if (pathSegments.size() < 2){
			throw new IllegalArgumentException("Invalid uri" + uri);
		}
		return Integer.parseInt(pathSegments.get(pathSegments.size() - 2));
	}
	
	private int getTileItemCol(Uri uri){
		List<String> pathSegments = uri.getPathSegments();
		if (pathSegments.size() < 2){
			throw new IllegalArgumentException("Invalid uri" + uri);
		}
		return Integer.parseInt(pathSegments.get(pathSegments.size() - 1));
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		ContentValues v = values == null ? new ContentValues() : new ContentValues(values);
		if (v.containsKey(Tile.KEY_ROW) || v.containsKey(Tile.KEY_COL)){
			throw new IllegalArgumentException("Can't change row or column of the tile");
		}
		if (!v.containsKey(Tile.LAST_ACCESS)){
			v.put(Tile.LAST_ACCESS, System.currentTimeMillis());
		}
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int count = 0;
		switch (sMatcher.match(uri)) {
		case TILE_ID:
			int row = getTileItemRow(uri), col = getTileItemCol(uri);
			count = db.update(Tile.TABLE_NAME, v, Tile.KEY_ROW + "=" + row + " AND " + Tile.KEY_COL + "=" + col, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown uri " + uri);
		}
		if (count > 0){
			getContext().getContentResolver().notifyChange(Tile.CONTENT_URI, null);
		}
		return count;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int count = 0;
		switch (sMatcher.match(uri)) {
		case TILE_ID:{
			int row = getTileItemRow(uri), col = getTileItemCol(uri);
			count = removeTile(selection, selectionArgs, db, row, col);
			break;
		}
		case TILES_OLDEST:{
			count = removeOldTiles(selection, selectionArgs, db);
			break;
		}
		default:
			throw new IllegalArgumentException("Unknown uri " + uri);
		}
		if (count > 0){
			getContext().getContentResolver().notifyChange(Tile.CONTENT_URI, null);			
		}
		return count;
	}

	private int removeOldTiles(String selection,
			String[] selectionArgs, SQLiteDatabase db) {
		Cursor c = null;
		int count = 0;
		try{
			c = query(Tile.CONTENT_URI, new String[]{Tile.KEY_ROW, Tile.KEY_COL, Tile.DATA, Tile.LAST_ACCESS}, 
					null, null, Tile.LAST_ACCESS + " DESC");
			
			if (c.moveToFirst()){
				do {
					int row = c.getInt(c.getColumnIndex(Tile.KEY_ROW));
					int col = c.getInt(c.getColumnIndex(Tile.KEY_COL));
					removeTile(selection, selectionArgs, db, row, col);
					
				} while (c.moveToNext() && c.getCount() - (++count) > SAVE_TILES_COUNT);
				Log.d("Content Provider", count + " old tiles removed from " + c.getCount());
			}
		} finally {
			if (c != null){
				c.close();
			}
		}
		return count;
	}

	private int removeTile(String selection, String[] selectionArgs,
			SQLiteDatabase db, int row, int col) {
		int count = db.delete(Tile.TABLE_NAME, 
				Tile.KEY_ROW + "=" + row + 
				" AND " + Tile.KEY_COL + "=" + col  +
				(!TextUtils.isEmpty(selection) ? " AND (" +selection + ')' : ""), 
				selectionArgs);
		if (count > 0){
			String file = getAbsoluteFile(row, col);
			File f = new File(file);
			if (f.exists()){
				f.delete();
			}
		}
		return count;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)
			throws FileNotFoundException {
		if (sMatcher.match(uri) != TILE_ID){
			throw new IllegalArgumentException("Can only open single file, your uri is " + uri);
		}
		ParcelFileDescriptor descriptor = openFileHelper(uri, mode);
//		update(uri, new ContentValues(), null, null);
		return descriptor;
	}
	
	private String getAbsoluteFile(int row, int col){
		return new File(getContext().getCacheDir(), 
				Tile.TABLE_NAME + "_" + row + "_" + col + ".png").getAbsolutePath();
	}
	
	static {
		sMatcher.addURI(AUTHORITY, Tile.CONTENT_URI_PATH, TILES_ALL);
		sMatcher.addURI(AUTHORITY, Tile.CONTENT_URI_PATH + "/#/#", TILE_ID);
		sMatcher.addURI(AUTHORITY, Tile.CONTENT_URI_PATH + "/old", TILES_OLDEST);
	}
	
	private static class DbHelper extends SQLiteOpenHelper{
		private static final String DB_NAME = "tiles";
		private static final int DB_VERSION = 1; 
		
		public DbHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + Tile.TABLE_NAME + " " +
					"(" +
						Tile._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
						Tile.KEY_ROW + " INTEGER, " +
						Tile.KEY_COL + " INTEGER, " +
						Tile.DATA + " TEXT, " +						
						Tile.LAST_ACCESS + " REAL" + 
					")");
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + Tile.TABLE_NAME);
			onCreate(db);
		}
	}
}
