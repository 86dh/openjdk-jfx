/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package test.javafx.util.converter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javafx.util.converter.DateTimeStringConverter;
import javafx.util.converter.DateTimeStringConverterShim;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 */
public class DateTimeStringConverterTest {
    private static final Date VALID_DATE_WITH_SECONDS;
    private static final Date VALID_DATE_WITHOUT_SECONDS;

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 1985);
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 12);
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 34);
        c.set(Calendar.SECOND, 56);
        c.set(Calendar.MILLISECOND, 0);
        VALID_DATE_WITH_SECONDS = c.getTime();
        c.set(Calendar.SECOND, 0);
        VALID_DATE_WITHOUT_SECONDS = c.getTime();
    }

    private static Collection implementations() {
        return Arrays.asList(new Object[][] {
                { new DateTimeStringConverter(),
                        Locale.getDefault(Locale.Category.FORMAT), DateFormat.DEFAULT, DateFormat.DEFAULT,
                        VALID_DATE_WITH_SECONDS, null, null },

                { new DateTimeStringConverter(DateFormat.SHORT, DateFormat.SHORT),
                        Locale.getDefault(Locale.Category.FORMAT), DateFormat.SHORT, DateFormat.SHORT,
                        VALID_DATE_WITHOUT_SECONDS, null, null },

                { new DateTimeStringConverter(Locale.UK),
                        Locale.UK, DateFormat.DEFAULT, DateFormat.DEFAULT,
                        VALID_DATE_WITH_SECONDS, null, null },

                { new DateTimeStringConverter(Locale.UK, DateFormat.SHORT, DateFormat.SHORT),
                        Locale.UK, DateFormat.SHORT, DateFormat.SHORT,
                        VALID_DATE_WITHOUT_SECONDS, null, null },

                { new DateTimeStringConverter("dd MM yyyy HH mm ss"),
                        Locale.getDefault(Locale.Category.FORMAT), DateFormat.DEFAULT, DateFormat.DEFAULT,
                        VALID_DATE_WITH_SECONDS, "dd MM yyyy HH mm ss", null },

                { new DateTimeStringConverter(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL)),
                        Locale.getDefault(Locale.Category.FORMAT), DateFormat.DEFAULT, DateFormat.DEFAULT,
                        VALID_DATE_WITH_SECONDS, null, DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL) },
        });
    }

    private DateTimeStringConverter converter;
    private Locale locale;
    private int dateStyle;
    private int timeStyle;
    private String pattern;
    private DateFormat dateFormat;
    private Date validDate;
    private DateFormat validFormatter;

    private void setUp(DateTimeStringConverter converter, Locale locale, int dateStyle, int timeStyle, Date validDate, String pattern, DateFormat dateFormat) {
        this.converter = converter;
        this.locale = locale;
        this.dateStyle = dateStyle;
        this.timeStyle = timeStyle;
        this.validDate = validDate;
        this.pattern = pattern;
        this.dateFormat = dateFormat;

        if (dateFormat != null) {
            validFormatter = dateFormat;
        } else if (pattern != null) {
            validFormatter = new SimpleDateFormat(pattern);
        } else {
            validFormatter = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
        }
    }

    /*********************************************************************
     * Test constructors
     ********************************************************************/

    @ParameterizedTest
    @MethodSource("implementations")
    public void testConstructor(DateTimeStringConverter converter, Locale locale, int dateStyle, int timeStyle, Date validDate, String pattern, DateFormat dateFormat) {
        setUp(converter, locale, dateStyle, timeStyle, validDate, pattern, dateFormat);
        assertEquals(locale, DateTimeStringConverterShim.getLocale(converter));
        assertEquals(dateStyle, DateTimeStringConverterShim.getDateStyle(converter));
        assertEquals(pattern, DateTimeStringConverterShim.getPattern(converter));
        assertEquals(dateFormat, DateTimeStringConverterShim.getDateFormatVar(converter));
        assertEquals(timeStyle, DateTimeStringConverterShim.getTimeStyle(converter));
    }

    /*********************************************************************
     * Test methods
     ********************************************************************/

    @ParameterizedTest
    @MethodSource("implementations")
    public void getDateFormat_default(DateTimeStringConverter converter, Locale locale, int dateStyle, int timeStyle, Date validDate, String pattern, DateFormat dateFormat) {
        setUp(converter, locale, dateStyle, timeStyle, validDate, pattern, dateFormat);
        assertNotNull(DateTimeStringConverterShim.getDateFormat(converter));
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void getDateFormat_nonNullPattern(DateTimeStringConverter converter, Locale locale, int dateStyle, int timeStyle, Date validDate, String pattern, DateFormat dateFormat) {
        setUp(converter, locale, dateStyle, timeStyle, validDate, pattern, dateFormat);
        converter = new DateTimeStringConverter("yyyy/MM/dd HH:mm:ss");
        assertTrue(DateTimeStringConverterShim.getDateFormat(converter)
                instanceof SimpleDateFormat);
    }

    /*********************************************************************
     * Test toString / fromString methods
     ********************************************************************/

    @ParameterizedTest
    @MethodSource("implementations")
    public void fromString_testValidInput(DateTimeStringConverter converter, Locale locale, int dateStyle, int timeStyle, Date validDate, String pattern, DateFormat dateFormat) {
        setUp(converter, locale, dateStyle, timeStyle, validDate, pattern, dateFormat);
        String input = validFormatter.format(validDate);
        assertEquals(validDate, converter.fromString(input), "Input = " + input);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void fromString_testValidInputWithWhiteSpace(DateTimeStringConverter converter, Locale locale, int dateStyle, int timeStyle, Date validDate, String pattern, DateFormat dateFormat) {
        setUp(converter, locale, dateStyle, timeStyle, validDate, pattern, dateFormat);
        String input = validFormatter.format(validDate);
        assertEquals(validDate, converter.fromString("      " + input + "      "), "Input = " + input);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void fromString_testInvalidInput(DateTimeStringConverter converter, Locale locale, int dateStyle, int timeStyle, Date validDate, String pattern, DateFormat dateFormat) {
        setUp(converter, locale, dateStyle, timeStyle, validDate, pattern, dateFormat);
        assertThrows(RuntimeException.class, () -> converter.fromString("abcdefg"));
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void toString_validOutput(DateTimeStringConverter converter, Locale locale, int dateStyle, int timeStyle, Date validDate, String pattern, DateFormat dateFormat) {
        setUp(converter, locale, dateStyle, timeStyle, validDate, pattern, dateFormat);
        assertEquals(validFormatter.format(validDate), converter.toString(validDate));
    }
}
