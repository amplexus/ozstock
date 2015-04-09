/**
 * 
 */
package org.amplexus.app.ozstock2;

import java.util.Calendar;

import org.amplexus.app.ozstock2.helper.BusinessLogicHelper;
import org.amplexus.app.ozstock2.helper.DatabaseOpenHelper;
import org.amplexus.app.ozstock2.values.Holding;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

/**
 * @author craig
 */
public class HoldingActivity extends Activity {

    private static final String TAG = HoldingActivity.class.getSimpleName() ;

    public static final String EXTRA_PORTFOLIO_ID = "portfolioId";
	public static final String EXTRA_HOLDING_ID = "holdingId";

	private Holding mHolding ;										// The holding we read from the database
	private boolean mIsNewHolding ;									// Indicates whether we are creating a new holding or updating an existing one

    private BusinessLogicHelper mDbHelper ;							// Persistent store of stock holdings

    private AutoCompleteTextView mStockCodeAutoCompleteTextView ; 	//
    private Button mPurchaseDateButton ;							//
    private EditText mPurchaseQuantityEditText ;					//
    private EditText mPurchaseUnitPriceEditText ;					//
    private DatePicker mPurchaseDateEditor ;						//
    private Button mOkButton ;
    private Button mCancelButton ;
    
    private Cursor mCursor ;										//
    private StockTickerAutoTextAdapter mAdapter ;					//

    /**
     * Note that the onDateSet() year parameter is the proper year - ie 2012 etc, not years past 1900.
     */
    private DatePickerDialog.OnDateSetListener mDateSetListener =
			new DatePickerDialog.OnDateSetListener() {
				public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
					Log.i(TAG, "onDateSet(): year=" + year + ", month=" + monthOfYear + ", day=" + dayOfMonth) ;
					mHolding.getPurchaseDate().setDate(dayOfMonth) ;
					mHolding.getPurchaseDate().setMonth(monthOfYear) ;
					mHolding.getPurchaseDate().setYear(year-1900) ;
					mPurchaseDateButton.setText((mHolding.getPurchaseDate().getYear() + 1900) + "-" + 
							(mHolding.getPurchaseDate().getMonth() + 1) + "-" +
							(mHolding.getPurchaseDate().getDate())) ;
					Log.i(TAG, "onDateSet(): purchaseDate=" + mHolding.getPurchaseDate().toLocaleString()) ;
				}
			};
            
    /**
     * Called when the activity is first created. This is where you should do all of your normal static 
     * set up to create views, bind data to lists, and so on. This method is passed a Bundle object 
     * containing the activity's previous state, if that state was captured. Always followed by onStart().
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {	
    	super.onCreate(savedInstanceState) ;
        setContentView(R.layout.holding) ;

        mStockCodeAutoCompleteTextView = (AutoCompleteTextView)findViewById(R.id.holdingStockCode) ;
        mPurchaseDateButton = (Button)findViewById(R.id.holdingPurchaseDate) ;
        mPurchaseUnitPriceEditText = (EditText)findViewById(R.id.holdingPurchaseUnitPrice) ;
        mPurchaseQuantityEditText = (EditText)findViewById(R.id.holdingPurchaseQuantity) ;
        mOkButton = (Button)findViewById(R.id.holdingOKButton) ;
        mCancelButton = (Button)findViewById(R.id.holdingCancelButton) ;
        
        mPurchaseDateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_DATE_PICKER) ;
            }
        });

        mOkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateFromDisplay() ;
                if(mHolding.getStockCode() != null && mHolding.getStockCode().length() > 0 && mHolding.getRemainingQuantity() > 0) {
//                	mDbHelper.writeHolding(mHolding) ;
                    setResult(RESULT_OK, (new Intent()).setAction("Holding Added"));
                    finish() ;
                }
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	finish() ;
            }
        });

        mDbHelper = new BusinessLogicHelper(getApplicationContext()) ;
        mCursor = mDbHelper.getStockRefByInputTextCursor("") ;
        mAdapter = new StockTickerAutoTextAdapter(getApplication(), mCursor) ;
        mStockCodeAutoCompleteTextView.setAdapter(mAdapter);
        mStockCodeAutoCompleteTextView.setOnItemClickListener(mAdapter);

        
        Bundle extras = getIntent().getExtras() ;

        mHolding = new Holding() ; 
        mHolding.setPortfolioId(extras.getLong(HoldingActivity.EXTRA_PORTFOLIO_ID)) ; // This is ignored for existing holdings
        mHolding.setId(extras.getLong(HoldingActivity.EXTRA_HOLDING_ID, -1)) ;
        mIsNewHolding = mHolding.getId() == -1 ;

        if(mIsNewHolding) {
        	mHolding.setPurchaseDate(Calendar.getInstance().getTime()) ;
    		mPurchaseDateButton.setText((mHolding.getPurchaseDate().getYear() + 1900) + "-" + 
    				(mHolding.getPurchaseDate().getMonth() + 1) + "-" +
    				(mHolding.getPurchaseDate().getDate())) ;
        }

        if(!mIsNewHolding) {
        	mHolding = mDbHelper.getHoldingById(mHolding.getId()) ;
        	updateToDisplay() ;
        }
        
        if(mIsNewHolding) {
        	mStockCodeAutoCompleteTextView.requestFocus() ;
        } else {
//        	mStockCodeAutoCompleteTextView.clearFocus() ;
        	mStockCodeAutoCompleteTextView.setEnabled(false) ;
        	mPurchaseQuantityEditText.requestFocus() ;
        }
        
        Log.i(TAG, "onCreate(): portfolioId=" +mHolding.getPortfolioId() ) ;
        Log.i(TAG, "onCreate(): holdingId=" +mHolding.getId() ) ;
    }

    /**
     * Called just before the activity becomes visible to the user. Followed by onResume() if the activity 
     * comes to the foreground, or onStop() if it becomes hidden.
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart() done") ;
    }
    
    /**
     * Called after the activity has been stopped, just prior to it being started again. Always followed by onStart().
     */
    @Override
    protected void onRestart() {
    	super.onRestart() ;
        Log.i(TAG, "onRestart() done") ;
    }

    /**
     * Called just before the activity starts interacting with the user. At this point the activity is at
     * the top of the activity stack, with user input going to it. Always followed by onPause().
     */
    @Override
    protected void onResume() {
        super.onResume();
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
        Log.i(TAG, "onPause() done") ;
    }
    
    /**
     * Called when the activity is no longer visible to the user. This may happen because it is being
     * destroyed, or because another activity (either an existing one or a new one) has been resumed 
     * and is covering it. Followed either by onRestart() if the activity is coming back to interact 
     * with the user, or by onDestroy() if this activity is going away.
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop() done") ;
    }
    
    /**
     * Called before the activity is destroyed. This is the final call that the activity will receive. 
     * It could be called either because the activity is finishing (someone called finish() on it), 
     * or because the system is temporarily destroying this instance of the activity to save space. 
     * You can distinguish between these two scenarios with the isFinishing() method.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy() done") ;
    }
    
    /**
     * Used to record the transient state of the activity (the state of the UI).
     * 
     * The callback method in which you can save information about the current state of your activity is 
     * onSaveInstanceState(). The system calls this method before making the activity vulnerable to being 
     * destroyed and passes it a Bundle object. The Bundle is where you can store state information about 
     * the activity as name-value pairs, using methods such as putString(). Then, if the system kills 
     * your activity's process and the user navigates back to your activity, the system passes the Bundle 
     * to onCreate() so you can restore the activity state you saved during onSaveInstanceState(). If 
     * there is no state information to restore, then the Bundle passed to onCreate() is null.
     */
    @Override
    public void onSaveInstanceState(Bundle bundle) {
    	super.onSaveInstanceState(bundle) ;
        updateFromDisplay() ;
    	bundle.putSerializable("mHolding", mHolding) ;
        Log.i(TAG, "onSaveInstanceState() done") ;
    }
    
    /**
     * This method is called after onStart() when the activity is being re-initialized from a previously saved 
     * state, given here in savedInstanceState. Most implementations will simply use onCreate(Bundle) to restore 
     * their state, but it is sometimes convenient to do it here after all of the initialisation has been done 
     * or to allow subclasses to decide whether to use your default implementation. The default implementation 
     * of this method performs a restore of any view state that had previously been frozen by 
     * onSaveInstanceState(Bundle).
     * 
     * This method is called between onStart() and onPostCreate(Bundle).
     */
    @Override
    public void onRestoreInstanceState(Bundle bundle) {
    	super.onRestoreInstanceState(bundle) ;
    	mHolding = (Holding) bundle.getSerializable("mHolding") ;
    	updateToDisplay() ;
        Log.i(TAG, "onRestoreInstanceState() done") ;
    }

    static final int DIALOG_DATE_PICKER = 1 ;

    /**
     * Create the various dialogs.
     * 
     * @param id the dialog id as per the above static integers.
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;

        switch(id) {
        case DIALOG_DATE_PICKER:
    		Log.i(TAG, "onCreateDialog(): purchaseDate=" + mHolding.getPurchaseDate().toLocaleString()) ;
            dialog = new DatePickerDialog(this, mDateSetListener, 
            		mHolding.getPurchaseDate().getYear() + 1900, 
            		mHolding.getPurchaseDate().getMonth(), 
            		mHolding.getPurchaseDate().getDate()) ;
            break ;
        default:
            dialog = null ;
            break ;
        }
        return dialog;
    }
    
	/**
	 * 
	 */
	private void updateToDisplay() {
		Log.i(TAG, "updateToDisplay(): year=" + (mHolding.getPurchaseDate().getYear()+1900)) ;
		mStockCodeAutoCompleteTextView.setText(mHolding.getStockCode()) ;
		mPurchaseQuantityEditText.setText(String.valueOf(mHolding.getRemainingQuantity())) ;
		mPurchaseUnitPriceEditText.setText(String.valueOf(mHolding.getPurchaseUnitPrice())) ;		
		mPurchaseDateButton.setText((mHolding.getPurchaseDate().getYear() + 1900) + "-" + 
				(mHolding.getPurchaseDate().getMonth() + 1) + "-" +
				(mHolding.getPurchaseDate().getDate())) ;
	}
	
	private void updateFromDisplay() {
		Log.i(TAG, "updateFromDisplay(): starts") ;

		mHolding.setStockCode(mStockCodeAutoCompleteTextView.getText().toString().trim()) ;
        if(mPurchaseQuantityEditText.getText().length() > 0)
        	mHolding.setRemainingQuantity(Long.valueOf(mPurchaseQuantityEditText.getText().toString().trim())) ;
        
        if(mPurchaseUnitPriceEditText.getText().length() > 0)
        	mHolding.setPurchaseUnitPrice(Double.valueOf(mPurchaseUnitPriceEditText.getText().toString().trim())) ;
        
        // No need to sync purchase date because it is updated immediately by the date picker dialog
	}
	
    class StockTickerAutoTextAdapter extends CursorAdapter implements android.widget.AdapterView.OnItemClickListener {

        private final String TAG = StockTickerAutoTextAdapter.class.getSimpleName() ;

        public StockTickerAutoTextAdapter(Context app, Cursor cursor) {
            super(app, cursor);
		}
		
        /**
         * Invoked by the AutoCompleteTextView field to get completions for the current input.
         *
         * NOTE: If this method either throws an exception or returns null, the Filter class that invokes it will log an error with the traceback,
         * but otherwise ignore the problem. No choice list will be displayed. Watch those error logs!
         *
         * @param constraint The input entered thus far. The resulting query will search for Items whose description begins with this string.
         * @return A Cursor that is positioned to the first row (if one exists) and managed by the activity.
         */
        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            Log.i(TAG, "runQueryOnBackgroundThread(): begins");
            if (getFilterQueryProvider() != null) {
                Log.i(TAG, "runQueryOnBackgroundThread(): ends - using queryFilterProvider");
                return getFilterQueryProvider().runQuery(constraint);
            }

            Cursor cursor = mDbHelper.getStockRefByInputTextCursor((constraint != null ? constraint.toString() : ""));
            Log.i(TAG, "runQueryOnBackgroundThread(): ends - got cursor");
            return cursor;
        }

        /**
         * Called by the AutoCompleteTextView field to get the text that will be entered in the field after a choice has been made.
         *
         * @param Cursor The cursor, positioned to a particular row in the list.
         * @return A String representing the row's text value. (Note that this specializes the base class return value for this method, which is {@link CharSequence}.)
         */
        @Override
        public String convertToString(Cursor cursor) {
            Log.i(TAG, "convertToString(): begins");
            final int columnIndex = cursor.getColumnIndexOrThrow(DatabaseOpenHelper.COL_SREF_STOCK_CODE);
            final String str = cursor.getString(columnIndex);
            Log.i(TAG, "convertToString(): ends - stockCode=" + str);
            return str;
        }

        /**
         * Called by the AutoCompleteTextView field when a choice has been made by the user.
         *
         * @param listView The ListView containing the choices that were displayed to the user.
         * @param view The field representing the selected choice
         * @param position The position of the choice within the list (0-based)
         * @param id The id of the row that was chosen (as provided by the _id column in the cursor.
         */
		@Override
		public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
            Log.i(TAG, "onItemClick(): begins");
            Cursor cursor = (Cursor) listView.getItemAtPosition(position);

            // Get the Item Number from this row in the database.
            String stockCode = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseOpenHelper.COL_SREF_STOCK_CODE));

            // Update the value
//            mStockCodeAutoCompleteTextView.setText(stockCode);
            mPurchaseQuantityEditText.requestFocus() ;
            Log.i(TAG, "onItemClick(): selected stock=" + stockCode);
		}

        /**
         * Called by the ListView for the AutoCompleteTextView field to display the text for a particular choice in the list.
         *
         * @param view The TextView used by the ListView to display a particular choice.
         * @param context The context (Activity) to which this form belongs;
         * @param cursor The cursor for the list of choices, positioned to a particular row.
         */
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
            Log.i(TAG, "bindView(): begins");
            final int stockCodeColumnIndex = cursor.getColumnIndexOrThrow(DatabaseOpenHelper.COL_SREF_STOCK_CODE);
            final int stockNameColumnIndex = cursor.getColumnIndexOrThrow(DatabaseOpenHelper.COL_SREF_STOCK_NAME);
            TextView stockCodeTextView = (TextView) view.findViewById(R.id.stockCode);
            stockCodeTextView.setText(cursor.getString(stockCodeColumnIndex));
            TextView stockNameTextView = (TextView) view.findViewById(R.id.stockName);
            stockNameTextView.setText(cursor.getString(stockNameColumnIndex));
            Log.i(TAG, "bindView(): ends");
        }

        /**
         * Called by the AutoCompleteTextView field to display the text for a particular choice in the list.
         *
         * @param context The context (Activity) to which this form belongs;
         * @param cursor the cursor for the list of choices, positioned to a particular row.
         * @param parent The ListView that contains the list of choices.
         *
         * @return A new View (really, a TextView) to hold a particular choice.
         */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final View view = inflater.inflate(R.layout.stock_ticker_selector, parent, false);
            return view;
		}
    }
}