package org.amplexus.app.ozstock2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

import org.amplexus.app.ozstock2.helper.BusinessLogicHelper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Handles downloading stock prices from google finance.
 * 
 * Once downloaded, the stock prices are stored in a shared preference file.
 * 
 * Once downloaded, a broadcast message is sent to all receivers interested in knowing the latest stock prices have been downloaded.
 * 
 * @author craig
 */

/*
 * Google:
 * - upto 100 quotes in one call
 * - quotes: http://www.google.com/finance/info?infotype=infoquoteall&q=ASX:BHP,ASX:RIO
		// [
		{
		"id": "676140"
		,"t" : "BHP"
		,"e" : "ASX"
		,"l" : "33.90"
		,"l_cur" : "A$33.90"
		,"s": "0"
		,"ltt":"4:10PM AEST"
		,"lt" : "Apr 12, 4:10PM AEST"
		,"c" : "+0.31"
		,"cp" : "0.92"
		,"ccol" : "chg"
		,"eo" : ""
		,"delay": "20"
		,"op" : "33.67"
		,"hi" : "33.90"
		,"lo" : "33.61"
		,"vo" : "8.70M"
		,"avvo" : ""
		,"hi52" : "49.33"
		,"lo52" : "33.59"
		,"mc" : "180.48B"
		,"pe" : "8.22"
		,"fwpe" : ""
		,"beta" : ""
		,"eps" : "4.12"
		,"shares" : "5.32B"
		,"inst_own" : ""
		,"name" : "BHP Billiton Limited"
		,"type" : "Company"
		}
		,{
		"id": "676309"
		,"t" : "RIO"
		,"e" : "ASX"
		,"l" : "64.46"
		,"l_cur" : "A$64.46"
		,"s": "0"
		,"ltt":"4:10PM AEST"
		,"lt" : "Apr 12, 4:10PM AEST"
		,"c" : "+0.94"
		,"cp" : "1.48"
		,"ccol" : "chg"
		,"eo" : ""
		,"delay": "20"
		,"op" : "63.90"
		,"hi" : "64.62"
		,"lo" : "63.85"
		,"vo" : "2.78M"
		,"avvo" : ""
		,"hi52" : "87.87"
		,"lo52" : "58.52"
		,"mc" : "119.20B"
		,"pe" : "22.36"
		,"fwpe" : ""
		,"beta" : ""
		,"eps" : "2.88"
		,"shares" : "1.85B"
		,"inst_own" : ""
		,"name" : "Rio Tinto Limited"
		,"type" : "Company"
		}
		]
 * - codes:
	  avvo    * Average volume (float with multiplier, like '3.54M')
	  beta    * Beta (float)
	  c       * Amount of change while open (float)
	  ccol    * (unknown) (chars)
	  cl        Last perc. change
	  cp      * Change perc. while open (float)
	  e       * Exchange (text, like 'NASDAQ')
	  ec      * After hours last change from close (float)
	  eccol   * (unknown) (chars)
	  ecp     * After hours last chage perc. from close (float)
	  el      * After. hours last quote (float)
	  el_cur  * (unknown) (float)
	  elt       After hours last quote time (unknown)
	  eo      * Exchange Open (0 or 1)
	  eps     * Earnings per share (float)
	  fwpe      Forward PE ratio (float)
	  hi      * Price high (float)
	  hi52    * 52 weeks high (float)
	  id      * Company id (identifying number)
	  l       * Last value while open (float)
	  l_cur   * Last value at close (like 'l')
	  lo      * Price low (float)
	  lo52    * 52 weeks low (float)
	  lt        Last value date/time
	  ltt       Last trade time (Same as "lt" without the data)
	  mc      * Market cap. (float with multiplier, like '123.45B')
	  name    * Company name (text)
	  op      * Open price (float)
	  pe      * PE ratio (float)
	  t       * Ticker (text)
	  type    * Type (i.e. 'Company')
	  vo      * Volume (float with multiplier, like '3.54M') 
 * 
 * TODO: Only pull down the stock data i'm interested in, not all the other ebitda guff etc.
 * TODO: Implement timeout mechanism
 * TODO: Handle google's max 100 tickers limitation.
 * 
 * @author craig
 *
 */
public class FinanceService extends IntentService {

	private static final String	TAG = FinanceService.class.getSimpleName() ;
	
	public static final String ACTION_NOTIFY = "org.amplexus.app.ozstock2.intent.action.ACTION_FINANCE_SERVICE_NOTIFY" ;

	public static final String REQUEST_EXTRA_FORCE_REFRESH = "ozstock.force" ; 		// consumer is requesting force refresh
	public static final String RESPONSE_EXTRA_STATUS = "ozstock.status" ;			// the indicator of refresh success
	
	private static final String	GOOGLE_URL_PREFIX = "http://www.google.com/finance/info?infotype=infoquoteall&q=" ;
	private static final String	GOOGLE_URL_SEPARATOR = "," ;						// Tickers are separated by commas
	private static final int	GOOGLE_MAX_STOCKS_PER_REQUEST = 100 ;				// Google won't let us send more than 100 tickers at a time

    private BusinessLogicHelper mBusinessLogicHelper ;								// Persistent store of stock holdings
	
	public FinanceService() {
		this("FinanceService") ;
	}
	
	public FinanceService(String name) {
		super(name);
		mBusinessLogicHelper = new BusinessLogicHelper(this) ;
	}

	/**
	 * Handles requests for updating stock quotes.
	 *
	 * Does the following:
	 * 
	 * - Checks to see if it's too soon to refresh stock prices and aborts (without error) if so - though this can be overridden by a "force refresh" flag 
	 * - Gets a list of all the stock codes from the database whose prices we want to download.
	 * - Generates a URL to query google
	 * - Sends the HTTP request to google
	 * - Parses the JSON response from google
	 * - Stores the stock prices in a shared preference file.
	 * - Sends a broadcast message indicating stock prices are updated
	 * 
	 * @param intent the input request
	 * 
	 * TODO: Break the data up into 100 stock blocks because Google doesn't like > 100 stocks. 
	 */
	@Override
	protected void onHandleIntent(Intent intent) {

		Log.i(TAG, "onHandleIntent() Starting") ;
		
		ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		 
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork.isConnectedOrConnecting();
		if(!isConnected) {
			Log.e(TAG, "onHandleIntent(): Error: We don't have any network connectivity") ;

			Intent broadcastIntent = new Intent() ;
			broadcastIntent.setAction(FinanceService.ACTION_NOTIFY);
			broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
			broadcastIntent.putExtra(RESPONSE_EXTRA_STATUS, false) ;
			sendBroadcast(broadcastIntent);
			
			return ;			
		}
		
		boolean forceRefreshRequested = intent.getBooleanExtra(FinanceService.REQUEST_EXTRA_FORCE_REFRESH, false) ;
		Log.i(TAG, "onHandleIntent(): force refresh requested = " + forceRefreshRequested ) ;

		/*
		 * If we last updated really recently, then nothing to do - unless user is forcing an update.
		 * Just pretend we succeeded and the consuming activity will pick up the previously fetched prices.
		 */
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this) ;
		long lastPriceTimeMillis = prefs.getLong(MainActivity.SHARED_PREFS_LAST_PRICE_TIME_ATTR, 0L) ;
		int priceFetchIntervalMins = prefs.getInt(MainActivity.SHARED_PREFS_PRICE_FETCH_INTERVAL_MINS_ATTR, MainActivity.DEFAULT_PRICE_FETCH_INTERVAL_MINS) ;
		long priceFetchIntervalMillis = priceFetchIntervalMins * 60 * 1000 ;

		/*
		 * It's too soon if the last fetch + fetch interval > now.
		 * 
		 * Added a 10 second buffer due to the inexact nature of inexact repeating otherwise the alarm is skipped because it's out by a few seconds.
		 */
		long lastFetchedMillisAgo = Calendar.getInstance().getTimeInMillis() - lastPriceTimeMillis ;
		boolean refreshDue = lastFetchedMillisAgo > priceFetchIntervalMillis - (10 * 1000) ;
		
		if( ! refreshDue && ! forceRefreshRequested) {
			Log.w(TAG, "onHandleIntent(): Prices were already fetched " + ((Calendar.getInstance().getTimeInMillis() - lastPriceTimeMillis) / 1000) + " seconds ago - no need to fetch prices from google.") ;

			Intent broadcastIntent = new Intent() ;
			broadcastIntent.setAction(ACTION_NOTIFY);
			broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
			broadcastIntent.putExtra(RESPONSE_EXTRA_STATUS, true) ;
			sendBroadcast(broadcastIntent);
			
			return ;
		}
		
		if(!forceRefreshRequested)
			Log.i(TAG, "onHandleIntent(): Refresh is due - last fetched " + ((Calendar.getInstance().getTimeInMillis() - lastPriceTimeMillis) / 1000) + " seconds ago.") ;

		/*
		 * Retrieve the list of stocks we want quotes for from the database.
		 */
		ArrayList<String> stockList = mBusinessLogicHelper.readAllPortfolioStockCodes() ;
		
		/*
		 * If there are no stocks in any portfolio, then nothing to do - but this is NOT an error.
		 */
		if(stockList.size() == 0) {
			Log.w(TAG, "onHandleIntent(): There are no stocks in any portfolio - no need to fetch prices from google.") ;

			Intent broadcastIntent = new Intent() ;
			broadcastIntent.setAction(FinanceService.ACTION_NOTIFY);
			broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
			broadcastIntent.putExtra(RESPONSE_EXTRA_STATUS, true) ;
			sendBroadcast(broadcastIntent);
			
			return ;
		}
		
		/*
		 * Generate a URL that we send to Google to retrieve the quotes
		 */
		String url = toGoogleQuoteURL(stockList) ;
		
		/*
		 * Invoke the URL and wait for the response in JSON format
		 */
		String stockPrices = readGoogleStockPrices(url) ;

		Log.i(TAG, "onHandleIntent(): Unparsed JSON result is " + stockPrices) ;
		
		if(stockPrices.length() == 0) {
			Log.e(TAG, "onHandleIntent(): Error: stockprice list returned from google is empty. Either a code bug or comms error occurred.") ;

			Intent broadcastIntent = new Intent() ;
			broadcastIntent.setAction(FinanceService.ACTION_NOTIFY);
			broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
			broadcastIntent.putExtra(RESPONSE_EXTRA_STATUS, false) ;
			sendBroadcast(broadcastIntent);
			
			return ;
		}

		/*
		 * Remove the slashes at the beginning of the JSON result
		 */
		stockPrices = stockPrices.substring(2) ;

		/*
		 * Download quotes from google and store in shared preferences file.
		 */
		try {
			SharedPreferences lastPricePrefs = getSharedPreferences(MainActivity.SHARED_PREFS_LAST_PRICE_FILENAME, MODE_PRIVATE) ;

			SharedPreferences.Editor lastPriceEditor = lastPricePrefs.edit();
			lastPriceEditor.clear() ;

			JSONArray jsonArray = new JSONArray(stockPrices) ;
			Log.i(TAG, "onHandleIntent(): Number of quotes fetched from google is: " + jsonArray.length()) ;
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i) ;
				String ticker = jsonObject.getString("t") ;
				float price = (float)jsonObject.getDouble("l") ;
				lastPriceEditor.putFloat(ticker, price) ;
//				Log.i(TAG, "onHandleIntent(): google-finance: retrieved quote " + ticker + "=" + price) ;
			}
			
			lastPriceEditor.commit() ;

			/*
			 * Update the lastPriceTime in the application prefs
			 */
			SharedPreferences.Editor prefsEditor = prefs.edit();
			prefsEditor.putLong(MainActivity.SHARED_PREFS_LAST_PRICE_TIME_ATTR, Calendar.getInstance().getTimeInMillis()) ;
			prefsEditor.commit() ;

			Log.i(TAG, "onHandleIntent(): Prices downloaded successfully, broadcasting results") ;

			Intent broadcastIntent = new Intent() ;
			broadcastIntent.setAction(FinanceService.ACTION_NOTIFY);
			broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
			broadcastIntent.putExtra(RESPONSE_EXTRA_STATUS, true) ;
			sendBroadcast(broadcastIntent);
			
		} catch (Exception e) {
			Log.i(TAG, "onHandleIntent(): Error parsing JSON", e) ;
			
			Intent broadcastIntent = new Intent() ;
			broadcastIntent.setAction(FinanceService.ACTION_NOTIFY);
			broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
			broadcastIntent.putExtra(RESPONSE_EXTRA_STATUS, false) ;
			sendBroadcast(broadcastIntent);
			
			return ;
		}
	}
	
	/**
	 * Takes the list of stock codes and generates a URL that can be used to query their stock prices on google finance.
	 * 
	 * @param stockList the list of stock codes
	 * @return
	 */
	private String toGoogleQuoteURL(ArrayList<String> stockList) {
		StringBuilder builder = new StringBuilder(GOOGLE_URL_PREFIX);
		
		boolean firstTime = true ;
		for(String stock : stockList) {
			if(!firstTime)
				builder.append(GOOGLE_URL_SEPARATOR) ;
			firstTime = false ;
			builder.append("ASX:" + stock) ;
		}
		return builder.toString() ;
	}

	/**
	 * Sends the HTTP request to google finance and reads the result.
	 * 
	 * @param url
	 * @return
	 */
	public String readGoogleStockPrices(String url) {
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);
		
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				Log.e(TAG, "readGoogleStockPrices(): Failed to download quotes: " + statusLine) ;
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG, "readGoogleStockPrices(): Failed to download quotes: " + e.getMessage(), e) ;
		} catch (IOException e) {
			Log.e(TAG, "readGoogleStockPrices(): Failed to download quotes: " + e.getMessage(), e) ;
		}
		return builder.toString();
	}
	
}
