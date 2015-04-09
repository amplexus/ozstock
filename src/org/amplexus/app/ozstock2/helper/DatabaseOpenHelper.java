/**
 * 
 */
package org.amplexus.app.ozstock2.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Manages the creation / upgrade of the OZSTOCK database.
 * 
 * ENTITY RELATIONSHIP DIAGRAM
 * 
 * 		PORTFOLIO_REF
 * 				|
 * 				-
 * 				|
 * 				|
 *				o
 *				^ 
 * 		STOCK_HOLDINGS -|-------|< STOCK_TRANSACTIONS 
 * 				|						/		\
 * 				v					   o		 o
 * 				|					  /			  \
 * 				|					 /			   \
 *				-					-				-
 *				|				   /				 \
 *			STOCK_REF 		STOCK_TRANS_BUY		STOCK_TRANS_SELL -|-------|< SALE_ALLOCATIONS
 *
 * @TODO track managed funds
 * @TODO track stock split history
 * @TODO track stock code change history
 * @TODO track buyouts history - ie resulting in allocation of acquirer's stock
 * @TODO track dividend history
 * @TODO track delisted stocks
 * @TODO support imports from bt, trading room, morningstar
 * @TODO support emailed exports to CSV, googledocs spreadsheet
 *  
 * @author craig
 * 
 */
public class DatabaseOpenHelper  extends SQLiteOpenHelper {

    private static final String TAG = DatabaseOpenHelper.class.getSimpleName() ;

	public static final String	DATABASE_NAME = "ozstock.db" ;
	public static final int		DATABASE_VERSION = 5 ;

	private SQLiteDatabase db ;
	
	/**
	 * PORTFOLIO_REF table
	 * 
	 * Stock holdings can be grouped into named portfolios. This table contains the authoritative list of named portfolios.
	 * 
	 * Field Name				Description
	 * -------------------------------------------------------------------------------------
	 * ID						The auto-incremented primary key
	 * NAME						The portfolio name
	 * 
	 */
	public static final String TBL_PORTFOLIO_REF = "PORTFOLIO_REF" ;
	public static final String COL_PREF_PORTFOLIO_ID = "_id" ;
	public static final String COL_PREF_PORTFOLIO_NAME = "NAME" ;
	public static final String PORTFOLIO_REF_TABLE_CREATE =
            "CREATE TABLE " + TBL_PORTFOLIO_REF + " (" +
	                COL_PREF_PORTFOLIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	                COL_PREF_PORTFOLIO_NAME + " TEXT);" ;

	/**
	 * STOCK_REF table TBD
	 * 
	 * Stores the list of tracked stocks contained in any of the portfolios. Tracks unit price and stock / company name.
	 * 
	 * Field Name				Description
	 * -------------------------------------------------------------------------------------
	 * CODE						The ASX stock code - primary key
	 * NAME						The stock name as fetched from the stock exchange
	 * LAST_PRICE				The last fetched unit price
	 * 
	 */
	public static final String TBL_STOCK_REF = "STOCK_REF" ;
	public static final String COL_SREF_ID = "_id" ;
	public static final String COL_SREF_STOCK_CODE = "CODE" ;
	public static final String COL_SREF_STOCK_NAME = "NAME" ;
	public static final String COL_SREF_LAST_PRICE = "LAST_PRICE" ;
	public static final String STOCK_REF_TABLE_CREATE =
            "CREATE TABLE " + TBL_STOCK_REF + " (" +
	                COL_SREF_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	                COL_SREF_STOCK_CODE + " TEXT, " +
	                COL_SREF_STOCK_NAME + " TEXT," +
	                COL_SREF_LAST_PRICE + " FLOAT " +
	                ");" ;

	
	/**
	 * STOCK_HOLDINGS table
	 * 
	 * Stores the stock holdings in all portfolios. A separate record is maintained for each purchase event. This means
	 * that there can be multiple records for a single stock.
	 * 
	 * Field Name				Description
	 * -------------------------------------------------------------------------------------
	 * ID						Auto-incremented primary key
	 * CODE						The stock code this holding represents - foreign key into STOCK_REF
	 * PORTFOLIO_ID				The portfolio this holding belongs in - foreign into PORTFOLIO_REF
	 * PURCHASE_PRICE			The purchase (unit) price of the stock
	 * PURCHASE_T				The purchase date
	 * UNITS_PURCHASED			The quantity of units purchased
	 * UNITS_UNSOLD				The quantity of units that have not yet been sold - must be: 0 <= UNITS_UNSOLD <= UNITS_PURCHASED
	 * 
	 */
	public static final String TBL_STOCK_HOLDINGS = "STOCK_HOLDINGS" ;
	public static final String COL_STK_ID = "_id" ;
	public static final String COL_STK_STOCK_CODE = "CODE" ;
	public static final String COL_STK_PORTFOLIO_ID = "PORTFOLIO_ID" ;
	public static final String COL_STK_PURCHASE_PRICE = "PURCHASE_PRICE" ;
	public static final String COL_STK_PURCHASE_TIME = "PURCHASE_T" ;
	public static final String COL_STK_UNITS_REMAINING = "UNITS_REMAINING" ;
	public static final String COL_STK_UNITS_PURCHASED = "UNITS_PURCHASED" ;
	public static final String STOCK_HOLDINGS_TABLE_CREATE =
            "CREATE TABLE " + TBL_STOCK_HOLDINGS + " (" +
	                COL_STK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	                COL_STK_STOCK_CODE + " TEXT, " +
	                COL_STK_PORTFOLIO_ID + " INTEGER, " +
	                COL_STK_PURCHASE_TIME + " DATETIME, " +
	                COL_STK_UNITS_PURCHASED + " INTEGER, " +
	                COL_STK_UNITS_REMAINING + " INTEGER, " +
	                COL_STK_PURCHASE_PRICE + " INTEGER, " +
	                "FOREIGN KEY (" + COL_STK_PORTFOLIO_ID + ") REFERENCES " + TBL_PORTFOLIO_REF + "(" + COL_PREF_PORTFOLIO_ID + ")," + 
	                "FOREIGN KEY (" + COL_STK_STOCK_CODE + ") REFERENCES " + TBL_STOCK_REF + "(" + COL_SREF_STOCK_CODE + ")" + 
	                ");" ;

	/**
	 * STOCK_TRANSACTION table
	 * 
	 * Stores stock transactions.
	 * 
	 * Field Name				Description
	 * -------------------------------------------------------------------------------------
	 * ID						The auto-incremented primary key
	 * STK_ID					The stock holding this transaction pertains to - foreign key referencing STOCK_HOLDINGS
	 * STOCK_CODE				Redundant field stored here so we can in future delete holding and still report p/l. 
	 * TRANSACTION_TYPE			The type of transaction: BUY, SELL, SPP (share purchase plan), BUYBACK etc
	 * TRANSACTION_DATE			The date of this transaction
	 * AMOUNT					THe amount of this transaction
	 * 
	 */
	public static final String TBL_STOCK_TRANSACTIONS = "STOCK_TRANSACTIONS" ;
	public static final String COL_STX_ID = "_id" ;
	public static final String COL_STX_TRANSACTION_TYPE = "TRANSACTION_TYPE" ;
	public static final String COL_STX_STOCK_CODE = "STOCK_CODE" ;
	public static final String COL_STX_TRANSACTION_DATE = "TRANSACTION_DATE" ;
	public static final String COL_STX_AMOUNT = "AMOUNT" ;
	public static final String STOCK_TRANSACTIONS_TABLE_CREATE =
            "CREATE TABLE " + TBL_STOCK_TRANSACTIONS + " (" +
	                COL_STX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	                COL_STX_TRANSACTION_TYPE + " TEXT," +
	                COL_STX_STOCK_CODE + " TEXT," +
	                COL_STX_TRANSACTION_DATE + " DATETIME," +
	                COL_STX_AMOUNT + " FLOAT" +
	                ");" ;

	/**
	 * STOCK_TRANSACTIONS_BUY table.
	 * 
	 * Stores the details of the individual stock purchase transactions. 
	 * 
	 * Field Name				Description
	 * -------------------------------------------------------------------------------------
	 * ID						The auto-incremented primary key
	 * STX_ID					The stock transaction record - foreign key referencing STOCK_TRANSACTIONS 
	 * UNIT_PRICE				The unit price of the transaction 
	 * QUANTITY					The quantity of units involved in this transaction
	 * TRANSACTION_FEE			The dealer's fee associated with the transaction.
	 */
	public static final String TBL_STOCK_TRANSACTIONS_BUY = "STOCK_TRANSACTIONS_BUY" ;
	public static final String COL_BUY_ID = "_id" ;
	public static final String COL_BUY_STX_ID = "STX_ID" ;
	public static final String COL_BUY_UNIT_PRICE = "UNIT_PRICE" ;
	public static final String COL_BUY_QUANTITY = "QUANTITY" ;
	public static final String COL_BUY_TRANSACTION_FEE = "TRANSACTION_FEE" ;
	public static final String COL_BUY_STK_ID = "STK_ID" ;
	public static final String STOCK_TRANSACTIONS_BUY_TABLE_CREATE =
            "CREATE TABLE " + TBL_STOCK_TRANSACTIONS_BUY + " (" +
	                COL_BUY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	                COL_BUY_STX_ID + " INTEGER, " +
	                COL_BUY_STK_ID + " INTEGER, " +
	                COL_BUY_UNIT_PRICE + " FLOAT," +
	                COL_BUY_QUANTITY + " INTEGER," +
	                COL_BUY_TRANSACTION_FEE + " FLOAT," +
	                "FOREIGN KEY (" + COL_BUY_STX_ID + ") REFERENCES " + TBL_STOCK_TRANSACTIONS + "(" + COL_STX_ID + ")" + 
	                "FOREIGN KEY (" + COL_BUY_STK_ID + ") REFERENCES " + TBL_STOCK_HOLDINGS + "(" + COL_STK_ID + ")" + 
	                ");" ;

	/**
	 * STOCK_TRANSACTIONS_SELL table.
	 * 
	 * Stores the details of the individual stock sale transactions.
	 * 
	 * Note that the quantity sold is based on the sum of the quantities in the sale allocations table.
	 * 
	 * Field Name				Description
	 * -------------------------------------------------------------------------------------
	 * ID						The auto-incremented primary key
	 * STX_ID					The stock transaction record - foreign key referencing STOCK_TRANSACTIONS 
	 * UNIT_PRICE				The unit price of the transaction 
	 * TRANSACTION_FEE			The dealer's fee associated with the transaction.
	 */
	public static final String TBL_STOCK_TRANSACTIONS_SELL = "STOCK_TRANSACTIONS_SELL" ;
	public static final String COL_SEL_ID = "_id" ;
	public static final String COL_SEL_STX_ID = "STX_ID" ;
	public static final String COL_SEL_UNIT_PRICE = "UNIT_PRICE" ;
	public static final String COL_SEL_TRANSACTION_FEE = "TRANSACTION_FEE" ;
	public static final String STOCK_TRANSACTIONS_SELL_TABLE_CREATE =
            "CREATE TABLE " + TBL_STOCK_TRANSACTIONS_SELL + " (" +
	                COL_SEL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	                COL_SEL_STX_ID + " INTEGER, " +
	                COL_SEL_UNIT_PRICE + " FLOAT," +
	                COL_SEL_TRANSACTION_FEE + " FLOAT," +
	                "FOREIGN KEY (" + COL_SEL_STX_ID + ") REFERENCES " + TBL_STOCK_TRANSACTIONS + "(" + COL_STX_ID + ")" + 
	                ");" ;

	
	/**
	 * SALE_ALLOCATIONS table
	 * 
	 * For "SELL" transactions, this table records which previous stock purchases were part of the sale. This allows us to track the
	 * capital gains / losses and thus tax implications of the sale based on whether the prior purchase(s) were done >= 12 months prior.   
	 * 
	 * A given "SELL" transaction will have one or more SALE_ALLOCATIONS.
	 * 
	 * Field Name				Description
	 * -------------------------------------------------------------------------------------
	 * ID						The auto-incrementing primary key
	 * PURCHASED_STK_ID			The stock that was purchased - foreign key referencing STOCK_HOLDINGS
	 * SELL_ID					The sale transaction - foreign key referencing STOCK_TRANSACTIONS_SELL
	 * SOLD_QUANTITY			The quantity of units that were sold
	 * 
	 */
	public static final String TBL_SALE_ALLOCATIONS = "SALE_ALLOCATIONS" ;
	public static final String COL_SAL_ID = "_id" ;
	public static final String COL_SAL_PURCHASED_STK_ID = "PURCHASED_STK_ID" ;
	public static final String COL_SAL_SELL_ID = "SELL_ID" ;
	public static final String COL_SAL_SOLD_QUANTITY = "SOLD_QUANTITY" ;
	public static final String SALE_ALLOCATION_TABLE_CREATE =
            "CREATE TABLE " + TBL_SALE_ALLOCATIONS + " (" +
	                COL_SAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	                COL_SAL_PURCHASED_STK_ID + " INTEGER, " +
	                COL_SAL_SELL_ID + " INTEGER, " +
	                COL_SAL_SOLD_QUANTITY + " INTEGER," +
	                "FOREIGN KEY (" + COL_SAL_PURCHASED_STK_ID + ") REFERENCES " + TBL_STOCK_HOLDINGS + "(" + COL_STK_ID + ")" + 
	                "FOREIGN KEY (" + COL_SAL_SELL_ID + ") REFERENCES " + TBL_STOCK_TRANSACTIONS_SELL + "(" + COL_SEL_ID + ")" + 
	                ");" ;

	public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PORTFOLIO_REF_TABLE_CREATE);
        db.execSQL(STOCK_REF_TABLE_CREATE);
        db.execSQL(STOCK_HOLDINGS_TABLE_CREATE);
        db.execSQL(STOCK_TRANSACTIONS_TABLE_CREATE);
        db.execSQL(STOCK_TRANSACTIONS_BUY_TABLE_CREATE);
        db.execSQL(STOCK_TRANSACTIONS_SELL_TABLE_CREATE);
        db.execSQL(SALE_ALLOCATION_TABLE_CREATE);
    }

    /**
     * TODO: This is pretty ordinary.
     */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TBL_STOCK_REF) ;
		db.execSQL("DROP TABLE IF EXISTS " + TBL_PORTFOLIO_REF) ;
		db.execSQL("DROP TABLE IF EXISTS " + TBL_STOCK_HOLDINGS) ;
		db.execSQL("DROP TABLE IF EXISTS " + TBL_SALE_ALLOCATIONS) ;
		db.execSQL("DROP TABLE IF EXISTS " + TBL_STOCK_TRANSACTIONS_BUY) ;
		db.execSQL("DROP TABLE IF EXISTS " + TBL_STOCK_TRANSACTIONS_SELL) ;
		db.execSQL("DROP TABLE IF EXISTS " + TBL_STOCK_TRANSACTIONS) ;
		onCreate(db) ;
	}
}
