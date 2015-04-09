package org.amplexus.app.ozstock2;

import java.util.ArrayList;
import java.util.Map;

import org.amplexus.app.ozstock2.helper.BusinessLogicHelper;
import org.amplexus.app.ozstock2.helper.TextFormatHelper;
import org.amplexus.app.ozstock2.values.Holding;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.TextView;

/**
 * 
 * TODO: Disable option menu sell if nothing to sell 
 * 
 * @author craig
 *
 */
public class PortfolioActivity extends ListActivity {

    private static final String TAG = PortfolioActivity.class.getSimpleName() ;

    public static final String EXTRA_PORTFOLIO_ID = "portfolioId";
	public static final String EXTRA_PORTFOLIO_NAME = "portfolioName";

	private static final int REQUEST_CODE_BUY_HOLDING = 1 ;	// Purchase stock 
	public static final int REQUEST_CODE_SELL_HOLDING = 2 ;	// Sell stock
	
    private ListView mListView ;							// The ListView containing the stock holdings
    private TextView mPortfolioNameTextView ;				// The portfolio name displayed above the ListView.
    private TextView mPortfolioProfitLossTextView ;			// The portfolio P/L amount displayed above the ListView

	private TextFormatHelper mFormatHelper ;				// Number and date text formatting.
    private BusinessLogicHelper mBusinessLogicHelper ;		// Persistent store of stock holdings
    private int mSelectedPosition ;							// The currently selected holding (for context menu operations)
    private HoldingAdapter mAdapter ;						// An in-memory cache of holdings managed by the ListView 
    private long mPortfolioId ;								// Key of the portfolio whose holdings we are displaying.
    private String mPortfolioName ;							// Name of the portfolio whose holdings we are displaying.
    
    private PortfolioFinanceServiceReceiver mFinanceServiceReceiver ;	// The receiver that processes FinanceService broadcast messages.
    private AlarmReceiver mAlarmReceiver ;					// The receiver that processes alarm broadcast messages. 
    private SharedPreferences mLastPricePrefs ;				// The last fetched price
    
	private OnItemClickListener mListViewItemClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        	mSelectedPosition = position ;
            Intent intent = new Intent(getApplicationContext(), SellActivity.class) ;
            Bundle b = new Bundle() ;
            b.putLong(SellActivity.EXTRA_HOLDING_ID, mAdapter.getItem(mSelectedPosition).getId()) ;
            intent.putExtras(b) ;
            Log.i(TAG, "onItemClick(): holdingId=" + mAdapter.getItem(mSelectedPosition).getId() ) ;
            Log.i(TAG, "onItemClick(): portfolioId=" + mAdapter.getItem(mSelectedPosition).getPortfolioId() ) ;
            startActivity(intent) ;
        }
    } ;

    /**
     * Called when the activity is first created. This is where you should do all of your normal static 
     * set up to create views, bind data to lists, and so on. This method is passed a Bundle object 
     * containing the activity's previous state, if that state was captured. Always followed by onStart().
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
    	// TODO: Add context menu (delete, move, view tx, view stock news, view stock chart) and options menu (delete selected stocks, move selected stocks to another portfolio)
    	
    	super.onCreate(savedInstanceState) ;
        setContentView(R.layout.portfolio) ; // if you want to use your own layout, make sure the listview has an id "@android:id/list"
        
        mPortfolioNameTextView = (TextView)findViewById(R.id.portfolioName) ;        
        mPortfolioProfitLossTextView = (TextView)findViewById(R.id.portfolioProfitLoss) ;
        
        mBusinessLogicHelper = new BusinessLogicHelper(getApplicationContext()) ;
        mFormatHelper = new TextFormatHelper() ; 
        
        /*
         * Read intent parameters
         */
        Bundle extras = getIntent().getExtras() ;
        mPortfolioId = extras.getLong(EXTRA_PORTFOLIO_ID) ;
        mPortfolioName = extras.getString(EXTRA_PORTFOLIO_NAME) ;

        mPortfolioNameTextView.setText(mPortfolioName) ;        

        /*
         * Read holdings from the database.
         */
        ArrayList<Holding> holdingList = mBusinessLogicHelper.getHoldingsByPortfolio(mPortfolioId) ;

        mAdapter = new HoldingAdapter(getApplicationContext(), -1, holdingList) ;
        setListAdapter(mAdapter);

        mListView = getListView() ;
        mListView.setOnItemClickListener(mListViewItemClickListener) ;

        Log.i(TAG, "onCreate() done") ;
    }

    /**
     * Called just before the activity starts interacting with the user. At this point the activity is at
     * the top of the activity stack, with user input going to it. Always followed by onPause().
     */
    @Override
    protected void onResume() {
        super.onResume();
        
        /*
         * We want to listen to any price updates from the FinanceService
         */
        IntentFilter filter = new IntentFilter(FinanceService.ACTION_NOTIFY);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        mFinanceServiceReceiver = new PortfolioFinanceServiceReceiver();
        registerReceiver(mFinanceServiceReceiver, filter);
        
        /*
         * We want to listen to any wakeup calls from the alarm service
         */
		Log.i(TAG, "onResume() listen for wakeup calls from alarm service");
        IntentFilter alarmFilter = new IntentFilter(AlarmReceiver.RESPONSE_ACTION) ;
        alarmFilter.addCategory(Intent.CATEGORY_DEFAULT) ;
        mAlarmReceiver = new AlarmReceiver() ;
        registerReceiver(mAlarmReceiver, alarmFilter) ;
        registerForContextMenu(mListView) ;

        /*
         * Download latest stock prices
         */
       	refreshPrices(false) ;
        
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
        unregisterReceiver(mAlarmReceiver) ;
        unregisterReceiver(mFinanceServiceReceiver) ;
        unregisterForContextMenu(mListView) ;
        Log.i(TAG, "onPause() done") ;
    }
    
    /**
     * An adapter to manage the collection of portfolios and their binding to a GridView.
     * 
     * Overrides getView() so we can render a named icon in the GridView.
     */
    public class HoldingAdapter extends ArrayAdapter<Holding> {

        private final String TAG = HoldingAdapter.class.getSimpleName() ;
        
		private Context mContext ;
		private LayoutInflater vi = getLayoutInflater() ;


        public HoldingAdapter(Context context, int textViewResourceId, ArrayList<Holding> holdingList) {
			super(context, textViewResourceId, holdingList);
		}

		@Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view ;
        	ViewHolder holder ;

			Log.i(TAG, "getView(): rendering item in position: " + position + " with dbid=" + 
					getItem(position).getId()) ;

            if (convertView == null) {
    			view = vi.inflate(R.layout.portfolio_item, null);
                holder = new ViewHolder();
//                holder.selected = (CheckBox)view.findViewById(R.id.itemSelectedCheck);
                
                holder.stockCode = (TextView) view.findViewById(R.id.itemStockCode);
                holder.purchaseDate = (TextView) view.findViewById(R.id.itemPurchaseDate);
                
                holder.currentUnitPrice = (TextView) view.findViewById(R.id.itemCurrentUnitPrice);
                holder.purchaseUnitPrice = (TextView) view.findViewById(R.id.itemPurchaseUnitPrice);
                holder.purchaseQuantity = (TextView) view.findViewById(R.id.itemPurchaseQuantity);
                holder.purchaseTotalCost = (TextView) view.findViewById(R.id.itemPurchaseTotalCost);
                holder.currentValue = (TextView) view.findViewById(R.id.itemCurrentValue);
                holder.profitLossAmount = (TextView) view.findViewById(R.id.itemProfitLossAmount);
                holder.profitLossPercent = (TextView) view.findViewById(R.id.itemProfitLossPercent);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder) view.getTag() ;
            }

			Holding holding = getItem(position) ;
			
			holder.stockCode.setText(holding.getStockCode()) ;
			holder.purchaseDate.setText(mFormatHelper.formatDate(holding.getPurchaseDate())) ;

			if(mLastPricePrefs == null)
				mLastPricePrefs = getSharedPreferences(MainActivity.SHARED_PREFS_LAST_PRICE_FILENAME, MODE_PRIVATE) ;
			float currentPrice = mLastPricePrefs.getFloat(holding.getStockCode(), 0.0f) ;

			holder.currentUnitPrice.setText(mFormatHelper.formatCurrency(currentPrice, true)) ;
			
			double purchaseCost = holding.getPurchaseUnitPrice() * holding.getRemainingQuantity() ;

			holder.purchaseUnitPrice.setText(mFormatHelper.formatCurrency(holding.getPurchaseUnitPrice(), true)) ;
			holder.purchaseQuantity.setText(mFormatHelper.formatLong(holding.getRemainingQuantity())) ;
			holder.purchaseTotalCost.setText(mFormatHelper.formatCurrency(purchaseCost, false)) ;


			double currentValue = currentPrice * holding.getRemainingQuantity() ;
			double profitLossAmount = currentValue - purchaseCost ;
			double profitLossPercent = profitLossAmount / purchaseCost * 100 ;
			
			holder.currentValue.setText(mFormatHelper.formatCurrency(currentValue, false)) ;
			holder.profitLossAmount.setText(mFormatHelper.formatCurrency(Math.abs(profitLossAmount), false)) ;
			holder.profitLossPercent.setText(mFormatHelper.formatPercent(Math.abs(profitLossPercent))) ;
			
			if(profitLossAmount >= 0) {
				holder.profitLossAmount.setTextColor(Color.GREEN) ;
				holder.profitLossPercent.setTextColor(Color.GREEN) ;
			} else {
				holder.profitLossAmount.setTextColor(Color.RED) ;
				holder.profitLossPercent.setTextColor(Color.RED) ;
			}
			
            return view ;
        }

		class ViewHolder {
//        	CheckBox selected ;
            TextView stockCode ;
            TextView purchaseDate ;
            TextView currentUnitPrice ;
            TextView purchaseUnitPrice ;
            TextView purchaseQuantity ;
            TextView purchaseTotalCost ;
            TextView currentValue ;
            TextView profitLossAmount ;
            TextView profitLossPercent ;
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
        inflater.inflate(R.menu.portfolio_context_menu, menu);
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
        Intent intent ;
        Bundle b ;
        switch (item.getItemId()) {
        case R.id.buyHolding:
            intent = new Intent(getApplicationContext(), BuyActivity.class) ;
            b = new Bundle() ;
            b.putLong(BuyActivity.EXTRA_PORTFOLIO_ID, mAdapter.getItem(mSelectedPosition).getPortfolioId()) ;
            b.putString(BuyActivity.EXTRA_STOCK_CODE, mAdapter.getItem(mSelectedPosition).getStockCode()) ;
            intent.putExtras(b) ;
            startActivityForResult(intent, REQUEST_CODE_BUY_HOLDING) ;
            return true;
        case R.id.sellHolding:
            intent = new Intent(getApplicationContext(), SellActivity.class) ;
            b = new Bundle() ;
            b.putLong(SellActivity.EXTRA_HOLDING_ID, mAdapter.getItem(mSelectedPosition).getId()) ;
            intent.putExtras(b) ;
            startActivity(intent) ;
            return true;
        case R.id.deleteHolding:
        	showDialog(DIALOG_DELETE_HOLDING) ;
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * Create the options menu associated with the PortfolioActivity.
     * 
     * Inflates R.menu.portfolio_options_menu.
     * 
     * @param menu the menu in which we populate menu items.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.portfolio_options_menu, menu);
        return true;
    }
    
    /**
     * Process the selected menu item.
     * 
     * Handless refreshing stock prices, adding new portfolios and an "about" dialogue box.
     * 
     * @param item the selected menu item.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.refreshPrices:
            	if(! mAdapter.isEmpty())
            		refreshPrices(true) ;            	
                return true;
            // TODO: Add sellHolding option
            case R.id.buyHolding:
                Intent intent = new Intent(getApplicationContext(), BuyActivity.class) ;
                Bundle b = new Bundle() ;
                b.putLong(BuyActivity.EXTRA_PORTFOLIO_ID, mPortfolioId) ;
                intent.putExtras(b) ;
                startActivityForResult(intent, REQUEST_CODE_BUY_HOLDING) ;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshPrices(boolean force) {
        if(mAdapter.isEmpty())
        	return ;
        showDialog(DIALOG_PROGRESS) ;
    	Intent intent = new Intent(this, FinanceService.class);
		if(force) {
			Bundle b = new Bundle() ;
			b.putBoolean(FinanceService.REQUEST_EXTRA_FORCE_REFRESH, force) ;
			intent.putExtras(b) ;
		}
		Log.i(TAG, "Refreshing prices via FinanceService...") ;
    	startService(intent);		
	}

	static final int DIALOG_PROGRESS = 1 ;
    static final int DIALOG_DELETE_HOLDING = 2 ;

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
        case DIALOG_PROGRESS:
        	dialog = ProgressDialog.show(this, "", "Updating...", true);
        	break ;
        case DIALOG_DELETE_HOLDING:
        	dialog = makeDeleteHoldingDialog(this) ;
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
	private Dialog makeDeleteHoldingDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	builder = new AlertDialog.Builder(this);
    	builder.setMessage("All transaction history will be deleted. Are you sure?")
    		   .setTitle("Delete Holding")
    	       .setCancelable(true)
    	       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	               Log.i(TAG, "makeDeleteHoldingDialog(): onClick(): " +
    	            		   "mSelectedPosition=" + mSelectedPosition + ", dbid=" + mAdapter.getItem(mSelectedPosition).getId()) ;

    	        	   mBusinessLogicHelper.deleteHolding(mAdapter.getItem(mSelectedPosition).getId()) ;
    	        	   mAdapter.remove(mAdapter.getItem(mSelectedPosition)) ;
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

	/**
	 *
	 */
	public class PortfolioFinanceServiceReceiver extends BroadcastReceiver {
		private final String TAG = PortfolioFinanceServiceReceiver.class.getSimpleName() ;
		
		@Override
	    public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "onReceive() got a broadcast msg") ;
			
			/*
			 * Check whether the prices were fetched succesfully - if not then abort
			 */
			boolean updatedPrice = false ;
			boolean success = intent.getBooleanExtra(FinanceService.RESPONSE_EXTRA_STATUS, false) ;
			if(! success) {
				Log.e(TAG, "onReceive(): FinanceService says it can't fetch quotes") ;
	        	dismissDialog(DIALOG_PROGRESS) ;
				return ;
			}

			/*
			 * Grab the updated stock prices
			 */
			mLastPricePrefs = getSharedPreferences(MainActivity.SHARED_PREFS_LAST_PRICE_FILENAME, MODE_PRIVATE) ;

			Map<String, ?> lastPriceList = mLastPricePrefs.getAll() ;
			double totalCost = 0.0f ;
			double totalValue = 0.0f ;
			int count = mAdapter.getCount() ;
			for(int i = 0; i < count; i++) {
				Holding holding = mAdapter.getItem(i) ;
				float currentPrice = mLastPricePrefs.getFloat(holding.getStockCode(), -0.0f) ;
				if(currentPrice == 0.0f)
					continue ;

				double currentValue = currentPrice * holding.getRemainingQuantity() ;
				double purchaseCost = holding.getPurchaseUnitPrice() * holding.getRemainingQuantity() ;
				
				totalCost += purchaseCost ;
				totalValue += currentValue ;
				
				updatedPrice = true ;
			}

			double profitLossAmount = totalValue - totalCost ;
			double profitLossPercent = profitLossAmount / totalCost * 100 ;
			String profitLossMsg = mFormatHelper.formatCurrency(profitLossAmount, false) ;
			profitLossMsg = profitLossMsg.concat(" / ") ;
			profitLossMsg = profitLossMsg.concat(mFormatHelper.formatPercent(profitLossPercent)) ;
			mPortfolioProfitLossTextView.setText(profitLossMsg) ;
			
			if(profitLossAmount >= 0) {
				mPortfolioProfitLossTextView.setTextColor(Color.GREEN) ;
			} else {
				mPortfolioProfitLossTextView.setTextColor(Color.RED) ;
			}
			
			if(updatedPrice)
				mAdapter.notifyDataSetChanged() ;
        	dismissDialog(DIALOG_PROGRESS) ;
	    }
	}
	
	/**
	 * 
	 */
	public class AlarmReceiver extends BroadcastReceiver {
		public static final String RESPONSE_ACTION = "ozstock.alarm.receiver" ;
		public final String TAG = AlarmReceiver.class.getSimpleName() ;
	    @Override
	    public void onReceive(Context context, Intent intent) {
			showDialog(DIALOG_PROGRESS) ;
	    	Intent financeServiceIntent = new Intent(PortfolioActivity.this, FinanceService.class);
			Log.i(TAG, "AlarmReceiver: Refreshing prices via FinanceService...") ;
			context.startService(financeServiceIntent) ;
	    }
	}
	
    /**
     * This method is called when the sending activity has finished, with the
     * result it supplied.
     * 
     * @param requestCode The original request code as given to startActivity().
     * @param resultCode From sending activity as per setResult().
     * @param data From sending activity as per setResult().
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                Intent data) {
    	/*
    	 * If we added a new holding then refresh stock prices
    	 */
        if (requestCode == REQUEST_CODE_BUY_HOLDING && resultCode == RESULT_OK) {
        	Holding holding = (Holding)data.getSerializableExtra(BuyActivity.EXTRA_RESULT_HOLDING) ;
        	double purchasePrice = holding.getPurchaseUnitPrice() ;
        	long units = holding.getRemainingQuantity() ;

        	mAdapter.add(holding) ;
        	refreshPrices(true) ;
        } else if (requestCode == REQUEST_CODE_SELL_HOLDING && resultCode == RESULT_OK) {
        	boolean updated = false ;
        	
        	// FIXME: probably should make the holdingList a map
        	ArrayList<Holding> holdingList = (ArrayList<Holding>)data.getSerializableExtra(SellActivity.EXTRA_RESULT_HOLDING_LIST) ;
        	for(int i = 0; i < mAdapter.getCount(); i++) {
        		long holdingId = mAdapter.getItemId(i) ;
            	for(Holding modifiedHolding : holdingList) {
            		if(holdingId == modifiedHolding.getId()) {
            			Holding holding = mAdapter.getItem(i) ;
            			holding.setRemainingQuantity(modifiedHolding.getRemainingQuantity()) ;
            			updated = true ;
            		}
            	}
            	if(updated)
            		mAdapter.notifyDataSetChanged() ;
        	}
        }
    }
}
