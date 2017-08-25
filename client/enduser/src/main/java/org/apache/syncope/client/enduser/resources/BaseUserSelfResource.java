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
package org.apache.syncope.client.enduser.resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ListIterator;
import java.util.Set;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;

public abstract class BaseUserSelfResource extends BaseResource {

    private static final long serialVersionUID = -5892402817902884085L;

    protected void dateToMillis(final Set<AttrTO> attrs, final PlainSchemaTO plainSchema)
            throws ParseException {

        final FastDateFormat fmt = FastDateFormat.getInstance(plainSchema.getConversionPattern());
        attrs.stream().
                filter(attr -> (attr.getSchema().equals(plainSchema.getKey()))).
                forEachOrdered(attr -> {
                    for (ListIterator<String> itor = attr.getValues().listIterator(); itor.hasNext();) {
                        String value = itor.next();
                        try {
                            itor.set(String.valueOf(fmt.parse(value).getTime()));
                        } catch (ParseException ex) {
                            LOG.error("Unable to parse date {}", value);
                        }
                    }
                });
    }

    protected void millisToDate(final Set<AttrTO> attrs, final PlainSchemaTO plainSchema)
            throws IllegalArgumentException {

        final FastDateFormat fmt = FastDateFormat.getInstance(plainSchema.getConversionPattern());
        attrs.stream().
                filter(attr -> (attr.getSchema().equals(plainSchema.getKey()))).
                forEachOrdered(attr -> {
                    for (ListIterator<String> itor = attr.getValues().listIterator(); itor.hasNext();) {
                        String value = itor.next();
                        try {
                            itor.set(fmt.format(Long.valueOf(value)));
                        } catch (NumberFormatException ex) {
                            LOG.error("Invalid format value for {}", value);
                        }
                    }
                });
    }

    protected void buildResponse(final ResourceResponse response, final int statusCode, final String message) {
        response.setTextEncoding(StandardCharsets.UTF_8.name());
        response.setStatusCode(statusCode);
        response.setWriteCallback(new WriteCallback() {

            @Override
            public void writeData(final Attributes attributes) throws IOException {
                attributes.getResponse().write(message);
            }
        });
    }

}
