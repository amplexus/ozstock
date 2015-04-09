package org.amplexus.app.ozstock2.values;

public class StockRef implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String stockCode ;
	String stockName ;
    
	public String getStockCode() {
		return stockCode;
	}
	public void setStockCode(String stockCode) {
		this.stockCode = stockCode;
	}
	public String getStockName() {
		return stockName;
	}
	public void setStockName(String stockName) {
		this.stockName = stockName;
	}
}
