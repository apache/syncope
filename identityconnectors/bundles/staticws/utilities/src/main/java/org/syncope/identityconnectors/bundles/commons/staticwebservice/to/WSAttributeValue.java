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
package org.syncope.identityconnectors.bundles.commons.staticwebservice.to;

import java.io.File;
import java.net.URI;
import java.util.Date;

public class WSAttributeValue extends WSAttribute {

    private Object value = null;

    public WSAttributeValue() {
        super();
    }

    public WSAttributeValue(WSAttribute wsAttribute) {
        super();

        if (wsAttribute != null) {
            setType(wsAttribute.getType());
            setName(wsAttribute.getName());
            setKey(wsAttribute.isKey());
            setNullable(wsAttribute.isNullable());
            setPassword(wsAttribute.isPassword());
        }
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getStringValue() {
        if (getType() == null || !"String".equals(getType())) {
            throw new IllegalArgumentException("Invalid type declaration");
        }

        return (String) value;
    }

    public Boolean getBooleanValue() {
        if (getType() == null || !"Boolean".equals(getType())) {
            throw new IllegalArgumentException("Invalid type declaration");
        }

        return (Boolean) value;
    }

    public Long getLongValue() {
        if (getType() == null || !"Long".equals(getType())) {
            throw new IllegalArgumentException("Invalid type declaration");
        }

        return (Long) value;
    }

    public Float getFloadValue() {
        if (getType() == null || !"Float".equals(getType())) {
            throw new IllegalArgumentException("Invalid type declaration");
        }

        return (Float) value;
    }

    public Double getDoubleValue() {
        if (getType() == null || !"Double".equals(getType())) {
            throw new IllegalArgumentException("Invalid type declaration");
        }

        return (Double) value;
    }

    public Integer getIntegerValue() {
        if (getType() == null || !"Integer".equals(getType())) {
            throw new IllegalArgumentException("Invalid type declaration");
        }

        return (Integer) value;
    }

    public Date getDateValue() {
        if (getType() == null || !"Date".equals(getType())) {
            throw new IllegalArgumentException("Invalid type declaration");
        }

        return (Date) value;
    }

    public Character getCharacterValue() {
        if (getType() == null || !"Character".equals(getType())) {
            throw new IllegalArgumentException("Invalid type declaration");
        }

        return (Character) value;
    }

    public URI getURIValue() {
        if (getType() == null || !"URI".equals(getType())) {
            throw new IllegalArgumentException("Invalid type declaration");
        }

        return (URI) value;
    }

    public File getFileValue() {
        if (getType() == null || !"File".equals(getType())) {
            throw new IllegalArgumentException("Invalid type declaration");
        }

        return (File) value;
    }
}
