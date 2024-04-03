package nkp.pspValidator.shared.engine;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests regular expressions for ISO 8601 Date and Times
 *
 * @see <a href="https://www.pelagodesign.com/blog/2009/05/20/iso-8601-date-validation-that-doesnt-suck/">https://www.pelagodesign.com/blog/2009/05/20/iso-8601-date-validation-that-doesnt-suck/</a>
 * @see <a href="https://www.oreilly.com/library/view/regular-expressions-cookbook/9781449327453/ch04s07.html">https://www.oreilly.com/library/view/regular-expressions-cookbook/9781449327453/ch04s07.html</a>
 */
public class Iso8601Test {

    List<String> regexps8601AtLeastMinutes;
    List<String> regexps8601AtLeastSeconds;
    List<String> regexpsOwnFormat;

    @Before
    public void initRegexps() {

        /*
        //year
        regexps8601DateMinutes.add("^(?<year>[0-9]{4})");
        //year, month
        regexps8601DateMinutes.add("^(?<year>[0-9]{4})-(?<month>1[0-2]|0[1-9])$");
        //year, month, day
        regexps8601DateMinutes.add("^(?<year>[0-9]{4})(?<hyphen>-?)(?<month>1[0-2]|0[1-9])\\k<hyphen>(?<day>3[01]|0[1-9]|[12][0-9])$"); //
        //ordinal date: year + day of the year
        regexps8601DateMinutes.add("^(?<year>[0-9]{4})-?(?<day>36[0-6]|3[0-5][0-9]|[12][0-9]{2}|0[1-9][0-9]|00[1-9])$");
        //week: year + week of the year
        regexps8601DateMinutes.add("^(?<year>[0-9]{4})-?W(?<week>5[0-3]|[1-4][0-9]|0[1-9])$");
        //week day: year + week of the year + day of the week
        regexps8601DateMinutes.add("^(?<year>[0-9]{4})-?W(?<week>5[0-3]|[1-4][0-9]|0[1-9])-?(?<day>[1-7])$");
        */

        regexps8601AtLeastMinutes = new ArrayList<>();
        //basic
        regexps8601AtLeastMinutes.add("^" +
                "(?<year>[0-9]{4})(?<month>1[0-2]|0[1-9])(?<day>3[01]|0[1-9]|[12][0-9])" +
                "T(?<hour>2[0-3]|[01][0-9])(?<minute>[0-5][0-9])(?<second>[0-5][0-9](.(?<millis>[0-9]{3}))?)?" +
                "(?<timezone>Z|[+-](2[0-3]|[01][0-9])([0-5][0-9])?)?" +
                "$");


        //extended
        regexps8601AtLeastMinutes.add("^" +
                "(?<year>[0-9]{4})-(?<month>1[0-2]|0[1-9])-(?<day>3[01]|0[1-9]|[12][0-9])" +
                "T(?<hour>2[0-3]|[01][0-9]):(?<minute>[0-5][0-9])(:(?<second>[0-5][0-9])(.(?<millis>[0-9]{3}))?)?" +
                "(?<timezone>Z|[+-](2[0-3]|[01][0-9])(:([0-5][0-9])?)?)?" +
                "$");


        regexps8601AtLeastSeconds = new ArrayList<>();
        //basic
        regexps8601AtLeastSeconds.add("^" +
                "(?<year>[0-9]{4})(?<month>1[0-2]|0[1-9])(?<day>3[01]|0[1-9]|[12][0-9])" +
                "T(?<hour>2[0-3]|[01][0-9])(?<minute>[0-5][0-9])(?<second>[0-5][0-9])(.(?<millis>[0-9]{3}))?" +
                "(?<timezone>Z|[+-](2[0-3]|[01][0-9])([0-5][0-9])?)?" +
                "$");
        //extended
        regexps8601AtLeastSeconds.add("^" +
                "(?<year>[0-9]{4})-(?<month>1[0-2]|0[1-9])-(?<day>3[01]|0[1-9]|[12][0-9])" +
                "T(?<hour>2[0-3]|[01][0-9]):(?<minute>[0-5][0-9])(:(?<second>[0-5][0-9])(.(?<millis>[0-9]{3}))?)" +
                "(?<timezone>Z|[+-](2[0-3]|[01][0-9])(:([0-5][0-9])?)?)?" +
                "$");

        regexpsOwnFormat = new ArrayList<>();
        regexpsOwnFormat.add("^(?<year>[0-9]{4})$"); //RRRR
        regexpsOwnFormat.add("^(?<month>1[0-2]|0[1-9])\\.(?<year>[0-9]{4})$"); //MM.RRRR
        regexpsOwnFormat.add("^(?<month1>1[0-2]|0[1-9])\\.-(?<month2>1[0-2]|0[1-9])\\.(?<year>[0-9]{4})$"); //MM.-MM.RRR
        regexpsOwnFormat.add("^(?<day>3[01]|0[1-9]|[12][0-9])\\.(?<month>1[0-2]|0[1-9])\\.(?<year>[0-9]{4})$"); //DD.MM.RRRR
        regexpsOwnFormat.add("^(?<day1>3[01]|0[1-9]|[12][0-9])\\.-(?<day2>3[01]|0[1-9]|[12][0-9])\\.(?<month>1[0-2]|0[1-9])\\.(?<year>[0-9]{4})$");//DD.-DD.MM.RRRR
    }

    private boolean isValidIso8601AtLeastMinutes(String str) {
        for (String regexp : regexps8601AtLeastMinutes) {
            if (str.matches(regexp)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidIso8601AtLeastSeconds(String str) {
        for (String regexp : regexps8601AtLeastSeconds) {
            if (str.matches(regexp)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidOwn(String str) {
        for (String regexp : regexpsOwnFormat) {
            if (str.matches(regexp)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void ownFormat() {
        //see eborn_mon_2.3 supplement's mods:dateIssued
        assertTrue(isValidOwn("17.11.2020"));
        assertTrue(isValidOwn("16.-17.11.2020"));
        assertTrue(isValidOwn("10.-11.2020"));
        assertTrue(isValidOwn("11.2020"));
        assertTrue(isValidOwn("2020"));
    }

    @Test
    public void iso8601DatePartial() {
        //month
        assertFalse(isValidIso8601AtLeastMinutes("201908"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08"));
        //year
        assertFalse(isValidIso8601AtLeastMinutes("2019"));
    }

    @Test
    public void iso8601DateNoTime() {
        //basic format
        assertFalse(isValidIso8601AtLeastMinutes("20190830"));
        //extended format
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30"));
        //mixed formats
        assertFalse(isValidIso8601AtLeastMinutes("201908-30"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-0830"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830"));
    }

    @Test
    public void iso8601TimeWithHoursOnly() {
        //hours basic format
        assertFalse(isValidIso8601AtLeastMinutes("20190830T15"));
        //hours extended format
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15"));
        //hours mixed formats
        assertFalse(isValidIso8601AtLeastMinutes("201908-30T15"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-0830T15"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T15"));
    }

    @Test
    public void iso8601HoursValues() {
        //00 - ok
        assertTrue(isValidIso8601AtLeastMinutes("20190830T0059"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T0059Z"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T0059+01"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T0059+0100"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T0059+0130"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T0059-23"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T0059-2300"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T0059-2330"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T00:59"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T00:59Z"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T00:59+01"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T00:59+01:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T00:59+01:30"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T00:59-23"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T00:59-23:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T00:59-23:30"));
        //23 - ok
        assertTrue(isValidIso8601AtLeastMinutes("20190830T2359"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T2359Z"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T2359+01"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T2359+0100"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T2359+0130"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T2359-23"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T2359-2300"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T2359-2330"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T23:59"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T23:59Z"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T23:59+01"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T23:59+01:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T23:59+01:30"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T23:59-23"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T23:59-23:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T23:59-23:30"));
        //24 - wrong
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2459"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2459Z"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2459+01"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2459+0100"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2459+0130"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2459-23"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2459-2300"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2459-2330"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T24:59"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T24:59Z"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T24:59+01"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T24:59+01:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T24:59+01:30"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T24:59-23"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T24:59-23:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T24:59-23:30"));
        //25 - wrong
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2559"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2559Z"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2559+01"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2559+0100"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2559+0130"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2559-23"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2559-2300"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T2559-2330"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T25:59"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T25:59Z"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T25:59+01"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T25:59+01:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T25:59+01:30"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T25:59-23"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T25:59-23:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T25:59-23:30"));
    }

    @Test
    public void iso8601MinutesValues() {
        //00 - ok
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1500"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1500Z"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1500+01"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1500+0100"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1500+0130"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1500-23"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1500-2300"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1500-2330"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:00Z"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:00+01"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:00+01:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:00+01:30"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:00-23"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:00-23:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:00-23:30"));
        //59 - ok
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1559"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1559Z"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1559+01"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1559+0100"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1559+0130"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1559-23"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1559-2300"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1559-2330"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:59"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:59Z"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:59+01"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:59+01:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:59+01:30"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:59-23"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:59-23:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:59-23:30"));
        //60 - wrong
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1560"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1560Z"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1560+01"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1560+0100"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1560+0130"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1560-23"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1560-2300"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1560-2330"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:60"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:60Z"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:60+01"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:60+01:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:60+01:30"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:60-23"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:60-23:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:60-23:30"));
        //66 - wrong
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1566"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1566Z"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1566+01"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1566+0100"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1566+0130"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1566-23"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1566-2300"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1566-2330"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:66"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:66Z"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:66+01"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:66+01:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:66+01:30"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:66-23"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:66-23:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:66-23:30"));
    }

    @Test
    public void iso8601SecondsValues() {
        //44
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544Z"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544+01"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544+0100"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544+0130"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544-23"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544-2300"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544-2330"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44Z"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44+01"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44+01:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44+01:30"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44-23"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44-23:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44-23:30"));
        //00 - ok
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153500"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153500Z"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153500+01"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153500+0100"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153500+0130"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153500-23"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153500-2300"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153500-2330"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:00Z"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:00+01"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:00+01:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:00+01:30"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:00-23"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:00-23:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:00-23:30"));
        //59 - ok
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153559"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153559Z"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153559+01"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153559+0100"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153559+0130"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153559-23"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153559-2300"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153559-2330"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:59"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:59Z"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:59+01"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:59+01:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:59+01:30"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:59-23"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:59-23:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:59-23:30"));
        //60 - wrong
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153560"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153560Z"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153560+01"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153560+0100"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153560+0130"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153560-23"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153560-2300"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153560-2330"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:60"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:60Z"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:60+01"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:60+01:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:60+01:30"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:60-23"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:60-23:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:60-23:30"));
        //66 - wrong
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153566"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153566Z"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153566+01"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153566+0100"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153566+0130"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153566-23"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153566-2300"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153566-2330"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:66"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:66Z"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:66+01"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:66+01:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:66+01:30"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:66-23"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:66-23:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:66-23:30"));
    }

    @Test
    public void iso8601MinutesBasicFormat() {
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1535"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1535Z"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1535+01"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1535+0100"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1535+0130"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1535-23"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1535-2300"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T1535-2330"));

        assertFalse(isValidIso8601AtLeastMinutes("20190830T1535+01:00"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1535+01:30"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1535-23:00"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T1535-23:30"));
    }

    @Test
    public void iso8601MinutesExtendedFormat() {
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35Z"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35+01"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35+01:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35+01:30"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35-23"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35-23:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35-23:30"));

        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35+0100"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35+0130"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35-2300"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35-2330"));
    }

    @Test
    public void iso8601MinutesMixedFormats() {
        assertFalse(isValidIso8601AtLeastMinutes("201908-30T15:35"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-0830T15:35"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T1535"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T15:35"));
    }

    @Test
    public void iso8601SecondsBasicFormat() {
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544Z"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544+01"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544+0100"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544+0130"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544-23"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544-2300"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544-2330"));

        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544+01:00"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544+01:30"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544-23:00"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544-23:30"));
    }

    @Test
    public void iso8601SecondsExtendedFormat() {
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44Z"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44+01"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44+01:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44+01:30"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44-23"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44-23:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44-23:30"));

        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44+0100"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44+0130"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44-2300"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44-2330"));
    }

    @Test
    public void iso8601SecondsMixedFormats() {
        assertFalse(isValidIso8601AtLeastMinutes("201908-30T15:35:44"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-0830T15:35:44"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T153544"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T15:35:44"));
    }

    @Test
    public void iso8601AtLeastSeconds() {
        //minutes only
        assertFalse(isValidIso8601AtLeastSeconds("20190830T1535"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T1535Z"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T1535+01"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T1535+0100"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T1535+0130"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T1535-23"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T1535-2300"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T1535-2330"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35Z"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35+01"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35+01:00"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35+01:30"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35-23"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35-23:00"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35-23:30"));
        //seconds
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544Z"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544+01"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544+0100"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544+0130"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544-23"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544-2300"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544-2330"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44Z"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44+01"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44+01:00"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44+01:30"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44-23"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44-23:00"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44-23:30"));
    }

    @Test
    public void iso8601AtLeastSecondsMillis() {
        //SS.s
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.1"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.1Z"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.1+01"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.1+0100"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.1+0130"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.1-23"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.1-2300"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.1-2330"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.1"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.1Z"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.1+01"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.1+01:00"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.1+01:30"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.1-23"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.1-23:00"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.1-23:30"));
        //SS.ss
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.12"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.12Z"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.12+01"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.12+0100"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.12+0130"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.12-23"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.12-2300"));
        assertFalse(isValidIso8601AtLeastSeconds("20190830T153544.12-2330"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.12"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.12Z"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.12+01"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.12+01:00"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.12+01:30"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.12-23"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.12-23:00"));
        assertFalse(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.12-23:30"));
        //SS.sss
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544.123"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544.123Z"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544.123+01"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544.123+0100"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544.123+0130"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544.123-23"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544.123-2300"));
        assertTrue(isValidIso8601AtLeastSeconds("20190830T153544.123-2330"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.123"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.123Z"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.123+01"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.123+01:00"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.123+01:30"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.123-23"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.123-23:00"));
        assertTrue(isValidIso8601AtLeastSeconds("2019-08-30T15:35:44.123-23:30"));
    }

    @Test
    public void iso8601AtLeastMinutesMillis() {
        //SS.s
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.1"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.1Z"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.1+01"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.1+0100"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.1+0130"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.1-23"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.1-2300"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.1-2330"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.1"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.1Z"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.1+01"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.1+01:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.1+01:30"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.1-23"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.1-23:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.1-23:30"));
        //SS.ss
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.12"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.12Z"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.12+01"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.12+0100"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.12+0130"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.12-23"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.12-2300"));
        assertFalse(isValidIso8601AtLeastMinutes("20190830T153544.12-2330"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.12"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.12Z"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.12+01"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.12+01:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.12+01:30"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.12-23"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.12-23:00"));
        assertFalse(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.12-23:30"));
        //SS.sss
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544.123"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544.123Z"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544.123+01"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544.123+0100"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544.123+0130"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544.123-23"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544.123-2300"));
        assertTrue(isValidIso8601AtLeastMinutes("20190830T153544.123-2330"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.123"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.123Z"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.123+01"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.123+01:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.123+01:30"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.123-23"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.123-23:00"));
        assertTrue(isValidIso8601AtLeastMinutes("2019-08-30T15:35:44.123-23:30"));
    }

    //TODO: testy pro nižší detail, než minuty - takové věci by projít neměly

   /* @Test
    public void iso8601Year() {
        assertTrue(isIso8601DateMinutes("2019"));
        assertFalse(isIso8601DateMinutes("19"));
    }

    @Test
    public void iso8601YearMonth() {
        assertTrue(isIso8601DateMinutes("2009-03"));
        assertFalse(isIso8601DateMinutes("200903"));
    }

    @Test
    public void iso8601YearMonthDay() {
        assertTrue(isIso8601DateMinutes("20090320"));
        assertTrue(isIso8601DateMinutes("2009-03-20"));
        assertFalse(isIso8601DateMinutes("2009-0320"));
        assertFalse(isIso8601DateMinutes("200903-20"));
    }

    @Test
    public void iso8601OrdinalDate() {
        assertTrue(isIso8601DateMinutes("2008-243"));
        assertFalse(isIso8601DateMinutes("2008-367"));
        assertTrue(isIso8601DateMinutes("2008243"));
        assertFalse(isIso8601DateMinutes("2008367"));
    }

    @Test
    public void iso8601Week() {
        assertTrue(isIso8601DateMinutes("2008-W35"));
        assertTrue(isIso8601DateMinutes("2008W35"));
        assertFalse(isIso8601DateMinutes("2008-W00"));
        assertFalse(isIso8601DateMinutes("2008W0"));
        assertFalse(isIso8601DateMinutes("2008-W54"));
        assertFalse(isIso8601DateMinutes("2008W54"));
        assertFalse(isIso8601DateMinutes("2008-W1"));
        assertFalse(isIso8601DateMinutes("2008W1"));
    }

    @Test
    public void iso8601WeekDay() {
        assertTrue(isIso8601DateMinutes("2008-W35-6"));
        assertTrue(isIso8601DateMinutes("2008W356"));
    }*/

}
