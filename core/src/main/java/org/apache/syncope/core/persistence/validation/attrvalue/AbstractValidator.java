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
package org.apache.syncope.core.persistence.validation.attrvalue;

import java.io.Serializable;

import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractValidator implements Validator, Serializable {

    private static final long serialVersionUID = -5439345166669502493L;

    /*
     * Logger
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractValidator.class);

    protected final AbstractSchema schema;

    public AbstractValidator(final AbstractSchema schema) {
        this.schema = schema;
    }

    @Override
    public <T extends AbstractAttrValue> void validate(final String value, final T attrValue)
            throws ParsingValidationException, InvalidAttrValueException {

        attrValue.parseValue(schema, value);
        doValidate(attrValue);
    }

    protected abstract <T extends AbstractAttrValue> void doValidate(T attributeValue)
            throws InvalidAttrValueException;
}
