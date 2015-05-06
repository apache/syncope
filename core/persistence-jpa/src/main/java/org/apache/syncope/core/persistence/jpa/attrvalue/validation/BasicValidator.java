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
package org.apache.syncope.core.persistence.jpa.attrvalue.validation;

import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;

public class BasicValidator extends AbstractValidator {

    private static final long serialVersionUID = -2606728447694223607L;

    public BasicValidator(final PlainSchema schema) {
        super(schema);
    }

    @Override
    protected void doValidate(final PlainAttrValue attrValue) {
        if (AttrSchemaType.Enum == schema.getType()) {
            final String[] enumeration = schema.getEnumerationValues().split(SyncopeConstants.ENUM_VALUES_SEPARATOR);
            final String value = attrValue.getStringValue();

            boolean found = false;
            for (int i = 0; i < enumeration.length && !found; i++) {
                if (enumeration[i].trim().equals(value)) {
                    found = true;
                }
            }

            if (!found) {
                throw new InvalidPlainAttrValueException(
                        "'" + value + "' is not one of: " + schema.getEnumerationValues());
            }
        }
    }
}
