package com.android.sumit.securenotes.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.security.keystore.KeyProperties;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.sumit.securenotes.R;
import com.android.sumit.securenotes.database.DatabaseHelper;
import com.android.sumit.securenotes.database.model.Note;

import java.io.IOException;
import java.lang.ref.WeakReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;

import static com.android.sumit.securenotes.activity.NoteListAcitivity.notesList;

import static com.android.sumit.securenotes.activity.NoteListAcitivity.mAdapter;


public class NoteActivity extends AppCompatActivity {

    private TextInputEditText noteTitle;
    private TextInputEditText noteText;
    private int id;
    private int position;
    private Menu menu;
    private Cipher mCipher;
    private boolean saved = false;
    private boolean use_fingerprint;


    private static class SaveData extends AsyncTask<String,Void,Void>{

        private ProgressDialog progress;
        private DatabaseHelper db;
        private WeakReference<Context> contextWeakReference;
        private WeakReference<NoteActivity> noteListActivityWeakReference;


        SaveData(WeakReference<Context> contextWeakReference){
            this.contextWeakReference = contextWeakReference;
            db = new DatabaseHelper(this.contextWeakReference.get().getApplicationContext());
            noteListActivityWeakReference = new WeakReference<>((NoteActivity) contextWeakReference.get());

        }

        private void createNote(final String note,final String title)  {
            // inserting note in db and getting
            // newly inserted note id
                    long id = 0;
                        id = db.insertNote(note, title,null);



            // get the newly inserted note from db
                    final Note n = db.getNote(id);
            ((Activity)contextWeakReference.get()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (n != null) {
                                // adding new note to array list at 0 position
                                notesList.add(0, n);

                                // refreshing the list
                                mAdapter.notifyDataSetChanged();
                            }
                            noteListActivityWeakReference.get().position = 0;
                            NoteListAcitivity.toggleEmptyNotes();

                        }
                    });

        }
        private void updateNote(final String note, final String title, final int position) {
            Note n = notesList.get(position);


                n.setNote(note);



                n.setTitle(title);

            // updating note in db
            db.updateNote(n);

            // refreshing the list
            notesList.set(position, n);
            ((Activity)contextWeakReference.get()).runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    NoteListAcitivity.mAdapter.notifyItemChanged(position);
                    NoteListAcitivity.toggleEmptyNotes();
                }
            });


        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progress = new ProgressDialog(contextWeakReference.get());
            progress.setMessage("Saving data");
            progress.setCancelable(false);
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.show();
        }

        @Override
        protected Void doInBackground(String... strings) {
            Context context = contextWeakReference.get();
            if(strings[0].equals(context.getApplicationContext().getString(R.string.save)))
            createNote(strings[1],strings[2]);

            else if(strings[0].equals(context.getApplicationContext().getString(R.string.update)))
                updateNote(strings[1],strings[2],Integer.parseInt(strings[3]));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            db.close();
            noteListActivityWeakReference.get().saved = true;
            progress.dismiss();
            Toast.makeText(contextWeakReference.get(), "Note saved", Toast.LENGTH_SHORT).show();
        }
    }



    private void deleteNote(int position) {

       if(position!=-1){
           NoteListAcitivity.deleteNote(position);
       }

    }

    private boolean checkEmpty(String note, String title){

        if(TextUtils.isEmpty(title)){
            noteTitle.setError("title can't be empty");

            if(TextUtils.isEmpty(note)){
                noteText.setError("can't save empty data");
                return true;
                //noteText.requestFocus();
            }
            noteTitle.requestFocus();
            return true;
        }

        if(TextUtils.isEmpty(note)){
            noteText.setError("can't save empty data");
            noteText.requestFocus();
            return true;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu,menu);
        this.menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.delete:

                new AlertDialog.Builder(this)
                        .setMessage("Delete note")
                        .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(position!=-1){
                                    delete();

                                }
                                else{
                                    dialogInterface.dismiss();
                                    finish();
                                }


                            }
                        })
                        .setNegativeButton("no", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .setCancelable(false)
                        .show();
                return true;

            case R.id.edit:
                Log.i("info","edit");
                item.setVisible(false);
                menu.findItem(R.id.done).setVisible(true);
                noteTitle.setFocusableInTouchMode(true);
                noteTitle.setClickable(true);
                noteText.setFocusableInTouchMode(true);
                noteText.setClickable(true);
                findViewById(R.id.guide).setVisibility(View.INVISIBLE);
                return true;

            case R.id.done:
                if(!checkEmpty(noteText.getText().toString(),noteTitle.getText().toString())){
                    if(id==0){
                        new AlertDialog.Builder(NoteActivity.this)
                                .setMessage("Save note")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {


                                        dialog.dismiss();
                                        save();




                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .setCancelable(false)
                                .show();

                    }
                    else{

                        new AlertDialog.Builder(NoteActivity.this)
                                .setMessage("Update note")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {


                                        update();


                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .show();
                    }
                    return true;
                }
            case android.R.id.home:
                if( !saved ) {
                    new AlertDialog.Builder(NoteActivity.this)
                            .setMessage("All unsaved data will be lost")
                            .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setNegativeButton("Stay", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .setCancelable(false)
                            .show();
                }
                else
                    finish();
                return true;
            default: return false;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem editItem = menu.findItem(R.id.edit);
        MenuItem doneItem = menu.findItem(R.id.done);
        if(id==0)
            doneItem.setVisible(true);
        else
            editItem.setVisible(true);
        return super.onPrepareOptionsMenu(menu);
    }

    public void save(){
    new SaveData(new WeakReference<Context>(this)).execute(getApplicationContext().getString(R.string.save),
            noteText.getText().toString(),noteTitle.getText().toString());

    }


    public void delete(){
    deleteNote(position);
    finish();
    }

    public void update(){

    new SaveData(new WeakReference<Context>(this)).execute(getApplicationContext().getString(R.string.update),
            noteText.getText().toString(),noteTitle.getText().toString(),String.valueOf(position));

    }

    private String getData(String text){
    String result = null;

        result = text;

        return result;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        SharedPreferences mSharedPreferences = getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.shared_preference),
                Context.MODE_PRIVATE);

        noteTitle = findViewById(R.id.noteTitle);
        noteText = findViewById(R.id.noteText);
        saved = true;
        use_fingerprint = mSharedPreferences.getBoolean(getApplicationContext().getString(R.string.fingerprint),true) &&
                mSharedPreferences.getBoolean(getApplicationContext().getString(R.string.use_fingerprint_future),true);

        final TextView dateView = findViewById(R.id.dateView);



        Intent it = getIntent();
        id = it.getIntExtra("id",0);
        position = it.getIntExtra("position",-1);

        if(id==0){
            Date c = Calendar.getInstance().getTime();
            SimpleDateFormat df = new SimpleDateFormat("MMM d");
            String formattedDate = df.format(c);
            dateView.setText(formattedDate);
        }

        else{
            DatabaseHelper db = new DatabaseHelper(this);
            Note note = db.getNote(id);
            noteTitle.setText(getData(note.getTitle()));

            noteText.setText(getData(note.getNote()));

            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //fmt.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getDisplayName(false,TimeZone.SHORT)));
            try {
                Date date = fmt.parse(note.getTimestamp());
                fmt = new SimpleDateFormat("EEEE MMM d, yyyy HH:mm:ss");
                dateView.setText(fmt.format(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }


            noteTitle.setFocusable(false);
            noteTitle.setClickable(false);
            noteText.setFocusable(false);
            noteText.setClickable(false);
            findViewById(R.id.guide).setVisibility(View.VISIBLE);
        }


        noteTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                saved = false;
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

    }

            @Override
            public void onBackPressed() {

                if( !saved ) {
                    new AlertDialog.Builder(NoteActivity.this)
                            .setMessage("All unsaved data will be lost")
                            .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setNegativeButton("Stay", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .setCancelable(false)
                            .show();
                }

                else {
                    System.out.println("out");
                    super.onBackPressed();
                }
    }

}