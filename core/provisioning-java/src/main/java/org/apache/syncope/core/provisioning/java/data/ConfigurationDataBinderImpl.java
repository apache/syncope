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
package org.apache.syncope.core.provisioning.java.data;

import java.util.ArrayList;
import org.apache.syncope.core.provisioning.api.data.ConfigurationDataBinder;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.conf.Conf;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationDataBinderImpl extends AbstractAnyDataBinder implements ConfigurationDataBinder {

    @Override
    public List<AttrTO> getConfTO(final Conf conf) {
        final List<AttrTO> attrTOs = new ArrayList<>();
        for (final CPlainAttr plainAttr : conf.getPlainAttrs()) {
            final AttrTO attrTO = new AttrTO();
            attrTO.setSchema(plainAttr.getSchema().getKey());
            attrTO.getValues().addAll(plainAttr.getValuesAsStrings());
            attrTO.setReadonly(plainAttr.getSchema().isReadonly());
            attrTOs.add(attrTO);
        }
        return attrTOs;
    }

    @Override
    public AttrTO getAttrTO(final CPlainAttr attr) {
        AttrTO attributeTO = new AttrTO();
        attributeTO.setSchema(attr.getSchema().getKey());
        attributeTO.getValues().addAll(attr.getValuesAsStrings());
        attributeTO.setReadonly(attr.getSchema().isReadonly());

        return attributeTO;
    }

    private void fillAttribute(final List<String> values,
            final PlainSchema schema, final CPlainAttr attr, final SyncopeClientException invalidValues) {

        // if schema is multivalue, all values are considered for addition;
        // otherwise only the fist one - if provided - is considered
        List<String> valuesProvided = schema.isMultivalue()
                ? values
                : (values.isEmpty()
                        ? Collections.<String>emptyList()
                        : Collections.singletonList(values.iterator().next()));

        for (String value : valuesProvided) {
            if (value == null || value.isEmpty()) {
                LOG.debug("Null value for {}, ignoring", schema.getKey());
            } else {
                try {
                    PlainAttrValue attrValue;
                    if (schema.isUniqueConstraint()) {
                        attrValue = entityFactory.newEntity(CPlainAttrUniqueValue.class);
                        ((PlainAttrUniqueValue) attrValue).setSchema(schema);
                    } else {
                        attrValue = entityFactory.newEntity(CPlainAttrValue.class);
                    }

                    attr.add(value, attrValue);
                } catch (InvalidPlainAttrValueException e) {
                    LOG.warn("Invalid value for attribute " + schema.getKey() + ": " + value, e);

                    invalidValues.getElements().add(schema.getKey() + ": " + value + " - " + e.getMessage());
                }
            }
        }
    }

    @Override
    public CPlainAttr getAttribute(final AttrTO attributeTO) {
        PlainSchema schema = getPlainSchema(attributeTO.getSchema());
        if (schema == null) {
            throw new NotFoundException("Conf schema " + attributeTO.getSchema());
        } else {
            SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

            CPlainAttr attr = entityFactory.newEntity(CPlainAttr.class);
            attr.setSchema(schema);
            fillAttribute(attributeTO.getValues(), schema, attr, invalidValues);

            if (!invalidValues.isEmpty()) {
                throw invalidValues;
            }
            return attr;
        }
    }

}
