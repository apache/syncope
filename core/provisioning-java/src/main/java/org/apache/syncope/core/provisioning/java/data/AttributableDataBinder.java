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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AttributableTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.DropdownValueProvider;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AttributableDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(AttributableDataBinder.class);

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final PlainAttrValidationManager validator;

    protected final DerAttrHandler derAttrHandler;

    protected final MappingManager mappingManager;

    protected final IntAttrNameParser intAttrNameParser;

    protected final JexlTools jexlTools;

    private final Map<String, DropdownValueProvider> dropdownValueProviders = new ConcurrentHashMap<>();

    protected AttributableDataBinder(
            final PlainSchemaDAO plainSchemaDAO,
            final PlainAttrValidationManager validator,
            final DerAttrHandler derAttrHandler,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final JexlTools jexlTools) {

        this.plainSchemaDAO = plainSchemaDAO;
        this.validator = validator;
        this.derAttrHandler = derAttrHandler;
        this.mappingManager = mappingManager;
        this.intAttrNameParser = intAttrNameParser;
        this.jexlTools = jexlTools;
    }

    protected Optional<PlainSchema> getPlainSchema(final String schemaName) {
        PlainSchema schema = null;
        if (StringUtils.isNotBlank(schemaName)) {
            schema = plainSchemaDAO.findById(schemaName).orElse(null);

            // safely ignore invalid schemas from Attr
            if (schema == null) {
                LOG.debug("Ignoring invalid schema {}", schemaName);
            } else if (schema.isReadonly()) {
                schema = null;
                LOG.debug("Ignoring readonly schema {}", schemaName);
            }
        }

        return Optional.ofNullable(schema);
    }

    protected void checkMandatory(
            final PlainSchema schema,
            final PlainAttr attr,
            final Attributable attributable,
            final SyncopeClientException reqValMissing) {

        if (attr == null
                && !schema.isReadonly()
                && jexlTools.evaluateMandatoryCondition(schema.getMandatoryCondition(), attributable, derAttrHandler)) {

            LOG.error("Mandatory schema {} not provided with values", schema.getKey());

            reqValMissing.getElements().add(schema.getKey());
        }
    }

    protected void fillAttr(
            final AttributableTO attributableTO,
            final List<String> values,
            final PlainSchema schema,
            final PlainAttr attr,
            final SyncopeClientException invalidValues) {

        // if schema is multivalue, all values are considered for addition;
        // otherwise only the fist one - if provided - is considered
        List<String> valuesProvided = schema.isMultivalue()
                ? values
                : (values.isEmpty() || values.getFirst() == null
                ? List.of()
                : List.of(values.getFirst()));

        valuesProvided.forEach(value -> {
            if (StringUtils.isBlank(value)) {
                LOG.debug("Null value for {}, ignoring", schema.getKey());
            } else {
                try {
                    switch (schema.getType()) {
                        case Enum -> {
                            if (!schema.getEnumValues().containsKey(value)) {
                                throw new InvalidPlainAttrValueException(
                                        '\'' + value + "' is not one of: " + schema.getEnumValues().keySet());
                            }
                        }

                        case Dropdown -> {
                            List<String> dropdownValues = List.of();
                            try {
                                DropdownValueProvider provider = ImplementationManager.build(
                                        schema.getDropdownValueProvider(),
                                        () -> dropdownValueProviders.get(
                                                schema.getDropdownValueProvider().getKey()),
                                        instance -> dropdownValueProviders.put(
                                                schema.getDropdownValueProvider().getKey(), instance));
                                dropdownValues = provider.getChoices(attributableTO);
                            } catch (Exception e) {
                                LOG.error("While getting dropdown values for {}", schema.getKey(), e);
                            }

                            if (!dropdownValues.contains(value)) {
                                throw new InvalidPlainAttrValueException(
                                        '\'' + value + "' is not one of: " + dropdownValues);
                            }
                        }

                        default -> {
                        }
                    }

                    attr.add(validator, value);
                } catch (InvalidPlainAttrValueException e) {
                    LOG.warn("Invalid value for attribute {}: {}",
                            schema.getKey(), StringUtils.abbreviate(value, 20), e);

                    invalidValues.getElements().add(schema.getKey() + ": " + value + " - " + e.getMessage());
                }
            }
        });
    }

    protected PropagationByResource<String> propByRes(
            final Map<String, ConnObject> before,
            final Map<String, ConnObject> after) {

        PropagationByResource<String> propByRes = new PropagationByResource<>();

        after.forEach((resource, connObject) -> {
            if (before.containsKey(resource)) {
                ConnObject beforeObject = before.get(resource);
                if (!beforeObject.equals(connObject)) {
                    propByRes.add(ResourceOperation.UPDATE, resource);

                    beforeObject.getAttr(Uid.NAME).map(attr -> attr.getValues().getFirst()).
                            ifPresent(value -> propByRes.addOldConnObjectKey(resource, value));
                }
            } else {
                propByRes.add(ResourceOperation.CREATE, resource);
            }
        });
        propByRes.addAll(
                ResourceOperation.DELETE,
                before.keySet().stream().filter(resource -> !after.containsKey(resource)).collect(Collectors.toSet()));

        return propByRes;
    }
}
