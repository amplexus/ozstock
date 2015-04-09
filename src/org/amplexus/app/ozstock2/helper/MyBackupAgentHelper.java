package org.amplexus.app.ozstock2.helper;

import java.io.IOException;

import org.amplexus.app.ozstock2.MainActivity;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/**
 * A backup agent helper for backing up our database.
 * 
 * References:
 * - http://stackoverflow.com/questions/3952863/android-2-2-data-backup-how-to-backup-defaultsharedpreferences
 * - http://stackoverflow.com/questions/5282936/android-backup-restore-how-to-backup-an-internal-database
 * - http://developer.android.com/guide/topics/data/backup.html
 * - https://developers.google.com/android/backup/
 * - http://developer.android.com/resources/samples/BackupRestore/src/com/example/android/backuprestore/FileHelperExampleAgent.html
 * 
 * @author craig
 *
 */
public class MyBackupAgentHelper extends BackupAgentHelper {
	private static final String TAG = MyBackupAgentHelper.class.getSimpleName() ;

    static final String DATABASE_BACKUP_KEY	= "ozstock.database.backup.key";
    static final String PREFS_BACKUP_KEY	= "ozstock.prefs.backup.key";
    
    @Override
    public void onCreate() {

    	Log.i(TAG, "onCreate(): adding shared prefs helper") ;
        /*
    	 * The default shared preference filename is "<packagename>_preferences.xml". Don't include the ".xml".
    	 */
    	SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, MainActivity.class.getPackage().getName() + "_preferences") ;
        addHelper(PREFS_BACKUP_KEY, helper);
        
        /*
         * When backing up database files, the directory location must be relative to getFilesDir() - hence ../databases/...
         */

        // This might be useful in place of the hardcoded path below
        String dbPath = getApplicationContext().getDatabasePath(DatabaseOpenHelper.DATABASE_NAME).getAbsolutePath() ;

    	Log.i(TAG, "onCreate(): adding db helper") ;
        FileBackupHelper fileHelper = new FileBackupHelper(this, "../databases/" + DatabaseOpenHelper.DATABASE_NAME) ;
        addHelper(DATABASE_BACKUP_KEY, fileHelper);
    }
    
    /**
     * We want to ensure that the UI is not trying to rewrite the data file
     * while we're reading it for backup, so we override this method to
     * supply the necessary locking.
     */
    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
             ParcelFileDescriptor newState) throws IOException {
    	
    	Log.i(TAG, "onBackup(): starts") ;

        // Hold the lock while the FileBackupHelper performs the backup operation
//        synchronized (BackupRestoreActivity.sDataLock) {
            super.onBackup(oldState, data, newState);
//        }
       	Log.i(TAG, "onBackup(): ends") ;
    }

    /**
     * Adding locking around the file rewrite that happens during restore is
     * similarly straightforward.
     */
    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
            ParcelFileDescriptor newState) throws IOException {
    	Log.i(TAG, "onRestore(): starts") ;

        // Hold the lock while the FileBackupHelper restores the file from
        // the data provided here.
//        synchronized (BackupRestoreActivity.sDataLock) {
            super.onRestore(data, appVersionCode, newState);
//        }
       	Log.i(TAG, "onRestore(): ends") ;
    }
}
