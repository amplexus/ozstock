/**
 * 
 */
package org.amplexus.app.ozstock2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.amplexus.app.ozstock2.helper.BusinessLogicHelper;
import org.amplexus.app.ozstock2.helper.DatabaseOpenHelper;
import org.amplexus.app.ozstock2.helper.TextFormatHelper;
import org.amplexus.app.ozstock2.values.Holding;
import org.amplexus.app.ozstock2.values.SellTransaction;
import org.amplexus.app.ozstock2.values.SellAllocation;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Displays the stock sale screen.
 * 
 * PROCESSING
 * 
 * When a stock sale takes place, that will result in an updated row in the STOCK_HOLDING table and a 
 * new row in the STOCK_TRANSACTION table.
 * 
 * Captures stock sale information from the user and stores it in a Holding member variable.
 * 
 * If the OK button is clicked
 * 		Validates that all display fields are populated - if not valid then does nothing
 * 		Updates the row in the STOCK_HOLDING table to represent the reduced quantity of the holding arising from this sale.
 * 		Inserts a row in the STOCK_TRANSACTION table representing the sale.
 * 		Sets the activity result to RESULT_OK
 * 		Closes the activity
 * Otherwise
 * 		Sets the activity result to RESULT_CANCELED
 * 		Closes the activity
 * 
 * INPUTS (extracted from the activity intent's extras bundle)
 * 
 * - portfolioId - specifies the portfolio this purchase will belong to.
 * - holdingId - the holding we are selling
 * 
 * DISPLAY FIELDS
 * 
 * - stockCode - the stock code we are selling
 * - sellDate - a date picker - defaults to today
 * - sellQuantity - defaults to the holding quantity 
 * - sellPrice - defaults to the last price
 * - okButton
 * - cancelButton
 * 
 * RETURNS
 * 
 *  RESULT_OK if the OK button was clicked and the sale was successfully stored in the database.
 *  RESULT_CANCELED if the cancel button was clicked.
 *  
 * TODO: show tax estimate for sale once allocated
 * 
 * @author craig
 */
public class SellActivity extends Activity {

    private static final String TAG = SellActivity.class.getSimpleName() ;

	public static final String EXTRA_HOLDING_ID = "holdingId";				// Input parameter from the activity intent's extras bundle
	public static final String EXTRA_RESULT_TRANSACTION = "transaction" ;	// Output parameter - the transaction that was created
	public static final String EXTRA_RESULT_HOLDING_LIST = "holdingList" ;	// Output parameter - list of holdings that were affected

    private static final int DIALOG_DATE_PICKER = 1 ;						// Date picker dialog id

	public static final int REQUEST_SALE_ALLOCATION = 1 ;

	// NOTE: Some of these member variables are public so we can easily test them.
	
    public BusinessLogicHelper mBusinessLogic ;								// Business logic layer
	private TextFormatHelper mFormatHelper ;								// Number and date text formatting.

    public SellTransaction mTransaction ;									// The transaction associated with this sale
	public ArrayList<Holding> mHoldingList ;								// Which holdings are for the selected stock code

	private long mSellQuantity ;											// The quantity we desire to sell
	private double mProfitLossAmount ;										// The amount of profit or loss based on the sale allocation
	private long mMaxSellableQuantity ;									// The total number of units we may sell - ie total holding 

    private AutoCompleteTextView mStockTickerAutoCompleteTextView ;			// The stock code picker
    public Cursor mStockTickerCursor ;										// The stock code picker's cursor
    private StockTickerAutoTextAdapter mStockTickerAdapter ;				// The stock code picker's adapter

    private Button mSellDateButton ;										// Clicking this brings up the sell date picker
    private TextView mProfitLossAmountTextView ;							// 
    private EditText mSellQuantityEditText ;								// 
    private EditText mSellUnitPriceEditText ;								// Sell unit price
    private DatePicker mSellDatePicker ;									// Sell date picker
    private Button mOkButton ;												// Triggers storing the sale in the database
    private Button mCancelButton ;											// Triggers cancellation of the activity without storing anything in the database
	private Spinner mSellAllocationMethodSpinner ;							// Choose between allocation strategies
	private Button mSellSelectButton ;										// If manual allocation is chosen, this button triggers the sale allocation screen

	public ArrayAdapter<CharSequence> mSellAllocationMethodSpinnerAdapter;	// How would we like to allocate the sale?

    /**
     * Called when the activity is first created. This is where you should do all of your normal static 
     * set up to create views, bind data to lists, and so on. This method is passed a Bundle object 
     * containing the activity's previous state, if that state was captured. Always followed by onStart().
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {	
    	super.onCreate(savedInstanceState) ;
        setContentView(R.layout.sell) ;

        /*
         * Find widgets
         */
        mStockTickerAutoCompleteTextView = (AutoCompleteTextView)findViewById(R.id.sellStockCode) ;
        mSellDateButton = (Button)findViewById(R.id.sellDate) ;
        mSellUnitPriceEditText = (EditText)findViewById(R.id.sellUnitPrice) ;
        mSellQuantityEditText = (EditText)findViewById(R.id.sellQuantity) ;
        mOkButton = (Button)findViewById(R.id.sellOKButton) ;
        mCancelButton = (Button)findViewById(R.id.sellCancelButton) ;
        mSellAllocationMethodSpinner = (Spinner) findViewById(R.id.sellAllocationMethodSpinner);
		mSellSelectButton = (Button)findViewById(R.id.sellSelectButton) ;
        mProfitLossAmountTextView = (TextView)findViewById(R.id.profitLossAmount) ;

        /*
    	 * Setup Spinner 
    	 */
        mSellAllocationMethodSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.sellAllocationSpinnerArray, android.R.layout.simple_spinner_item) ;
        mSellAllocationMethodSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSellAllocationMethodSpinner.setAdapter(mSellAllocationMethodSpinnerAdapter);

        /*
         * Assign listeners to widgets
         */
        mSellDateButton.setOnClickListener(mPurchaseDateOnClickListener) ;
        mOkButton.setOnClickListener(mOkButtonOnClickListener) ;
        mCancelButton.setOnClickListener(mCancelButtonOnClickListener) ;
        mSellAllocationMethodSpinner.setOnItemSelectedListener(mSellAllocationMethodSpinnerOnItemSelectedListener) ;
        mSellSelectButton.setOnClickListener(mSellSelectButtonOnClickListener) ;
        mSellQuantityEditText.setOnFocusChangeListener(mSellQuantityFocusChangeListener) ;
        mSellUnitPriceEditText.setOnFocusChangeListener(mSellUnitPriceFocusChangeListener) ;

        /*
         * Initialise helpers
         */
        mBusinessLogic = new BusinessLogicHelper(getApplicationContext()) ;
        mFormatHelper = new TextFormatHelper() ;
        
        /*
         * Grab the startActivity's intent parameters
         */
        Bundle extras = getIntent().getExtras() ;
        
        long holdingId = -1 ;
        if(extras != null)
        	holdingId = extras.getLong(SellActivity.EXTRA_HOLDING_ID, -1) ;
        Log.i(TAG, "onCreate(): holdingId=" + holdingId ) ;

       	/*
       	 * Initialise some fields
       	 */
        mTransaction = new SellTransaction() ;
        mTransaction.setTransactionDate(Calendar.getInstance().getTime()) ;
		mTransaction.setTransactionType(SellTransaction.TRANS_SELL) ;
		mTransaction.setId(-1) ; // A new transaction!

       	/*
       	 * If we received a holding id on input, we can pre-fill a sale allocation
       	 */
       	if(holdingId > 0) {
       		
       		// TODO: could optimise by passing the whole holding from PortfolioActivity
           	// TODO: instead of fudging an allocation here, redirect to the sale allocation screen and pre-select the holding
           	
       		Holding h = mBusinessLogic.getHoldingById(holdingId) ;
           	
       		mTransaction.setStockCode(h.getStockCode()) ;
       		mTransaction.setUnitPrice(h.getPurchaseUnitPrice()) ;
       		
       		SellAllocation alloc = new SellAllocation() ;
	       	alloc.setHoldingId(h.getId()) ;
	       	alloc.setQuantity(h.getRemainingQuantity()) ;
	       	mTransaction.getSaleAllocations().add(alloc) ;

	       	mSellQuantity = h.getRemainingQuantity() ;

           	/*
             * Grab all the holdings for the given stock code - in case the user wants to sell other holdings for the same stock.
             * But do it once only here rather than each time they click on the view/edit allocations button.
             * 
             * FIXME: Take a just-in-time approach - ie do it when they click view/edit allocations, but only if mHoldingList is null
             */
    		mHoldingList = mBusinessLogic.getHoldingsByStockCode(mTransaction.getStockCode()) ;
    		
    		mMaxSellableQuantity = sumTotalHoldings(mHoldingList) ;

        	mProfitLossAmount = mBusinessLogic.calculateProfitLoss(mHoldingList, mTransaction) ;
    		
       	} else {
       		mSellQuantity = 0 ;
       		mTransaction.setStockCode("") ;
       	}

        /*
         * Setup the autocomplete stock ticker selector
         */
       	
        mStockTickerCursor = mBusinessLogic.getAllPortfolioStockCodesCursor() ;
        mStockTickerAdapter = new StockTickerAutoTextAdapter(getApplication(), mStockTickerCursor) ;
        mStockTickerAutoCompleteTextView.setAdapter(mStockTickerAdapter);
        mStockTickerAutoCompleteTextView.setOnItemClickListener(mStockTickerAdapter) ;
    	startManagingCursor(mStockTickerCursor) ;

		/*
		 * If the stock code passed into this activity is a valid 3 character stock code, disable the stock ticker
		 * selector, calculate the max sellable quantity and give the quantity field focus.  
		 */
        if(mTransaction.getStockCode().length() == 3) {
        	mStockTickerAutoCompleteTextView.setEnabled(false) ;
        	mSellQuantityEditText.requestFocus() ;
        } else {
        	mStockTickerAutoCompleteTextView.requestFocus() ;
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
    	bundle.putSerializable("mTransaction", mTransaction) ;
    	bundle.putLong("mSellQuantity", mSellQuantity) ;
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
    	mTransaction = (SellTransaction)bundle.getSerializable("mTransaction") ;
    	mSellQuantity = bundle.getLong("mSellQuantity") ;
    	
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
    		Log.i(TAG, "onCreateDialog(): sellDate=" + mTransaction.getTransactionDate().toLocaleString()) ;
            dialog = new DatePickerDialog(this, mDateSetListener, mTransaction.getTransactionDate().getYear() + 1900, mTransaction.getTransactionDate().getMonth(),	mTransaction.getTransactionDate().getDate()) ;
            break ;
        default:
            dialog = null ;
            break ;
        }
        return dialog;
    }
    
	/**
	 * Synchronise the display with the contents of the Holding and SellTransaction value objects
	 */
	private void updateToDisplay() {
		mStockTickerAutoCompleteTextView.setText(mTransaction.getStockCode()) ;
		mSellQuantityEditText.setText(String.valueOf(mSellQuantity)) ;
		mSellUnitPriceEditText.setText(String.valueOf(mTransaction.getUnitPrice())) ;		
		mSellDateButton.setText((mTransaction.getTransactionDate().getYear() + 1900) + "-" + (mTransaction.getTransactionDate().getMonth() + 1) + "-" + (mTransaction.getTransactionDate().getDate())) ;
	
		if(mTransaction.getSaleAllocations().isEmpty())
			mSellSelectButton.setEnabled(false) ;
		else
			mSellSelectButton.setEnabled(true) ;

		mProfitLossAmountTextView.setText(mFormatHelper.formatCurrency(mProfitLossAmount, false)) ;

		if(mProfitLossAmount < 0)
			mProfitLossAmountTextView.setTextColor(Color.RED) ;
		else
			mProfitLossAmountTextView.setTextColor(Color.GREEN) ;
	}
	
	/**
	 * Synchronise the SellTransaction and Holding value objects with the displayed values
	 */
	private void updateFromDisplay() {
		Log.i(TAG, "updateFromDisplay(): starts") ;

		mTransaction.setStockCode(mStockTickerAutoCompleteTextView.getText().toString().trim()) ;
        if(mSellQuantityEditText.getText().length() > 0)
        	mSellQuantity = Long.valueOf(mSellQuantityEditText.getText().toString().trim()) ;
        
        if(mSellUnitPriceEditText.getText().length() > 0)
        	mTransaction.setUnitPrice(Double.valueOf(mSellUnitPriceEditText.getText().toString().trim())) ;
        
        // No need to sync sell date because it is updated immediately by the date picker dialog
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

            /*
             * What stock code did we select?
             */
            String stockCode = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseOpenHelper.COL_SREF_STOCK_CODE));
            
            /*
             * If we selected the same stock code, no need to re-read the db
             */
            if(stockCode.compareTo(mTransaction.getStockCode()) == 0) 
            	return ;
            
            mTransaction.setStockCode(stockCode) ;
            
            /*
             * Grab all the holdings for the newly selected stock code
             */
    		mHoldingList = mBusinessLogic.getHoldingsByStockCode(mTransaction.getStockCode()) ;

    		/*
    		 * How many units of this stock do we have at our disposal?
    		 */
    		mMaxSellableQuantity = sumTotalHoldings(mHoldingList) ;
            mSellQuantityEditText.requestFocus() ;
            
            Log.i(TAG, "onItemClick(): selected stock=" + mTransaction.getStockCode());
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
					mTransaction.getTransactionDate().setDate(dayOfMonth) ;
					mTransaction.getTransactionDate().setMonth(monthOfYear) ;
					mTransaction.getTransactionDate().setYear(year-1900) ;
					mSellDateButton.setText((mTransaction.getTransactionDate().getYear() + 1900) + "-" + 
							(mTransaction.getTransactionDate().getMonth() + 1) + "-" +
							(mTransaction.getTransactionDate().getDate())) ;

					Log.i(TAG, "onDateSet(): sellDate=" + mTransaction.getTransactionDate().toLocaleString()) ;
				}
			};

	/**
	 * Show the purchase date dialog
	 */
	private OnClickListener mPurchaseDateOnClickListener = 
			new OnClickListener() {
		        public void onClick(View v) {
		            showDialog(DIALOG_DATE_PICKER) ;
		        }
		    };

	/**
	 * Save the sell transaction.
	 * 
	 * Validates that:
	 * - the stockcode is valid
	 * - the sell quantity is > 0 and <= total stock holdings
	 * 
	 * If valid, the sale is persisted and the affected holdings are returned to the caller.
	 */
	private OnClickListener mOkButtonOnClickListener =
			new OnClickListener() {
		        public void onClick(View v) {
		            updateFromDisplay() ;
		            
		            /*
		             * Check that the stock code and transaction quantity are valid
		             */
		            if(mTransaction.getStockCode() != null && mTransaction.getStockCode().length() == 3 && mSellQuantity > 0 && mSellQuantity <= mMaxSellableQuantity) {
		            	
		            	ArrayList<Holding> holdingList = mBusinessLogic.sellStock(mTransaction) ;
		            	
		            	Intent intent = new Intent("Sale complete") ;
		            	/*
		            	 * Pass the list of affected holdings back to PortfolioActivity so it can resync it's display
		            	 * without re-reading the db
		            	 * TODO: make sure PortfolioManager updates it's display from this holding list
		            	 */
		            	intent.putExtra(EXTRA_RESULT_HOLDING_LIST, holdingList) ; 
		            	intent.putExtra(EXTRA_RESULT_TRANSACTION, mTransaction) ;
		                setResult(RESULT_OK, intent);
		                finish() ;
		            } else if(mSellQuantity > mMaxSellableQuantity) {
						Toast.makeText(getApplicationContext(), "You only have " + mMaxSellableQuantity + " units to sell", Toast.LENGTH_LONG).show() ;
		            	mSellQuantityEditText.requestFocus() ; 
		            }
		        }
		    };

	/**
	 * Cancellation means close the activity.
	 */
	private OnClickListener mCancelButtonOnClickListener =
			new View.OnClickListener() {
		        public void onClick(View v) {
		            setResult(RESULT_CANCELED, (new Intent()).setAction("Cancelled"));
		        	finish() ;
		        }
		    };

    /**
     * Choose the method of allocation.
     * 
     * Can be one of:
     * - minimise capital gains
     * - maximise capital gains
     * - do it yourself
     */
	private OnItemSelectedListener mSellAllocationMethodSpinnerOnItemSelectedListener  =
			new OnItemSelectedListener() {
				private double mProfitLossAmount;

				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					mSellSelectButton.setEnabled(true) ;
					mTransaction.getSaleAllocations().clear() ;
					String choice = (String) mSellAllocationMethodSpinnerAdapter.getItem(position) ;
					if(choice.compareTo("Min gains") == 0) {
						mBusinessLogic.calculateQuantityBasedSaleAllocationToMinimiseCapitalGains(mSellQuantity, mHoldingList, mTransaction) ;
					} else if(choice.compareTo("Max gains") == 0) {
						mBusinessLogic.calculateQuantityBasedSaleAllocationToMaximiseCapitalGains(mSellQuantity, mHoldingList, mTransaction) ;
					} else
						;
					updateFromDisplay() ; // Put displayed unit price / quantity into transaction

					if(mTransaction.getSaleAllocations().isEmpty())
						mSellSelectButton.setEnabled(false) ;
					else
						mSellSelectButton.setEnabled(true) ;

					mProfitLossAmount = mBusinessLogic.calculateProfitLoss(mHoldingList, mTransaction) ;
					updateToDisplay() ; // Put calculated profit / loss amount into display field
				}
		
				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					mSellSelectButton.setEnabled(false) ;
				}
		    };

	/**
	 * Launch the sale allocation activity.
	 */
	private OnClickListener mSellSelectButtonOnClickListener =
			new View.OnClickListener() {
		        public void onClick(View v) {
		        	// TODO: Call sell allocation activity
//		        	startActivityForResult(intent, REQUEST_SALE_ALLOCATION) ;
		        }
		    };

		    
    /**
     * Process the result from the sale allocation activity. 
     */
	@Override()
	public void onActivityResult(int requestCode, int responseCode, Intent data) {
		if(requestCode == REQUEST_SALE_ALLOCATION && responseCode == RESULT_OK) {
			// FIXME: replace extra name with constant
			// TODO: implement sale allocation screen
			
			/*
			 * Grab the updated allocation from SellAllocationActivity
			 */
			mTransaction = (SellTransaction)data.getSerializableExtra("transaction") ;
			
			/*
			 * Replace our previous sale allocations with whatever was selected in SellAllocationActivity 
			 */
			mSellQuantity = 0 ;
			for(SellAllocation alloc : mTransaction.getSaleAllocations()) {
				mSellQuantity += alloc.getQuantity() ;
			}
			
			mProfitLossAmount = mBusinessLogic.calculateProfitLoss(mHoldingList, mTransaction) ;
			updateToDisplay() ;
		}
	}

	private OnFocusChangeListener mSellQuantityFocusChangeListener = new OnFocusChangeListener() {
		
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if(!hasFocus) {
				updateFromDisplay() ;
				if(mSellQuantity > mMaxSellableQuantity)
					mSellQuantity = mMaxSellableQuantity ;
				mProfitLossAmount = mBusinessLogic.calculateProfitLoss(mHoldingList, mTransaction) ;
				updateToDisplay() ;
			}
		}
	} ;

	private OnFocusChangeListener mSellUnitPriceFocusChangeListener = new OnFocusChangeListener() {
		
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if(!hasFocus) {
				updateFromDisplay() ;
				mProfitLossAmount = mBusinessLogic.calculateProfitLoss(mHoldingList, mTransaction) ;
				updateToDisplay() ;
			}
		}
	} ;

	/**
	 * Convenience method to sum the current holdings for the stock.
	 * 
	 * @param mHoldingList
	 * @return
	 */
	private long sumTotalHoldings(ArrayList<Holding> mHoldingList) {
    	long totalHoldings = 0 ;
    	for(Holding h : mHoldingList) {
    		totalHoldings += h.getRemainingQuantity() ;
    	}
    	return totalHoldings ;
	}
}