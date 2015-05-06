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

import java.io.Serializable;
import org.apache.syncope.core.persistence.api.attrvalue.validation.Validator;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractValidator implements Validator, Serializable {

    private static final long serialVersionUID = -5439345166669502493L;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractValidator.class);

    protected final PlainSchema schema;

    public AbstractValidator(final PlainSchema schema) {
        this.schema = schema;
    }

    @Override
    public void validate(final String value, final PlainAttrValue attrValue) {
        attrValue.parseValue(schema, value);
        doValidate(attrValue);
    }

    protected abstract void doValidate(PlainAttrValue attrValue);
}
