package com.example.activityrecognition;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.activityrecognition.Database.ActivityDataBaseHelper;
import com.example.activityrecognition.Database.DbSchema.ActivitiesTable;

public class DatabaseLab {
	private static DatabaseLab dbLab;
	private Context mContext;
	private SQLiteDatabase mDatabase;

	public static DatabaseLab get(Context context) {
		if(dbLab == null) {
			dbLab = new DatabaseLab(context);
		}
		return dbLab;
	}

	private DatabaseLab(Context context) {
		mContext = context.getApplicationContext();
		mDatabase = new ActivityDataBaseHelper(mContext).getWritableDatabase();
	}

	public void saveActivity(String activity, long duration) {
		ContentValues values = new ContentValues();
		values.put(ActivitiesTable.Cols.ACTIVITY, activity);
		values.put(ActivitiesTable.Cols.DURATION, duration);
		mDatabase.insert(ActivitiesTable.NAME, null, values);
	}
}
