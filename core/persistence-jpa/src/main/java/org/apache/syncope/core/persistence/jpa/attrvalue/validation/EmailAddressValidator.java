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

import java.util.regex.Matcher;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;

public class EmailAddressValidator extends AbstractValidator {

    private static final long serialVersionUID = 792457177290331518L;

    @Override
    protected void doValidate(final PlainAttrValue attrValue) {
        Matcher matcher = Entity.EMAIL_PATTERN.matcher(attrValue.<CharSequence>getValue());
        if (!matcher.matches()) {
            throw new InvalidPlainAttrValueException("\"" + attrValue.getValue() + "\" is not a valid email address");
        }
    }
}
