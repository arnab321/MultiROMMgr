package com.tassadar.multirommgr;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class BackupsActivity extends BackupsActivityBase
{
	int lastPos=0;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        m_active_in_list = true;
        super.onCreate(savedInstanceState);
        getListView().setOnItemLongClickListener(m_longClick);
    }
    
    @Override
    public boolean onPrepareOptionsMenu (Menu menu)
    {
        return prepareMenu(menu);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return prepareMenu(menu);
    }
    
    private boolean prepareMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        menu.clear();
        inflater.inflate(R.menu.backups_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        switch(item.getItemId())
        {
            case R.id.menu_reload:
                if(getListView().isEnabled())
                    LoadBackups();
                return true;
            case R.id.menu_move_act:
                if(getListView().isEnabled())
                    MoveActToBack();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onListItemClick (ListView l, View v, int position, long id)
    {
        if(m_folder_backups == null)
        {
            ShowToast(getResources().getString(R.string.backups_wait));
            return;
        }

        if(position==0)
        	return;
        
        String name = (String) ((TextView)v.findViewById(R.id.title)).getText();

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.back_dialog, (ViewGroup)findViewById(R.id.back_dialog));
        ((TextView)layout.findViewById(R.id.text)).setText(name);
        ((TextView)layout.findViewById(R.id.text_orig)).setText(name);

        new AlertDialog.Builder(this)
        .setView(layout)
        .setNeutralButton(getResources().getString(R.string.rename), new DialogInterface.OnClickListener()
        {
            public void onClick(final DialogInterface d, int arg1)
            {
                String name_from = ((TextView)((AlertDialog)d).findViewById(R.id.text_orig)).getText().toString();
                String name = ((TextView)((AlertDialog)d).findViewById(R.id.text)).getText().toString();
                
                if(name.equals("") || !name.matches("^[a-zA-Z0-9-_#|]+$"))
                {
                    ShowToast(getResources().getString(R.string.wrong_name));
                    return;
                }
                m_backLoading.sendEmptyMessage(2);
                RenameThread r = new RenameThread(name_from, name);
                r.start();
            }
        })
        .setTitle(getResources().getString(R.string.rename_back))
        .setCancelable(true)
        .show();
    }
    
    private final OnItemLongClickListener m_longClick = new OnItemLongClickListener()
    {
        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View v, int pos, long id) {
            lastPos=pos;
            m_selectedBackup = ((TextView)v.findViewById(R.id.title)).getText().toString();
            final CharSequence[] items =// getResources().getStringArray(R.array.backup_options);
            	{pos==0?"Move to backup":"Switch with active", "Erase"};
            new AlertDialog.Builder(con).
            setTitle(R.string.select).
            setItems(items, m_onOptionsClick).
            show();
            return false;
        }
        
    };
    
    private final OnClickListener m_onOptionsClick = new OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch(which)
            {
                case 0:
                	if(lastPos==0)
                		MoveActToBack();
                	else
                    SwitchWithActive();
                    break;
                case 1:
                    Erase();
                    break;
            }
        }
    };

    private void MoveActToBack()
    {
        if(!m_activePresent)
        {
            ShowToast(getResources().getString(R.string.no_active));
            return;
        }
        ShowLoading(getResources().getString(R.string.working));
        new Thread(new Runnable() {
            public void run() {
                String res =
                        MultiROMMgrActivity.runRootCommand("mv " + m_folder_main + " " + m_folder_backups + getNewBackName());
                m_backLoading.sendMessage(m_backLoading.obtainMessage(4, res != null && res.equals("") ? 0 : -1, 0));
            }
        }).start();
    }
    
    private void Erase()
    {
        ShowLoading(getResources().getString(R.string.working));
        new Thread(new Runnable() {
            public void run() {
                String res = MultiROMMgrActivity.runRootCommand("rm -r " + 
            (lastPos==0? m_folder_main:
            (m_folder_backups + m_selectedBackup)) );
                m_backLoading.sendMessage(m_backLoading.obtainMessage(3, res != null && res.equals("") ? 1 : 0, 0));
            }
        }).start();
    }

    private void SwitchWithActive()
    {
        if(!m_activePresent)
            ShowToast(getResources().getString(R.string.main_not_found));

        ShowLoading(getResources().getString(R.string.working));
        new Thread(new Runnable() {
            public void run() {
                String res;
                if(m_activePresent)
                {
                    res = MultiROMMgrActivity.runRootCommand(
                             "mv " + m_folder_main + " " + m_folder_backups + getNewBackName());
                    if(res == null || !res.equals(""))
                    {
                        send(-1);
                        return;
                    }
                }
                else
                {
                    res = MultiROMMgrActivity.runRootCommand("rm -r " + m_folder_main);
                    if(res == null || (!res.equals("") && !res.contains("No such file")))
                    {
                        send(-3);
                        return;
                    }
                }
                
                res = MultiROMMgrActivity.runRootCommand(
                        "mv " + m_folder_backups + m_selectedBackup + " " + m_folder_main);
                if(res == null || !res.equals(""))
                {
                    send(-2);
                    return;
                }
                send(0);
            }
            private void send(int res)
            {
                m_backLoading.sendMessage(m_backLoading.obtainMessage(4, res, 0));
            }
        }).start();
    }
    
    private class RenameThread extends Thread
    {
        private String m_f;
        private String m_t;

        public RenameThread(String from, String to)
        {
            m_f = from;
            m_t = to;
        }
        
        public void run()
        {
            String res = MultiROMMgrActivity.runRootCommand("mv " + m_folder_backups + m_f + " " + m_folder_backups + m_t);
            m_backLoading.sendMessage(m_backLoading.obtainMessage(1, res != null && res.equals("") ? 1 : 0, 0));
        }
    }

    //private AlertDialog m_renameDial;
}
