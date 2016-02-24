package com.moneydance.modules.features.mynetworth;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created with IntelliJ IDEA.
 * User: gad
 * Date: 2/9/13
 * Time: 9:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class MyUtils {
    public static Calendar calFromDateInt(int date) {
        // date 20130204
        int day = date % 100;
        date = date/100;
        int month = date % 100;
        int year = date / 100;
        Calendar cal = Calendar.getInstance();
        cal.set(year,month-1,day);
        return cal;
    }

    public static int dateIntFromCalendar(Calendar cal) {
        return (cal.get(cal.YEAR) * 100 + (cal.get(cal.MONTH)+1))*100 + cal.get(cal.DAY_OF_MONTH);
    }
    static String calToString(Calendar cal)
    {
        // java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("MM/dd/yyyy");
        DateFormat f = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
        return f.format(cal.getTime());
    }

   public static String dateIntToString(int date) {
       return calToString(calFromDateInt(date));
   }
}
