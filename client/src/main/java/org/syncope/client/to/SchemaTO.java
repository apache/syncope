/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.client.to;

import java.util.HashSet;
import java.util.Set;
import org.syncope.client.AbstractBaseBean;
import org.syncope.types.SchemaValueType;

public class SchemaTO extends AbstractBaseBean {

    private String name;
    private SchemaValueType type;
    private boolean virtual;
    private boolean mandatory;
    private boolean multivalue;
    private boolean uniquevalue;
    private boolean readonly;
    private String conversionPattern;
    private String validatorClass;
    private Set<String> derivedSchemas;
    private int attributes;

    public SchemaTO() {
        derivedSchemas = new HashSet<String>();
    }

    public String getConversionPattern() {
        return conversionPattern;
    }

    public void setConversionPattern(String conversionPattern) {
        this.conversionPattern = conversionPattern;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isMultivalue() {
        return multivalue;
    }

    public void setMultivalue(boolean multivalue) {
        this.multivalue = multivalue;
    }

    public boolean isUniquevalue() {
        return uniquevalue;
    }

    public void setUniquevalue(boolean uniquevalue) {
        this.uniquevalue = uniquevalue;
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

    public SchemaValueType getType() {
        return type;
    }

    public void setType(SchemaValueType type) {
        this.type = type;
    }

    public String getValidatorClass() {
        return validatorClass;
    }

    public void setValidatorClass(String validatorClass) {
        this.validatorClass = validatorClass;
    }

    public boolean addDerivedSchema(String derivedSchema) {
        return derivedSchemas.add(derivedSchema);
    }

    public boolean removeDerivedSchema(String derivedSchema) {
        return derivedSchemas.remove(derivedSchema);
    }

    public Set<String> getDerivedSchemas() {
        return derivedSchemas;
    }

    public void setDerivedSchemas(Set<String> derivedSchemas) {
        this.derivedSchemas = derivedSchemas;
    }

    public int getAttributes() {
        return attributes;
    }

    public void setAttributes(int attributes) {
        this.attributes = attributes;
    }
}
