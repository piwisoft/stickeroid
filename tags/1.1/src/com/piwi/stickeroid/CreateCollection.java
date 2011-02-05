package com.piwi.stickeroid;

import java.io.FileInputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class CreateCollection extends Activity implements OnClickListener, TextWatcher
{
    public static final String COLLECTION_NAME = "NAME";

    public static final String OLD_COLLECTION_NAME = "OLD_NAME";

    public static final String COLLECTION_SIZE = "SIZE";

    private EditText mNameEditText;

    private EditText mSizeEditText;

    private Button mConfirmButton;

    private String mOldName;

    private int mSize;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_collection);

        mNameEditText = (EditText) findViewById(R.id.name);
        mSizeEditText = (EditText) findViewById(R.id.size);

        mNameEditText.addTextChangedListener(this);
        mSizeEditText.addTextChangedListener(this);

        mConfirmButton = (Button) findViewById(R.id.confirm);
        mConfirmButton.setOnClickListener(this);
        mConfirmButton.setEnabled(false);

        Bundle extras = getIntent().getExtras();
        if(extras != null)
        {
            mOldName = extras.getString(COLLECTION_NAME);
            if(mOldName != null)
            {
                setTitle(R.string.modify_collection_title);
                mNameEditText.setText(mOldName);
                mConfirmButton.setText(R.string.modify_collection_confirm);
                try
                {
                    Collection col = new Collection(mOldName);
                    FileInputStream fis = openFileInput(col.getFileName());
                    if(col.load(fis))
                    {
                        int size = col.getData().length;
                        mSizeEditText.setText(Integer.toString(size));
                    }
                    fis.close();
                }
                catch(IOException e)
                {
                }
            }
        }
    }

    @Override
    public void onClick(View v)
    {
        Bundle bundle = new Bundle();

        bundle.putString(COLLECTION_NAME, mNameEditText.getText().toString());
        bundle.putString(OLD_COLLECTION_NAME, mOldName);
        bundle.putInt(COLLECTION_SIZE, mSize);

        Intent i = new Intent();
        i.putExtras(bundle);
        setResult(RESULT_OK, i);

        finish();
    }

    @Override
    public void afterTextChanged(Editable s)
    {
        mConfirmButton.setEnabled(checkValidity());
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count)
    {
    }

    private boolean checkValidity()
    {
        if(mNameEditText.getText().length() == 0)
        {
            return false;
        }

        Editable ed = mSizeEditText.getText();

        if(ed.length() == 0)
        {
            return false;
        }

        try
        {
            mSize = Integer.valueOf(ed.toString());
            if(mSize <= 0)
            {
                return false;
            }
        }
        catch(NumberFormatException e)
        {
            return false;
        }

        return true;
    }
}
