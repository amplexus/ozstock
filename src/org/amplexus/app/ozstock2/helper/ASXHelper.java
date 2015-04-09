package org.amplexus.app.ozstock2.helper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;

import org.amplexus.app.ozstock2.values.StockRef;
import org.apache.http.util.ByteArrayBuffer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;

public class ASXHelper {
	public static final String TAG = ASXHelper.class.getSimpleName() ;
	
	public static final String ASX_LISTED_COMPANIES_CSV_URL = "http://www.asx.com.au/asx/research/ASXListedCompanies.csv" ;
	public static final String ASX_LISTED_COMPANIES_CSV_ASSET_FILENAME = "ASXListedCompanies.csv" ;
	public static final String ASX_LISTED_COMPANIES_CSV_DOWNLOAD_FILENAME = 
			Environment.getExternalStorageDirectory() + File.separator + "ozstock2" + File.separator + ASX_LISTED_COMPANIES_CSV_ASSET_FILENAME ;
	
	private Context mContext ;
	private BusinessLogicHelper mBusinessLogic ;

	public ASXHelper(Context context) {
		mContext = context ;
		mBusinessLogic = new BusinessLogicHelper(context) ;
	}
	
	/**
	 * Loads the STOCK_REF table from the downloaded listed companies file.
	 *  
	 * @return true if the file was loaded successfully, false if not.
	 */
	public boolean loadStockRefFromDownloadedFile(ArrayList<StockRef> stockRefList) {

		Log.i(TAG, "loadStockRef(): no rows found in DB. seeding DB from " + ASXHelper.ASX_LISTED_COMPANIES_CSV_DOWNLOAD_FILENAME) ;
		try {
			InputStream is = new FileInputStream(ASXHelper.ASX_LISTED_COMPANIES_CSV_DOWNLOAD_FILENAME) ;
			loadStockRefFromStream(is, stockRefList);
			is.close() ;
		} catch (IOException e) {
			Log.e(TAG, "loadStockRefFromDownloadedFile(): Error reading " + ASXHelper.ASX_LISTED_COMPANIES_CSV_DOWNLOAD_FILENAME	+ " -> " + e.getMessage(), e);
			return false ;
		}
		Log.i(TAG, "loadStockRefFromDownloadedFile(): loaded " + stockRefList.size() + " records into DB");
		return true ;
	}
	
	/**
	 * Loads the STOCK_REF table from the downloaded listed companies file.
	 *  
	 * @return true if the file was loaded successfully, false if not.
	 */
	public boolean loadStockRefFromAssets(ArrayList<StockRef>stockRefList) {

		Log.i(TAG, "loadStockRef(): reading from DB");

		try {
			InputStream is = mContext.getAssets().open(ASXHelper.ASX_LISTED_COMPANIES_CSV_ASSET_FILENAME);
			loadStockRefFromStream(is, stockRefList);
			is.close() ;
		} catch (IOException e) {
			Log.e(TAG, "loadStockRefFromAssets(): Error reading " + ASXHelper.ASX_LISTED_COMPANIES_CSV_ASSET_FILENAME + " -> " + e.getMessage(), e);
			return false ;
		}
		Log.i(TAG, "loadStockRefFromAssets(): loaded " + stockRefList.size() + " records into DB");
		return true ;
	}
	
	/**
	 * Helper for loading the STOCK_REF table and populating the accompanying cache.
	 * 
	 * Deletes all rows in the STOCK_REF table
	 * Resets the cache
	 * Reads the listed companies file.
	 * For each record:
	 *  - inserts a row into STOCK_REF
	 *  - inserts an entry into the cached copy
	 * @param is
	 * @throws IOException
	 */
	private void loadStockRefFromStream(InputStream is, ArrayList<StockRef> stockRefList) throws IOException {

		stockRefList.clear() ;

		long startMillis = Calendar.getInstance().getTimeInMillis() ;
		BufferedReader br = new BufferedReader(new InputStreamReader(is)) ;
		String readLine = null ;
		while ((readLine = br.readLine()) != null) {
			String[] fields = readLine.split(",") ;
			if (fields.length >= 3) {
				StockRef stockRef = new StockRef() ;
				stockRef.setStockCode(fields[1]) ;
				String stockName = fields[0].replaceAll("\"", "") ;
				stockRef.setStockName(stockName) ;
				stockRefList.add(stockRef) ;
			} else {
				Log.i(TAG, "loadStockRefFromStream(): ignoring record with " + fields.length + " fields: " + readLine) ;
			}
		}
		long fileLoadEndMillis = Calendar.getInstance().getTimeInMillis() ;
		Log.i(TAG, "loadStockRefFromStream: stock ref file loaded in " + (fileLoadEndMillis - startMillis) + "ms") ;
		startMillis = Calendar.getInstance().getTimeInMillis() ;
		mBusinessLogic.writeAllStockRef(stockRefList) ;
		long tableLoadEndMillis = Calendar.getInstance().getTimeInMillis() ;
		Log.i(TAG, "loadStockRefFromStream: stock ref table loaded in " + (tableLoadEndMillis - startMillis) + "ms") ;
	}
	
	/**
	 * Downloads the listed companies reference file from the ASX website.
	 * 
	 * @param outputFileName
	 */
	public boolean downloadListedCompanies(String urlName, String outputFileName) {
		Log.i(TAG, "downloadListedCompanies(): starting: outputFileName=" + outputFileName);
		try {
			String dirName = Environment.getExternalStorageDirectory() + File.separator + "ozstock2" ;
			File dir = new File(dirName) ;
			if(! dir.exists())
				dir.mkdir() ;
			
			ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			 
			NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
			boolean isConnected = activeNetwork.isConnectedOrConnecting();
			if(!isConnected) {
				Log.e(TAG, "downloadListedCompanies(): No network connection - aborting") ;
				return false ;
			}
			
			
			URL url = new URL(urlName) ;
			File outputFile = new File(outputFileName);

			long startTime = System.currentTimeMillis();
			Log.i(TAG, "download begining");
			Log.i(TAG, "download url: " + url);
			Log.i(TAG, "downloaded file name: " + outputFileName) ;

			/*
			 * Open a connection to that URL.
			 */
			URLConnection ucon = url.openConnection();

			/*
			 * Define InputStreams to read from the URLConnection.
			 */
			InputStream is = ucon.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);

			/*
			 * Read bytes to the Buffer until there is nothing more to read(-1).
			 */
			ByteArrayBuffer baf = new ByteArrayBuffer(1024);
			int current = 0;
			while ((current = bis.read()) != -1) {
				baf.append((byte) current);
			}

			/* Convert the Bytes read to a String. */
			FileOutputStream fos = new FileOutputStream(outputFile);
			fos.write(baf.toByteArray());
			fos.close();
			Log.i(TAG, "download completed in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");

		} catch (IOException e) {
			Log.e(TAG, "Error downloading from " + urlName	+ ": " + e.getMessage(), e);
			return false ;
		}
		Log.i(TAG, "downloadListedCompanies(): done");
		return true ;
	}

}
