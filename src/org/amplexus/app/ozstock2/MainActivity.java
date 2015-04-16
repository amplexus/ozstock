package org.amplexus.app.ozstock2;

import java.util.ArrayList;
import java.util.Map;

import org.amplexus.app.ozstock2.helper.ASXHelper;
import org.amplexus.app.ozstock2.helper.BusinessLogicHelper;
import org.amplexus.app.ozstock2.helper.DatabaseHelper;
import org.amplexus.app.ozstock2.helper.DatabaseOpenHelper;
import org.amplexus.app.ozstock2.values.Holding;
import org.amplexus.app.ozstock2.values.Portfolio;
import org.amplexus.app.ozstock2.values.StockRef;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

/**
 * The main activity class for the OzStock application.
 * 
 * Displays each portfolio as a named icon. The portfolios can be added, deleted, renamed and their holdings edited.
 * 
 * Supports an options menu for adding new portfolios, refreshing stock prices, editing preferences, downloading stock codes from the ASX and an "About" dialogue.
 * 
 * Supports a long click context menu for renaming and deleting a selected portfolio or viewing its holdings.
 */

 /* ======== 
 * PATTERNS 
 * ========
 * 
 * ViewHolder Pattern 
 * ------------------
 * 
 * The ViewHolder pattern consists in storing a data structure in the tag of the 
 * view returned by getView(). This data structures contains references to the
 * views we want to bind data to, thus avoiding calls to findViewById() every
 * time getView() is invoked.
 * 
 * Combine this approach with utilising the adapter getView's convertView for
 * super efficient list processing.
 * 
 * Collapsed ListAdapter Pattern 
 * -----------------------------
 * 
 * Create the list item XML defining the summary line's visibility as
 * View.VISIBLE and the detail line(s) as View.GONE.
 * 
 * Define a onListItemClick listener that toggles the visibility (setVisible).
 * The current state can be stored in the ViewHolder...
 * 
 * Section Header / Separator Pattern 
 * ----------------------------------
 * 
 * In the adapter's isEnabled(int position) method, conditionally set to false
 * if this item is a section header. This means treating the section header as a
 * discrete list item, which implies the list / cursor should return a section
 * header element embedded in the collection.
 * 
 * SimpleCursorAdapter.ViewBinder 
 * ------------------------------
 * 
 * The SimpleCursorAdapter maps columns from a cursor to TextViews / ImageViews
 * in an XML file. If any of the views in the XML file are not TextView /
 * ImageView, or if any of the columns in the cursor are not appropriate for
 * mapping in their default format, then use the viewBinder to map them.
 * 
 * Expandable Lists Using Cursors 
 * ------------------------------
 * 
 * The activity extends ExpandableListActivity. The onCreate() method
 * instantiates a QueryHandler for the top-level grouping, and a custom adapter
 * that extends SimpleCursorTreeAdapter. The adapter executes the query for leaf
 * nodes.
 * 
 * Fragment Pattern 
 * ----------------
 * 
 * Extend Fragment. Inflate the fragment's view in its onCreateView(). Declare
 * the <fragment> inside the activitie's layout file - this can be quite
 * nuanced, for example a news paper app might display an article list and
 * article details:
 * 
 * - layout/newspaper.xml could be for phones and so would have a single article list fragment. The article details could be displayed via a separate article details activity. 
 * - layout-land/newspaper.xml could be for phones, but may have an article list fragment on the left, and an article detail fragment on the right. 
 * - layout-wxga/newspaper.xml could be for 10" tablets, and would have an article list fragment in the top half, and an article detail fragment in the bottom half. 
 * - layout-land-wxga/newspaper.xml could be for 10" tablets, and would have an article list fragment in the left half, and an article details fragment in the right half. 
 * - layout-wsvga/newspaper.xml could be for 7" tablets, and would have an article list fragment in the top half, and an article detail fragment in the bottom half. 
 * - layout-land-wsva/newspaper.xml could be for 7" tablets, and would have an article list fragment in the left half, and an article details fragment in the right half.
 * 
 * To share info between fragments (ie to tell the detail fragment what list
 * item in the list fragment was selected):
 * 
 * - Have the list fragment define an interface "on<Something>SelectedListener" that defines a method "on<Something>Selected(Uri something). 
 * - Have the *activity* implement the above interface 
 * - The list fragment can grab a reference to the activity's listener implementation in its onAttach(Activity) method 
 * - just cast the activity to the interface 
 * - ie: mListener = (On<Someting>SelectedListener)activity. 
 * - The list activity's onListItemClick() method invokes the activity's on<Something>Selected() method, which was captured in the previous step. 
 * - The activity's on<Something>Selected() method would finally pass an intent to the detail fragment that contains the details on what was selected.
 * 
 * NOTES
 * 
 * - top level activities swallow backstack
 * - Look at the following samples: 
 * o Views -> Lists -> Efficent Adapter (ViewHolder): com.example.android.apis.view.List14 (layout/list_item_icon_text.xml) 
 * o Views -> Lists -> List Adapter Collapsed (6) 
 * o Views -> Layout -> Table Layout -> Simple Form: com.example.android.apis.view.TableLayout10 (layouts/table_layout_10.xml) 
 * o Views -> Lists -> Separators (5): o Views -> Lists -> Cursor Phones (3) 
 * o Views -> Expandable Lists -> Custom Adapter (1) o App -> Fragment -> Layout:com.example.android.apis.app.FragmentLayout (layouts/fragment_layout.xml) 
 * o Views -> Grid -> IconGrid (1) o App -> Action Bar -> Action Bar Mechanics 
 * o App -> Activity -> Persistent State o App -> Notifications -> Status Bar 
 * o App -> Alarm -> Alarm Service and Alarm Controller o Preferences -> Advanced Preferences
 * 
 * - Custom objects in listview: 
 * o http://www.josecgomez.com/2010/05/03/android-putting-custom-objects-in-listview/ 
 * o http://www.softwarepassion.com/android-series-custom-listview-items-and-adapters/ 
 * o http://nickcharlton.net/post/building-custom-android-listviews
 * 
 * - SQLLite: http://www.sqlite.org/
 * 
 * Emulator Shortcuts: 
 * - Home = home screen 
 * - Control-F11 = previous layout 
 * - Control-F12 = next layout 
 * - ESC = back 
 * - F2 = menu 
 * - F5 = search 
 * - F8 = toggle cell networking 
 * - F9 = toggle code profiling (only with -trace startup option)
 * 
 * ============ 
 * PROJECT PLAN 
 * ============
 * 
 * -------------------------------
 * PHASE 1 – Basic Features (free) 
 * -------------------------------
 * 
 * [DONE] 1) AsyncTask for db updates and refresh of ASX companies list – plus add menu option to sync companies list
 * 
 * [DONE] 2) No-wake alarm for price updates – updating all portfolios, not just the currently displayed one
 * 
 * [DONE] 3) Stop refreshing prices every time onCreate is executed - check lastUpdate time in prefs
 * 
 * [DONE] 4) Fix table layout to use 0dip width and weight
 * 
 * [DONE] 6) Be battery state & network aware - depends on #2
 * 
 * [DONE] 7) PortfolioActivity to show portfolio summary at the top
 * 
 * [DONE] 8) Use AutoCompleteTextView with DB CursorAdapter for selecting stock code from reference table
 * 
 * [DONE] 9) Backup / sync to cloud / gmail (TO BE TESTED PROPERLY - need backup key!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!)
 * 
 * 10) [optional] Switch portfolio icongrid to listview with portfolio summary p/l (name, cost, value, p/l $, p/l %)
 * 
 * 11) [optional] Support for moving holdings between portfolios
 * 
 * 12) [optional] Support export to CSV then email
 * 
 * 13) [optional] Preferences screen - refresh interval
 * 
 * 14) [optional] Implement the remote call design pattern outlined at Google I/O 2011
 * 
 * 15) [optional] Swipe mode - portfolio activity and holding activity - http://developer.android.com/reference/android/support/v4/view/ViewPager.html
 * 
 * ---------------------------------
 * PHASE 2 – Advanced Features ($$$) 
 * ---------------------------------
 * 
 * 1) Switch from add / edit / remove holding style to buy / sell / adjust holding style
 * 
 * 2) Financial year summary report – including option to email PDF / CSV
 * 
 * 3) Sell Holding to support allocation from previous buys
 * 
 * 4) [optional] Various transaction support: share purchase plan, dividend, delisting, buyback, stock split, ticker change
 * 
 * -----------------------------------
 * PHASE 3 – Prepare For Google Market 
 * -----------------------------------
 * 
 * 1) Report crash stats to cloud DB
 * 
 * 2) Small app to view crash stats on cloud DB
 * 
 * 3) Tablet support (fragments)
 * 
 * 4) Various screen size support
 * 
 * 5) Split app into free-mode functionality and $$$ mode functionality
 * 
 * 6) Package security
 * 
 * 7) Iconography / Logo
 * 
 * 8) Put all strings in a values file
 * 
 * 9) Setup an account on google market, plus an email address to be used for the account.
 * 
 * TODO: http://developer.android.com/resources/articles/faster-screen-orientation-change.html 
 * 
 * BACKUP HOWTO:
 *
 * - adb shell bmgr backup org.amplexus.app.ozstock2
 * - adb shell bmgr run
 * 
 * MINI TODO:
 * - Transaction ListView - filter by nothing, portfolio id, or portfolio id and holding id
 * - Sell Allocation Activity
 * - P/L Summary ListView - filter by financial year, financial year and portfolio id, or financial year, portfolio id and holding id
 *
 * @author craig
 */
public class MainActivity extends Activity implements OnItemClickListener {

	private static final String TAG = MainActivity.class.getSimpleName();

	public static final String SHARED_PREFS_LAST_PRICE_FILENAME = "LastPrice" ;
	public static final String SHARED_PREFS_LAST_PRICE_TIME_ATTR = "lastUpdateTimeMillis";
	public static final String SHARED_PREFS_PRICE_FETCH_INTERVAL_MINS_ATTR = "priceFetchIntervalMinutes";

	public static final int DEFAULT_PRICE_FETCH_INTERVAL_MINS = 15 ;

	/**
	 * Dialog box unique ids
	 */
	static final int DIALOG_ABOUT = 0;
	static final int DIALOG_PROGRESS_DOWNLOAD_LATEST_STOCK_PRICES = 1;
	static final int DIALOG_PROGRESS_UPDATE_STOCK_REF = 2;
	static final int DIALOG_PROGRESS_DOWNLOAD_LISTED_COMPANIES = 3;
	static final int DIALOG_NEW_PORTFOLIO = 4;
	static final int DIALOG_RENAME_PORTFOLIO = 5;
	static final int DIALOG_DELETE_PORTFOLIO = 6;
	static final int DIALOG_PROGRESS_UPGRADING = 7;

//    private BackupManager mBackupManager ;

    private GridView mGrid ;																	// Display list of portfolios in a grid
	private PortfolioIconAdapter mAdapter ;														// A bridge between the portfolios map and GridView
    public BusinessLogicHelper mBusinessLogic ;													// Business logic layer
	private int mSelectedPosition ;												 			// The currently selected portfolio (for context menu operations)
	private long mAddedPortfolioId ;															// The database id of a portfolio added via the New Portfolio dialog - will be -1 if the dialog is cancelled
	private String mAddedPortfolioName ;														// The name of the portfolio added via the New Portfolio dialog
	ArrayList<StockRef> mStockRefList;															// The cached copy of the STOCK_REF table

	private DownloadListedCompaniesTask mDownloadListedCompaniesTask ;							// an async task for downloading the company list from the ASX website
	private LoadStockRefTableFromDownloadedFileTask mLoadStockRefTableFromDownloadedFileTask ;	// an async task for populating the STOCK_REF table from file
	private LoadStockRefTableFromAssetFileTask mLoadStockRefTableFromAssetFileTask ;			// an async task for populating the STOCK_REF table from file
	private UpgradingDbTask mUpgradingDbTask ;													// an async task for upgrading the database

    private MainFinanceServiceReceiver mFinanceReceiver ;										// The receiver that processes FinanceService stock price updates.
    private AlarmReceiver mAlarmReceiver ;														// The receiver that processes alarms.

    private int mPrefPriceFetchIntervalMinutes ;	// How often we fetch prices from google finance

	private boolean mBatteryOkay = true ;
	private boolean mIsCharging = false ;

    private ASXHelper mAsxHelper ;

	private ArrayList<Portfolio> mPortfolioList;
    
	/**
	 * Called when the activity is first created. This is where you should do
	 * all of your normal static set up to create views, bind data to lists, and
	 * so on. This method is passed a Bundle object containing the activity's
	 * previous state, if that state was captured. Always followed by onStart().
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mGrid = (GridView) findViewById(R.id.portfolioGrid);
		mGrid.setOnItemClickListener(this);

		mBusinessLogic = new BusinessLogicHelper(getApplicationContext());
		mAsxHelper = new ASXHelper(getApplicationContext()) ;

		mStockRefList = new ArrayList<StockRef>() ;
		
		/*
		 * Initialise AsyncTasks for downloading companies list and upgrading database
		 */
		mUpgradingDbTask = new UpgradingDbTask() ;		
		mDownloadListedCompaniesTask = new DownloadListedCompaniesTask() ;
		mLoadStockRefTableFromDownloadedFileTask = new LoadStockRefTableFromDownloadedFileTask() ;
		mLoadStockRefTableFromAssetFileTask = new LoadStockRefTableFromAssetFileTask() ;

		/*
		 * Read the default interval from the prefs
		 */
		Log.i(TAG, "onCreate() read the interval from prefs");
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this) ;
	    mPrefPriceFetchIntervalMinutes = prefs.getInt(SHARED_PREFS_PRICE_FETCH_INTERVAL_MINS_ATTR, DEFAULT_PRICE_FETCH_INTERVAL_MINS) ; 

	    /*
	     * Kick off the periodic stock price refresh
	     */
	    calibrateAlarm() ;

	    /*
		 * Check if db upgrade is required, and check if stock ref table needs seeding.
		 */
	    upgradeAndStockRefSeedCheck() ;
	    
	    /*
	     * Instantiate backup manager 
	     */
//	    mBackupManager = new BackupManager(this);
	    
		/*
		 * Read all portfolios from the database.
		 */
		mPortfolioList = mBusinessLogic.getPortfolios() ;
		Log.i(TAG, "mPortfolioList.size=" + mPortfolioList.size()) ;
		for(int i = 0; i < mPortfolioList.size(); i++)
			Log.i(TAG, "mPortfolioList[" + i + "]=" + mPortfolioList.get(i).getId() + ":" + mPortfolioList.get(i).getPortfolioName()) ;

		/*
		 * Associate GridView with adapter
		 */
		mAdapter = new PortfolioIconAdapter(this, mPortfolioList) ; 
		mGrid.setAdapter(mAdapter);

		Log.i(TAG, "onCreate() done");
	}

	/**
	 * Kicks off the db upgrade check. Once complete, this will kick off the seeding of the STOCK_REF table if required.
	 * 
	 * A database upgrade will be automatically triggered when we call getWritableDb(). As this can be a time consuming process, we don't
	 * want it to occur on the UI thread. So we first getReadableDb() and find out if upgrade is required, and if so, we getWritableDb() in
	 * a separate asynctask.
	 * 
	 * The async task will upon completion check if the STOCK_REF (which is used for the stock code combobox displayed when adding a new 
	 * holding) table needs seeding.
	 * 
	 * Note that even if a database upgrade is not needed, we check if STOCK_REF table needs seeding anyway.
	 * 
	 * We do these async tasks in sequence so their respective progress bars don't obscure each other and because we don't want a database
	 * upgrade happening at the same time as we're seeding STOCK_REF.
	 */
	private void upgradeAndStockRefSeedCheck() {
		DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext()) ;
		boolean needUpgrade = false ;
		SQLiteDatabase db = null ;
		
		/*
		 * Seems that when installing the app first time, even opening a read-only db to check if an upgrade is needed
		 * causes a hissy fit.
		 */
		try {
			db = dbHelper.getReadableDb() ;
			needUpgrade = db.needUpgrade(DatabaseOpenHelper.DATABASE_VERSION) ;
		} catch (RuntimeException e) {
			Log.e(TAG, "upgradeAndStockRefSeedCheck(): got exception checking if db needs upgrade!", e) ;
			needUpgrade = true ;
		} finally {
			if(db != null && db.isOpen())
				db.close() ;
		}
		
		if(needUpgrade) {
			mUpgradingDbTask.execute() ; // NOTE: This will also kick off the loading of the stock ref table from the assets file if necessary
		} else {
			/*
			 * Since no DB upgrade is required, we check if the STOCK_REF table needs to be seeded anyway.
			 */
			mStockRefList = mBusinessLogic.getAllStockRef() ;
			if (mStockRefList.size() == 0) {
				mLoadStockRefTableFromAssetFileTask.execute() ;
			}			
		}
	}

	/**
	 * Instantiate an alarm to trigger stock price refreshes. 
	 * 
	 * The alarm is based on the preferred interval specified in mPrefPriceFetchIntervalMinutes.
	 * 
	 * Uses AlarmManager.setInexactRepeating() because it is more power efficient and we don't need anything more precise.
	 * Uses AlarmManager.ELAPSED_REALTIME so we don't wake the phone if it's sleeping.
	 * 
	 * TODO: use AlarmManager.INTERVAL_???? instead of specifying millis because they are more efficient 
	 */
	private void calibrateAlarm() {
		
	    /*
         * We want an alarm triggered every N minutes so we can refresh stock prices
         */
		int minutes = mPrefPriceFetchIntervalMinutes ;

		/*
		 * But if battery is low and we're not charging, slow down the frequency of updates
		 */
	    if(! mIsCharging && ! mBatteryOkay) {
	    	minutes = 10 * minutes ;
			Log.i(TAG, "calibrateAlarm() battery is low and we're not charging - throttling updates 10 fold") ;
	    } else {
			Log.i(TAG, "calibrateAlarm() battery is okay or charging - NOT throttling") ;
	    }

		Log.i(TAG, "calibrateAlarm() trigger an alarm every " + minutes + " minutes") ;

		int millis = minutes * 60 * 1000 ;

	    AlarmManager alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE) ;
        
        Intent intent = new Intent(AlarmReceiver.RESPONSE_ACTION) ;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        /*
         * WARNING: The below "new Intent(this, AlarmReceiver.class)" below does NOT work with *programatically* registered broadcast receivers
         * PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, AlarmReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT);
         */
        
        alarmMgr.cancel(pendingIntent) ;
        
        /*
         * Use inexact repeating which is easier on battery (system can phase events and not wake at exact times)
         * Add 15 seconds to first alarm, because due to inexact nature, it might fire before the desired threshold and so will be ignored.
         */
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + millis + (15 * 1000), millis, pendingIntent);
        
		Log.i(TAG, "calibrateAlarm() done") ;
	}

	/**
	 * Called just before the activity becomes visible to the user. Followed by
	 * onResume() if the activity comes to the foreground, or onStop() if it
	 * becomes hidden.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "onStart() done");
	}

	/**
	 * Called after the activity has been stopped, just prior to it being
	 * started again. Always followed by onStart().
	 */
	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i(TAG, "onRestart() done");
	}

	/**
	 * Read portfolios from the database, register receivers, register context menu and download stock prices (if required).
	 * 
	 * Called just before the activity starts interacting with the user. At this
	 * point the activity is at the top of the activity stack, with user input
	 * going to it. Always followed by onPause().
	 */
	@Override
	protected void onResume() {
		super.onResume();
		
		registerForContextMenu(mGrid) ;

		/*
		 * We want to listen to any stock price updates from the FinanceService
		 */
        IntentFilter financeServiceFilter = new IntentFilter(FinanceService.ACTION_NOTIFY) ;
        financeServiceFilter.addCategory(Intent.CATEGORY_DEFAULT) ;
        mFinanceReceiver = new MainFinanceServiceReceiver() ;
        registerReceiver(mFinanceReceiver, financeServiceFilter) ;

        /*
         * We want to listen to any wakeup calls from the alarm service
         */
		Log.i(TAG, "onResume() listen for wakeup calls from alarm service");
        IntentFilter alarmFilter = new IntentFilter(AlarmReceiver.RESPONSE_ACTION) ;
        alarmFilter.addCategory(Intent.CATEGORY_DEFAULT) ;
        mAlarmReceiver = new AlarmReceiver() ;
        registerReceiver(mAlarmReceiver, alarmFilter) ;
        
		/*
         * Download latest stock prices
		 */
		refreshPrices(false) ;
		
		Log.i(TAG, "onResume() done");
	}

	/**
	 * Deregister receivers and context menu.
	 * 
	 * Called when the system is about to start resuming another activity. This
	 * method is typically used to commit unsaved changes to persistent data,
	 * stop animations and other things that may be consuming CPU, and so on. It
	 * should do whatever it does very quickly, because the next activity will
	 * not be resumed until it returns. Followed either by onResume() if the
	 * activity returns back to the front, or by onStop() if it becomes
	 * invisible to the user.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		
		unregisterForContextMenu(mGrid) ;
		unregisterReceiver(mAlarmReceiver) ;
		unregisterReceiver(mFinanceReceiver) ;
		Log.i(TAG, "onPause() done");
	}

	/**
	 * Called when the activity is no longer visible to the user. This may
	 * happen because it is being destroyed, or because another activity (either
	 * an existing one or a new one) has been resumed and is covering it.
	 * Followed either by onRestart() if the activity is coming back to interact
	 * with the user, or by onDestroy() if this activity is going away.
	 */
	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "onStop() done");
	}

	/**
	 * Called before the activity is destroyed. This is the final call that the
	 * activity will receive. It could be called either because the activity is
	 * finishing (someone called finish() on it), or because the system is
	 * temporarily destroying this instance of the activity to save space. You
	 * can distinguish between these two scenarios with the isFinishing()
	 * method.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy() done");
	}

	/**
	 * Used to record the transient state of the activity (the state of the UI).
	 * 
	 * The callback method in which you can save information about the current
	 * state of your activity is onSaveInstanceState(). The system calls this
	 * method before making the activity vulnerable to being destroyed and
	 * passes it a Bundle object. The Bundle is where you can store state
	 * information about the activity as name-value pairs, using methods such as
	 * putString(). Then, if the system kills your activity's process and the
	 * user navigates back to your activity, the system passes the Bundle to
	 * onCreate() so you can restore the activity state you saved during
	 * onSaveInstanceState(). If there is no state information to restore, then
	 * the Bundle passed to onCreate() is null.
	 */
	@Override
	public void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		Log.i(TAG, "onSaveInstanceState() done");
	}

	/**
	 * An adapter to manage the collection of portfolios and their binding to a
	 * GridView.
	 * 
	 * Overrides getView() so we can render a named icon in the GridView.
	 */
	public class PortfolioIconAdapter extends ArrayAdapter<Portfolio> {

		private final String TAG = PortfolioIconAdapter.class.getSimpleName();
		private Context mContext;
		private LayoutInflater vi = getLayoutInflater();

		public PortfolioIconAdapter(Context context, ArrayList<Portfolio> portfolioList) {
			super(context, -1, portfolioList);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			ViewHolder holder;

			Log.i(TAG, "getView(): rendering item in position: " + position
					+ " with dbid=" + getItem(position).getId());

			if (convertView == null) {
				view = vi.inflate(R.layout.main_icon, null);
				holder = new ViewHolder();
				holder.text = (TextView) view.findViewById(R.id.icon_text);
				view.setTag(holder);
			} else {
				view = convertView;
				holder = (ViewHolder) view.getTag();
			}

			Portfolio portfolio = getItem(position);
			holder.text.setText(portfolio.getPortfolioName());
			return view;
		}

		class ViewHolder {
			TextView text;
		}

	}

	/**
	 * Creates the Context menu.
	 * 
	 * Inflates the context menu from R.menu.main_context_menu.
	 * 
	 * @param menu the menu we are inflating into.
	 * @param v the view the menu will be attached to.
	 * @param menuInfo additional info for the menu creation that can be used to
	 *            customise the menu options.
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_context_menu, menu);
	}

	/**
	 * Process the selected context menu item.
	 * 
	 * Handles viewing, renaming and deleting the selected portfolio.
	 * 
	 * @param item the menu item selected.
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo() ;
		mSelectedPosition = info.position ;
		
		Intent intent ;
		Bundle b ;
		
		switch (item.getItemId()) {
		case R.id.viewPortfolio:
			intent = new Intent(getApplicationContext(), PortfolioActivity.class);
			b = intent.getExtras();
			b.putString(PortfolioActivity.EXTRA_PORTFOLIO_NAME, mAdapter.getItem(mSelectedPosition).getPortfolioName());
			b.putLong(PortfolioActivity.EXTRA_PORTFOLIO_ID, mAdapter.getItem(mSelectedPosition).getId());
			startActivity(intent);
			return true;
		case R.id.viewPortfolioTransactions:
			intent = new Intent(getApplicationContext(), TransactionListActivity.class);
			b = intent.getExtras();
			b.putLong(TransactionListActivity.EXTRA_PORTFOLIO_ID, mAdapter.getItem(mSelectedPosition).getId());
			startActivity(intent);
			return true;
		case R.id.renamePortfolio:
			showDialog(DIALOG_RENAME_PORTFOLIO);
			return true;
		case R.id.deletePortfolio:
			showDialog(DIALOG_DELETE_PORTFOLIO);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * Create the options menu associated with the MainActivity.
	 * 
	 * Inflates R.menu.main_options_menu.
	 * 
	 * @param menu the menu in which we populate menu items.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_options_menu, menu);
		return true;
	}

	/**
	 * Process the selected menu item.
	 * 
	 * Handless refreshing stock prices, adding new portfolios and an "about"
	 * dialogue box.
	 * 
	 * @param item the selected menu item.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.refreshPrices:
			showDialog(DIALOG_PROGRESS_DOWNLOAD_LATEST_STOCK_PRICES);
			refreshPrices(true) ;
			return true;
		case R.id.downloadCompanyList:
			mDownloadListedCompaniesTask.execute() ;
			return true ;
		case R.id.newPortfolio:
			showDialog(DIALOG_NEW_PORTFOLIO);
			return true;
		case R.id.viewAllTransactions:
			Intent intent = new Intent(getApplicationContext(), TransactionListActivity.class);
			startActivity(intent);
			return true;
		case R.id.preferences:
			return true;
		case R.id.about:
			showDialog(DIALOG_ABOUT);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Create the various dialogs.
	 * 
	 * @param id
	 *            the dialog id as per the above static integers.
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		AlertDialog.Builder builder;

		switch (id) {
		case DIALOG_ABOUT:
			builder = new AlertDialog.Builder(this);
			builder.setMessage("OzStock v1.0 (c) Craig Jackson 2012").setTitle("About") ;
			builder.setCancelable(false);
			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
						}
					});
			dialog = builder.create();
			break;
		case DIALOG_PROGRESS_DOWNLOAD_LATEST_STOCK_PRICES:
			dialog = ProgressDialog.show(this, "", "Downloading prices...", true);
			break;
		case DIALOG_PROGRESS_UPGRADING:
			dialog = ProgressDialog.show(this, "", "Upgrading DB. Please wait...", true);
			break;
		case DIALOG_PROGRESS_UPDATE_STOCK_REF:
			dialog = ProgressDialog.show(this, "", "Loading stock tickers (once only)...", true);
			break;
		case DIALOG_PROGRESS_DOWNLOAD_LISTED_COMPANIES:
			dialog = ProgressDialog.show(this, "", "Downloading listed companies...", true);
			break;
		case DIALOG_NEW_PORTFOLIO:
			dialog = makeNewPortfolioDialog(this);
			break;
		case DIALOG_RENAME_PORTFOLIO:
			dialog = makeRenamePortfolioDialog(this);
			break;
		case DIALOG_DELETE_PORTFOLIO:
			dialog = makeDeletePortfolioDialog(this);
			break;
		default:
			dialog = null;
			break;
		}
		return dialog;
	}

	/**
	 * Create a dialogue box for adding a new portfolio.
	 * 
	 * Prompts for the name of the portfolio, and adds it to both the database
	 * and adapter.
	 * 
	 * @param context
	 * @return the dialog
	 */
	private Dialog makeNewPortfolioDialog(Context context) {

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_edit_portfolio_name,
				(ViewGroup) findViewById(R.id.layoutEditPortfolioName));

		final EditText editPortfolioName = (EditText) layout
				.findViewById(R.id.editPortfolioName);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("New Portfolio");
		builder.setView(layout);
		builder.setCancelable(true);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				mAddedPortfolioName = editPortfolioName.getText().toString().trim();

				// TODO: Check if portfolio name is unique - in business logic layer
				if (mAddedPortfolioName.length() != 0) {
					Portfolio portfolio = new Portfolio();
					portfolio.setId(-1);
					portfolio.setPortfolioName(mAddedPortfolioName);
					mBusinessLogic.addPortfolio(portfolio) ;
					mAdapter.add(portfolio);

					dialog.dismiss();

					Intent intent = new Intent(getApplicationContext(),	PortfolioActivity.class);
					Bundle b = new Bundle();
					b.putString(PortfolioActivity.EXTRA_PORTFOLIO_NAME, mAddedPortfolioName);
					b.putLong(PortfolioActivity.EXTRA_PORTFOLIO_ID,	mAddedPortfolioId);
					intent.putExtras(b);
					startActivity(intent);
				}
			}
		});
		builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						mAddedPortfolioId = -1;
					}
				});

		Dialog dialog = builder.create();
		return dialog;
	}

	/**
	 * Create a dialogue box for renaming a portfolio.
	 * 
	 * Prompts for the new portfolio name, and renames it in both the database
	 * and adapter.
	 * 
	 * It is presumed the portfolio to be operated on is represented by
	 * mSelectedPosition.
	 * 
	 * @param context
	 * @return the dialog
	 */
	private Dialog makeRenamePortfolioDialog(Context context) {

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_edit_portfolio_name,	(ViewGroup) findViewById(R.id.layoutEditPortfolioName));

		final EditText editPortfolioName = (EditText) layout.findViewById(R.id.editPortfolioName);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Rename Portfolio");
		builder.setView(layout);
		builder.setCancelable(true);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				String portfolioName = editPortfolioName.getText().toString().trim();

				// TODO: Check if portfolio name is unique
				if (portfolioName.length() != 0) {
					Portfolio portfolio = mAdapter.getItem(mSelectedPosition);
					portfolio.setPortfolioName(portfolioName);
					mBusinessLogic.updatePortfolio(portfolio);
					mAdapter.notifyDataSetChanged();
				}
			}
		});
		builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

		Dialog dialog = builder.create();
		return dialog;
	}

	/**
	 * Create a dialogue box for portfolio deletion.
	 * 
	 * Confirms the user wants to delete the selected portfolio, and removes it
	 * from both the database and adapter.
	 * 
	 * It is presumed the portfolio to be operated on is represented by
	 * mSelectedPosition.
	 * 
	 * @param context
	 * @return the dialog
	 */
	private Dialog makeDeletePortfolioDialog(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder = new AlertDialog.Builder(this);
		builder.setMessage("All holdings will be deleted. Are you sure?")
				.setTitle("Delete Portfolio")
				.setCancelable(true)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Log.i(TAG, "makeDeletePortfolioDialog(): onClick(): " + "mSelectedPosition=" + mSelectedPosition + ", dbid=" + mAdapter.getItem(mSelectedPosition).getId());
						mBusinessLogic.deletePortfolio(mAdapter.getItem(mSelectedPosition).getId()) ;
						mAdapter.remove(mAdapter.getItem(mSelectedPosition));
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		Dialog dialog = builder.create();
		return dialog;
	}

	/**
	 * An asynchronous task to perform any required database upgrade. 
	 *
	 */
	private class UpgradingDbTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected void onPreExecute() {
	 		showDialog(DIALOG_PROGRESS_UPGRADING);
		}
		
		/**
		 * Displays a dialog and downloads the listed companies reference file from the ASX website.
		 */
		@Override
	     protected Boolean doInBackground(String ...args) {
			// Force db upgrade check here - needs writable DB! Do this before any other DB activity.
			return true ;
	     }

		/**
		 * Closes the dialog. Then kick off the seeding of the STOCK_REF table if required.
		 */
		@Override
	    protected void onPostExecute(Boolean result) {
	 		dismissDialog(DIALOG_PROGRESS_UPGRADING);

	 		/*
			 * If the STOCK_REF table is empty, seed it from the asset copy of the ASX company reference file.
			 * 
			 * FIXME: Do I need to load the whole table to find out if it's empty?
			 */
	 		mStockRefList = mBusinessLogic.getAllStockRef();
			if (mStockRefList.size() == 0) {
				mLoadStockRefTableFromAssetFileTask.execute() ;
			}
	    }
	}

	
	/**
	 * An asynchronous task to download the listed companies reference file from the ASX website.
	 */
	private class DownloadListedCompaniesTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected void onPreExecute() {
	 		showDialog(DIALOG_PROGRESS_DOWNLOAD_LISTED_COMPANIES);
		}
		
		/**
		 * Displays a dialog and downloads the listed companies reference file from the ASX website.
		 */
		@Override
	     protected Boolean doInBackground(String ...args) {
	        return mAsxHelper.downloadListedCompanies(ASXHelper.ASX_LISTED_COMPANIES_CSV_URL, ASXHelper.ASX_LISTED_COMPANIES_CSV_DOWNLOAD_FILENAME) ;
	     }

		/**
		 * Closes the dialog.
		 */
		@Override
	    protected void onPostExecute(Boolean result) {
	 		dismissDialog(DIALOG_PROGRESS_DOWNLOAD_LISTED_COMPANIES);
	 		mLoadStockRefTableFromDownloadedFileTask.execute() ;
	    }
		
	}
	
	/**
	 * An asynchronous task to seed the STOCK_REF table and accompanying cache.
	 */
	private class LoadStockRefTableFromDownloadedFileTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected void onPreExecute() {
	 		showDialog(DIALOG_PROGRESS_UPDATE_STOCK_REF);
		}

		/**
		 * Opens a progress dialog and commences loading the companies list into the database and in-memory cache
		 */
		@Override
	     protected Boolean doInBackground(String ...args) {
	 		return mAsxHelper.loadStockRefFromDownloadedFile(mStockRefList) ;
	     }

		/**
		 * Closes the progress dialog
		 */
		@Override
	    protected void onPostExecute(Boolean result) {
	 		dismissDialog(DIALOG_PROGRESS_UPDATE_STOCK_REF);
	    }
		
	}
	
	/**
	 * An asynchronous task to seed the STOCK_REF table and accompanying cache.
	 */
	private class LoadStockRefTableFromAssetFileTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected void onPreExecute() {
	 		showDialog(DIALOG_PROGRESS_UPDATE_STOCK_REF);
		}
		
		/**
		 * Opens a progress dialog and commences loading the companies list into the database and in-memory cache
		 */
		@Override
	     protected Boolean doInBackground(String ...args) {
	 		return mAsxHelper.loadStockRefFromAssets(mStockRefList) ;
	     }

		/**
		 * Closes the progress dialog
		 */
		@Override
	    protected void onPostExecute(Boolean result) {
	 		dismissDialog(DIALOG_PROGRESS_UPDATE_STOCK_REF);
	    }
	}

	/**
	 * Displays a progress dialog and asks FinanceService to download the latest stock prices.
	 * 
	 * Note that if this is a forced refresh, the alarm will be reset.
	 * 
	 * @param force whether we should refresh the stock prices even if a refresh occurred only moments ago.
	 */
    private void refreshPrices(boolean force) {
    
    	if(force)
    		calibrateAlarm() ;
    	
    	if(mAdapter.isEmpty()) 
    		return ;
    	showDialog(DIALOG_PROGRESS_DOWNLOAD_LATEST_STOCK_PRICES) ;
    	Intent intent = new Intent(this, FinanceService.class);
		Log.i(TAG, "Refreshing prices via FinanceService...") ;
		if(force) {
			Bundle b = new Bundle() ;
			b.putBoolean(FinanceService.REQUEST_EXTRA_FORCE_REFRESH, force) ;
			intent.putExtras(b) ;
		}
    	startService(intent);		
	}

	/**
	 * Receives broadcasts from FinanceService and (eventually) refreshes the portfolio value and profit / loss.
	 * 
	 * The broadcast may indicate failure to download, in which case we do nothing.
	 */
	public class MainFinanceServiceReceiver extends BroadcastReceiver {
		private final String TAG = MainFinanceServiceReceiver.class.getSimpleName() ;
		
		@Override
	    public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "onReceive() got a broadcast msg") ;
			boolean updatedPrice = false ;
			boolean success = intent.getBooleanExtra(FinanceService.RESPONSE_EXTRA_STATUS, false) ;
			if(! success) {
				Log.e(TAG, "onReceive(): FinanceService says it can't fetch quotes") ;
	        	dismissDialog(DIALOG_PROGRESS_DOWNLOAD_LATEST_STOCK_PRICES) ;
				return ;
			}
			
			SharedPreferences lastPricePrefs = getSharedPreferences(MainActivity.SHARED_PREFS_LAST_PRICE_FILENAME, MODE_PRIVATE) ;

			Map<String, ?> lastPriceList = lastPricePrefs.getAll() ;
			
			int count = mAdapter.getCount() ;
			for(int i = 0; i < count; i++) {
				Portfolio portfolio = mAdapter.getItem(i) ;
				
				for(Holding holding : portfolio.getHoldings()) {
					float price = lastPricePrefs.getFloat(holding.getStockCode(), -1.0f) ;
					if(price < 0.0f)
						continue ;
					
					// TODO: sum holding current price and purchase price here  
					
					updatedPrice = true ;
				}
				// TODO: calc profit / loss and update portfolio summary here 
			}

			if(updatedPrice)
				mAdapter.notifyDataSetChanged() ;
			dismissDialog(DIALOG_PROGRESS_DOWNLOAD_LATEST_STOCK_PRICES) ;
	    }
	}

	/**
	 * Triggers at fixed intervals and asks the FinanceService to refresh stock prices.
	 */
	public class AlarmReceiver extends BroadcastReceiver {
		public static final String RESPONSE_ACTION = "ozstock.alarm.receiver" ;
		public final String TAG = AlarmReceiver.class.getSimpleName() ;
	    @Override
	    public void onReceive(Context context, Intent intent) {
			showDialog(DIALOG_PROGRESS_DOWNLOAD_LATEST_STOCK_PRICES) ;
	    	Intent financeServiceIntent = new Intent(MainActivity.this, FinanceService.class);
			Log.i(TAG, "AlarmReceiver: Refreshing prices via FinanceService...") ;
			context.startService(financeServiceIntent) ;
	    }
	}

	/**
	 * Triggers when battery state changes
	 */
	public class BatteryLevelReceiver extends BroadcastReceiver {
		public final String TAG = "BatteryLevelReceiver" ;
	    @Override
	    public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "BatteryLevelReceiver: Calibrating alarming...") ;
	    	String action = intent.getAction() ;
	    	if(action.compareTo(Intent.ACTION_BATTERY_LOW) == 0)
	    		mBatteryOkay = false ;
	    	else if(action.compareTo(Intent.ACTION_BATTERY_OKAY) == 0)
		    		mBatteryOkay = true ;

	        calibrateAlarm() ;
	    }
	}

	/**
	 * Triggers when battery state changes
	 */
	public class PowerConnectionReceiver extends BroadcastReceiver {
		public final String TAG = "PowerConnectionReceiver" ;
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	String action = intent.getAction() ;
			Log.i(TAG, "PowerConnectionReceiver: Calibrating alarming...") ;
			
	        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
	        mIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
	                            status == BatteryManager.BATTERY_STATUS_FULL;
	    
	        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
	        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
	        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
	        
	        calibrateAlarm() ;
	    }
	}

	/**
	 * Launches a PortfolioActivity when a portfolio icon is selected.
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mSelectedPosition = position;
		Intent intent = new Intent(getApplicationContext(), PortfolioActivity.class);
		Bundle b = new Bundle();
		b.putString(PortfolioActivity.EXTRA_PORTFOLIO_NAME, mAdapter.getItem(mSelectedPosition).getPortfolioName());
		b.putLong(PortfolioActivity.EXTRA_PORTFOLIO_ID, mAdapter.getItem(mSelectedPosition).getId());
		intent.putExtras(b);
		Log.i(TAG, "onItemClick(): launching PortfolioActivity with selected portfolioId=" + mAdapter.getItem(mSelectedPosition).getId()) ;
		startActivity(intent);
	}
}
