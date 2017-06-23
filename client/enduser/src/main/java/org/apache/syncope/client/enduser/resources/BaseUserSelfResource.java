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
import java.util.HashSet;
import org.apache.commons.collections4.Predicate;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.client.enduser.model.CustomAttributesInfo;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;

public abstract class BaseUserSelfResource extends BaseResource {

    private static final long serialVersionUID = -5892402817902884085L;

    protected void dateToMillis(final Set<AttrTO> attrs, final PlainSchemaTO plainSchema)
            throws ParseException {
        final FastDateFormat fmt = FastDateFormat.getInstance(plainSchema.getConversionPattern());

        for (AttrTO attr : attrs) {
            if (attr.getSchema().equals(plainSchema.getKey())) {
                CollectionUtils.transform(attr.getValues(), new Transformer<String, String>() {

                    @Override
                    public String transform(final String input) {
                        try {
                            return String.valueOf(fmt.parse(input).getTime());
                        } catch (ParseException ex) {
                            LOG.error("Unable to parse date {}", input);
                            return input;
                        }
                    }
                });
            }
        }
    }

    protected void millisToDate(final Set<AttrTO> attrs, final PlainSchemaTO plainSchema)
            throws IllegalArgumentException {
        final FastDateFormat fmt = FastDateFormat.getInstance(plainSchema.getConversionPattern());
        for (AttrTO attr : attrs) {
            if (attr.getSchema().equals(plainSchema.getKey())) {
                CollectionUtils.transform(attr.getValues(), new Transformer<String, String>() {

                    @Override
                    public String transform(final String input) {
                        try {
                            return fmt.format(Long.valueOf(input));
                        } catch (NumberFormatException ex) {
                            LOG.error("Invalid format value for {}", input);
                            return input;
                        }
                    }
                });
            }
        }
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

    protected void customizeAttrTOs(final Set<AttrTO> attrs, final CustomAttributesInfo customAttributesInfo) {
        if (customAttributesInfo != null
                && customAttributesInfo.getShow()
                && !customAttributesInfo.getAttributes().isEmpty()) {
            Set<AttrTO> attrsToAdd = new HashSet<>();
            for (AttrTO attr : attrs) {
                if (customAttributesInfo.getAttributes().containsKey(attr.getSchema())) {
                    attrsToAdd.add(attr);
                }
            }
            attrs.clear();
            attrs.addAll(attrsToAdd);
        } else if (customAttributesInfo != null && !customAttributesInfo.getShow()) {
            attrs.clear();
        }
    }

    protected void customizeAttrPatches(final Set<AttrPatch> attrs, final CustomAttributesInfo customAttributesInfo) {
        if (customAttributesInfo != null
                && customAttributesInfo.getShow()
                && !customAttributesInfo.getAttributes().isEmpty()) {
            CollectionUtils.filter(attrs, new Predicate<AttrPatch>() {

                @Override
                public boolean evaluate(final AttrPatch patchPlainAttr) {
                    // if membership attribute clean schema name coming from custom form
                    return customAttributesInfo.getAttributes().containsKey(patchPlainAttr.getAttrTO().getSchema());
                }
            });
        } else if (customAttributesInfo != null && !customAttributesInfo.getShow()) {
            attrs.clear();
        }
    }

}
