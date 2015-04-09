package org.amplexus.app.ozstock2;

import java.text.DecimalFormat;
import java.util.Date;

import org.amplexus.app.ozstock2.helper.BusinessLogicHelper;
import org.amplexus.app.ozstock2.helper.DatabaseOpenHelper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * 
 * 
 * @author craig
 *
 */
public class TransactionListActivity extends ListActivity {

    private static final String TAG = TransactionListActivity.class.getSimpleName() ;

    public static final String EXTRA_PORTFOLIO_ID = "portfolioId";
	public static final String EXTRA_HOLDING_ID = "holdingId";

	private static final int REQUEST_CODE_EDIT_TRANACTION = 0;

    private ListView mListView ;							// The ListView containing the stock holdings

    private BusinessLogicHelper mBusinessLogic ;					// Business layer
    private int mSelectedPosition ;							// The currently selected transaction (for context menu operations)
    private long mPortfolioId ;								// Key of the portfolio whose transactions we are displaying.
    private long mHoldingId ;								// Key of the holding whose transactions we are displaying.
    private TransactionAdapter mAdapter ;					// An in-memory cache of transactions managed by the ListView 
	private Cursor mCursor ;								// The ListView cursor managed via the adapter

	private long mSelectedTransactionId ;					// The selected transaction id
	
    /**
     * Called when the activity is first created. This is where you should do all of your normal static 
     * set up to create views, bind data to lists, and so on. This method is passed a Bundle object 
     * containing the activity's previous state, if that state was captured. Always followed by onStart().
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
    	super.onCreate(savedInstanceState) ;
        setContentView(R.layout.transaction_list) ; // if you want to use your own layout, make sure the listview has an id "@android:id/list"

        mListView = getListView() ;
        mListView.setOnItemClickListener(mListViewOnClickListener );

        /*
         * Initialise the database
         */
        mBusinessLogic = new BusinessLogicHelper(getApplicationContext()) ;

        /*
         * Grab the input parameters
         */
        Bundle extras = getIntent().getExtras() ;
        mPortfolioId = extras.getLong(EXTRA_PORTFOLIO_ID) ;
        mHoldingId = extras.getLong(EXTRA_HOLDING_ID) ;

        /*
         * Initialise the ListView
         */
        if(mPortfolioId != -1)
        	mCursor  = mBusinessLogic.getTransactionsByPortfolioCursor(mPortfolioId) ;
        else if(mHoldingId != -1)
        	mCursor  = mBusinessLogic.getTransactionsByHoldingCursor(mHoldingId) ;
        else
        	mCursor = mBusinessLogic.getTransactionsCursor() ;

        startManagingCursor(mCursor) ;
        
    	String[] fields = {
        	DatabaseOpenHelper.COL_STX_TRANSACTION_DATE,
        	DatabaseOpenHelper.COL_STX_TRANSACTION_TYPE, 
        	DatabaseOpenHelper.COL_STX_AMOUNT 
        } ;
        	
        int[] columns = {
        		R.id.txStockCode, R.id.txTransactionDate, R.id.txTransactionType, R.id.txAmount
        } ;

        mAdapter = new TransactionAdapter(getApplicationContext(), mCursor, fields, columns) ;
        setListAdapter(mAdapter);

        Log.i(TAG, "onCreate() done") ;
    }

    /**
     * Called just before the activity starts interacting with the user. At this point the activity is at
     * the top of the activity stack, with user input going to it. Always followed by onPause().
     */
    @Override
    protected void onResume() {
        super.onResume();
        
        registerForContextMenu(mListView) ;
        
        Log.i(TAG, "onResume() done") ;
    }
    
	/**
     * Called when the system is about to start resuming another activity. This method is typically used
     * to commit unsaved changes to persistent data, stop animations and other things that may be consuming 
     * CPU, and so on. It should do whatever it does very quickly, because the next activity will not be 
     * resumed until it returns. Followed either by onResume() if the activity returns back to the front, 
     * or by onStop() if it becomes invisible to the user.
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterForContextMenu(mListView) ;
        Log.i(TAG, "onPause() done") ;
    }

	/**
     * An adapter to manage the collection of portfolios and their binding to a GridView.
     * 
     * Overrides getView() so we can render a named icon in the GridView.
     */
    public class TransactionAdapter extends SimpleCursorAdapter {

        private final String TAG = TransactionAdapter.class.getSimpleName() ;
        
		private Context mContext ;
		private LayoutInflater vi = getLayoutInflater() ;

        public TransactionAdapter(Context context, Cursor cursor, String[] fields, int[] columns) {
			super(context, R.layout.transaction_list_item, cursor, fields, columns) ;
		}
    }
    
    /**
     * Creates the Context menu.
     * 
     * Inflates the context menu from R.menu.portfolio_context_menu.
     * 
     * @param menu the menu we are inflating into.
     * @param v the view the menu will be attached to. 
     * @param menuInfo additional info for the menu creation that can be used to customise the menu options.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.transaction_context_menu, menu);
    }
    
    /**
     * Process the selected context menu item.
     * 
     * Handles editing and deleting the selected holding.
     * 
     * @param item the menu item selected.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        mSelectedPosition = info.position ;
        switch (item.getItemId()) {
        case R.id.viewTransaction:
        	if(mSelectedPosition >= 0) {
        		mCursor.moveToPosition(mSelectedPosition) ;
                long transactionId = mCursor.getLong(mCursor.getColumnIndex(DatabaseOpenHelper.COL_STX_ID)) ;

//                Intent intent = new Intent(getApplicationContext(), TransactionDetailsActivity.class) ;
//                Bundle b = new Bundle() ;
//                b.putLong(TransactionDetailsActivity.EXTRA_TRANSACTION_ID, transactionId) ;
//                intent.putExtras(b) ;
//                startActivity(intent) ;
        	}
            return true;
        case R.id.deleteTransaction:
        	showDialog(DIALOG_DELETE_TRANSACTION) ;
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    static final int DIALOG_DELETE_TRANSACTION = 1 ;
  
    /**
     * Create the various dialogs.
     * 
     * @param id the dialog id as per the above static integers.
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        AlertDialog.Builder builder ;
        
        switch(id) {
        case DIALOG_DELETE_TRANSACTION:
        	dialog = makeDeleteTransactionDialog(this) ;
        	break ;
        default:
            dialog = null;
            break ;
        }
        return dialog;
    }

	/**
	 * Create a dialogue box for holding deletion.
	 * 
	 * Confirms the user wants to delete the selected holding, and removes it from
	 * both the database and adapter.
	 * 
	 * It is presumed the holding to be operated on is represented by mSelectedPosition.
	 * 
	 * @param context
	 * @return the dialog
	 */
	private Dialog makeDeleteTransactionDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	builder = new AlertDialog.Builder(this);
    	builder.setMessage("This will affect tax estimates. Are you sure?")
    		   .setTitle("Delete Transaction")
    	       .setCancelable(true)
    	       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	               Log.i(TAG, "makeDeleteTransactionDialog(): onClick(): " +
    	            		   "mSelectedPosition=" + mSelectedPosition + ", dbid=" + mSelectedTransactionId) ;

    	        	   mSelectedTransactionId = mAdapter.getItemId(mSelectedPosition) ;
    	        	   mBusinessLogic.deleteTransaction(mSelectedTransactionId) ;
    	           }
    	       })
    	       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	           }
    	       });
    	Dialog dialog = builder.create();
    	return dialog ;
	}
	
	private CharSequence formatDate(Date date) {
		return DateFormat.format("yy/MM/dd", date) ;
	}

	DecimalFormat currencyWithCentsFormatter = new DecimalFormat("'$'0.00");
	DecimalFormat currencyNoCentsFormatter = new DecimalFormat("'$'0");

	private String formatCurrency(double amount, boolean showCents) {
		
		if(showCents) {
			return currencyWithCentsFormatter.format(amount) ;
		} else {
			return currencyNoCentsFormatter.format(amount) ;
		}
	}
	
    /**
     * This method is called when the sending activity has finished, with the
     * result it supplied.
     * 
     * @param requestCode The original request code as given to
     *                    startActivity().
     * @param resultCode From sending activity as per setResult().
     * @param data From sending activity as per setResult().
     */
    @Override
        protected void onActivityResult(int requestCode, int resultCode,
                Intent data) {
    	/*
    	 * If we added a new holding then refresh stock prices
    	 */
    }

	private OnItemClickListener mListViewOnClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        	mSelectedPosition = position ;
        	if(mSelectedPosition >= 0) {
                Cursor c = (Cursor) parent.getItemAtPosition(mSelectedPosition) ;
                mSelectedTransactionId = c.getLong(c.getColumnIndex(DatabaseOpenHelper.COL_STX_ID)) ;
                
//                Intent intent = new Intent(getApplicationContext(), TransactionDetailsActivity.class) ;
//                Bundle b = new Bundle() ;
//                b.putLong(TransactionDetailsActivity.EXTRA_TRANSACTION_ID, transactionId) ;
//                intent.putExtras(b) ;
//                startActivity(intent) ;
        	}
        }
    } ;
}
