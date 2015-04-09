package org.amplexus.app.ozstock2.values;

import android.os.Parcel;
import android.os.Parcelable;

public class StockPriceQuote implements Parcelable {

	private String ticker ;
	private Double lastPrice ;

    public static final Parcelable.Creator<StockPriceQuote> CREATOR = new Parcelable.Creator<StockPriceQuote>() {
		public StockPriceQuote createFromParcel(Parcel in) {
		    return new StockPriceQuote(in);
		}
		
		public StockPriceQuote[] newArray(int size) {
		    return new StockPriceQuote[size];
		}
    };

	public StockPriceQuote() {
	}
	public StockPriceQuote(Parcel in) {
		readFromParcel(in) ;
	}
	public String getTicker() {
		return ticker;
	}
	public void setTicker(String ticker) {
		this.ticker = ticker;
	}
	public Double getLastPrice() {
		return lastPrice;
	}
	public void setLastPrice(Double lastPrice) {
		this.lastPrice = lastPrice;
	}

	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(ticker);
		dest.writeDouble(lastPrice);
	}
	
	private void readFromParcel(Parcel in) {
		ticker = in.readString() ;
		lastPrice = in.readDouble() ;
	}
}
