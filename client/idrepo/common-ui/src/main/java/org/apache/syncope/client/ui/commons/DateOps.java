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

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.wicket.model.IModel;

public final class DateOps {

    public static class Format implements Serializable {

        private static final long serialVersionUID = 27103019852866L;

        private final FastDateFormat fdf;

        public Format(final FastDateFormat fdf) {
            this.fdf = fdf;
        }

        public String format(final Date date) {
            return Optional.ofNullable(date).map(fdf::format).orElse(StringUtils.EMPTY);
        }

        public String format(final OffsetDateTime date) {
            return Optional.ofNullable(date).map(v -> fdf.format(convert(date))).orElse(StringUtils.EMPTY);
        }
    }

    public static class WrappedDateModel implements IModel<Date>, Serializable {

        private static final long serialVersionUID = 31027882183172L;

        private final IModel<OffsetDateTime> wrapped;

        public WrappedDateModel(final IModel<OffsetDateTime> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Date getObject() {
            return convert(wrapped.getObject());
        }

        @Override
        public void setObject(final Date object) {
            wrapped.setObject(convert(object));
        }
    }

    public static final ZoneOffset DEFAULT_OFFSET = OffsetDateTime.now().getOffset();

    public static Date convert(final OffsetDateTime date) {
        return Optional.ofNullable(date).map(v -> new Date(v.toInstant().toEpochMilli())).orElse(null);
    }

    public static OffsetDateTime convert(final Date date) {
        return Optional.ofNullable(date).map(v -> v.toInstant().atOffset(DEFAULT_OFFSET)).orElse(null);
    }

    private DateOps() {
        // private constructor for static utility class
    }
}
