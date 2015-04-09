package org.amplexus.app.ozstock2.helper;

import java.text.DecimalFormat;
import java.util.Date;

import android.text.format.DateFormat;

public class TextFormatHelper {
	DecimalFormat numberFormatter = new DecimalFormat("0");
    public String formatLong(long number) {
        String formattedNumber = numberFormatter.format(number) ;
    	return formattedNumber ;
	}

	DecimalFormat percentFormatter = new DecimalFormat("0'%'");
	public String formatPercent(double percent) {
	    String formattedPercent = percentFormatter.format(percent) ;
	    return formattedPercent ;
	}

	public CharSequence formatDate(Date date) {
		return DateFormat.format("yy/MM/dd", date) ;
	}

	DecimalFormat currencyWithCentsFormatter = new DecimalFormat("'$'0.00");
	DecimalFormat currencyNoCentsFormatter = new DecimalFormat("'$'0");

	public String formatCurrency(double amount, boolean showCents) {
		
		if(showCents) {
			return currencyWithCentsFormatter.format(amount) ;
		} else {
			return currencyNoCentsFormatter.format(amount) ;
		}
	}
}
