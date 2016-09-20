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
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.provisioning.java.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationDataBinderImpl extends AbstractAnyDataBinder implements ConfigurationDataBinder {

    @Autowired
    private ConfDAO confDAO;

    @Override
    public List<AttrTO> getConfTO() {
        List<AttrTO> attrTOs = new ArrayList<>();
        for (CPlainAttr attr : confDAO.get().getPlainAttrs()) {
            attrTOs.add(getAttrTO(attr));
        }
        return attrTOs;
    }

    @Override
    public AttrTO getAttrTO(final CPlainAttr attr) {
        return new AttrTO.Builder().
                schemaInfo(schemaDataBinder.getPlainSchemaTO(attr.getSchema())).
                schema(attr.getSchema().getKey()).
                values(attr.getValuesAsStrings()).
                build();
    }

    private void fillAttr(final List<String> values,
            final PlainSchema schema, final CPlainAttr attr, final SyncopeClientException invalidValues) {

        // if schema is multivalue, all values are considered for addition;
        // otherwise only the fist one - if provided - is considered
        List<String> valuesProvided = schema.isMultivalue()
                ? values
                : (values.isEmpty()
                ? Collections.<String>emptyList()
                : Collections.singletonList(values.iterator().next()));

        if (valuesProvided.isEmpty()) {
            JexlContext jexlContext = new MapContext();
            JexlUtils.addPlainAttrsToContext(confDAO.get().getPlainAttrs(), jexlContext);

            if (!schema.isReadonly()
                    && Boolean.parseBoolean(JexlUtils.evaluate(schema.getMandatoryCondition(), jexlContext))) {

                LOG.error("Mandatory schema " + schema.getKey() + " not provided with values");

                SyncopeClientException reqValMissing = SyncopeClientException.build(
                        ClientExceptionType.RequiredValuesMissing);
                reqValMissing.getElements().add(schema.getKey());
                throw reqValMissing;
            }
        }

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
    public CPlainAttr getAttr(final AttrTO attrTO) {
        PlainSchema schema = getPlainSchema(attrTO.getSchema());
        if (schema == null) {
            throw new NotFoundException("Conf schema " + attrTO.getSchema());
        } else {
            SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

            CPlainAttr attr = entityFactory.newEntity(CPlainAttr.class);
            attr.setSchema(schema);
            fillAttr(attrTO.getValues(), schema, attr, invalidValues);

            if (!invalidValues.isEmpty()) {
                throw invalidValues;
            }
            return attr;
        }
    }

}
