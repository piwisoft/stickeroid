package com.piwi.stickeroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Main extends ListActivity
{
    public final static String LOG_TAG = "stickeroid";
    
    private final static int EDIT_ID = Menu.FIRST;

    private final static int DELETE_ID = Menu.FIRST + 1;

    private final static int ACTIVITY_CREATE = 0;

    private final static int ACTIVITY_EDIT = 1;

    private final static String BACKUP_PATH = "/Android/data/com.piwi.stickeroid/files";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collections);

        updateList();

        registerForContextMenu(getListView());
        
        backupCollections();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, EDIT_ID, 0, R.string.menu_edit_collection);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete_collection);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item)
    {
        switch(item.getItemId())
        {
        case R.id.main_menu_add_item:
        {
            Intent i = new Intent(this, CreateCollection.class);
            startActivityForResult(i, ACTIVITY_CREATE);
            return true;
        }
        case R.id.main_menu_restore:
            listRestorableCollections();
            return true;
        case R.id.main_menu_preferences:
        {
            Intent i = new Intent(this, Preferences.class);
            startActivity(i);
            return true;
        }
        case R.id.main_menu_about:
        {
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.about_dialog);
            dialog.setTitle("Stickeroid v1.0");
            dialog.show();
            return true;
        }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch(item.getItemId())
        {
        case EDIT_ID:
        {
            Intent i = new Intent(this, CreateCollection.class);
            i.putExtra(CreateCollection.COLLECTION_NAME,
                    (String) getListAdapter().getItem(info.position));
            startActivityForResult(i, ACTIVITY_EDIT);
            return true;
        }
        case DELETE_ID:
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setTitle(R.string.delete_collection_dialog_title);
            builder.setMessage(R.string.delete_collection_dialog_msg);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    if(deleteFile(Collection.getFileName((String) getListAdapter().getItem(
                            info.position))))
                    {
                        updateList();
                    }
                }
            });
            builder.setNegativeButton(R.string.no, null);
            AlertDialog dlg = builder.create();
            dlg.setOwnerActivity(this);
            dlg.show();
            return true;
        }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        super.onListItemClick(l, v, position, id);

        Intent i = new Intent(this, EditCollection.class);
        i.putExtra(EditCollection.NAME_EXTRA, (String) getListAdapter().getItem(position));
        startActivity(i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode)
        {
        case ACTIVITY_CREATE:
            if(resultCode == RESULT_OK)
            {
                onActivityCreateResult(data.getExtras());
            }
            break;

        case ACTIVITY_EDIT:
            if(resultCode == RESULT_OK)
            {
                onActivityEditResult(data.getExtras());
            }
            break;
        }
    }

    private void onActivityCreateResult(Bundle extras)
    {
        boolean success = false;

        try
        {
            String name = extras.getString(CreateCollection.COLLECTION_NAME);
            int size = extras.getInt(CreateCollection.COLLECTION_SIZE);
            Collection nc = new Collection(name, size);
            FileOutputStream fos = openFileOutput(nc.getFileName(), MODE_PRIVATE);
            success = nc.save(fos);
        }
        catch(IOException e)
        {
        }

        if(success)
        {
            updateList();
        }
    }

    private void onActivityEditResult(Bundle extras)
    {
        boolean success = false;

        try
        {
            String currentName = extras.getString(CreateCollection.OLD_COLLECTION_NAME);

            Collection col = new Collection(currentName);

            FileInputStream fis = openFileInput(col.getFileName());
            if(col.load(fis))
            {
                fis.close();

                String newName = extras.getString(CreateCollection.COLLECTION_NAME);
                int newSize = extras.getInt(CreateCollection.COLLECTION_SIZE);

                if(!newName.equalsIgnoreCase(currentName) || newSize != col.getData().length)
                {
                    deleteFile(col.getFileName());

                    Collection newCol = new Collection(newName, newSize, col);

                    FileOutputStream fos = openFileOutput(newCol.getFileName(), MODE_PRIVATE);
                    success = newCol.save(fos);
                    fos.close();
                }
            }
            else
            {
                fis.close();
            }
        }
        catch(IOException e)
        {
        }

        if(success)
        {
            updateList();
        }
    }

    private void backupCollections()
    {
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state))
        {
            File destDir = new File(Environment.getExternalStorageDirectory(), BACKUP_PATH);
            if(!destDir.exists())
            {
                if(!destDir.mkdirs())
                {
                    Utils.showToaster(this, R.string.backup_can_t_create);
                    return;
                }
            }

            File srcDir = getFilesDir();
            if(srcDir != null && srcDir.exists())
            {
                File[] files = srcDir.listFiles();
                if(files != null)
                {
                    int nbFilesToBackup = 0;
                    int nbBackups = 0;

                    for(File f : files)
                    {
                        if(f.isFile() && Collection.match(f.getName()))
                        {
                            nbFilesToBackup++;

                            File file = new File(destDir, f.getName());

                            if(Utils.copyFile(f, file))
                            {
                                nbBackups++;
                            }
                        }
                    }

                    int textId = nbBackups == nbFilesToBackup ? R.string.backup_completed
                            : R.string.backup_error;
                    Utils.showToaster(this, textId);
                }
            }
        }
        else
        {
            Utils.showToaster(this, R.string.backup_can_t_access);
        }
    }

    private void listRestorableCollections()
    {
        List<String> choices = getRestorableList();

        if(!choices.isEmpty())
        {
            String[] choiceStrings = new String[choices.size()];
            boolean[] defaultValues = new boolean[choices.size()];

            choices.toArray(choiceStrings);
            for(int i = 0; i < defaultValues.length; i++)
            {
                defaultValues[i] = true;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.restore_dialog_title);
            builder.setMultiChoiceItems(choiceStrings, defaultValues,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton,
                                boolean isChecked)
                        {
                        }
                    });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    AlertDialog dlg = (AlertDialog) dialog;
                    ListView lv = dlg.getListView();
                    restoreCollections(lv);
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            AlertDialog dlg = builder.create();
            dlg.setOwnerActivity(this);
            dlg.show();
        }
    }

    private List<String> getRestorableList()
    {
        List<String> result = new ArrayList<String>();

        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state))
        {
            File backupDir = new File(Environment.getExternalStorageDirectory(), BACKUP_PATH);
            File[] files = backupDir.listFiles();
            if(files != null)
            {
                java.text.DateFormat df = DateFormat.getDateFormat(this);
                java.text.DateFormat tf = DateFormat.getTimeFormat(this);

                for(File f : files)
                {
                    if(f.isFile() && Collection.match(f.getName()))
                    {
                        StringBuilder sb = new StringBuilder();
                        sb.append(Collection.getCollectionName(f.getName()));
                        sb.append("\n(");
                        Date d = new Date(f.lastModified());
                        sb.append(df.format(d));
                        sb.append(" ");
                        sb.append(tf.format(d));
                        sb.append(")");
                        result.add(sb.toString());
                    }
                }
            }
        }

        return result;
    }

    private void restoreCollections(ListView lv)
    {
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state))
        {
            File backupDir = new File(Environment.getExternalStorageDirectory(), BACKUP_PATH);
            File restoreDir = getFilesDir();
            int nbToRestore = 0;
            int nbRestored = 0;
            for(int i = 0; i < lv.getCount(); i++)
            {
                if(lv.isItemChecked(i))
                {
                    nbToRestore++;
                    String s = (String) lv.getItemAtPosition(i);
                    int pos = s.indexOf('\n');
                    String name = Collection.getFileName(s.substring(0, pos));
                    File in = new File(backupDir, name);
                    File out = new File(restoreDir, name);
                    if(Utils.copyFile(in, out))
                    {
                        nbRestored++;
                    }
                }
            }
            
            updateList();
            
            if(nbRestored == nbToRestore)
            {
                Utils.showToaster(this, R.string.restore_completed);
            }
            else
            {
                Utils.showToaster(this, R.string.restore_error);
            }
        }
    }

    private void updateList()
    {
        List<String> filenames = new ArrayList<String>();

        File dir = getFilesDir();
        if(dir != null && dir.exists())
        {
            File[] files = dir.listFiles();
            if(files != null)
            {
                for(File f : files)
                {
                    if(f.isFile())
                    {
                        String filename = f.getName();
                        if(Collection.match(filename))
                        {
                            filenames.add(filename.substring(0, filename.length() - 4));
                        }
                    }
                }
            }
        }

        setListAdapter(new ArrayAdapter<String>(this, R.layout.collections_row, filenames));
    }
}
