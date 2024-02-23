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
package org.apache.syncope.core.persistence.common.attrvalue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValueValidator;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultPlainAttrValidationManager implements PlainAttrValidationManager {

    protected static final Logger LOG = LoggerFactory.getLogger(DefaultPlainAttrValidationManager.class);

    protected static final PlainAttrValueValidator BASIC_VALIDATOR = new BasicValidator();

    protected final Map<String, PlainAttrValueValidator> perContextValidators = new ConcurrentHashMap<>();

    @Override
    public void validate(final PlainSchema schema, final String value, final PlainAttrValue attrValue) {
        PlainAttrValueValidator validator = null;

        if (schema.getValidator() != null) {
            try {
                validator = ImplementationManager.build(
                        schema.getValidator(),
                        () -> perContextValidators.get(schema.getValidator().getKey()),
                        instance -> perContextValidators.put(schema.getValidator().getKey(), instance));
            } catch (Exception e) {
                LOG.error("While building {}", schema.getValidator(), e);
            }
        }

        if (validator == null) {
            validator = BASIC_VALIDATOR;
        }

        validator.validate(schema, value, attrValue);
    }
}
