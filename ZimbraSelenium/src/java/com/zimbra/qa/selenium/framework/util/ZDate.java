package com.zimbra.qa.selenium.framework.util;

import java.text.*;
import java.util.*;

import org.apache.log4j.*;

import com.zimbra.common.soap.Element;

public class ZDate {
	Logger logger = LogManager.getLogger(ZDate.class);

	public static final TimeZone TimeZoneUTC = TimeZone.getTimeZone("UTC");
	public static final TimeZone TimeZoneHST = TimeZone.getTimeZone("Pacific/Honolulu");
	public static final TimeZone TimeZoneAKST = TimeZone.getTimeZone("America/Juneau");
	public static final TimeZone TimeZonePST = TimeZone.getTimeZone("America/Los_Angeles");
	public static final TimeZone TimeZoneMST = TimeZone.getTimeZone("America/Denver");
	public static final TimeZone TimeZoneCST = TimeZone.getTimeZone("America/Chicago");
	public static final TimeZone TimeZoneEST = TimeZone.getTimeZone("America/New_York");
	
	protected Calendar calendar = null;
	
	/**
	 * Create a ZDate object in UTC timezone
	 * @param year, i.e. 2011
	 * @param month, i.e. 1 through 12
	 * @param monthday, i.e. 1 through 31
	 * @param hour, i.e. 0 through 24
	 * @param minutes, i.e. 0 through 59
	 * @param seconds, i.e. 0 through 59
	 */
	public ZDate(int year, int month, int monthday, int hour, int minutes, int seconds) {
		this(year, month, monthday, hour, minutes, seconds, ZDate.TimeZoneUTC);
	}
	
	/**
	 * Create a ZDate object in the specified timezone
	 * @param year, i.e. 2011
	 * @param month, i.e. 1 through 12
	 * @param monthday, i.e. 1 through 31
	 * @param hour, i.e. 0 through 24
	 * @param minutes, i.e. 0 through 59
	 * @param seconds, i.e. 0 through 59
	 * @param timezone, i.e. 0 through 59
	 * @throws HarnessException if the timezone cannot be found
	 */
	public ZDate(int year, int month, int monthday, int hour, int minutes, int seconds, String timezone) throws HarnessException {
		this(year, month, monthday, hour, minutes, seconds, ZDate.getTimeZone(timezone));
	}
	
	/**
	 * Create a ZDate object in the specified TimeZone
	 * @param year, i.e. 2011
	 * @param month, i.e. 1 through 12
	 * @param monthday, i.e. 1 through 31
	 * @param hour, i.e. 0 through 24
	 * @param minutes, i.e. 0 through 59
	 * @param seconds, i.e. 0 through 59
	 * @param timezone
	 */
	public ZDate(int year, int month, int monthday, int hour, int minutes, int seconds, TimeZone timezone) {
		
		// TODO: Handle errors (such as month = 0)
		
		calendar = Calendar.getInstance();
		
		calendar.setTimeZone(timezone);
		
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month - 1);
		calendar.set(Calendar.DAY_OF_MONTH, monthday);
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minutes);
		calendar.set(Calendar.SECOND, seconds);
		
		logger.info("New "+ ZDate.class.getName());
	}
	
	/**
	 * Create a ZDate object by parsing a Zimbra SOAP element, such as <s d="20140101T070000" u="1388577600000" tz="America/New_York"/>
	 * @param e
	 * @throws HarnessException
	 */
	public ZDate(Element e) throws HarnessException {
		
		String d = e.getAttribute("d", null);
		String tz = e.getAttribute("tz", null);
		String u = e.getAttribute("u", null);
		
		calendar = Calendar.getInstance();
		
		if ( (tz == null) || (tz.trim().length() == 0)) {
			calendar = Calendar.getInstance(ZDate.TimeZoneUTC);
		} else {
			calendar = Calendar.getInstance(TimeZone.getTimeZone(tz));
		}
		
		if ( u != null ) {
			
			// Parse the unix time, which is in GMT
			long unix = new Long(u).longValue();
			calendar.setTimeInMillis(unix);
			return;
			
		}
		
		if ( d != null ) {
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
			try {
				calendar.setTime( formatter.parse(d) );
			} catch (ParseException ex) {
				throw new HarnessException("Unable to parse date element "+ e.prettyPrint(), ex);
			}
			return;
			
		}
		
		throw new HarnessException("Unable to parse time element "+ e.prettyPrint());
	}
	
	
	/**
	 * Convert the current time + timezone to the specified timezone
	 * @param timezone
	 * @return
	 * @throws HarnessException
	 */
	public ZDate toTimeZone(String timezone) throws HarnessException {
		if ( timezone == null )
			throw new HarnessException("TimeZone cannot be null");
		
		return (toTimeZone(ZDate.getTimeZone(timezone)));
	}	

	/**
	 * Convert the current time + timezone to the specified timezone
	 * @param timezone
	 * @return
	 * @throws HarnessException
	 */
	public ZDate toTimeZone(TimeZone timezone) throws HarnessException {
		if ( timezone == null )
			throw new HarnessException("TimeZone cannot be null");
		
		// Create the new object to return
		// Use basic settings for the time, since it will be reset later
		ZDate other = new ZDate(2011, 2, 22, 12, 0, 0, timezone);
		
		// Set the time
		other.calendar.setTime(this.calendar.getTime());
		
		// return it
		return (other);
	}
	
	/**
	 * Return the ZDate as milliseconds since epoch
	 * @return
	 */
	/**
	 * @return
	 */
	public long toMillis() {
		if ( calendar == null ) {
			calendar = Calendar.getInstance();
		}
		long t = calendar.getTimeInMillis();
		return ( (t / 1000) * 1000); // strip any millisecond blah
	}
	
	public String toYYYYMMDDTHHMMSSZ() throws HarnessException {
		return (format("yyyyMMdd'T'HHmmss'Z'"));
	}
	
	public String toYYYYMMDDTHHMMSS() throws HarnessException {
		return (format("yyyyMMdd'T'HHmmss"));
	}

	/**
	 * MM/DD/YYYY (i.e. 12/25/2011)
	 * @return
	 * @throws HarnessException 
	 */
	public String toMM_DD_YYYY() throws HarnessException {
		return (format("MM/dd/yyyy"));
	}

	/**
	 * hh:mm aa (i.e. 04:30 PM)
	 * @return
	 * @throws HarnessException 
	 */
	public String tohh_mm_aa() throws HarnessException {
		return (format("hh:mm aa"));
	}


	public String toYYYYMMDD() throws HarnessException {
		return (format("yyyyMMdd"));
	}

	public String toYYYYMMDDHHMMSSZ() throws HarnessException {
		return (format("yyyyMMddHHmmss"));
	}

	public String toMMM_dC_yyyy() throws HarnessException {
		return (format("MMM d, yyyy"));
	}

	public String toMMM_dd_yyyy_A_hCmm_a() throws HarnessException {
		return (format("MMM d, yyyy @ h:mm a"));
	}


	protected String format(String format) throws HarnessException {
		try {
			SimpleDateFormat converter = new SimpleDateFormat(format);
			converter.setTimeZone(calendar.getTimeZone());
			return (converter.format(calendar.getTime()));
		} catch (IllegalArgumentException e) {
			throw new HarnessException("Unable to format date: "+ calendar, e);
		}
	}

	/**
	 * Return a new ZDate object with the adjusted offset (+/-)
	 * @param amount
	 * @return
	 */
	public ZDate addDays(int amount) {
		return (addHours(amount * 24));
	}

	/**
	 * Return a new ZDate object with the adjusted offset (+/-)
	 * @param amount
	 * @return
	 */
	public ZDate addHours(int amount) {
		return (addMinutes(amount * 60));
	}

	/**
	 * Return a new ZDate object with the adjusted offset (+/-)
	 * @param amount
	 * @return
	 */
	public ZDate addMinutes(int amount) {
		return (addSeconds(amount * 60));
	}

	/**
	 * Return a new ZDate object with the adjusted offset (+/-)
	 * @param amount
	 * @return
	 */
	public ZDate addSeconds(int amount) {
		
		// Create the new object to return
		ZDate other = new ZDate(
				this.calendar.get(Calendar.YEAR),
				this.calendar.get(Calendar.MONTH) + 1,
				this.calendar.get(Calendar.DAY_OF_MONTH),
				this.calendar.get(Calendar.HOUR_OF_DAY),
				this.calendar.get(Calendar.MINUTE),
				this.calendar.get(Calendar.SECOND),
				this.calendar.getTimeZone()
			);
		
		// Adjust it
		other.calendar.add(Calendar.SECOND, amount);
		
		// return it
		return (other);
	}

	/**
	 * Convert a timezone string identifier to a TimeZone object
	 * <p>
	 * @param timezone
	 * @return the TimeZone object
	 * @throws HarnessException, if the timezone cannot be found
	 */
	public static TimeZone getTimeZone(String timezone) throws HarnessException {
		if ( timezone == null )
			throw new HarnessException("TimeZone string cannot be null");

		for ( String t : TimeZone.getAvailableIDs() ) {
			if ( t.equals(timezone) ) {
				// Found it
				return (TimeZone.getTimeZone(timezone));
			}
		}
		
		throw new HarnessException("Unable to determine the TimeZone from the string: "+ timezone);
	}
	

	@Override
	public String toString() {
		try {
			return (format("MM/dd/yyyy HH:mm:ss z"));
		} catch (HarnessException e) {
			logger.error(e);
			return (calendar.toString());
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((calendar == null) ? 0 : calendar.hashCode());
		return result;
	}

	@Override
	/**
	 * Return true/false whether the two ZDates are the same UTC time
	 * @param amount
	 * @return
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ZDate other = (ZDate) obj;
		return (toMillis() == other.toMillis());
	}


}
