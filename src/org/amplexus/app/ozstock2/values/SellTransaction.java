package org.amplexus.app.ozstock2.values;

import java.util.ArrayList;

public class SellTransaction extends GenericTransaction implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String TRANSACTION_TYPE = "SELL" ;

	double unitPrice ;
	long sellTransactionId ;
	ArrayList<SellAllocation> saleAllocations = new ArrayList<SellAllocation>() ;
	
	public long getSellTransactionId() {
		return sellTransactionId;
	}
	public void setSellTransactionId(long sellTransactionId) {
		this.sellTransactionId = sellTransactionId;
	}

	public long getQuantity() {
		long quantity = 0 ;
		for(SellAllocation sale : saleAllocations) {
			quantity += sale.getQuantity() ;
		}
		return quantity ;
	}
	public double getUnitPrice() {
		return unitPrice;
	}
	public void setUnitPrice(double unitPrice) {
		this.unitPrice = unitPrice;
	}
	
	public ArrayList<SellAllocation> getSaleAllocations() {
		return saleAllocations ;
	}
	
	public void setSaleAllocations(ArrayList<SellAllocation> saleAllocations) {
		this.saleAllocations = saleAllocations ;
	}
	
}
