package org.amplexus.app.ozstock2.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import org.amplexus.app.ozstock2.values.BuyTransaction;
import org.amplexus.app.ozstock2.values.Holding;
import org.amplexus.app.ozstock2.values.Portfolio;
import org.amplexus.app.ozstock2.values.SellTransaction;
import org.amplexus.app.ozstock2.values.SellAllocation;
import org.amplexus.app.ozstock2.values.StockRef;
import org.amplexus.app.ozstock2.values.GenericTransaction;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Maps high level business functions into low level database operations.
 */
public class BusinessLogicHelper {

	private static final String TAG = BusinessLogicHelper.class.getSimpleName() ;

	private Context mContext;
	private DatabaseHelper mDb ;

	public BusinessLogicHelper(Context c) {
		mContext = c ;
		mDb = new DatabaseHelper(c) ;
		
	}
	
	public ArrayList<StockRef> getAllStockRef() {
		SQLiteDatabase db = mDb.getReadableDb() ;
		ArrayList<StockRef> stockRefList = new ArrayList<StockRef>() ;
		try {
			mDb.readAllStockRef(db, stockRefList) ;
		} finally {
			db.close() ;
		}
		return stockRefList ;
	}
	
	/**
	 * 
	 * @param db
	 * @param inputText
	 * @return
	 * @throws SQLException
	 */
	SQLiteDatabase cursorDb ;
    public Cursor getStockRefByInputTextCursor(String inputText) throws SQLException {
    	if(cursorDb == null)
    		cursorDb = mDb.getReadableDb() ;

    	return mDb.getStockRefByInputTextCursor(cursorDb, inputText) ;
    }

	
	public void writeStockRef(StockRef stockRef) {
		SQLiteDatabase db = mDb.getWritableDb() ;
		try {
			mDb.writeStockRef(db, stockRef) ;
		} finally {
			db.close() ;
		}
	}

	public void writeAllStockRef(ArrayList<StockRef> stockRefList) {
		deleteAllStockRef() ;
		SQLiteDatabase db = mDb.getWritableDb() ;
		try {
			for(StockRef stockRef : stockRefList) {
				mDb.writeStockRef(db, stockRef) ;
			}
		} finally {
			db.close() ;
		}
	}

	public void deleteAllStockRef() {
		SQLiteDatabase db = mDb.getWritableDb() ;
		try {
			mDb.deleteAllStockRef(db) ;
		} finally {
			db.close() ;
		}
	}

	public ArrayList<Portfolio> getPortfolios() {
		SQLiteDatabase db = mDb.getReadableDb() ;
		try {
			ArrayList<Portfolio> portfolioList = new ArrayList<Portfolio>() ;
			mDb.readAllPortfolios(db, portfolioList) ;
			return portfolioList ;
		} finally {
			db.close() ;
		}
	}
	
	public Portfolio getPortfolio(long portfolioId) {
		Portfolio p = new Portfolio() ;
		p.setId(portfolioId) ;
		SQLiteDatabase db = mDb.getReadableDb() ;
		try {
			mDb.readPortfolio(db, p) ;
		} finally {
			db.close() ;
		}
		return p ;
	}

	public void addPortfolio(Portfolio p) {
		SQLiteDatabase db = mDb.getWritableDb() ;
		try {
			mDb.writePortfolio(db, p) ;
		} finally {
			db.close() ;
		}
	}
	
	public void updatePortfolio(Portfolio p) {
		SQLiteDatabase db = mDb.getWritableDb() ;
		try {
			mDb.writePortfolio(db, p) ;
		} finally {
			db.close() ;
		}
	}
	
	public void deletePortfolio(long portfolioId) {
		SQLiteDatabase db = mDb.getWritableDb() ;
		try {
			ArrayList<Holding> holdingList = new ArrayList<Holding>() ;
			mDb.readHoldingsByPortfolioId(db, portfolioId, holdingList) ;
			for(Holding h : holdingList) {
				mDb.deleteHolding(db, h.getId()) ;
			}
			mDb.deletePortfolio(db, portfolioId) ;
		} finally {
			db.close() ;
		}
	}
	
	public ArrayList<Holding> getHoldingsByPortfolio(long portfolioId) {
		SQLiteDatabase db = mDb.getReadableDb() ;
		try {
			ArrayList<Holding> holdingList = new ArrayList<Holding>() ;
			mDb.readHoldingsByPortfolioId(db, portfolioId, holdingList) ;
			return holdingList ;
		} finally {
			db.close() ;
		}
	}
	
	public ArrayList<Holding> getHoldingsByStockCode(String stockCode) {
		SQLiteDatabase db = mDb.getReadableDb() ;
		try {
			ArrayList<Holding> holdingList = new ArrayList<Holding>() ;
			mDb.readHoldingsByStockCode(db, stockCode, holdingList) ;
			return holdingList ;
		} finally {
			db.close() ;
		}
	}
	
	public Holding getHoldingById(long holdingId) {
		SQLiteDatabase db = mDb.getReadableDb() ;
		try {
			Holding h = new Holding() ;
			h.setId(holdingId) ;
			mDb.readHolding(db, h) ;
			return h ;
		} finally {
			db.close() ;
		}
	}

	public void deleteHolding(long holdingId) {
		SQLiteDatabase db = mDb.getWritableDb() ;
		try {
			// Delete transactions
			// FIXME: retrieve the list!
			// FIXME: think more carefully about ramifications of deleting transactions
			ArrayList<GenericTransaction> transactionList = new ArrayList<GenericTransaction>() ;
			for(GenericTransaction t : transactionList) {
				mDb.deleteTransaction(db, t.getId()) ;
			}
			
			// Delete holding
			mDb.deleteHolding(db, holdingId) ;
		} finally {
			db.close() ;
		}
	}
	
	public Cursor getTransactionsCursor() {
		SQLiteDatabase db = mDb.getReadableDb() ;
		return mDb.getAllTransactionsCursor(db) ;
	}
	
	public Cursor getTransactionsByPortfolioCursor(long portfolioId) {
		SQLiteDatabase db = mDb.getReadableDb() ;
		return mDb.getTransactionsByPortfolioIdCursor(db, portfolioId) ;
	}
	
	public Cursor getTransactionsByHoldingCursor(long holdingId) {
		SQLiteDatabase db = mDb.getReadableDb() ;
		return mDb.getTransactionsByHoldingIdCursor(db, holdingId) ;
	}

	public GenericTransaction getTransaction(long transactionId) {
		SQLiteDatabase db = mDb.getReadableDb() ;
		try {
			GenericTransaction t = new GenericTransaction() ;
			t.setId(transactionId) ;
			mDb.readTransaction(db, t) ;
			if(t.getTransactionType().compareTo(BuyTransaction.TRANSACTION_TYPE) == 0) {
				BuyTransaction buyTransaction = new BuyTransaction() ;
				buyTransaction.setId(transactionId) ;
				mDb.readBuyTransaction(db, buyTransaction) ;
				return buyTransaction ;
			} else if(t.getTransactionType().compareTo(SellTransaction.TRANSACTION_TYPE) == 0) {
				SellTransaction sellTransaction = new SellTransaction() ;
				sellTransaction.setId(transactionId) ;
				mDb.readSellTransaction(db, sellTransaction) ;
				return sellTransaction ;
			} else {
				throw new RuntimeException("Unsupported transaction type: " + t.getTransactionType()) ;
			}
			
		} finally {
			db.close() ;
		}
	}
	
	/**
	 * 
	 * @param h
	 * @param t
	 * 
	 * TODO: Generate holding on the fly instead of passing as a parameter
	 */
	public void purchaseStock(long portfolioId, BuyTransaction t) {
		SQLiteDatabase db = mDb.getWritableDb() ;
		try {
	    	Holding h = new Holding() ;
	    	h.setPortfolioId(portfolioId) ;
	    	h.setPurchaseDate(t.getTransactionDate()) ;
	    	h.setPurchaseQuantity(t.getQuantity()) ;
	    	h.setPurchaseUnitPrice(t.getUnitPrice()) ;
	    	h.setRemainingQuantity(h.getPurchaseQuantity()) ;
	    	h.setStockCode(t.getStockCode()) ;
			mDb.writeHolding(db, h) ;

			t.setHoldingId(h.getId()) ;
			mDb.writeBuyTransaction(db, t) ;
		} finally {
			db.close() ;
		}
	}
	
	public ArrayList<Holding> sellStock(SellTransaction t) {
		SQLiteDatabase db = mDb.getWritableDb() ;
		ArrayList<Holding> holdingList = new ArrayList<Holding>() ; 
				
		try {
			mDb.writeSellTransaction(db, t) ;
			for(SellAllocation s : t.getSaleAllocations()) {
				Holding h = new Holding() ;
				h.setId(s.getHoldingId()) ;
				mDb.readHolding(db, h) ;
				h.setRemainingQuantity(h.getRemainingQuantity() - s.getQuantity()) ;
				mDb.writeHolding(db, h) ;
				holdingList.add(h) ;
			}
		} finally {
			db.close() ;
		}
		return holdingList ;
	}

	public void deleteTransaction(long transactionId) {
		SQLiteDatabase db = mDb.getWritableDb() ;
		try {
			mDb.deleteTransaction(db, transactionId) ;
		} finally {
			db.close() ;
		}
	}

	public Cursor getAllPortfolioStockCodesCursor() {
		SQLiteDatabase db = mDb.getReadableDb() ;
		return mDb.getAllUniquePortfolioStockCodesCursor(db) ;
	}

	public ArrayList<String> readAllPortfolioStockCodes() {
		SQLiteDatabase db = mDb.getWritableDb() ;
		ArrayList<String> stockList = new ArrayList<String>() ;
		try {
			mDb.readAllUniquePortfolioStockCodes(db, stockList) ;
		} finally {
			db.close() ;
		}
		return stockList ;
	}

	/**
	 * 
	 * TODO: support allocation by quantity, amount and percentage
	 * 
	 * @param stockCode
	 * @param sellQuantity
	 * @param holdingList
	 * @param transaction
	 */
	public void calculateQuantityBasedSaleAllocationToMinimiseCapitalGains(long sellQuantity, ArrayList<Holding> holdingList, SellTransaction transaction) {
		// Priority: 
		//	(1) any loss makers any time order by loss desc
		//	(2) gainers purchased > 12 months before sale order by gain asc
		//	(3) gainers purchased <= 12 months before sale order by gain asc
		
	    Collections.sort(holdingList, new LosersDescendingByPriceComparator()) ;
	    for(Holding h : holdingList) {
	    	if(h.getRemainingQuantity() == 0)
	    		continue ;
	    	SellAllocation alloc = new SellAllocation() ;
	    	alloc.setHoldingId(h.getId()) ;
	    	long holdingQuantityToSell = h.getRemainingQuantity() >= sellQuantity ? sellQuantity : h.getRemainingQuantity() ; 
	    	alloc.setQuantity(holdingQuantityToSell) ;
	    	sellQuantity -= holdingQuantityToSell ;
	    	transaction.getSaleAllocations().add(alloc) ;
	    	if(sellQuantity <= 0)
	    		break ;
	    }
	}
	
	public class LosersDescendingByPriceComparator implements Comparator<Holding> {
		
		@Override
		public int compare(Holding lhs, Holding rhs) {
			
			double priceDifference = lhs.getPurchaseUnitPrice() - rhs.getPurchaseUnitPrice() ;
			if(priceDifference < 0)
				return 1 ;
			else if (priceDifference > 0) 
				return -1 ;
			else return 0 ;
		}
	}

	/**
	 * 
	 * TODO: support allocation by quantity, amount and percentage
	 * 
	 * @param stockCode
	 * @param sellQuantity
	 * @param holdingList
	 * @param transaction
	 */
	public void calculateQuantityBasedSaleAllocationToMaximiseCapitalGains(long sellQuantity, ArrayList<Holding> holdingList, SellTransaction transaction) {
		// Priority:
		//	(2) gainers purchased > 12 months before sale order by gain desc
		//	(3) gainers purchased <= 12 months before sale order by gain desc
		//	(3) any loss makers any time order by loss asc
		
	    Collections.sort(holdingList, new GainersAscendingByDateAndPriceComparator()) ;
	    for(Holding h : holdingList) {
	    	if(h.getRemainingQuantity() == 0)
	    		continue ;
	    	SellAllocation alloc = new SellAllocation() ;
	    	alloc.setHoldingId(h.getId()) ;
	    	long holdingQuantityToSell = h.getRemainingQuantity() >= sellQuantity ? sellQuantity : h.getRemainingQuantity() ; 
	    	alloc.setQuantity(holdingQuantityToSell) ;
	    	sellQuantity -= holdingQuantityToSell ;
	    	transaction.getSaleAllocations().add(alloc) ;
	    	if(sellQuantity <= 0)
	    		break ;
	    }
	}
	
	public class GainersAscendingByDateAndPriceComparator implements Comparator<Holding> {
		@Override
		public int compare(Holding lhs, Holding rhs) {
			
			long timeDifference = lhs.getPurchaseDate().getTime() - rhs.getPurchaseDate().getTime() ;
			double priceDifference = lhs.getPurchaseUnitPrice() - rhs.getPurchaseUnitPrice() ;

			if(timeDifference < 0)
				return -1 ;
			else if(timeDifference > 0)
				return 1 ;
			else if(priceDifference < 0)
				return -1 ;
			else if (priceDifference > 0) 
				return 1 ;
			else return 0 ;
		}
	}

	public double calculateProfitLoss(ArrayList<Holding> holdingList, SellTransaction transaction) {
		
		Log.i(TAG, "calculateProfitLoss(): Starts") ;
		
		double totalProfitLossAmount = 0.0 ;
		double totalTaxGuestimate = 0.0 ;
		Date sellDate = transaction.getTransactionDate() ;
		long sellMillis = sellDate.getTime() ;
		double sellPrice = transaction.getUnitPrice() ;

		for(SellAllocation alloc : transaction.getSaleAllocations()) {
			long holdingId = alloc.getHoldingId() ;
			for(Holding h : holdingList) {
				if(holdingId == h.getId()) {
					/*
					 * Calculate profit / loss for this sale allocation
					 */
					double purchasePrice = h.getPurchaseUnitPrice() ;
					long sellQty = alloc.getQuantity() ;
					double profitLossAmount = (sellQty * sellPrice) - (sellQty * purchasePrice) ;
					totalProfitLossAmount += profitLossAmount ;

					Log.i(TAG, "calculateProfitLoss(): profitLoss for holdingId=" + holdingId + " == " + profitLossAmount) ;

					/*
					 * Calculate crude tax estimate for this sale allocation
					 */
					double taxGuestimate = 0.0 ;
					Date purchaseDate = h.getPurchaseDate() ;
					long buyMillis = purchaseDate.getTime() ;
					long diffMillis = sellMillis - buyMillis ;
					double diffYears = diffMillis / (1000 * 60 * 60 * 24 * 365) ; 
					if(diffYears < 1.0) {
						taxGuestimate = profitLossAmount * 0.45 ;
					} else {
						taxGuestimate = profitLossAmount * 0.25 ;
					}
					totalTaxGuestimate += taxGuestimate ; 
				}
			}
		}
		Log.i(TAG, "calculateProfitLoss(): Ends: totalProfitLoss = " + totalProfitLossAmount) ;
		return totalProfitLossAmount ;
	}
}
