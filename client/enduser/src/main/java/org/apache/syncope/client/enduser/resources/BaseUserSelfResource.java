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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;

public abstract class BaseUserSelfResource extends BaseResource {

    private static final long serialVersionUID = -5892402817902884085L;

    protected void dateToMillis(final Map<String, AttrTO> plainAttrMap, final PlainSchemaTO plainSchema)
            throws ParseException {
        if (plainAttrMap.containsKey(plainSchema.getKey())) {
            FastDateFormat fmt = FastDateFormat.getInstance(plainSchema.getConversionPattern());

            AttrTO dateAttr = plainAttrMap.get(plainSchema.getKey());
            List<String> milliValues = new ArrayList<>(dateAttr.getValues().size());
            for (String value : dateAttr.getValues()) {
                milliValues.add(String.valueOf(fmt.parse(value).getTime()));
            }
            dateAttr.getValues().clear();
            dateAttr.getValues().addAll(milliValues);
        }
    }

    protected void millisToDate(final Map<String, AttrTO> plainAttrMap, final PlainSchemaTO plainSchema)
            throws IllegalArgumentException {
        if (plainAttrMap.containsKey(plainSchema.getKey())) {
            FastDateFormat fmt = FastDateFormat.getInstance(plainSchema.getConversionPattern());

            AttrTO dateAttr = plainAttrMap.get(plainSchema.getKey());
            List<String> formattedValues = new ArrayList<>(dateAttr.getValues().size());
            for (String value : dateAttr.getValues()) {
                try {
                    formattedValues.add(fmt.format(Long.valueOf(value)));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid format value for " + value);
                }
            }
            dateAttr.getValues().clear();
            dateAttr.getValues().addAll(formattedValues);
        }
    }

}
