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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.junit.jupiter.api.Test;

public class FormatUtilsTest extends AbstractTest {

    private final Calendar calendar = Calendar.getInstance();

    private final Date date = calendar.getTime();

    private String conversionPattern;

    @Test
    public void formatDate() {
        assertEquals(new SimpleDateFormat(SyncopeConstants.DEFAULT_DATE_PATTERN).format(date),
                FormatUtils.format(date));

        conversionPattern = "dd/MM/yyyy";
        assertEquals(new SimpleDateFormat(conversionPattern).format(date),
                FormatUtils.format(date, false, conversionPattern));
    }

    @Test
    public void formatLongNumber() {
        long number = date.getTime();
        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        assertEquals(df.format(number), FormatUtils.format(number));

        conversionPattern = "###,###";
        df = new DecimalFormat(conversionPattern);
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        assertEquals(df.format(number), FormatUtils.format(number, conversionPattern));
    }

    @Test
    public void formatDoubleNumber() {
        double number = date.getTime();
        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        assertEquals(df.format(number), FormatUtils.format(number));

        conversionPattern = "###,###";
        df = new DecimalFormat(conversionPattern);
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        assertEquals(df.format(number), FormatUtils.format(number, conversionPattern));
    }

    @Test
    public void parseDate() throws ParseException {
        String source = new SimpleDateFormat(SyncopeConstants.DEFAULT_DATE_PATTERN).format(date);
        assertEquals(DateUtils.parseDate(source, SyncopeConstants.DATE_PATTERNS),
                FormatUtils.parseDate(source));

        conversionPattern = "dd-MM-yyyy";
        source = new SimpleDateFormat(conversionPattern).format(date);
        assertEquals(DateUtils.parseDate(source, conversionPattern),
                FormatUtils.parseDate(source, conversionPattern));
    }

    @Test
    public void parseNumber() throws ParseException {
        String source = String.valueOf(date.getTime());
        conversionPattern = "###,###";
        assertEquals(Long.valueOf(source), FormatUtils.parseNumber(source, conversionPattern));
    }
}
