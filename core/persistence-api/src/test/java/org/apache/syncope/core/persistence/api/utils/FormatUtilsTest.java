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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.junit.jupiter.api.Test;

public class FormatUtilsTest extends AbstractTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final OffsetDateTime DATE = OffsetDateTime.now();

    @Test
    public void formatDate() {
        assertEquals(
                DATE.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                FormatUtils.format(DATE));

        String conversionPattern = "dd/MM/yyyy";
        assertEquals(
                DATE.format(DateTimeFormatter.ofPattern(conversionPattern)),
                FormatUtils.format(DATE, conversionPattern));
    }

    @Test
    public void formatLongNumber() {
        long number = RANDOM.nextLong();
        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        assertEquals(df.format(number), FormatUtils.format(number));

        String conversionPattern = "###,###";
        df = new DecimalFormat(conversionPattern);
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        assertEquals(df.format(number), FormatUtils.format(number, conversionPattern));
    }

    @Test
    public void formatDoubleNumber() {
        double number = RANDOM.nextDouble();
        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        assertEquals(df.format(number), FormatUtils.format(number));

        String conversionPattern = "###,###";
        df = new DecimalFormat(conversionPattern);
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        assertEquals(df.format(number), FormatUtils.format(number, conversionPattern));
    }

    @Test
    public void parseDate() throws ParseException {
        String source = DATE.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(
                OffsetDateTime.parse(source, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                FormatUtils.parseDate(source));

        String conversionPattern = "dd-MM-yyyy";
        source = DATE.format(DateTimeFormatter.ofPattern(conversionPattern));
        assertEquals(
                LocalDate.parse(source, DateTimeFormatter.ofPattern(conversionPattern)).
                        atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(),
                FormatUtils.parseDate(source, conversionPattern));
    }

    @Test
    public void parseNumber() throws ParseException {
        String source = String.valueOf(RANDOM.nextLong());
        String conversionPattern = "###,###";
        assertEquals(Long.valueOf(source), FormatUtils.parseNumber(source, conversionPattern));
    }
}
