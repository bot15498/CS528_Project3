package com.example.activityrecognition.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.activityrecognition.Database.DbSchema.ActivitiesTable;
import com.example.activityrecognition.Database.DbSchema.VisitTable;

public class ActivityDataBaseHelper extends SQLiteOpenHelper {
	private static final String TAG = "CrimeBaseHelper";
	private static final int VERSION = 1;
	private static final String DATABASE_NAME = "database.db";

	public ActivityDataBaseHelper(Context context) {
		super(context, DATABASE_NAME, null, VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL("create table " + ActivitiesTable.NAME + "(" +
				"_id integer primary key autoincrement, " +
				ActivitiesTable.Cols.ACTIVITY + ", " +
				ActivitiesTable.Cols.DURATION +
				")"
		);

		sqLiteDatabase.execSQL("create table " + VisitTable.NAME + "(" +
				"_id integer primary key autoincrement, " +
				VisitTable.Cols.PLACE + ", " +
				VisitTable.Cols.DURATION +
				")"
		);
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
	}
}
