/**
 * 
 */
package com.chartbeat.androidsdk;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.util.Log;

/**
 * @author bjorn
 *
 */
public final class UserInfo {
	private static final String TAG = "Chartbeat userdata";
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	
	private final String userId;
	private final boolean newUser;
	private final TreeSet<GregorianCalendar> visitedDates;
	private final SharedPreferences prefs;
	/**
	 * 
	 */
	public UserInfo(Context context) {
		prefs = context.getSharedPreferences("com.chartbeat.androidsdk.user", Context.MODE_PRIVATE);
		String userId = prefs.getString("userid", null);
		
		if( userId == null ) {
			userId = UUID.randomUUID().toString();
			SharedPreferences.Editor ed = prefs.edit();
			ed.putString("userid", userId);
			ed.commit();
			newUser = true;
		} else {
			newUser = false;
		}
		this.userId = userId;
		

		visitedDates = new TreeSet<GregorianCalendar>();
		String vd = prefs.getString("visits-" + userId, null);
		GregorianCalendar sixteenDaysAgo = today();
		sixteenDaysAgo.roll(GregorianCalendar.DATE, -16);
		if( vd != null ) {
			for( String dateString : vd.split(",") ) {
				GregorianCalendar gc = new GregorianCalendar();
				try {
					gc.setTime(dateFormat.parse(dateString));
					if( gc.after(sixteenDaysAgo) )
						visitedDates.add(gc);
				} catch (ParseException e) {
					Log.w(TAG, "error reading date in user info: " + e );
				}
			}
		}
	}
	
	public boolean isNewUser() {
		return newUser;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public void visited() {
		GregorianCalendar gc = today();
		if( visitedDates.add(gc) ) {
			// the set is modified, so we must store it.
			String s = "";
			boolean first = true;
			for( GregorianCalendar c : visitedDates ) {
				if( !first )
					s = "," + s ;
				s = s + dateFormat.format(c.getTime()) ;
				first = false;
			}
			Log.d(TAG, "Storing user visited dates: " + s );
			SharedPreferences.Editor ed = prefs.edit();
			ed.putString("visits-" + userId, s);
			ed.commit();
		}
	}
	
	public String getUserVisitFrequencyString() {
		char a = 'F';
		
		GregorianCalendar today = today();
		GregorianCalendar cal[] = new GregorianCalendar[16];
		for( int i=0; i<16; ++i ) {
			cal[i] = (GregorianCalendar) today.clone();
			cal[i].roll(GregorianCalendar.DATE, i);
		};
		int b =  (visitedDates.contains(cal[15])?1:0) << 3
			| (visitedDates.contains(cal[14])?1:0) << 2
			| (visitedDates.contains(cal[13])?1:0) << 1
			| (visitedDates.contains(cal[12])?1:0) << 0 ;
		int c =  (visitedDates.contains(cal[11])?1:0) << 3
			| (visitedDates.contains(cal[10])?1:0) << 2
			| (visitedDates.contains(cal[ 9])?1:0) << 1
			| (visitedDates.contains(cal[ 8])?1:0) << 0 ;
		int d =  (visitedDates.contains(cal[ 7])?1:0) << 3
			| (visitedDates.contains(cal[ 6])?1:0) << 2
			| (visitedDates.contains(cal[ 5])?1:0) << 1
			| (visitedDates.contains(cal[ 4])?1:0) << 0 ;
		int e =  (visitedDates.contains(cal[ 3])?1:0) << 3
			| (visitedDates.contains(cal[ 2])?1:0) << 2
			| (visitedDates.contains(cal[ 1])?1:0) << 1
			| (visitedDates.contains(cal[ 0])?1:0) << 0 ;
		
		return new String( new char[] { a, toHexDigit(b), toHexDigit(c), toHexDigit(d), toHexDigit(e) } );
	}
	
	private static GregorianCalendar today() {
		GregorianCalendar gc = new GregorianCalendar();
		gc = new GregorianCalendar( gc.get(GregorianCalendar.YEAR), gc.get(GregorianCalendar.MONTH), gc.get(GregorianCalendar.DAY_OF_MONTH) );
		return gc;
	}
	
	public static final char toHexDigit( int i ) {
		if( i <= 9 && i >= 0 ) {
			return (char) ('0' + i) ;
		} else if( i > 9 && i < 16 ) {
			return (char) ('A' + i - 10 );
		} else {
			throw new RuntimeException( "I is not in hex digit range: " + i );
		}
	}
}
