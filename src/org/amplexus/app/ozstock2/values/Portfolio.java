package org.amplexus.app.ozstock2.values;

import java.util.ArrayList;

public class Portfolio implements java.io.Serializable {
	
	public static final String STATUS_ACTIVE = "ACTIVE" ;
	public static final String STATUS_DELETED = "DELETED" ;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long id ;
	private String portfolioName ;
	private String status ;
	private ArrayList<Holding> holdings = new ArrayList<Holding>();
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getPortfolioName() {
		return portfolioName;
	}
	public void setPortfolioName(String portfolioName) {
		this.portfolioName = portfolioName;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public ArrayList<Holding> getHoldings() {
		return holdings;
	}
	public void setHoldings(ArrayList<Holding> holdings) {
		this.holdings = holdings;
	}
}    
