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
package org.apache.syncope.common.rest.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Date;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.common.lib.SyncopeConstants;

public class DateParamConverterProvider implements ParamConverterProvider {

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(SyncopeConstants.DEFAULT_DATE_PATTERN);

    private static class DateParamConverter implements ParamConverter<Date> {

        @Override
        public Date fromString(final String value) {
            try {
                return DATE_FORMAT.parse(value);
            } catch (final ParseException e) {
                throw new IllegalArgumentException("Unparsable date: " + value, e);
            }
        }

        @Override
        public String toString(final Date value) {
            return DATE_FORMAT.format(value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(
            final Class<T> rawType, final Type genericType, final Annotation[] annotations) {

        if (Date.class.equals(rawType)) {
            return (ParamConverter<T>) new DateParamConverter();
        }

        return null;
    }

}
