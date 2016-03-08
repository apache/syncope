/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.provisioning.api.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.syncope.common.lib.SyncopeConstants;

/**
 * Utility class for parsing / formatting date and numbers.
 */
public final class FormatUtils {

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat();
            sdf.applyPattern(SyncopeConstants.DEFAULT_DATE_PATTERN);
            return sdf;
        }
    };

    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = new ThreadLocal<DecimalFormat>() {

        @Override
        protected DecimalFormat initialValue() {
            DecimalFormat df = new DecimalFormat();
            df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
            return df;
        }
    };

    public static String format(final Date date) {
        return format(date, true);
    }

    public static String format(final Date date, final boolean lenient) {
        return format(date, lenient, null);
    }

    public static String format(final Date date, final boolean lenient, final String conversionPattern) {
        SimpleDateFormat sdf = DATE_FORMAT.get();
        if (conversionPattern != null) {
            sdf.applyPattern(conversionPattern);
        }
        sdf.setLenient(lenient);
        return sdf.format(date);
    }

    public static String format(final long number) {
        return format(number, null);
    }

    public static String format(final long number, final String conversionPattern) {
        DecimalFormat df = DECIMAL_FORMAT.get();
        if (conversionPattern != null) {
            df.applyPattern(conversionPattern);
        }
        return df.format(number);
    }

    public static String format(final double number) {
        return format(number, null);
    }

    public static String format(final double number, final String conversionPattern) {
        DecimalFormat df = DECIMAL_FORMAT.get();
        if (conversionPattern != null) {
            df.applyPattern(conversionPattern);
        }
        return df.format(number);
    }

    public static Date parseDate(final String source) throws ParseException {
        return DateUtils.parseDate(source, SyncopeConstants.DATE_PATTERNS);
    }

    public static Date parseDate(final String source, final String conversionPattern) throws ParseException {
        SimpleDateFormat sdf = DATE_FORMAT.get();
        sdf.applyPattern(conversionPattern);
        sdf.setLenient(false);
        return sdf.parse(source);
    }

    public static Number parseNumber(final String source, final String conversionPattern) throws ParseException {
        DecimalFormat df = DECIMAL_FORMAT.get();
        df.applyPattern(conversionPattern);
        return df.parse(source);
    }

    public static void clear() {
        DATE_FORMAT.remove();
        DECIMAL_FORMAT.remove();
    }

    private FormatUtils() {
        // private empty constructor
    }
}
