/**
 * 
 */
package org.amplexus.app.ozstock2;

import java.util.Calendar;

import org.amplexus.app.ozstock2.helper.BusinessLogicHelper;
import org.amplexus.app.ozstock2.helper.DatabaseOpenHelper;
import org.amplexus.app.ozstock2.values.BuyTransaction;
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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Displays the stock purchase screen.
 * 
 * PROCESSING
 * 
 * When a stock purchase takes place, that will result in a new row in the STOCK_HOLDING table and a 
 * corresponding row in the STOCK_TRANSACTION table.
 * 
 * Captures stock purchase information from the user and stores it in a Holding member variable.
 * 
 * A stock code picker list is displayed when selecting the stock to purchase.
 * 
 * If the OK button is clicked
 * 		Validates that all display fields are populated - if not valid then does nothing
 * 		Inserts a row in the STOCK_HOLDING table representing this new holding and it's association with a portfolio.
 * 		Inserts a row in the STOCK_TRANSACTION table representing the purchase.
 * 		Sets the activity result to RESULT_OK
 * 		Closes the activity
 * Otherwise
 * 		Sets the activity result to RESULT_CANCELED
 * 		Closes the activity
 * 
 * INPUTS (extracted from the activity intent's extras bundle)
 * 
 * - portfolioId - specifies the portfolio this purchase will belong to.
 * - stockCode - optional value that will be used to pre-populate the stock code picker.
 * 
 * DISPLAY FIELDS
 * 
 * - stockCode - a stock code picker
 * - purchaseDate - a date picker
 * - purchaseQuantity - 
 * - purchasePrice
 * - okButton
 * - cancelButton
 * 
 * RETURNS
 * 
 *  RESULT_OK if the OK button was clicked and the purchase was successfully stored in the database.
 *  RESULT_CANCELED if the cancel button was clicked.
 * 
 * @author craig
 */
public class BuyActivity extends Activity {

    private static final String TAG = BuyActivity.class.getSimpleName() ;

    public static final String EXTRA_PORTFOLIO_ID = "portfolioId";	// Input parameter from the activity intent's extras bundle
    public static final String EXTRA_STOCK_CODE = "stockCode";		// Input parameter from the activity intent's extras bundle

    private static final int DIALOG_DATE_PICKER = 1 ;				// Date picker dialog id

	public static final String EXTRA_RESULT_HOLDING = "holding" ;
	public static final String EXTRA_RESULT_TRANSACTION = "transaction" ;

    private BusinessLogicHelper mBusinessLogic ;							// Business logic layer

	private Holding mHolding ;										// The holding value object we will store in the database
	
    private AutoCompleteTextView mStockCodeAutoCompleteTextView ; 	// The stock code picker
    private Cursor mCursor ;										// The stock code picker's cursor
    private StockTickerAutoTextAdapter mAdapter ;					// The stock code picker's adapter

    private Button mBuyDateButton ;									// Clicking this brings up the purchase date picker
    private EditText mBuyQuantityEditText ;							// Purchase quantity
    private EditText mBuyUnitPriceEditText ;						// Purchase unit price
    private DatePicker mBuyDateEditor ;								// Purchase date picker
    private Button mOkButton ;										// Triggers storing the purchase in the database
    private Button mCancelButton ;									// Triggers cancellation of the activity without storing anything in the database
    
    /**
     * Called when the activity is first created. This is where you should do all of your normal static 
     * set up to create views, bind data to lists, and so on. This method is passed a Bundle object 
     * containing the activity's previous state, if that state was captured. Always followed by onStart().
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {	
    	super.onCreate(savedInstanceState) ;
        setContentView(R.layout.buy) ;

        /*
         * Find widgets
         */
        mStockCodeAutoCompleteTextView = (AutoCompleteTextView)findViewById(R.id.buyStockCode) ;
        mBuyDateButton = (Button)findViewById(R.id.buyDate) ;
        mBuyUnitPriceEditText = (EditText)findViewById(R.id.buyUnitPrice) ;
        mBuyQuantityEditText = (EditText)findViewById(R.id.buyQuantity) ;
        mOkButton = (Button)findViewById(R.id.buyOKButton) ;
        mCancelButton = (Button)findViewById(R.id.buyCancelButton) ;
        
        /*
         * Assign listeners to widgets
         */
        mBuyDateButton.setOnClickListener(mBuyDateOnClickListener);
        mOkButton.setOnClickListener(mOkButtonOnClickListener);
        mCancelButton.setOnClickListener(mCancelButtonOnClickListener);

        /*
         * Initialise business logic helper
         */
        mBusinessLogic = new BusinessLogicHelper(getApplicationContext()) ;

        /*
         * Grab the startActivity's intent parameters
         */
        Bundle extras = getIntent().getExtras() ;
        long portfolioId = extras.getLong(BuyActivity.EXTRA_PORTFOLIO_ID) ;
        String filterStockCode = extras.getString(BuyActivity.EXTRA_STOCK_CODE) ;
        if(filterStockCode == null)
        	filterStockCode = "" ;
        
        Log.i(TAG, "onCreate(): portfolioId=" + portfolioId) ;
        Log.i(TAG, "onCreate(): filterStockCode=" + filterStockCode) ;

        /*
         * Initialise our Holding value object
         */
    	mHolding = new Holding() ; 
        mHolding.setPortfolioId(portfolioId) ;
        mHolding.setId(-1) ;
    	mHolding.setPurchaseDate(Calendar.getInstance().getTime()) ; // Default purchase date to today
    	if(filterStockCode.length() == 3) {
    		mHolding.setStockCode(filterStockCode) ;
    	}
    		

    	/*
    	 * Initialise the purchase date button's text
    	 */
//		mBuyDateButton.setText((mHolding.getPurchaseDate().getYear() + 1900) + "-" + 
//				(mHolding.getPurchaseDate().getMonth() + 1) + "-" +
//				(mHolding.getPurchaseDate().getDate())) ;

        /*
         * Setup the autocomplete stock ticker selector
         */
        mCursor = mBusinessLogic.getStockRefByInputTextCursor(filterStockCode) ;
        startManagingCursor(mCursor) ;
        mAdapter = new StockTickerAutoTextAdapter(getApplication(), mCursor) ;
        mStockCodeAutoCompleteTextView.setAdapter(mAdapter) ;
        mStockCodeAutoCompleteTextView.setOnItemClickListener(mAdapter) ;
        mStockCodeAutoCompleteTextView.setText(filterStockCode) ;
        
		/*
		 * If the stock code passed into this activity is a valid 3 character stock code, give the quantity field focus.  
		 */
        if(filterStockCode.length() == 3) {
        	mBuyQuantityEditText.requestFocus() ;
        } else {
        	mStockCodeAutoCompleteTextView.requestFocus() ;
        }

        /*
         * Sync display with fetched data
         */
        updateToDisplay() ;
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
	 * Synchronise the display with the contents of the Holding value object
	 */
	private void updateToDisplay() {
		Log.i(TAG, "updateToDisplay(): starts") ;

		mStockCodeAutoCompleteTextView.setText(mHolding.getStockCode()) ;
		mBuyQuantityEditText.setText(String.valueOf(mHolding.getRemainingQuantity())) ;
		mBuyUnitPriceEditText.setText(String.valueOf(mHolding.getPurchaseUnitPrice())) ;		
		mBuyDateButton.setText((mHolding.getPurchaseDate().getYear() + 1900) + "-" + 
				(mHolding.getPurchaseDate().getMonth() + 1) + "-" +
				(mHolding.getPurchaseDate().getDate())) ;
	}

	/**
	 * Synchronise the Holding value object with the displayed values
	 */
	private void updateFromDisplay() {
		Log.i(TAG, "updateFromDisplay(): starts") ;

		mHolding.setStockCode(mStockCodeAutoCompleteTextView.getText().toString().trim()) ;
        if(mBuyQuantityEditText.getText().length() > 0)
        	mHolding.setRemainingQuantity(Long.valueOf(mBuyQuantityEditText.getText().toString().trim())) ;
        
        if(mBuyUnitPriceEditText.getText().length() > 0)
        	mHolding.setPurchaseUnitPrice(Double.valueOf(mBuyUnitPriceEditText.getText().toString().trim())) ;
        
        // No need to sync purchase date because it is updated immediately by the date picker dialog
	}
	
	/**
	 * The cursor adapter used to display the stock ticker pick list
	 */
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

            Cursor cursor = mBusinessLogic.getStockRefByInputTextCursor((constraint != null ? constraint.toString() : ""));
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

            /*
             * Once a stock code is selected, move focus to the quantity field.
             */
            mBuyQuantityEditText.requestFocus() ;
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
    
    /**
     * Note that the onDateSet() year parameter is the proper year - ie 2012 etc, not years past 1900.
     */
    private DatePickerDialog.OnDateSetListener mDateSetListener =
			new DatePickerDialog.OnDateSetListener() {
				public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
					mHolding.getPurchaseDate().setDate(dayOfMonth) ;
					mHolding.getPurchaseDate().setMonth(monthOfYear) ;
					mHolding.getPurchaseDate().setYear(year-1900) ;
					mBuyDateButton.setText((mHolding.getPurchaseDate().getYear() + 1900) + "-" + 
							(mHolding.getPurchaseDate().getMonth() + 1) + "-" +
							(mHolding.getPurchaseDate().getDate())) ;

					Log.i(TAG, "onDateSet(): purchaseDate=" + mHolding.getPurchaseDate().toLocaleString()) ;
				}
			};

	/**
	 * 
	 */
	private OnClickListener mBuyDateOnClickListener = 
			new OnClickListener() {
		        public void onClick(View v) {
		            showDialog(DIALOG_DATE_PICKER) ;
		        }
		    };

	/**
	 * 
	 */
	private OnClickListener mOkButtonOnClickListener =
			new OnClickListener() {
		        public void onClick(View v) {
		            updateFromDisplay() ;
		            if(mHolding.getStockCode() != null && mHolding.getStockCode().length() == 3 && mHolding.getRemainingQuantity() > 0) {
		            	/*
		            	 * Create a transaction record based on the holding
		            	 */
		            	BuyTransaction transaction = new BuyTransaction() ;
		            	transaction.setTransactionType(BuyTransaction.TRANS_BUY) ;
		                transaction.setQuantity(mHolding.getRemainingQuantity()) ;
		                transaction.setUnitPrice(mHolding.getPurchaseUnitPrice()) ;
						transaction.setTransactionDate(mHolding.getPurchaseDate()) ;
						transaction.setAmount(transaction.getUnitPrice() * transaction.getQuantity()) ;
						transaction.setStockCode(mHolding.getStockCode()) ;

		            	mBusinessLogic.purchaseStock(mHolding.getPortfolioId(), transaction) ;
		            	
		            	Intent intent = new Intent("Purchase complete") ;
		            	intent.putExtra(EXTRA_RESULT_HOLDING, mHolding) ;
		            	intent.putExtra(EXTRA_RESULT_TRANSACTION, transaction) ;
		                setResult(RESULT_OK, intent);
		                finish() ;
		            }
		        }
		    };

	/**
	 * 
	 */
	private OnClickListener mCancelButtonOnClickListener =
			new View.OnClickListener() {
		        public void onClick(View v) {
		            setResult(RESULT_CANCELED, (new Intent()).setAction("Cancelled"));
		        	finish() ;
		        }
		    };
}