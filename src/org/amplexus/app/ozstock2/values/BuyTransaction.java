package org.amplexus.app.ozstock2.values;

public class BuyTransaction extends GenericTransaction implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final String TRANSACTION_TYPE = "BUY" ;
	
	long quantity ;
	double unitPrice ;
	long holdingId ;
	long buyTransactionId ;
	public long getHoldingId() {
		return holdingId;
	}
	public void setHoldingId(long holdingId) {
		this.holdingId = holdingId;
	}

	public long getBuyTransactionId() {
		return buyTransactionId;
	}
	public void setBuyTransactionId(long buyTransactionId) {
		this.buyTransactionId = buyTransactionId;
	}
	public long getQuantity() {
		return quantity;
	}
	public void setQuantity(long quantity) {
		this.quantity = quantity;
	}
	public double getUnitPrice() {
		return unitPrice;
	}
	public void setUnitPrice(double unitPrice) {
		this.unitPrice = unitPrice;
	}
}
