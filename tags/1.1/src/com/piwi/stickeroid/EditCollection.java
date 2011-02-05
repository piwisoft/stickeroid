package com.piwi.stickeroid;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class EditCollection extends Activity implements OnClickListener, PropertyChangeListener
{
    public final static String NAME_EXTRA = "NAME";

    public final static String POSITION_KEY = "POSITION";

    public final static String FILTER_KEY = "FILTER";

    private Collection mCollection;

    private TextView mDetails;

    private StickersView mStickersView;

    private Button mMinusButton;

    private Button mPlusButton;

    private int mSelectedFilter;

    private boolean mModified;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_collection);

        mCollection = null;
        mModified = false;

        Bundle extras = getIntent().getExtras();
        if(extras != null)
        {
            String name = extras.getString(NAME_EXTRA);
            
            String format = getApplicationContext().getString(R.string.edit_collection_title);
            setTitle(String.format(format, name));

            mCollection = new Collection(name);

            try
            {
                FileInputStream fis = openFileInput(mCollection.getFileName());
                if(mCollection.load(fis))
                {
                    init(savedInstanceState);
                }
            }
            catch(IOException e)
            {
                mCollection = null;
            }
        }

        if(mCollection == null)
        {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item)
    {
        switch(item.getItemId())
        {
        case R.id.edit_menu_filters:
            String[] items = new String[3];
            items[0] = getResources().getString(R.string.filter_none);
            items[1] = getResources().getString(R.string.filter_missing);
            items[2] = getResources().getString(R.string.filter_duplicated);

            mSelectedFilter = -1;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.filter_choice);
            builder.setSingleChoiceItems(items, mStickersView.getFilterType(),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item)
                        {
                            mSelectedFilter = item;
                        }
                    });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    if(mSelectedFilter != -1)
                    {
                        mStickersView.setFilterType(mSelectedFilter);
                    }
                    dialog.cancel();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    dialog.cancel();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onClick(View v)
    {
        if(v == mMinusButton)
        {
            mStickersView.decrement();
        }
        else if(v == mPlusButton)
        {
            mStickersView.increment();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        mMinusButton.setEnabled(mStickersView.canDecrement());
        mPlusButton.setEnabled(mStickersView.canIncrement());

        updateDetails();

        mModified = true;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        saveState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putInt(POSITION_KEY, mStickersView.getPosition());
        outState.putInt(FILTER_KEY, mStickersView.getFilterType());
    }

    private void init(Bundle savedInstanceState)
    {
        updateWidgets();

        int pos = 0;
        int filter = Collection.NO_FILTER;
        if(savedInstanceState != null)
        {
            pos = savedInstanceState.getInt(POSITION_KEY, pos);
            filter = savedInstanceState.getInt(FILTER_KEY, filter);
        }
        
        mStickersView.init(mCollection.getData(), pos, filter);
    }

    private void updateDetails()
    {
        int total = mCollection.getData().length;
        int owned = total - mStickersView.getNbMissing();
        StringBuilder sb = new StringBuilder();
        sb.append(owned);
        sb.append('/');
        sb.append(total);
        sb.append(" M: ");
        sb.append(mStickersView.getNbMissing());
        sb.append(" D: ");
        sb.append(mStickersView.getNbDuplicated());
        sb.append(" (");
        sb.append(mStickersView.getTotalNbDuplicated());
        sb.append(')');
        mDetails.setText(sb.toString());
    }

    private void updateWidgets()
    {
        mDetails = (TextView) findViewById(R.id.details);

        mStickersView = (StickersView) findViewById(R.id.stickers);

        mMinusButton = (Button) findViewById(R.id.minusBtn);
        mPlusButton = (Button) findViewById(R.id.plusBtn);

        mMinusButton.setOnClickListener(this);
        mPlusButton.setOnClickListener(this);

        mStickersView.addPropertyChangeListener(this);
    }

    private void saveState()
    {
        if(mModified && mCollection != null)
        {
            try
            {
                FileOutputStream fos = openFileOutput(mCollection.getFileName(), MODE_PRIVATE);
                mCollection.save(fos);
                mModified = false;
            }
            catch(IOException e)
            {
            }
        }
    }
}
