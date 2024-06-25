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
package org.apache.syncope.core.persistence.neo4j.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.common.validation.PlainSchemaCheck;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jPlainSchema.NODE)
@PlainSchemaCheck
public class Neo4jPlainSchema extends Neo4jSchema implements PlainSchema {

    private static final long serialVersionUID = -4249639444022112516L;

    public static final String NODE = "PlainSchema";

    public static final String PLAIN_SCHEMA_DROPDOWN_VALUE_PROVIDER_REL = "PLAIN_SCHEMA_DROPDOWN_VALUE_PROVIDER";

    public static final String PLAIN_SCHEMA_ATTR_VALUE_VALIDATOR_REL = "PLAIN_SCHEMA_ATTR_VALUE_VALIDATOR";

    protected static final TypeReference<HashMap<String, String>> ENUMVALUES_TYPEREF =
            new TypeReference<HashMap<String, String>>() {
    };

    @NotNull
    private AttrSchemaType type = AttrSchemaType.String;

    @NotNull
    private String mandatoryCondition = Boolean.FALSE.toString();

    private Boolean multivalue = false;

    private Boolean uniqueConstraint = false;

    private Boolean readonly = false;

    private String conversionPattern;

    private String enumValues;

    @Transient
    private Map<String, String> enumValuesMap = new HashMap<>();

    @Relationship(type = PLAIN_SCHEMA_DROPDOWN_VALUE_PROVIDER_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jImplementation dropdownValueProvider;

    private String secretKey;

    private CipherAlgorithm cipherAlgorithm;

    private String mimeType;

    @Relationship(type = Neo4jAnyTypeClass.ANY_TYPE_CLASS_PLAIN_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jAnyTypeClass anyTypeClass;

    @Relationship(type = PLAIN_SCHEMA_ATTR_VALUE_VALIDATOR_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jImplementation validator;

    @Override
    public AttrSchemaType getType() {
        return type;
    }

    @Override
    public void setType(final AttrSchemaType type) {
        this.type = type;
    }

    @Override
    public String getMandatoryCondition() {
        return mandatoryCondition;
    }

    @Override
    public void setMandatoryCondition(final String condition) {
        this.mandatoryCondition = condition;
    }

    @Override
    public boolean isMultivalue() {
        return multivalue;
    }

    @Override
    public void setMultivalue(final boolean multivalue) {
        this.multivalue = multivalue;
    }

    @Override
    public boolean isUniqueConstraint() {
        return uniqueConstraint;
    }

    @Override
    public void setUniqueConstraint(final boolean uniquevalue) {
        this.uniqueConstraint = uniquevalue;
    }

    @Override
    public boolean isReadonly() {
        return readonly;
    }

    @Override
    public void setReadonly(final boolean readonly) {
        this.readonly = readonly;
    }

    @Override
    public String getConversionPattern() {
        return conversionPattern;
    }

    @Override
    public void setConversionPattern(final String conversionPattern) {
        this.conversionPattern = conversionPattern;
    }

    @Override
    public Implementation getDropdownValueProvider() {
        return dropdownValueProvider;
    }

    @Override
    public void setDropdownValueProvider(final Implementation dropdownValueProvider) {
        checkType(dropdownValueProvider, Neo4jImplementation.class);
        checkImplementationType(dropdownValueProvider, IdRepoImplementationType.DROPDOWN_VALUE_PROVIDER);
        this.dropdownValueProvider = (Neo4jImplementation) dropdownValueProvider;
    }

    @Override
    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public void setSecretKey(final String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public CipherAlgorithm getCipherAlgorithm() {
        return cipherAlgorithm;
    }

    @Override
    public void setCipherAlgorithm(final CipherAlgorithm cipherAlgorithm) {
        this.cipherAlgorithm = cipherAlgorithm;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public AnyTypeClass getAnyTypeClass() {
        return anyTypeClass;
    }

    @Override
    public void setAnyTypeClass(final AnyTypeClass anyTypeClass) {
        checkType(anyTypeClass, Neo4jAnyTypeClass.class);
        this.anyTypeClass = (Neo4jAnyTypeClass) anyTypeClass;
    }

    @Override
    public Implementation getValidator() {
        return validator;
    }

    @Override
    public void setValidator(final Implementation validator) {
        checkType(validator, Neo4jImplementation.class);
        checkImplementationType(validator, IdRepoImplementationType.ATTR_VALUE_VALIDATOR);
        this.validator = (Neo4jImplementation) validator;
    }

    @Override
    public Map<String, String> getEnumValues() {
        return enumValuesMap;
    }

    @Override
    protected void json2map(final boolean clearFirst) {
        super.json2map(clearFirst);

        if (clearFirst) {
            getEnumValues().clear();
        }
        if (enumValues != null) {
            getEnumValues().putAll(POJOHelper.deserialize(enumValues, ENUMVALUES_TYPEREF));
        }
    }

    @PostLoad
    @Override
    public void postLoad() {
        json2map(false);
    }

    @Override
    public void postSave() {
        json2map(true);
    }

    @Override
    public void map2json() {
        super.map2json();
        enumValues = POJOHelper.serialize(getEnumValues());
    }
}
