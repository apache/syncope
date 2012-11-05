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
package org.apache.syncope.to;

import org.apache.commons.lang.StringUtils;
import org.apache.syncope.AbstractBaseBean;
import org.apache.syncope.types.SchemaType;

public class SchemaTO extends AbstractBaseBean {

    private static final long serialVersionUID = -8133983392476990308L;

    private String name;

    private SchemaType type;

    private String mandatoryCondition;

    private String enumerationValues;

    private String enumerationKeys;

    private boolean multivalue;

    private boolean uniqueConstraint;

    private boolean readonly;

    private String conversionPattern;

    private String validatorClass;

    public SchemaTO() {
        type = SchemaType.String;
    }

    public String getConversionPattern() {
        return conversionPattern;
    }

    public void setConversionPattern(String conversionPattern) {
        this.conversionPattern = conversionPattern;
    }

    public String getMandatoryCondition() {
        return StringUtils.isNotBlank(mandatoryCondition)
                ? mandatoryCondition
                : "false";
    }

    public void setMandatoryCondition(String mandatoryCondition) {
        this.mandatoryCondition = mandatoryCondition;
    }

    public String getEnumerationValues() {
        return enumerationValues;
    }

    public void setEnumerationValues(String enumerationValues) {
        this.enumerationValues = enumerationValues;
    }

    public String getEnumerationKeys() {
        return enumerationKeys;
    }

    public void setEnumerationKeys(String enumerationKeys) {
        this.enumerationKeys = enumerationKeys;
    }

    public boolean isMultivalue() {
        return multivalue;
    }

    public void setMultivalue(boolean multivalue) {
        this.multivalue = multivalue;
    }

    public boolean isUniqueConstraint() {
        return uniqueConstraint;
    }

    public void setUniqueConstraint(boolean uniqueConstraint) {
        this.uniqueConstraint = uniqueConstraint;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SchemaType getType() {
        return type;
    }

    public void setType(SchemaType type) {
        this.type = type;
    }

    public String getValidatorClass() {
        return validatorClass;
    }

    public void setValidatorClass(String validatorClass) {
        this.validatorClass = validatorClass;
    }
}
