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
package org.apache.syncope.client.ui.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.Test;

class DateOpsTest {

    @Test
    void convertNull() {
        assertNull(DateOps.convert((OffsetDateTime) null));
        assertNull(DateOps.convert((ZonedDateTime) null));
        assertNull(DateOps.convert((LocalDateTime) null));
        assertNull(DateOps.toOffsetDateTime((Date) null));
        assertNull(DateOps.toZonedDateTime((Date) null));
        assertNull(DateOps.toLocalDateTime((Date) null));
    }

    @Test
    void convertOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.now();
        Date date = DateOps.convert(odt);
        assertNotNull(date);
        assertEquals(odt.toInstant().toEpochMilli(), date.getTime());

        OffsetDateTime back = DateOps.toOffsetDateTime(date);
        assertEquals(
                odt.toInstant().truncatedTo(ChronoUnit.MILLIS),
                back.toInstant().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void convertZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.now();
        Date date = DateOps.convert(zdt);
        assertNotNull(date);
        assertEquals(zdt.toInstant().toEpochMilli(), date.getTime());

        ZonedDateTime back = DateOps.toZonedDateTime(date);
        assertEquals(
                zdt.toInstant().truncatedTo(ChronoUnit.MILLIS),
                back.toInstant().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void convertLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.now();
        Date date = DateOps.convert(ldt);
        assertNotNull(date);
        assertEquals(ldt.atZone(DateOps.DEFAULT_ZONE).toInstant().toEpochMilli(), date.getTime());
    }
}
