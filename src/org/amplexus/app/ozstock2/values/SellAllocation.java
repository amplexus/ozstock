package org.amplexus.app.ozstock2.values;

public class SellAllocation implements java.io.Serializable {
	private long id ;
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	private long holdingId ;
	public long getHoldingId() {
		return holdingId;
	}
	public void setHoldingId(long holdingId) {
		this.holdingId = holdingId;
	}
	public long getQuantity() {
		return quantity;
	}
	public void setQuantity(long quantity) {
		this.quantity = quantity;
	}
	private long quantity ;
}
