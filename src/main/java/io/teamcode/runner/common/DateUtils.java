package io.teamcode.runner.common;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by chiang on 2017. 7. 15..
 */
public abstract class DateUtils {

    public static final String[] yearMonthArray() {
        DateFormat yearDateFormat = new SimpleDateFormat("yyyy");
        DateFormat monthDateFormat = new SimpleDateFormat("MM");

        Date now = new Date();

        return new String[]{yearDateFormat.format(now), monthDateFormat.format(now)};
    }

    public static final String[] artifactsDir() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMM");
        DateFormat dateFormat2 = new SimpleDateFormat("dd");

        Date now = new Date();

        return new String[]{dateFormat.format(now), dateFormat2.format(now)};
    }

}
