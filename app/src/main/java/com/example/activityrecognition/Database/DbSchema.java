package com.example.activityrecognition.Database;

public class DbSchema {
	public static final class VisitTable {
		public static final String NAME = "visits";

		public static final class Cols {
			public static final String PLACE = "place";
			public static final String DURATION = "duration";
		}
	}

	public static final class ActivitiesTable {
		public static final String NAME = "activities";

		public static final class Cols {
			public static final String ACTIVITY = "activity";
			public static final String DURATION = "duration";
		}
	}

	// Good database design tells us we should have this and reference it.
	public static final class PlacesTable {
		public static final String NAME = "places";

		public static final class Cols {
			public static final String UUID = "uuid";
			public static final String NAME = "name";
			public static final String LAT = "latitude";
			public static final String LONG = "longitude";
		}
	}
}
