package org.amplexus.app.ozstock2.values;

import java.util.Date;

public class GenericTransaction implements java.io.Serializable {

	public static final String TRANS_BUY 		= "BUY" ;
	public static final String TRANS_SELL 		= "SELL" ;
	public static final String TRANS_DIVIDEND 	= "DIVIDEND" ;
	public static final String TRANS_SPP 		= "SPP" ;
	public static final String TRANS_DELIST		= "DELIST" ;
	public static final String TRANS_RENAME		= "RENAME" ;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	long id ;
	Date transactionDate ;
	String transactionType ;
	String stockCode ;
	double amount ;

	public double getAmount() {
		return amount;
	}
	public void setAmount(double amount) {
		this.amount = amount;
	}
	public String getStockCode() {
		return stockCode;
	}
	public void setStockCode(String stockCode) {
		this.stockCode = stockCode;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public Date getTransactionDate() {
		return transactionDate;
	}
	public void setTransactionDate(Date transactionDate) {
		this.transactionDate = transactionDate;
	}
    public String getTransactionType() {
		return transactionType;
	}
	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}
}
