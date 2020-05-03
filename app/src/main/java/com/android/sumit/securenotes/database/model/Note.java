package com.android.sumit.securenotes.database.model;

public class Note {

    public static final String TABLE_NAME = "notes";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NOTE_TITLE = "title";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_NOTE = "note";

    private int id;
    private String title;
    private String timestamp;
    private String note;


    // Create table SQL query
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_NOTE_TITLE + " TEXT,"
                    + COLUMN_NOTE + " TEXT,"
                    + COLUMN_TIMESTAMP + " DATETIME"
                    + ")";

    public Note() {
    }

    public Note(int id, String title, String timestamp, String note) {
        this.id = id;
        this.title = title;
        this.timestamp = timestamp;
        this.note = note;
    }

    public int getId() {
        return id;
    }

    public String getNote() {
        return note;
    }

    public String getTitle(){
        return title;
    }


    public String getTimestamp() {
        return timestamp;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setTitle(String title){
        this.title = title;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}