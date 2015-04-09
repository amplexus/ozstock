package org.amplexus.app.ozstock2.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import org.amplexus.app.ozstock2.values.BuyTransaction;
import org.amplexus.app.ozstock2.values.Holding;
import org.amplexus.app.ozstock2.values.Portfolio;
import org.amplexus.app.ozstock2.values.SellTransaction;
import org.amplexus.app.ozstock2.values.SellAllocation;
import org.amplexus.app.ozstock2.values.StockRef;
import org.amplexus.app.ozstock2.values.GenericTransaction;

import static org.amplexus.app.ozstock2.helper.DatabaseOpenHelper.* ;

/**
 * A utility class for database CRUD operations.
 * 
 * @author craig
 *
 */
public class DatabaseHelper {

	private static final String TAG = DatabaseHelper.class.getSimpleName() ;
	
	DatabaseOpenHelper databaseOpenHelper ;

	/**
	 * Constructor.
	 * 
	 * Instantiates the DatabaseOpenHelper.
	 * 
	 * @param context the application context.
	 */
	public DatabaseHelper(Context context) {
		this.databaseOpenHelper = new DatabaseOpenHelper(context) ;
	}

	public SQLiteDatabase getWritableDb() {
		return databaseOpenHelper.getWritableDatabase() ;
	}

	public SQLiteDatabase getReadableDb() {
		return databaseOpenHelper.getReadableDatabase() ;
	}

	/**
	 * Read a portfolio.
	 * 
	 * @param id the database row id of the portfolio to be read.
	 * @return the portfolio name
	 */
	public void readPortfolio(SQLiteDatabase db, Portfolio portfolio) {

		Cursor c = db.query(
				true,									// distinct
				TBL_PORTFOLIO_REF, 						// table
				new String[] {COL_PREF_PORTFOLIO_NAME},	// columns / projection
				"_id = ?",		 						// where clause
				new String[] { String.valueOf(portfolio.getId()) },	// where values
				null, 									// group by
				null,									// having
				null,									// order by
				null									// limit
				) ;

		try {
			if(c.moveToFirst()) 
	    		portfolio.setPortfolioName(c.getString(c.getColumnIndex(COL_PREF_PORTFOLIO_NAME))) ;
			else 
				throw new SQLException("Failed to read row " + portfolio.getId() + " in " + TBL_PORTFOLIO_REF);
		} finally {
			c.close() ;
		}
	}

	/**
	 * Read all portfolios into the array adapter.
	 * 
	 * @param mAdapter the array adapter that the portfolio rows will be loaded into.
	 */
	public void readAllPortfolios(SQLiteDatabase db, ArrayList<Portfolio> portfolioList) {
		Log.i(TAG, "readAllPortfolios() reading all portfolios") ;

		portfolioList.clear() ;
		
		Cursor c = db.query(
				true,									// distinct
				TBL_PORTFOLIO_REF, 						// table
				new String[] {COL_PREF_PORTFOLIO_ID, COL_PREF_PORTFOLIO_NAME},	// columns / projection
				null,			 						// where clause
				null,									// where values
				null, 									// group by
				null,									// having
				COL_PREF_PORTFOLIO_NAME + " DESC",		// order by
				null									// limit
				) ;
        if(c != null) {
        	if(c.moveToFirst()) {
	        	do {
	        		long id = c.getLong(0) ;
	        		String portfolioName = c.getString(1) ;

	        		Log.i(TAG, "readAllPortfolios() read: " + id + "=" + portfolioName) ;
	        		
	        		Portfolio portfolio = new Portfolio() ;
	        		portfolio.setId(id) ;
	        		portfolio.setPortfolioName(portfolioName) ;
	        		portfolioList.add(portfolio) ;
	        	} while(c.moveToNext()) ;
        	} else {
        		Log.i(TAG, "readAllPortfolios() no rows found") ;
        	}
        	c.close();
    		Log.i(TAG, "readAllPortfolios() done") ;
        }
	}

	/**
	 * Writes a portfolio
	 * 
	 * If the id is < 0, this is an insert, otherwise it's an update.
	 *
	 * Does NOT update individual holdings - use writeHoldings for that.
	 * 
	 * @param portfolio the portfolio to insert or update.
	 * 
	 * @return the database row id.
	 */
	public void writePortfolio(SQLiteDatabase db, Portfolio portfolio) {

		if(portfolio.getId() <= 0) {
			Log.i(TAG, "writePortfolio() inserting: " + portfolio.getPortfolioName()) ;
			ContentValues values = new ContentValues() ;
			values.put(COL_PREF_PORTFOLIO_NAME, portfolio.getPortfolioName()) ;
			long id = db.insert(TBL_PORTFOLIO_REF, null, values ) ;
			if(id < 0)
				throw new SQLException("writePortfolio() Failed to insert row into " + TBL_PORTFOLIO_REF);
			portfolio.setId(id) ;
			Log.i(TAG, "writePortfolio() inserted record id: " + id) ;
		} else {
			ContentValues values = new ContentValues() ;
			values.put(COL_PREF_PORTFOLIO_NAME, portfolio.getPortfolioName()) ;
			int nrows = db.update(TBL_PORTFOLIO_REF, values, "_id = ?", new String[] { String.valueOf(portfolio.getId()) } ) ;
			if(nrows == 0)
				throw new SQLException("Failed to update row " + portfolio.getId() + " in " + TBL_PORTFOLIO_REF);
		}
	}

	/**
	 * Delete a portfolio.
	 * 
	 * @param id the database row id of the portfolio to be deleted.
	 */
	public void deletePortfolio(SQLiteDatabase db, long id) {
		
		Log.i(TAG, "deletePortfolio() deleting portfolio with id: " + id) ;

		int nrows = db.delete(TBL_PORTFOLIO_REF, "_id = ?", new String[] { String.valueOf(id) } ) ;
		if(nrows == 0)
			throw new SQLException("Failed to delete row " + id + " in " + TBL_PORTFOLIO_REF);
		nrows = databaseOpenHelper.getWritableDatabase().delete(TBL_STOCK_HOLDINGS, COL_STK_PORTFOLIO_ID + " = ?", new String[] { String.valueOf(id) } ) ;

		Log.i(TAG, "deletePortfolio() deleted: " + nrows + " rows") ;
	}

	/**
	 * Read the specified holding.
	 * 
	 * @param portfolioId the portfolio id that the holding belongs to.
	 * @param holdingId the holding id that uniquely identifies the holding.
	 * @param holding will store the holding we read from the db.
	 */
	public void readHolding(SQLiteDatabase db, Holding holding) {
		Log.i(TAG, "readHolding() reading holding with id=" + holding.getId()) ;

		Cursor c = db.query(
				true,											// distinct
				TBL_STOCK_HOLDINGS, 							// table
				new String[] {COL_STK_STOCK_CODE, COL_STK_PURCHASE_TIME, COL_STK_UNITS_REMAINING, COL_STK_UNITS_PURCHASED, COL_STK_PURCHASE_PRICE, COL_STK_PORTFOLIO_ID},	// columns / projection
				COL_STK_ID + "= ?",								// where clause
				new String[] { String.valueOf(holding.getId())},// where values
				null, 											// group by
				null,											// having
				null,											// order by
				null											// limit
				) ;
		if(c != null) {
        	if(c.moveToFirst()) {
        		String stockCode = c.getString(c.getColumnIndex(COL_STK_STOCK_CODE)) ;
        		Date purchaseDate = new Date(c.getLong(c.getColumnIndex(COL_STK_PURCHASE_TIME))) ;
        		long purchaseQuantity = c.getLong(c.getColumnIndex(COL_STK_UNITS_PURCHASED)) ;
        		long remainingQuantity = c.getLong(c.getColumnIndex(COL_STK_UNITS_REMAINING)) ;
        		double purchaseUnitPrice = c.getDouble(c.getColumnIndex(COL_STK_PURCHASE_PRICE)) ;
        		long portfolioId = c.getLong(c.getColumnIndex(COL_STK_PORTFOLIO_ID)) ;

        		Log.i(TAG, "readHolding() read: " + holding.getId() + "=" + stockCode) ;
        		
        		holding.setPortfolioId(portfolioId) ;
        		holding.setStockCode(stockCode) ;
        		holding.setPurchaseDate(purchaseDate) ;
        		holding.setRemainingQuantity(remainingQuantity) ;
        		holding.setPurchaseQuantity(purchaseQuantity) ;
        		holding.setPurchaseUnitPrice(purchaseUnitPrice) ;
        	} else {
        		Log.e(TAG, "readHolding() no rows found with id=" + holding.getId()) ;
    			throw new SQLException("Failed to read holding with stockId=" + holding.getId() + " in " + TBL_STOCK_HOLDINGS);
        	}
        	c.close();
        }
		Log.i(TAG, "readPortfolioHoldings() done") ;	
	}

	/**
	 * Read all holdings belonging to the specified portfolio into the adapter.
	 * 
	 * @param portfolioId the portfolio whose holdings we want.
	 * @param adapter the adapter to be populated.
	 */
	public void readHoldingsByPortfolioId(SQLiteDatabase db, long portfolioId, ArrayList<Holding> holdingList) {
		Log.i(TAG, "readHoldingsByPortfolioId() reading all holdings for portfolioId=" + portfolioId) ;

		holdingList.clear() ;
		
		Cursor c = db.query(
				true,											// distinct
				TBL_STOCK_HOLDINGS, 							// table
				new String[] {COL_STK_ID, COL_STK_STOCK_CODE, COL_STK_PURCHASE_TIME, COL_STK_UNITS_REMAINING, COL_STK_UNITS_PURCHASED, COL_STK_PURCHASE_PRICE},	// columns / projection
				COL_STK_PORTFOLIO_ID + "= ?",					// where clause
				new String[] { String.valueOf(portfolioId)},	// where values
				null, 											// group by
				null,											// having
				COL_STK_STOCK_CODE + " ASC",					// order by
				null											// limit
				) ;
        if(c != null) {
        	if(c.moveToFirst()) {
	        	do {
	        		long stockId = c.getLong(c.getColumnIndex(COL_STK_ID)) ;
	        		String stockCode = c.getString(c.getColumnIndex(COL_STK_STOCK_CODE)) ;
	        		Date purchaseDate = new Date(c.getLong(c.getColumnIndex(COL_STK_PURCHASE_TIME))) ;
	        		long purchaseQuantity = c.getLong(c.getColumnIndex(COL_STK_UNITS_PURCHASED)) ;
	        		long remainingQuantity = c.getLong(c.getColumnIndex(COL_STK_UNITS_REMAINING)) ;
	        		double purchaseUnitPrice = c.getDouble(c.getColumnIndex(COL_STK_PURCHASE_PRICE)) ;

	        		Log.i(TAG, "readHoldingsByPortfolioId() read: " + stockId + "=" + stockCode) ;
	        		
	        		Holding holding = new Holding() ;
	        		holding.setId(stockId) ;
	        		holding.setPortfolioId(portfolioId) ;
	        		holding.setStockCode(stockCode) ;
	        		holding.setPurchaseDate(purchaseDate) ;
	        		holding.setRemainingQuantity(remainingQuantity) ;
	        		holding.setPurchaseQuantity(purchaseQuantity) ;
	        		holding.setPurchaseUnitPrice(purchaseUnitPrice) ;
	        		holdingList.add(holding) ;
	        	} while(c.moveToNext()) ;
        	} else {
        		Log.i(TAG, "readHoldingsByPortfolioId() no rows found for portfolioId=" + portfolioId) ;
        	}
        	c.close();
        }
		Log.i(TAG, "readHoldingsByPortfolioId() done") ;
	}

	/**
	 * Write the holding to the database.
	 * 
	 * If the holding's id is < 0, then this is treated as an insert. If the holding id is >= 0, then this is treated
	 * as an existing holding to be updated.
	 * 
	 * @param holding the holding that will be written to the database.
	 */
	public void writeHolding(SQLiteDatabase db, Holding holding) {
		if(holding.getId() <= 0) {

			Log.i(TAG, "writeHolding() inserting new record") ;

			// insert
			
			ContentValues values = new ContentValues() ;
			values.put(COL_STK_PORTFOLIO_ID, holding.getPortfolioId()) ;
			values.put(COL_STK_STOCK_CODE, holding.getStockCode()) ;
			values.put(COL_STK_UNITS_REMAINING, holding.getRemainingQuantity()) ;
			values.put(COL_STK_UNITS_PURCHASED, holding.getPurchaseQuantity()) ;
			values.put(COL_STK_PURCHASE_PRICE, holding.getPurchaseUnitPrice()) ;
			values.put(COL_STK_PURCHASE_TIME, holding.getPurchaseDate().getTime()) ;

			long id = db.insert(TBL_STOCK_HOLDINGS, null, values ) ;
			if(id < 0)
				throw new SQLException("writeHolding(): Failed to insert row into " + TBL_STOCK_HOLDINGS);
			holding.setId(id) ;

			Log.i(TAG, "writeHolding() inserted new record with id=" + id) ;

		} else {
			
			Log.i(TAG, "writeHolding() updating record with id=" + holding.getId()) ;

			// update

			ContentValues values = new ContentValues() ;
			values.put(COL_STK_PORTFOLIO_ID, holding.getPortfolioId()) ;
			values.put(COL_STK_STOCK_CODE, holding.getStockCode()) ;
			values.put(COL_STK_UNITS_REMAINING, holding.getRemainingQuantity()) ;
			values.put(COL_STK_PURCHASE_PRICE, holding.getPurchaseUnitPrice()) ;
			values.put(COL_STK_PURCHASE_TIME, holding.getPurchaseDate().getTime()) ;
			
			String whereClause =  COL_STK_ID + " = ?" ;
			String[] whereArgs = {String.valueOf(holding.getId())} ;

			int nRows = db.update(TBL_STOCK_HOLDINGS, values, whereClause, whereArgs) ;
			if(nRows != 1) {
        		Log.e(TAG, "writeHolding() Failed to update holding " + holding.getId() + " - expected rows updated to equal 1, but was instead " + nRows) ;
    			throw new SQLException("Failed to update holding - expected rows updated to equal 1, but was instead " + nRows);
			}
			Log.i(TAG, "writeHolding() updated record") ;
		}
	}
	
	/**
	 * Delete the specified holding. 
	 *  
	 * @param id the holding id to delete.
	 */
	public void deleteHolding(SQLiteDatabase db, long id) {
		Log.i(TAG, "deleteHolding() deleting holding with id: " + id) ;

		int nrows = db.delete(TBL_STOCK_HOLDINGS, COL_STK_ID + " = ?", new String[] { String.valueOf(id) }) ;
		if(nrows == 0)
			throw new SQLException("Failed to delete row " + id + " in " + TBL_PORTFOLIO_REF);

		Log.i(TAG, "deleteHolding() deleted: " + nrows + " rows") ;
	}

	/**
	 * 
	 * @param stockRefList
	 * @return
	 */
	public void readAllStockRef(SQLiteDatabase db, ArrayList<StockRef> stockRefList) {
		Log.i(TAG, "readAllStockRef() starts") ;

		int count = 0 ;
		Cursor c = db.query(
				true,											// distinct
				TBL_STOCK_REF, 									// table
				new String[] {COL_SREF_ID, COL_SREF_STOCK_CODE, COL_SREF_STOCK_NAME},	// columns / projection
				null,											// where clause
				null,											// where values
				null, 											// group by
				null,											// having
				null,											// order by
				null											// limit
				) ;
		if(c != null) {
			count = c.getCount() ;
        	if(c.moveToFirst()) {
	        	do {
	        		String stockCode = c.getString(c.getColumnIndex(COL_SREF_STOCK_CODE)) ;
	        		String stockName = c.getString(c.getColumnIndex(COL_SREF_STOCK_NAME)) ;

//	        		Log.i(TAG, "readAllStockRef() read: " + stockCode + "=" + stockName) ;
	        		
	        		StockRef stockRef = new StockRef() ;
	        		stockRef.setStockCode(stockCode) ;
	        		stockRef.setStockName(stockName) ;
	        		stockRefList.add(stockRef) ;
	        	} while(c.moveToNext()) ;
        	} else {
        		Log.w(TAG, "readAllStockRef() no rows found") ;
        	}
        }
		Log.i(TAG, "readAllStockRef() ends: count=" + count) ;
	}
	
	/**
	 * 
	 * @param db
	 * @param inputText
	 * @return
	 * @throws SQLException
	 */
    public Cursor getStockRefByInputTextCursor(SQLiteDatabase db, String inputText) throws SQLException {
        Log.i(TAG, "getStockRefByInputText(): inputText=" + inputText);
        Cursor cursor = db.query(
        		true, 														// Distinct
        		TBL_STOCK_REF, 												// Table name
        		new String[] {COL_SREF_ID, COL_SREF_STOCK_CODE, COL_SREF_STOCK_NAME}, 	// Column names
        		COL_SREF_STOCK_CODE + " like '%" + inputText + "%' or " + 
        			COL_SREF_STOCK_NAME + " like '%" + inputText + "%'",	// Where clause  
        		null,														// Where values
                null, 														// Group by
                null, 														// Having
                COL_SREF_STOCK_CODE, 														// Order by
                null ) ;													// Limit
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

	/**
	 * 
	 * @param code
	 * @param name
	 * @return
	 */
	public long writeStockRef(SQLiteDatabase db, StockRef stockRef) {
//		Log.i(TAG, "writeStockRef() inserting: " + stockRef.getStockCode()) ;

		ContentValues values = new ContentValues() ;
		values.put(COL_SREF_STOCK_CODE, stockRef.getStockCode()) ;
		values.put(COL_SREF_STOCK_NAME, stockRef.getStockName()) ;
		
		long id = db.insert(TBL_STOCK_REF, null, values ) ;
		if(id <= 0)
			throw new SQLException("writeStockRef(): Failed to insert row into " + TBL_STOCK_REF);

//		Log.i(TAG, "insertStockRef() inserted record id: " + id) ;

		return id ;
	}

	/**
	 * 
	 * @param db
	 */
	public void deleteAllStockRef(SQLiteDatabase db) {
		Log.i(TAG, "deleteAllStockRef() starts") ;
		int nrows = db.delete(TBL_STOCK_REF, null, null ) ;
		Log.i(TAG, "deleteAllStockRef() deleted: " + nrows + " rows") ;
	}

	/**
	 * 
	 * @return an array list containing all stock codes in all portfolios.
	 */
	public void readAllUniquePortfolioStockCodes(SQLiteDatabase db, ArrayList<String> stockList) {
		Log.i(TAG, "readAllUniquePortfolioStockCodes() starts") ;

		Cursor c = getAllUniquePortfolioStockCodesCursor(db) ;
		if(c != null) {
        	if(c.moveToFirst()) {
	        	do {
	        		String stockCode = c.getString(c.getColumnIndex(COL_STK_STOCK_CODE)) ;
	        		stockList.add(stockCode) ;
	        	} while(c.moveToNext()) ;
        	} else {
        		Log.w(TAG, "readAllUniquePortfolioStockCodes() no rows found") ;
        	}
        }
		Log.i(TAG, "readAllUniquePortfolioStockCodes() ends: found " + stockList.size() + " stocks found") ;
	}

	/**
	 * 
	 * @return an array list containing all stock codes in all portfolios.
	 */
	public Cursor getAllUniquePortfolioStockCodesCursor(SQLiteDatabase db) {
		Log.i(TAG, "getAllUniquePortfolioStockCodesCursor() starts") ;

		String sql = "select _id, code, name " +
				"from stock_ref sref " + 
				"where exists (select null from stock_holdings stk where stk.code = sref.code and stk.units_remaining > 0)";
        Cursor cursor = db.rawQuery(sql, null) ;

		
//		Cursor cursor = db.query(
//				true,											// distinct
//				TBL_STOCK_HOLDINGS, 							// table
//				new String[] {COL_STK_STOCK_CODE},				// columns / projection
//				null,											// where clause
//				null,											// where values
//				null, 											// group by
//				null,											// having
//				null,											// order by
//				null											// limit
//				) ;
		Log.i(TAG, "getAllUniquePortfolioStockCodesCursor() ends") ;
        return cursor;
	}

	/**
	 * Read the specified transaction.
	 * 
	 * Internal only! 
	 * 
	 * @param portfolioId the portfolio id that the holding belongs to.
	 * @param holdingId the holding id that uniquely identifies the holding.
	 * @param holding will store the holding we read from the db.
	 * 
	 * @see readBuyTransaction
	 * @see readSellTransaction
	 */
	public void readTransaction(SQLiteDatabase db, GenericTransaction transaction) {
		Log.i(TAG, "readTransaction() reading transaction with id=" + transaction.getId()) ;

		Cursor c = db.query(
				true,											// distinct
				TBL_STOCK_TRANSACTIONS,							// table
				new String[] {COL_STX_TRANSACTION_TYPE, COL_STX_STOCK_CODE, COL_STX_TRANSACTION_DATE, COL_STX_AMOUNT},	// columns / projection
				COL_STX_ID + "= ?",								// where clause
				new String[] { String.valueOf(transaction.getId())},	// where values
				null, 											// group by
				null,											// having
				null,											// order by
				null											// limit
				) ;
		if(c != null) {
        	if(c.moveToFirst()) {
        		long timeInMillis = c.getLong(c.getColumnIndex(COL_STX_TRANSACTION_DATE)) ;
        		Date transactionDate = new Date(timeInMillis) ;
        		transaction.setTransactionDate(transactionDate) ;
        		transaction.setStockCode(c.getString(c.getColumnIndex(COL_STX_STOCK_CODE))) ;
        		transaction.setTransactionType(c.getString(c.getColumnIndex(COL_STX_TRANSACTION_TYPE))) ;
        		transaction.setAmount(c.getDouble(c.getColumnIndex(COL_STX_AMOUNT))) ;
        	} else {
        		Log.e(TAG, "readTransaction() no rows found with id=" + transaction.getId()) ;
    			throw new SQLException("Failed to read transaction with stockId=" + transaction.getId() + " in " + TBL_STOCK_HOLDINGS);
        	}
        	c.close();
        }
		Log.i(TAG, "readTransaction() done") ;	
	}

	/**
	 * 
	 * @param db
	 * @return
	 */
	public Cursor getAllTransactionsCursor(SQLiteDatabase db) {
        Log.i(TAG, "getAllTransactionsCursor(): Starts") ;
        Cursor cursor = db.query(
        		true, 														// Distinct
        		TBL_STOCK_TRANSACTIONS, 									// Table name
        		new String[] {COL_STX_ID, COL_STX_TRANSACTION_TYPE, COL_STX_TRANSACTION_DATE, COL_STX_AMOUNT}, 	// Column names
        		null,														// Where clause  
        		null,														// Where values
                null, 														// Group by
                null, 														// Having
                COL_STX_TRANSACTION_DATE,									// Order by
                null ) ;													// Limit
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
	}

	/**
	 * 
	 * @param db
	 * @param portfolioId
	 * @return
	 */
	public Cursor getTransactionsByPortfolioIdCursor(SQLiteDatabase db, long portfolioId) {
        Log.i(TAG, "getTransactionsByPortfolioIdCursor(): portfolioId=" + portfolioId);
        
        String sql =
        		"select	_id, transaction_type, transaction_date, amount " +
        		"from	STOCK_TRANSACTIONS stx " +
        		"where	exists " +
        		"	(select null " +
        		"	from stock_transactions_buy buy " + 
        		"	where	buy.stx_id = stx._id " +
        		"	and exists " +
        		"		(select null " +
        		"		from stock_holdings stk " +
        		"		where buy.stk_id = stk._id " + 
        		"		and stk.portfolio_id = ? )) " +
        		"or	 exists " +
        		"	(select null " + 
        		"	from	sale_allocations sal " +
        		"	where exists " +
        		"		(select null from stock_transactions_sell sell " +
        		"		where	sell._id = sal.sell_id " + 
        		"		and		sell.stx_id = stx._id)" +
        		"	and exists " +
        		"		(select null " +
        		"		from stock_holdings stk " +
        		"		where sal.purchased_stk_id = stk._id " + 
        		"		and stk.portfolio_id = ? )) " +
        		"order by transaction_date" ;

        Cursor cursor = db.rawQuery(sql, new String[] {String.valueOf(portfolioId), String.valueOf(portfolioId)}) ;
        
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
	}

	public Cursor getTransactionsByHoldingIdCursor(SQLiteDatabase db, long holdingId) {
        Log.i(TAG, "getTransactionsByPortfolioIdCursor(): portfolioId=" + holdingId);
        String sql = 
        		"select	_id, transaction_type, transaction_date, stock_code, amount " +
        		"from	STOCK_TRANSACTIONS stx " +
        		"where	exists " +
        		"	(select null " +
        		"	from stock_transactions_buy buy " + 
        		"	where	buy.stk_id = ? " +
        		"	and		buy.stx_id = stx._id) " +
        		"or	 exists " +
        		"	(select null " + 
        		"	from	sale_allocations sal " +
        		"	where	purchased_stk_id = ? " +
        		"	and exists " +
        		"		(select null from stock_transactions_sell sell " +
        		"		where	sell._id = sal.sell_id " + 
        		"		and		sell.stx_id = stx._id)) " +
        		"order by transaction_date" ;
        
        Cursor cursor = db.rawQuery(sql, new String[] {String.valueOf(holdingId), String.valueOf(holdingId)}) ;
        if (cursor != null) {
            cursor.moveToFirst();
        }
        Log.i(TAG, "getTransactionsByPortfolioIdCursor(): done");
        return cursor;
	}

	/**
	 * Read the buy transaction and corresponding transaction records.
	 * 
	 * @param db
	 * @param transaction
	 */
	public void readBuyTransaction(SQLiteDatabase db, BuyTransaction transaction) {
		Log.i(TAG, "readBuyTransaction() reading buy transaction with id=" + transaction.getId()) ;

		/*
		 * Read STOCK_TRANSACTIONS - populates the Transaction-specific fields
		 */
		readTransaction(db, transaction) ;
		if(transaction.getTransactionType().compareTo(BuyTransaction.TRANSACTION_TYPE) != 0) {
			throw new SQLException("Integrity error: buy transaction record: " + transaction.getId() + " has a transaction type of " + transaction.getTransactionType()) ;
		}
		
		/*
		 * Read STOCK_TRANSACTIONS_BUY table - populates the BuyTransaction-specific fields
		 */
		Cursor c = db.query(
				true,											// distinct
				TBL_STOCK_TRANSACTIONS_BUY,						// table
				new String[] {COL_BUY_ID, COL_BUY_QUANTITY, COL_BUY_UNIT_PRICE, COL_BUY_STK_ID},	// columns / projection
				COL_BUY_STX_ID + "= ?",								// where clause
				new String[] { String.valueOf(transaction.getId())},	// where values
				null, 											// group by
				null,											// having
				null,											// order by
				null											// limit
				) ;
		if(c != null) {
        	if(c.moveToFirst()) {
        		transaction.setBuyTransactionId(c.getLong(c.getColumnIndex(COL_BUY_ID))) ;
        		transaction.setQuantity(c.getLong(c.getColumnIndex(COL_BUY_QUANTITY))) ;
        		transaction.setUnitPrice(c.getDouble(c.getColumnIndex(COL_BUY_UNIT_PRICE))) ;
        		transaction.setHoldingId(c.getLong(c.getColumnIndex(COL_BUY_STK_ID))) ;
        	} else {
        		Log.e(TAG, "readBuyTransaction() no rows found with id=" + transaction.getId()) ;
    			throw new SQLException("Failed to read buy transaction with transactionId=" + transaction.getId() + " in " + TBL_STOCK_TRANSACTIONS_BUY);
        	}
        	c.close() ;
        }

		Log.i(TAG, "readBuyTransaction() done") ;	
	}

	/**
	 * Read the buy transaction and corresponding transaction records.
	 * 
	 * @param db
	 * @param transaction
	 */
	public void readSellTransaction(SQLiteDatabase db, SellTransaction transaction) {
		Log.i(TAG, "readSellTransaction() reading sell transaction with id=" + transaction.getId()) ;

		/*
		 * Read STOCK_TRANSACTIONS - populates the Transaction-specific fields
		 */
		readTransaction(db, transaction) ;
		if(transaction.getTransactionType().compareTo(SellTransaction.TRANSACTION_TYPE) != 0) {
			throw new SQLException("Integrity error: sell transaction record: " + transaction.getId() + " has a transaction type of " + transaction.getTransactionType()) ;
		}
		
		/*
		 * Read STOCK_TRANSACTIONS_SELL table - populates the SellTransaction-specific fields
		 * 
		 * Note we can't populate the quantity field - it is derived from the sale allocations. So we fetch the 
		 * sale allocations so it can be derived.
		 */
		Cursor c = db.query(
				true,											// distinct
				TBL_STOCK_TRANSACTIONS_SELL,					// table
				new String[] {COL_SEL_ID, COL_SEL_UNIT_PRICE},	// columns / projection
				COL_SEL_STX_ID + "= ?",							// where clause
				new String[] { String.valueOf(transaction.getId())},	// where values
				null, 											// group by
				null,											// having
				null,											// order by
				null											// limit
				) ;
		if(c != null) {
        	if(c.moveToFirst()) {
        		transaction.setSellTransactionId(c.getLong(c.getColumnIndex(COL_SEL_ID))) ;
        		transaction.setUnitPrice(c.getDouble(c.getColumnIndex(COL_SEL_UNIT_PRICE))) ;
        	} else {
        		Log.e(TAG, "readSellTransaction() no rows found with id=" + transaction.getId()) ;
    			throw new SQLException("Failed to read sell transaction with transactionId=" + transaction.getId() + " in " + TBL_STOCK_TRANSACTIONS_SELL);
        	}
        	c.close() ;
        }

		/*
		 * Read TBL_SALE_ALLOCATIONS
		 */
		Cursor c2 = db.query(
				true,											// distinct
				TBL_SALE_ALLOCATIONS,							// table
				new String[] {COL_SAL_ID, COL_SAL_SOLD_QUANTITY, COL_SAL_PURCHASED_STK_ID, COL_SAL_SELL_ID},// columns / projection
				COL_SAL_SELL_ID + "= ?",							// where clause
				new String[] { String.valueOf(transaction.getSellTransactionId())},	// where values
				null, 											// group by
				null,											// having
				null,											// order by
				null											// limit
				) ;
		if(c2 != null) {
        	if(c2.moveToFirst()) {
        		List<SellAllocation> allocations = transaction.getSaleAllocations() ;
        		do {
        			SellAllocation allocation = new SellAllocation() ;
        			
            		allocation.setId(c2.getLong(c2.getColumnIndex(COL_SAL_ID))) ;
            		allocation.setQuantity(c2.getLong(c2.getColumnIndex(COL_SAL_SOLD_QUANTITY))) ;
            		allocation.setHoldingId(c2.getLong(c2.getColumnIndex(COL_SAL_PURCHASED_STK_ID))) ;
            		allocation.setQuantity(c2.getLong(c2.getColumnIndex(COL_SAL_SOLD_QUANTITY))) ;
            		allocations.add(allocation) ;
            		
        		} while(c2.moveToNext()) ;
        	} else {
        		Log.e(TAG, "readSellTransaction() no rows found in sale allocations table for transactionId=" + transaction.getId()) ;
    			throw new SQLException("Failed to read sale allocation table with transactionId=" + transaction.getId() + " in " + TBL_SALE_ALLOCATIONS);
        	}
        	c.close() ;
        }
		
		Log.i(TAG, "readSellTransaction() done") ;	
	}

	/**
	 * 
	 */
	private void writeTransaction(SQLiteDatabase db, GenericTransaction transaction) {
		if(transaction.getId() <= 0) {
			Log.i(TAG, "writeTransaction() inserting") ;
			ContentValues values = new ContentValues() ;
			values.put(COL_STX_TRANSACTION_DATE, transaction.getTransactionDate().getTime()) ;
			values.put(COL_STX_TRANSACTION_TYPE, transaction.getTransactionType()) ;
			values.put(COL_STX_AMOUNT, transaction.getAmount()) ;
			values.put(COL_STX_STOCK_CODE, transaction.getStockCode()) ;
			long id = db.insert(TBL_STOCK_TRANSACTIONS, null, values ) ;
			Log.i(TAG, "writeTransaction() inserted transaction record id: " + id) ;
			if(id < 0)
				throw new SQLException("writeTransaction(): Failed to insert row into " + TBL_STOCK_TRANSACTIONS);
			transaction.setId(id) ; // transaction primary key
		} else {
			throw new RuntimeException("Operation Not Supported!") ;
		}
		
	}
	
	/**
	 * 
	 * @param db
	 * @param transaction
	 */
	public void writeBuyTransaction(SQLiteDatabase db, BuyTransaction transaction) {
		Log.i(TAG, "writeBuyTransaction() starts - transactionId=" + transaction.getId()) ;
		if(transaction.getId() <= 0) {

			writeTransaction(db, transaction) ;
			
			ContentValues values = new ContentValues() ;
			values.put(COL_BUY_STX_ID, transaction.getId()) ; // buy transaction is associated with a parent stock transaction
			values.put(COL_BUY_QUANTITY, transaction.getQuantity()) ;
			values.put(COL_BUY_UNIT_PRICE, transaction.getUnitPrice()) ;
			values.put(COL_BUY_STK_ID, transaction.getHoldingId()) ; // Buy transaction is associated with a specific holding
			long id = db.insert(TBL_STOCK_TRANSACTIONS_BUY, null, values ) ;
			if(id < 0)
				throw new SQLException("writeBuyTransaction(): Failed to insert row into " + TBL_STOCK_TRANSACTIONS_BUY);
			transaction.setBuyTransactionId(id) ; // buy transaction primary key
			Log.i(TAG, "writeBuyTransaction() inserted buy transaction record id: " + id) ;
		} else {
			throw new RuntimeException("Operation Not Supported!") ;
		}
		Log.i(TAG, "writeBuyTransaction() done") ;
	}
	
	/**
	 * 
	 * @param db
	 * @param transaction
	 */
	public void writeSellTransaction(SQLiteDatabase db, SellTransaction transaction) {
		Log.i(TAG, "writeSellTransaction() starts - transactionId=" + transaction.getId()) ;
		if(transaction.getId() <= 0) {

			/*
			 * Write STOCK_TRANSACTIONS
			 */
			writeTransaction(db, transaction) ;
			
			/*
			 * Write STOCK_TRANSACTIONS_SELL
			 */
			ContentValues values = new ContentValues() ;
			values.put(COL_SEL_STX_ID, transaction.getId()) ; // sell transaction is associated with a stock transaction
			values.put(COL_SEL_UNIT_PRICE, transaction.getUnitPrice()) ;
			long id = db.insert(TBL_STOCK_TRANSACTIONS_SELL, null, values ) ;
			if(id < 0)
				throw new SQLException("writeSellTransaction(): Failed to insert row into " + TBL_STOCK_TRANSACTIONS_SELL);
			transaction.setSellTransactionId(id) ; // sell transaction primary key
			Log.i(TAG, "writeSellTransaction() inserted sell transaction record id: " + id) ;

			/*
			 * Write SALE_ALLOCATION
			 */
			for(SellAllocation allocation: transaction.getSaleAllocations()) {
				values = new ContentValues() ;
				values.put(COL_SAL_PURCHASED_STK_ID, allocation.getHoldingId()) ;
				values.put(COL_SAL_SOLD_QUANTITY, allocation.getQuantity()) ;
				values.put(COL_SAL_SELL_ID, transaction.getSellTransactionId()) ;
				id = db.insert(TBL_SALE_ALLOCATIONS, null, values ) ;
				if(id < 0)
					throw new SQLException("writeSellTransaction(): Failed to insert row into " + TBL_SALE_ALLOCATIONS);
				allocation.setId(id) ;
				Log.i(TAG, "writeSellTransaction() inserted sale allocation record id: " + id) ;
			}
			
		} else {
			throw new RuntimeException("Operation Not Supported!") ;
		}
		Log.i(TAG, "writeSellTransaction() done") ;
	}
	
	/**
	 *  
	 * @param db
	 * @param transactionId
	 */
	public void deleteTransaction(SQLiteDatabase db, long transactionId) {
		Log.i(TAG, "deleteTransaction() deleting transaction with id: " + transactionId) ;

		int nrows = db.delete(TBL_STOCK_TRANSACTIONS, COL_STX_ID + " = ?", new String[] { String.valueOf(transactionId) }) ;
		if(nrows == 0)
			throw new SQLException("Failed to delete row " + transactionId + " in " + TBL_STOCK_TRANSACTIONS);

		Log.i(TAG, "deleteTransaction() deleted: " + nrows + " rows") ;
	}

	public long getTotalHoldingsForStockCode(SQLiteDatabase db, String stockCode) {
        Log.i(TAG, "getTotalHoldingsForStockCode(): stockCode=" + stockCode);
        
        String sql =
        		"select	sum(units_remaining) " +
        		"from	STOCK_TRANSACTIONS " +
        		"where	code = ? " ;

        Cursor cursor = db.rawQuery(sql, new String[] {String.valueOf(stockCode)}) ;
        long totalHoldings = 0 ;
        if (cursor != null) {
            if(cursor.moveToFirst()) {
            	totalHoldings = cursor.getLong(0) ;
            }
        }
		return totalHoldings ;
	}

	public void readHoldingsByStockCode(SQLiteDatabase db, String stockCode, ArrayList<Holding> holdingList) {
		Log.i(TAG, "readHoldingsByStockCode() reading all holdings for stockCode=" + stockCode) ;

		holdingList.clear() ;
		
		Cursor c = db.query(
				true,											// distinct
				TBL_STOCK_HOLDINGS, 							// table
				new String[] {COL_STK_ID, COL_STK_PORTFOLIO_ID, COL_STK_PURCHASE_TIME, COL_STK_UNITS_REMAINING, COL_STK_UNITS_PURCHASED, COL_STK_PURCHASE_PRICE},	// columns / projection
				COL_STK_STOCK_CODE + "= ?",					// where clause
				new String[] { String.valueOf(stockCode)},	// where values
				null, 											// group by
				null,											// having
				COL_STK_STOCK_CODE + " ASC",					// order by
				null											// limit
				) ;
        if(c != null) {
        	if(c.moveToFirst()) {
	        	do {
	        		long stockId = c.getLong(c.getColumnIndex(COL_STK_ID)) ;
	        		long portfolioId= c.getLong(c.getColumnIndex(COL_STK_PORTFOLIO_ID)) ;
	        		Date purchaseDate = new Date(c.getLong(c.getColumnIndex(COL_STK_PURCHASE_TIME))) ;
	        		long purchaseQuantity = c.getLong(c.getColumnIndex(COL_STK_UNITS_PURCHASED)) ;
	        		long remainingQuantity = c.getLong(c.getColumnIndex(COL_STK_UNITS_REMAINING)) ;
	        		double purchaseUnitPrice = c.getDouble(c.getColumnIndex(COL_STK_PURCHASE_PRICE)) ;

	        		Log.i(TAG, "readHoldingsByStockCode() read holdingId: " + stockId + "=" + stockCode) ;
	        		
	        		Holding holding = new Holding() ;
	        		holding.setId(stockId) ;
	        		holding.setPortfolioId(portfolioId) ;
	        		holding.setStockCode(stockCode) ;
	        		holding.setPurchaseDate(purchaseDate) ;
	        		holding.setRemainingQuantity(remainingQuantity) ;
	        		holding.setPurchaseQuantity(purchaseQuantity) ;
	        		holding.setPurchaseUnitPrice(purchaseUnitPrice) ;
	        		holdingList.add(holding) ;
	        	} while(c.moveToNext()) ;
        	} else {
        		Log.i(TAG, "readHoldingsByStockCode() no rows found for stockCode=" + stockCode) ;
        	}
        	c.close();
        }
	}
}