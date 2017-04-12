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
package org.apache.syncope.client.enduser.util;

import java.util.Map;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.client.enduser.model.CustomAttribute;
import org.apache.syncope.client.enduser.model.CustomAttributesInfo;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UserRequestValidator {

    private static final Logger LOG = LoggerFactory.getLogger(UserRequestValidator.class);

    private UserRequestValidator() {
    }

    public static boolean compliant(final UserTO userTO, final Map<String, CustomAttributesInfo> customForm,
            final boolean checkDefaultValues) {

        if (customForm.isEmpty()) {
            return true;
        }

        return validateAttributes(userTO.getPlainAttrMap(), customForm.get(SchemaType.PLAIN.name()), checkDefaultValues)
                && validateAttributes(userTO.getDerAttrMap(), customForm.get(SchemaType.DERIVED.name()),
                        checkDefaultValues)
                && validateAttributes(userTO.getVirAttrMap(), customForm.get(SchemaType.VIRTUAL.name()),
                        checkDefaultValues);
    }

    private static boolean validateAttributes(final Map<String, AttrTO> attrMap,
            final CustomAttributesInfo customAttrInfo, final boolean checkDefaultValues) {

        return IterableUtils.matchesAll(attrMap.entrySet(), new Predicate<Map.Entry<String, AttrTO>>() {

            @Override
            public boolean evaluate(final Map.Entry<String, AttrTO> entry) {
                String schemaKey = entry.getKey();
                AttrTO attrTO = entry.getValue();
                CustomAttribute customAttr = customAttrInfo.getAttributes().get(schemaKey);
                boolean compliant = customAttr != null && (!checkDefaultValues || isValid(attrTO, customAttr));
                if (!compliant) {
                    LOG.trace("Attribute [{}] or its values [{}] are not allowed by form customization rules",
                            attrTO.getSchema(), attrTO.getValues());
                }
                return compliant;
            }
        });

    }

    private static boolean isValid(final AttrTO attrTO, final CustomAttribute customAttribute) {
        return customAttribute.getReadonly()
                ? IterableUtils.matchesAll(attrTO.getValues(), new Predicate<String>() {

                    @Override
                    public boolean evaluate(final String object) {
                        return customAttribute.getDefaultValues().contains(object);
                    }
                })
                : true;
    }

}
