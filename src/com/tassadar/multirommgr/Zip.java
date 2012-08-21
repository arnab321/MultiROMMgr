package com.tassadar.multirommgr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
	
public class Zip extends AsyncTask<String, String, Integer> {
	    // 4MB buffer
	    private static byte[] BUFFER = new byte[4096 * 1024];
	    private static String updaterScript="META-INF/com/google/android/updater-script",
	    		bootImage="boot.img";
	    
	    String[] problem_causing_lines={
	    		"mount(",
	    		"unmount("/*,
	    		"write_raw_image(",
	    		"package_extract_file(\"boot.img\"",
	    		"package_extract_file(\"system/bin/backuptool.",
	    		"set_perm(0, 0, 0777, \"/tmp/backuptool.sh\")",
	    		"set_perm(0, 0, 0644, \"/tmp/backuptool.functions\")",
	    		"run_program(\"/tmp/backuptool"
	    		*/
	    };
	    		
	    ZipFile zfile;
	    ZipOutputStream output;
		ProgressBar p;
		Button save;
		
		final int HIDE_SAVE=1;
		final int SHOW_SAVE=2;
		
		EditText edit;
		TextView m;
	    int read=0;
	    boolean copied=false,editDone=false,doneUpdScr=false,bootImageFound=false;
		String outFileName;
	    
		protected void onPreExecute (){

		}
	    
	    /**
	     * copy input to output stream - available in several StreamUtils or Streams classes 
	     */    
	    private void copy(InputStream input, OutputStream output) throws IOException {
	        int bytesRead;
	        while ((bytesRead = input.read(BUFFER))!= -1) {
	            output.write(BUFFER, 0, bytesRead);
	        }
	    }

		@Override
		protected Integer doInBackground(String... path) {
	    	try{
	        zfile = new ZipFile(path[0]);
	        publishProgress("max",zfile.size()+"");
	        
	        String folder=path[0].substring(0,path[0].lastIndexOf("/")+1);
	        String file=path[0].substring(path[0].lastIndexOf("/")+1);
	       outFileName=folder+"mrom-"+file;
	        
	         output = new ZipOutputStream(new FileOutputStream(outFileName));

	        // first, copy contents from existing war
	        Enumeration<? extends ZipEntry> entries = zfile.entries();
	        while (entries.hasMoreElements()) {
	            ZipEntry e = entries.nextElement();
	            if (e.getName().equals(updaterScript)){
	            	openScript(zfile.getInputStream(e));
	            }
	            else if (e.getName().equals(bootImage)){
	            	bootImageFound=true;
	            	File f = new File(Updater.BACKUP_PATH);
	        		if (!f.exists())
	        		    f.mkdir();
	        		f=new File(Updater.TMP_BOOT_IMAGE_PATH);
	        		FileOutputStream tmp = new FileOutputStream(Updater.TMP_BOOT_IMAGE_PATH);

		            copy(zfile.getInputStream(e), tmp);
		            tmp.close();
		            
	        		} else{
	            	output.putNextEntry(e);
	            if (!e.isDirectory()) 
	                copy(zfile.getInputStream(e), output);
	            }
	            Log.i("copy", e.getName());
	            output.closeEntry();
	            read++;
	            publishProgress(null,read+"");
	        }


	    	}catch(IOException e){
	    		publishProgress("msg","Error");
	    		e.printStackTrace();
	    		close();
	    		return -1;
	    	}
			return 0;
			
		}
		void close(){
	        // close
			try{
	        zfile.close();
	        output.close();
			}catch(Exception e){}
			finally{
				BUFFER=null;
			}
	        Log.e("copy", "CLOSED");
		}
		@Override
	    protected void onProgressUpdate(String... val) {
			//Log.e("t "+val[0],val[1]);
			
			if(val[0]==null)
				p.setProgress(Integer.parseInt(val[1]));
			else if(val[0].equals("max"))
				p.setMax(Integer.parseInt(val[1]));
			else if(val[0].equals("msg"))
				m.setText(val[1]);
			else if(val[0].equals("edit")){
				edit.setVisibility(m.VISIBLE);
				edit.setText(val[1]);
				save.setVisibility(m.VISIBLE);
				m.setText("Preview of updater script\n(If you are stupified, press SAVE)");
			}
			else if(val[0].equals("Fedit")){
				edit.setVisibility(m.INVISIBLE);
				save.setVisibility(m.GONE);
				m.setText("Processing...");
			}
			else if(val[0].equals("edit2")){
				edit.setVisibility(m.VISIBLE);
				edit.setText(val[1]);
			}
			else if(val[0].equals("butn")){
				save.setVisibility(m.VISIBLE);
				save.setText(val[1]);
			}
	}
		
	    private void openScript(InputStream is) throws IOException{
	    	String sb="",
	    outp="";
	    	
	        int result;
	        while((result = is.read()) != -1) {
	          sb+=(char)result;
	        }        
	        
	        String[] statements=sb.split(";");
	        for(String statement:statements){
	        	statement=statement.trim();
	        	
	        		if(statement.startsWith("assert(")){
	        	/*		
	        			String[] asserts=statement.substring(7,statement.length()-1).split(",");
	        			statement="assert(";
	        			 for(String astatement:asserts){
	        				astatement=astatement.trim();
				        	astatement=filter(astatement);
				        	if(astatement!=null)
				        		statement+=astatement+", ";
	        			 }
	        			 statement+=")";
	        	*/}else	 
	        		
	        		statement=filter(statement);
	        		
	        		if(statement!=null)
	        			outp+=statement+";\n";
	        	
	        }
	        
	    	publishProgress("edit",outp);	    	
	    }
	    
	    String filter(String statement){
	    	String out=statement;
        	for(String prefix:problem_causing_lines){
        		if(statement.startsWith(prefix)){
        			out=null;
        			break;
        		}
        	}
        	
        	if(out!=null){
        		if(statement.startsWith("format(")){
        			if(statement.contains("system"))
        				out="run_program(\"/sbin/busybox\",\"rm\", \"-rf\", \"/system/*\")";
        			else if(statement.contains("cache"))
	        			out="run_program(\"/sbin/busybox\",\"rm\", \"-rf\", \"/cache/*\")";
        			else out=null;
        		}else if (statement.startsWith("run_program(") &&
        				statement.contains("busybox") &&(
        				statement.contains("mount")||
        				statement.contains("format")||
        				statement.contains("unmount")
        				))
        			out=null;
        		
        	}
        	
        	return out;
	    }
	    
	public void save(){
		publishProgress("Fedit","-");
		editDone=true;
		if(copied)
			append();
	    }
	
	private synchronized void append(){
		if(doneUpdScr)
			return;
    	try {
    	ZipEntry upd = new ZipEntry(updaterScript);
    	output.putNextEntry(upd);
        
/* header */ output.write((ZipActivity.c.getString(R.string.mod_unmount_script)+ZipActivity.c.getString(R.string.mod_header_script)).getBytes());
/* script */ output.write(edit.getText().toString().getBytes());
/* footer */ output.write(ZipActivity.c.getString(R.string.mod_unmount_script).getBytes());
    	output.closeEntry();
    	if(bootImageFound)
    		publishProgress("msg","...");
    	else
    		publishProgress("msg","Done!");
    	
    	}catch(IOException e){
    		publishProgress("msg","Error: "+e.toString());
    	}

    	if(bootImageFound){
    		publishProgress("butn","Embed multirom into boot.img");
    	}else{
    	close();
    	publishProgress("edit2","\nSaved to "+outFileName);
    	
    	}
    	doneUpdScr=true;
	}
	
	@Override
	protected void onCancelled (){
			close();
			File file=new File(outFileName);
			file.delete();
			Toast.makeText(ZipActivity.c, "Cancelled.", Toast.LENGTH_LONG).show();
		}
		
		@Override
	    protected void onPostExecute(Integer a) {
			if(a==-1)
				return;
			if(editDone)
				append();
			copied=true;
		}

		public synchronized void appendBoot() {
			// TODO Auto-generated method stub
			ZipEntry bt = new ZipEntry(bootImage);
			try{
	    	output.putNextEntry(bt);
	    	FileInputStream fi=new FileInputStream(Updater.TMP_BOOT_IMAGE_PATH);
	        
		    copy(fi,output);
	    	output.closeEntry();
	    	publishProgress("msg","Done!");
			}catch(IOException e){
				publishProgress("msg","Error: "+e.toString());
			}
	    	
	    	close();
	    	publishProgress("edit2","\nEmbedded modified boot.img into ROM zip.\n\nSaved to "+outFileName);
		}
		
	}


