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
package org.apache.syncope.core.persistence.api.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;

/**
 * Utility class for parsing / formatting dates and numbers.
 */
public final class FormatUtils {

    private static final String NO_CONVERSION_PATTERN = "#,##0.###";

    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormat df = new DecimalFormat(NO_CONVERSION_PATTERN);
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return df;
    });

    public static final ZoneOffset DEFAULT_OFFSET = OffsetDateTime.now().getOffset();

    public static String format(final TemporalAccessor temporal) {
        return OffsetDateTime.from(temporal).
                truncatedTo(ChronoUnit.SECONDS).
                format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static String format(final TemporalAccessor temporal, final String conversionPattern) {
        return OffsetDateTime.from(temporal).format(DateTimeFormatter.ofPattern(conversionPattern));
    }

    public static String format(final long number) {
        return format(number, NO_CONVERSION_PATTERN);
    }

    public static String format(final long number, final String conversionPattern) {
        DecimalFormat df = DECIMAL_FORMAT.get();

        String previous = df.toPattern();
        if (!previous.equals(conversionPattern)) {
            df.applyPattern(conversionPattern);
        }

        String formatted = df.format(number);

        if (!previous.equals(conversionPattern)) {
            df.applyPattern(previous);
        }

        return formatted;
    }

    public static String format(final double number) {
        return format(number, NO_CONVERSION_PATTERN);
    }

    public static String format(final double number, final String conversionPattern) {
        DecimalFormat df = DECIMAL_FORMAT.get();

        String previous = df.toPattern();
        if (!previous.equals(conversionPattern)) {
            df.applyPattern(conversionPattern);
        }

        String formatted = df.format(number);

        if (!previous.equals(conversionPattern)) {
            df.applyPattern(previous);
        }

        return formatted;
    }

    public static OffsetDateTime parseDate(final String source)
            throws DateTimeParseException {

        for (String pattern : SyncopeConstants.DATE_PATTERNS) {
            try {
                return parseDate(source, pattern);
            } catch (DateTimeParseException e) {
                // ignore
            }
        }

        throw new DateTimeParseException(
                "Could not parse with any of " + Arrays.asList(SyncopeConstants.DATE_PATTERNS), source, 0);
    }

    public static OffsetDateTime parseDate(final String source, final String conversionPattern)
            throws DateTimeParseException {

        DateTimeParseException dtpe;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(conversionPattern);
        try {
            if (StringUtils.containsIgnoreCase(conversionPattern, "Z")) {
                return OffsetDateTime.parse(source, dtf);
            } else {
                return LocalDateTime.parse(source, dtf).atZone(DEFAULT_OFFSET).toOffsetDateTime();
            }
        } catch (DateTimeParseException e) {
            dtpe = e;
        }
        try {
            return LocalDate.parse(source, dtf).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        } catch (DateTimeParseException e) {
            dtpe = e;
        }

        throw dtpe;
    }

    public static Number parseNumber(final String source, final String conversionPattern) throws ParseException {
        DecimalFormat df = DECIMAL_FORMAT.get();
        df.applyPattern(conversionPattern);
        return df.parse(source);
    }

    public static void clear() {
        DECIMAL_FORMAT.remove();
    }

    private FormatUtils() {
        // private empty constructor
    }
}
