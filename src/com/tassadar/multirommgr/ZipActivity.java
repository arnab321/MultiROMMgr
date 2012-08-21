package com.tassadar.multirommgr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ZipActivity  extends Activity {
	static Zip zip;
	static boolean mod=false;
	Intent i;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Zip.act=this;
        
       if(!mod && getIntent().getStringExtra("path")!=null){
        	
       setContentView(R.layout.zip);       
       zip= new Zip();
       zip.p=(ProgressBar) findViewById(R.id.zip_progress);
       zip.edit=(EditText) findViewById(R.id.updater_script);
       zip.m=(TextView) findViewById(R.id.zip_msg);
       zip.save= (Button) findViewById(R.id.save);
       zip.execute(getIntent().getStringExtra("path"));
       mod=true;
        }
    }
    
    public void append(View v){
    	if(zip.doneUpdScr){
    		i=new Intent(this, Updater.class);
    		
    		new AlertDialog.Builder(this)
    		.setTitle("multirom.zip")
    		.setItems(new String[]{"I have a copy on SDcard","Download from internet"}, 
                   	  new android.content.DialogInterface.OnClickListener()
                   	    {
                   	        @Override
                   	        public void onClick(DialogInterface arg0, int arg1) {
                   	        	i.putExtra("tmp_boot_img", arg1);
                   	        	if(arg1==0)
                   	        		MultiROMMgrActivity.showFileChooser("Select multirom.zip", ZipActivity.this);
                   	        	else
                   	        		startActivity(i);
                   	        }
                   	    })
                   	 .setNegativeButton("Cancel", new android.content.DialogInterface.OnClickListener(){

						@Override
						public void onClick(DialogInterface dialog, int which) {
							zip.close();
						}
                   		 
                   	 })
                    .setCancelable(false)
                    .show();
    		
    		
    	}else
    		zip.save();
    }
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
         if (requestCode == MultiROMMgrActivity.FILE_SELECT_CODE && resultCode == RESULT_OK){
            Uri uri = data.getData();
            Log.i("url",uri.getScheme()+" "+uri.getPath());
            
            String path ="";
            if ("file".equalsIgnoreCase(uri.getScheme()) || "content".equalsIgnoreCase(uri.getScheme()))
                path= uri.getPath();

            else return;

            i.putExtra("path", path);
            startActivity(i);
        
        }
        
    }
    @Override
    public void onBackPressed()
    {
    	if(!zip.doneUpdScr){
    		zip.close();
    		zip.cancel(true);
    	}
    	super.onBackPressed();
    }
}
