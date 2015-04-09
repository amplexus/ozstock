package org.amplexus.app.ozstock2.values;

import java.util.ArrayList;
import java.util.Date;

public class Holding implements java.io.Serializable {

	public static final String STATUS_ACTIVE = "ACTIVE" ;
	public static final String STATUS_DELETED = "DELETED" ;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	long id ;
	long portfolioId ;
	Date purchaseDate ;
	String stockCode ;
    double  purchaseUnitPrice ;
    long remainingQuantity ;
    long purchaseQuantity ;
    
    ArrayList<GenericTransaction> transactionList = new ArrayList<GenericTransaction>() ;
    
    public ArrayList<GenericTransaction> getTransactionList() {
		return transactionList;
	}
	public void setTransactionList(ArrayList<GenericTransaction> transactionList) {
		this.transactionList = transactionList;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
    public long getPortfolioId() {
		return portfolioId;
	}
	public void setPortfolioId(long id) {
		this.portfolioId = id;
	}
	public Date getPurchaseDate() {
		return purchaseDate;
	}
	public void setPurchaseDate(Date purchaseDate) {
		this.purchaseDate = purchaseDate;
	}
	public String getStockCode() {
		return stockCode;
	}
	public void setStockCode(String stockCode) {
		this.stockCode = stockCode;
	}
	public double getPurchaseUnitPrice() {
		return purchaseUnitPrice;
	}
	public void setPurchaseUnitPrice(double purchaseUnitPrice) {
		this.purchaseUnitPrice = purchaseUnitPrice;
	}
	public long getRemainingQuantity() {
		return remainingQuantity;
	}
	public void setRemainingQuantity(long remainingQuantity) {
		this.remainingQuantity = remainingQuantity;
	}
	public long getPurchaseQuantity() {
		return purchaseQuantity;
	}
	public void setPurchaseQuantity(long purchaseQuantity) {
		this.purchaseQuantity = purchaseQuantity;
	}
}
