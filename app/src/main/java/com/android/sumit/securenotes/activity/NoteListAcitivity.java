package com.android.sumit.securenotes.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.sumit.securenotes.App;
import com.android.sumit.securenotes.R;
import com.android.sumit.securenotes.database.DatabaseHelper;
import com.android.sumit.securenotes.database.model.Note;
import com.android.sumit.securenotes.utils.MyDividerItemDecoration;
import com.android.sumit.securenotes.utils.RecyclerTouchListener;
import com.android.sumit.securenotes.utils.SessionListener;
import com.android.sumit.securenotes.view.NotesAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;

public class NoteListAcitivity extends AppCompatActivity implements SessionListener {
    public  static NotesAdapter mAdapter;
    public static List<Note> notesList = new ArrayList<>();
    private static TextView noNotesView;
    private FloatingActionButton fab;
    public  static DatabaseHelper db;

    private SharedPreferences mSharedPreferences;
    private Cipher mCipher;
    public static boolean foregroundSessionTimeout;
    private boolean backgroundSessionTimeout;
    private App app;
    private boolean isMultiSelect = false;
    private HashMap< Note, View> selectedIds = new HashMap< Note, View>();
    private ActionMode actionModev;
    private TextView countTextView;
    private RecyclerView recyclerView;

    private ActionMode.Callback mActionModeCallback= new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(final ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.menu_select, menu);
            isMultiSelect = true;
            final MenuItem menuItem = menu.findItem(R.id.spinner);
            final LinearLayout linearLayout = (LinearLayout) menuItem.getActionView();
            countTextView = linearLayout.findViewById(R.id.count);
            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final PopupMenu popupMenu  = new PopupMenu(NoteListAcitivity.this,linearLayout);
                    popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

                    if(notesList.size() == selectedIds.size())
                        popupMenu.getMenu().findItem(R.id.selall).setEnabled(false);

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch(menuItem.getItemId()) {
                                case R.id.selall:
                                    View child;
                                    for(int i=0; i<recyclerView.getChildCount(); i++){
                                        child = recyclerView.getChildAt(i);
                                        child.setForeground(new ColorDrawable(ContextCompat.getColor(NoteListAcitivity.this, R.color.colorControlActivated)));
                                        Note note = notesList.get(i);
                                        if(!selectedIds.containsKey(note))
                                            selectedIds.put(note, child);
                                    }
                                    countTextView.setText(String.valueOf(recyclerView.getChildCount()));
                                    popupMenu.getMenu().findItem(R.id.selall).setEnabled(false);
                                    return true;

                                case R.id.desall:
                                    actionMode.finish();
                                    return true;

                                default:
                                    return false;

                            }
                        }
                    });
                    popupMenu.show();
                }
            });
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {

            return false;
        }

        private void delete() {
            Set set = selectedIds.entrySet();
            for (Object aSet : set) {
                Map.Entry m = (Map.Entry) aSet;
                ((View) m.getValue()).setForeground(new ColorDrawable(ContextCompat.getColor(NoteListAcitivity.this, android.R.color.transparent)));
                Note note = (Note) m.getKey();
                int index = notesList.indexOf(note);
                db.deleteNote(note);

                notesList.remove(index);
                mAdapter.notifyItemRemoved(index);

            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    toggleEmptyNotes();
                }
            },200);

            selectedIds.clear();
        }


        @Override
        public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.action_delete) {
                new AlertDialog.Builder(NoteListAcitivity.this)
                        .setTitle("Delete")
                        .setMessage("Delete selected item(s)")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                delete();
                                actionMode.finish();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();


                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            isMultiSelect = false;
            actionModev = null;
            Set set = selectedIds.entrySet();
            for (Object aSet : set) {
                Map.Entry m = (Map.Entry) aSet;
                ((View) m.getValue()).setForeground(new ColorDrawable(ContextCompat.getColor(NoteListAcitivity.this, android.R.color.transparent)));
            }

            selectedIds = null;
            fab.setVisibility(View.VISIBLE);
        }
    };



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_about,menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                startActivity(new Intent(NoteListAcitivity.this, About.class));
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notelist);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        recyclerView = findViewById(R.id.recycler_view);
        app = new App();
        app.registerListener(this);
        app.startForegorundSession();

        foregroundSessionTimeout = false;
        backgroundSessionTimeout = false;

        noNotesView = findViewById(R.id.empty_notes_view);


        db = new DatabaseHelper(getApplicationContext());



        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                startNoteActivity(0,-1);
            }
        });

        mAdapter = new NotesAdapter(this, notesList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new MyDividerItemDecoration(this, LinearLayoutManager.VERTICAL, 16));
        recyclerView.setAdapter(mAdapter);

        toggleEmptyNotes();


        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                recyclerView, new RecyclerTouchListener.ClickListener() {

            @Override
            public void onClick(View view, final int position) {
                if(isMultiSelect) multiSelect(view, position);

                else showActionsDialog(position);
            }

            @Override
            public void onLongClick(View view, int position) {

                if (!isMultiSelect){
                    selectedIds = new HashMap<Note ,View>();

                    if (actionModev == null){
                        actionModev = startSupportActionMode(mActionModeCallback); //show ActionMode.
                    }
                    multiSelect(view, position);
                }


            }

        }));


        mSharedPreferences = getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.shared_preference),
                Context.MODE_PRIVATE);


    }

    private void multiSelect(View view, int position) {
        fab.setVisibility(View.GONE);
        Note note = notesList.get(position);
        if (note != null){
            if (actionModev != null) {
                if (selectedIds.containsKey(note)){
                    selectedIds.remove(note);
                    view.setForeground(new ColorDrawable(ContextCompat.getColor(NoteListAcitivity.this,android.R.color.transparent)));

                }


                else{
                    selectedIds.put(note, view);
                    view.setForeground(new ColorDrawable(ContextCompat.getColor(NoteListAcitivity.this, R.color.colorControlActivated)));

                }

                if (selectedIds.size() > 0)
                    countTextView.setText(String.valueOf(selectedIds.size()));
                    //actionModev.setTitle(String.valueOf(selectedIds.size()) + " selected"); //show selected item count on action mode.
                else{
                    countTextView.setText("0");
                    //actionModev.setTitle(""); //remove item count from action mode.
                    actionModev.finish(); //hide action mode.
                }

            }
        }
    }

    private boolean checkInactivity(){
        return foregroundSessionTimeout;
    }
    /**
     * Deleting note from SQLite and removing the
     * item from the list by its position
     */
    public static void deleteNote(int position) {
        // deleting the note from db
        db.deleteNote(notesList.get(position));

        // removing the note from the list
        notesList.remove(position);
        mAdapter.notifyItemRemoved(position);

        toggleEmptyNotes();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        app.startForegorundSession();

    }

    /**
     * Opens dialog with Edit - Delete options
     * Edit - 0
     * Delete - 0
     */

    private void showActionsDialog(final int position) {
        startNoteActivity(notesList.get(position).getId(), position);


    }


    public void startNoteActivity(int id, int position){
        Intent it = new Intent(NoteListAcitivity.this,NoteActivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        it.putExtra("id",id);
        it.putExtra("position",position);
        startActivity(it);
    }

    /**
     * Toggling list and empty notes view
     */
    public static void toggleEmptyNotes() {
        // you can check notesList.size() > 0

        Log.i("info",String.valueOf(db.getNotesCount()));
        if (db.getNotesCount() > 0) {
            noNotesView.setVisibility(View.GONE);
        } else {
            noNotesView.setVisibility(View.VISIBLE);
        }
    }






    @Override
    protected void onResume() {
        super.onResume();
        notesList.clear();
        notesList.addAll(db.getAllNotes());



        app.cancelBackgroundSession();
        if(backgroundSessionTimeout)
            foregroundSessionTimeout = true;

  }

    @Override
    protected void onPause() {
        super.onPause();
        //app.cancelForegroundSession();
        backgroundSessionTimeout = false;
        app.startBackgroundSession();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        app.cancelBackgroundSession();
        app.cancelForegroundSession();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        new MaterialDialog.Builder(NoteListAcitivity.this)
                .title("Exit")
                .content("Are you sure you want to exit?")
                .positiveText("Yes")
                .negativeText("No")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();

                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .cancelable(true)
                .show();

    }


    @Override
    public void foregroundSessionExpired() {
        foregroundSessionTimeout = true;
    }

    @Override
    public void backgroundSessionExpired() {
        backgroundSessionTimeout = true;
    }

}
