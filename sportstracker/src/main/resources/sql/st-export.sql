-- SQLite schema definition for SportsTracker export.

-- All date-time values are stored as an INTEGER, which is the Unix Time format
-- (number of seconds since 1970-01-01 00:00:00 UTC).
-- All boolean values are stored as an INTEGER, 0 is false, 1 is true.

-- enable Foreign Key Support (disabled by default)
PRAGMA foreign_keys = ON;

CREATE TABLE SPORT_TYPE (
    ID INTEGER PRIMARY KEY NOT NULL,
    NAME TEXT NOT NULL,
    RECORD_DISTANCE INTEGER NOT NULL,
    COLOR TEXT NOT NULL,
    ICON TEXT
);

CREATE TABLE SPORT_SUBTYPE (
    -- the sport subtype ID is not unique, so a generated ID is needed
    ID INTEGER PRIMARY KEY AUTOINCREMENT,
    SPORT_SUBTYPE_ID INTEGER NOT NULL,
    SPORT_TYPE_ID INTEGER NOT NULL,
    NAME TEXT NOT NULL,
    UNIQUE (SPORT_SUBTYPE_ID, SPORT_TYPE_ID),
    FOREIGN KEY (SPORT_TYPE_ID) REFERENCES SPORT_TYPE (ID)
);

CREATE TABLE EQUIPMENT (
    -- the equipment ID is not unique, so a generated ID is needed
    ID INTEGER PRIMARY KEY AUTOINCREMENT,
    EQUIPMENT_ID INTEGER NOT NULL,
    SPORT_TYPE_ID INTEGER NOT NULL,
    NAME TEXT NOT NULL,
    UNIQUE (EQUIPMENT_ID, SPORT_TYPE_ID),
    FOREIGN KEY (SPORT_TYPE_ID) REFERENCES SPORT_TYPE (ID)
);

CREATE TABLE EXERCISE (
    ID INTEGER PRIMARY KEY NOT NULL,
    DATE_TIME INTEGER NOT NULL,
    SPORT_TYPE_ID INTEGER NOT NULL,
    SPORT_SUBTYPE_ID INTEGER NOT NULL,
    INTENSITY TEXT NOT NULL,
    -- duration of exercise in seconds
    DURATION INTEGER NOT NULL,
    -- distance of exercise in kilometers
    DISTANCE REAL,
    -- average speed of exercise in kilometers per hour
    AVG_SPEED REAL,
    -- average heartrate of exercise in beats per minute
    AVG_HEARTRATE INTEGER,
    -- ascent (height meters) of exercise in meters
    ASCENT INTEGER,
    -- descent (height meters) of exercise in meters
    DESCENT INTEGER,
    -- amount of kCalories consumed
    CALORIES INTEGER,
    HRM_FILE TEXT,
    EQUIPMENT_ID INTEGER,
    COMMENT TEXT,
    FOREIGN KEY (SPORT_TYPE_ID) REFERENCES SPORT_TYPE (ID),
    FOREIGN KEY (SPORT_SUBTYPE_ID) REFERENCES SPORT_SUBTYPE (ID),
    FOREIGN KEY (EQUIPMENT_ID) REFERENCES EQUIPMENT (ID)
);

CREATE TABLE NOTE (
    ID INTEGER PRIMARY KEY NOT NULL,
    DATE_TIME TEXT NOT NULL,
    COMMENT TEXT NOT NULL
);

CREATE TABLE WEIGHT (
    ID INTEGER PRIMARY KEY NOT NULL,
    DATE_TIME TEXT NOT NULL,
    -- weight value in kilograms
    VALUE REAL NOT NULL,
    COMMENT TEXT
);
