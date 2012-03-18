package csh.cryptonite;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import csh.cryptonite.storage.DropboxStorage;
import csh.cryptonite.storage.LocalStorage;
import csh.cryptonite.storage.Storage;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class CreateEncFS extends ListActivity {

    public static final int CREATE_LOCAL=20, CREATE_DB=21;
    public static final int CONFIG_PARANOIA=0, CONFIG_STANDARD=1, 
            CONFIG_COMPATIBLE=2, CONFIG_QUICK=3;
    public static final String RESULT_SELECTED_METHOD = "RESULT_SELECTED_METHOD";
    public static final String START_MODE = "START_MODE";
    
    private static final String ITEM_TITLE = "title";
    private static final String ITEM_DESC = "desc";

    private String[] mMethodTitles;
    private String[] mMethodDesc;
    
    private String currentReturnPath;
    private String passwordAttempt, passwordString;
    
    private int currentConfig;
    
    private Storage mStorage;

    private ArrayList<HashMap<String, String>> mList;

    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED, getIntent());
        
        setContentView(R.layout.create);
        
        if (getIntent().getIntExtra(START_MODE, CREATE_DB) == CREATE_DB) {
            mStorage = new DropboxStorage(this, ((CryptoniteApp)getApplication()));
        } else {
            mStorage = new LocalStorage(this, ((CryptoniteApp)getApplication()));
        }

        mList = new ArrayList<HashMap<String, String>>();
        setupList();
        
        SimpleAdapter methodList = new SimpleAdapter(this, mList,
                R.layout.create_dialog_row,
                new String[] { ITEM_TITLE, ITEM_DESC },
                new int[] {R.id.create_rowtext, R.id.create_rowdesc });
        methodList.notifyDataSetChanged();
        setListAdapter(methodList);
       // view.setOnItemClickListener(this); 
    }
    
    void setupList() {
        
        mMethodTitles = getResources().getStringArray(R.array.encfs_method_titles);
        mMethodDesc = getResources().getStringArray(R.array.encfs_method_desc);

        if (mMethodTitles.length != mMethodDesc.length) {
            Log.e(Cryptonite.TAG, "Method titles and descriptions have different lengths");
            return;
        }
        int ntitle = 0;
        for (String title: mMethodTitles) {
            HashMap<String, String> item = new HashMap<String, String>();
            item.put(ITEM_TITLE, title);
            item.put(ITEM_DESC, mMethodDesc[ntitle]);
            mList.add(item);
            ntitle++;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        currentConfig = position;
        
        /* Select target directory */
        /* Select destination directory for exported files */
        String dialogLabel = getString(R.string.select_create);
        String dialogButtonLabel = getString(R.string.select_create_short);
        int dialogMode = SelectionMode.MODE_OPEN_CREATE;
        String dialogStartPath = "";
        String dialogRoot = "";
        String dialogRootName = dialogRoot;
        String dialogDBEncFS = "";
        switch (mStorage.type) {
        case Storage.STOR_LOCAL:
            if (Cryptonite.externalStorageIsWritable()) {
                dialogStartPath = Environment
                        .getExternalStorageDirectory()
                        .getPath();
            } else {
                dialogStartPath = "/";
            }
            dialogRoot = "/";
            dialogRootName = dialogRoot;
            dialogDBEncFS = "";
            break;
        case Storage.STOR_DROPBOX:
            dialogMode = SelectionMode.MODE_OPEN_CREATE_DB;
            dialogStartPath = getPrivateDir(Cryptonite.BROWSEPNT, Context.MODE_PRIVATE).getPath();
            dialogRoot = dialogStartPath;
            dialogRootName = getString(R.string.dropbox_root_name);
            break;
        default:
        }
        launchBuiltinFileBrowser(dialogRoot, dialogDBEncFS, dialogRootName,
                dialogButtonLabel, dialogStartPath, dialogLabel, dialogMode);
        
    }

    /** Called upon exit from other activities */
    public synchronized void onActivityResult(final int requestCode,
                                              int resultCode, final Intent data) {

        switch (requestCode) {
        case SelectionMode.MODE_OPEN_CREATE:
        case SelectionMode.MODE_OPEN_CREATE_DB:
            /* file dialog */
            if (resultCode == Activity.RESULT_OK && data != null) {
                currentReturnPath = data.getStringExtra(FileDialog.RESULT_EXPORT_PATHS);
                if (currentReturnPath != null ) {
                    showDialog(Cryptonite.MY_PASSWORD_DIALOG_ID);

                }
            }
            break;
        default:
            Log.e(Cryptonite.TAG, "Unknown request code");
        }
    }

    @Override protected Dialog onCreateDialog(int id) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.password_dialog, (ViewGroup) findViewById(R.id.root));
        final EditText password = (EditText) layout.findViewById(R.id.EditText_Pwd);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);

        switch (id) {
         case Cryptonite.MY_PASSWORD_DIALOG_ID:
             builder.setTitle(R.string.title_password);
             builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int whichButton) {
                         removeDialog(Cryptonite.MY_PASSWORD_DIALOG_ID);
                     }
                 });
             builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int which) {
                         passwordAttempt = password.getText().toString();
                         removeDialog(Cryptonite.MY_PASSWORD_DIALOG_ID);
                         if (passwordAttempt.length() > 0) {
                             showDialog(Cryptonite.MY_PASSWORD_CONFIRM_DIALOG_ID);
                         } else {
                             showToast(R.string.empty_password);
                         }
                     }
                 });
             return builder.create();
         case Cryptonite.MY_PASSWORD_CONFIRM_DIALOG_ID:
             builder.setTitle(R.string.title_confirm_password);
             builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int whichButton) {
                         removeDialog(Cryptonite.MY_PASSWORD_CONFIRM_DIALOG_ID);
                     }
                 });
             builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int which) {
                         passwordString = password.getText().toString();
                         removeDialog(Cryptonite.MY_PASSWORD_CONFIRM_DIALOG_ID);
                         if (passwordString.length() > 0) {
                             if (!passwordString.equals(passwordAttempt)) {
                                 nullPasswords();
                                 showToast(R.string.pw_mismatch);
                                 return;
                             }
                             final ProgressDialog pd = ProgressDialog.show(CreateEncFS.this,
                                     getString(R.string.wait_msg),
                                     getString(R.string.creating_encfs), true);
                             new Thread(new Runnable(){
                                 public void run(){
                                     File browseRoot = getPrivateDir(Cryptonite.BROWSEPNT, Context.MODE_PRIVATE);
                                     mStorage.createEncFS(currentReturnPath, passwordString, browseRoot, currentConfig); 
                                     runOnUiThread(new Runnable(){
                                         public void run() {
                                             if (pd.isShowing())
                                                 pd.dismiss();
                                             nullPasswords();
                                             Cryptonite.jniResetVolume();
                                             setResult(RESULT_OK, getIntent());
                                             finish();
                                         }
                                     });
                                 }
                             }).start();
                             
                         } else {
                             showToast(R.string.empty_password);
                         }
                     }
                 });
             return builder.create();
         }
        return null;
    }

    private void launchBuiltinFileBrowser(String dialogRoot, String dialogDBEncFS, String dialogRootName,
            String dialogButtonLabel, String dialogStartPath, String dialogLabel, int dialogMode) 
    {
        Intent intent = new Intent(getBaseContext(), FileDialog.class);
        intent.putExtra(FileDialog.CURRENT_ROOT, dialogRoot);
        intent.putExtra(FileDialog.CURRENT_DBROOT, dialogDBEncFS);
        intent.putExtra(FileDialog.CURRENT_ROOT_NAME, dialogRootName);
        intent.putExtra(FileDialog.BUTTON_LABEL, dialogButtonLabel);
        intent.putExtra(FileDialog.START_PATH, dialogStartPath);
        intent.putExtra(FileDialog.LABEL, dialogLabel);
        intent.putExtra(FileDialog.SELECTION_MODE, dialogMode);
        startActivityForResult(intent, dialogMode);
    }
    
    public File getPrivateDir(String label, int mode) {
        /* Tear down and recreate the browse directory to make
         * sure we have appropriate permissions */
        File browseDirF = getBaseContext().getDir(label, mode);
        if (browseDirF.exists()) {
            if (!Cryptonite.deleteDir(browseDirF)) {
                showToast(R.string.target_dir_cleanup_failure);
                return null;
            }
        }
        browseDirF = getBaseContext().getDir(label, mode);
        return browseDirF;
    }
    
    private void showToast(int resId) {
        showToast(getString(resId));
    }

    private void showToast(final String msg) {
        showToast(msg, true);
    }
    
    private void showToast(final String msg, final boolean showLong) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (showLong) {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void nullPasswords() {
        char[] fill = new char[passwordAttempt.length()];
        Arrays.fill(fill, '\0');
        passwordAttempt = new String(fill);
        fill = new char[passwordString.length()];
        Arrays.fill(fill, '\0');
        passwordString = new String(fill);
    }
    
}