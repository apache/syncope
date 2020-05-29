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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.MappingPurpose;

public class ItemTO implements EntityTO {

    private static final long serialVersionUID = 2983498836767176862L;

    private String key;

    /**
     * Attribute schema to be mapped. Consider other we can associate tha same attribute schema more than once, with
     * different aliases, to different resource attributes.
     */
    private String intAttrName;

    /**
     * External resource's field to be mapped.
     */
    private String extAttrName;

    /**
     * Specify if the mapped target resource's field is the key.
     */
    private boolean connObjectKey;

    /**
     * Specify if the mapped target resource's field is the password.
     */
    private boolean password;

    /**
     * Specify if the mapped target resource's field is nullable.
     */
    private String mandatoryCondition = "false";

    /**
     * Mapping purposes.
     */
    private MappingPurpose purpose;

    /**
     * (Optional) JEXL expression to apply to values before propagation.
     */
    private String propagationJEXLTransformer;

    /**
     * (Optional) JEXL expression to apply to values before pull.
     */
    private String pullJEXLTransformer;

    private final List<String> transformers = new ArrayList<>();

    public boolean isConnObjectKey() {
        return connObjectKey;
    }

    public void setConnObjectKey(final boolean connObjectKey) {
        this.connObjectKey = connObjectKey;
    }

    public String getExtAttrName() {
        return extAttrName;
    }

    public void setExtAttrName(final String extAttrName) {
        this.extAttrName = extAttrName;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getMandatoryCondition() {
        return mandatoryCondition;
    }

    public void setMandatoryCondition(final String mandatoryCondition) {
        this.mandatoryCondition = mandatoryCondition;
    }

    public boolean isPassword() {
        return password;
    }

    public void setPassword(final boolean password) {
        this.password = password;
    }

    public String getIntAttrName() {
        return intAttrName;
    }

    public void setIntAttrName(final String intAttrName) {
        this.intAttrName = intAttrName;
    }

    public MappingPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(final MappingPurpose purpose) {
        this.purpose = purpose;
    }

    public String getPropagationJEXLTransformer() {
        return propagationJEXLTransformer;
    }

    public void setPropagationJEXLTransformer(final String propagationJEXLTransformer) {
        this.propagationJEXLTransformer = propagationJEXLTransformer;
    }

    public String getPullJEXLTransformer() {
        return pullJEXLTransformer;
    }

    public void setPullJEXLTransformer(final String pullJEXLTransformer) {
        this.pullJEXLTransformer = pullJEXLTransformer;
    }

    @JacksonXmlElementWrapper(localName = "transformers")
    @JacksonXmlProperty(localName = "transformer")
    public List<String> getTransformers() {
        return transformers;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ItemTO other = (ItemTO) obj;
        return new EqualsBuilder().
                append(connObjectKey, other.connObjectKey).
                append(password, other.password).
                append(key, other.key).
                append(intAttrName, other.intAttrName).
                append(extAttrName, other.extAttrName).
                append(mandatoryCondition, other.mandatoryCondition).
                append(purpose, other.purpose).
                append(propagationJEXLTransformer, other.propagationJEXLTransformer).
                append(pullJEXLTransformer, other.pullJEXLTransformer).
                append(transformers, other.transformers).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(intAttrName).
                append(extAttrName).
                append(connObjectKey).
                append(password).
                append(mandatoryCondition).
                append(purpose).
                append(propagationJEXLTransformer).
                append(pullJEXLTransformer).
                append(transformers).
                build();
    }
}
