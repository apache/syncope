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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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

        public String format(final ZonedDateTime date) {
            return Optional.ofNullable(date).map(v -> fdf.format(convert(date))).orElse(StringUtils.EMPTY);
        }

        public String format(final LocalDateTime date) {
            return Optional.ofNullable(date).map(v -> fdf.format(convert(v))).orElse(StringUtils.EMPTY);
        }
    }

    public static final class WrappedDateModel implements IModel<Date>, Serializable {

        private static final long serialVersionUID = 31027882183172L;

        public static WrappedDateModel ofOffset(final IModel<OffsetDateTime> offset) {
            WrappedDateModel instance = new WrappedDateModel();
            instance.offset = offset;
            return instance;
        }

        public static WrappedDateModel ofZoned(final IModel<ZonedDateTime> zoned) {
            WrappedDateModel instance = new WrappedDateModel();
            instance.zoned = zoned;
            return instance;
        }

        public static WrappedDateModel ofLocal(final IModel<LocalDateTime> local) {
            WrappedDateModel instance = new WrappedDateModel();
            instance.local = local;
            return instance;
        }

        private IModel<LocalDateTime> local;

        private IModel<OffsetDateTime> offset;

        private IModel<ZonedDateTime> zoned;

        private WrappedDateModel() {
            // private constructor for static utility class
        }

        @Override
        public Date getObject() {
            if (offset != null) {
                return convert(offset.getObject());
            } else if (zoned != null) {
                return convert(zoned.getObject());
            } else if (local != null) {
                return convert(local.getObject());
            }
            return null;
        }

        @Override
        public void setObject(final Date object) {
            if (offset != null) {
                offset.setObject(toOffsetDateTime(object));
            } else if (zoned != null) {
                zoned.setObject(toZonedDateTime(object));
            } else if (local != null) {
                local.setObject(toLocalDateTime(object));
            }
        }
    }

    public static final ZoneOffset DEFAULT_OFFSET = OffsetDateTime.now().getOffset();

    public static final ZoneId DEFAULT_ZONE = ZonedDateTime.now().getZone();

    public static Date convert(final OffsetDateTime date) {
        return Optional.ofNullable(date).map(v -> new Date(v.toInstant().toEpochMilli())).orElse(null);
    }

    public static Date convert(final ZonedDateTime date) {
        return Optional.ofNullable(date).map(v -> new Date(v.toInstant().toEpochMilli())).orElse(null);
    }

    public static Date convert(final LocalDateTime date) {
        return Optional.ofNullable(date).map(v -> Date.from(v.atZone(DEFAULT_ZONE).toInstant())).orElse(null);
    }

    public static OffsetDateTime toOffsetDateTime(final Date date) {
        return Optional.ofNullable(date).map(v -> v.toInstant().atOffset(DEFAULT_OFFSET)).orElse(null);
    }

    public static ZonedDateTime toZonedDateTime(final Date date) {
        return Optional.ofNullable(date).map(v -> ZonedDateTime.ofInstant(v.toInstant(), DEFAULT_ZONE)).orElse(null);
    }

    public static LocalDateTime toLocalDateTime(final Date date) {
        return Optional.ofNullable(date).map(v -> LocalDateTime.ofInstant(v.toInstant(), DEFAULT_ZONE)).orElse(null);
    }

    private DateOps() {
        // private constructor for static utility class
    }
}
