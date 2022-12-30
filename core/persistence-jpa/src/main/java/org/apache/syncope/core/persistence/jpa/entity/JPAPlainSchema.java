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
package org.apache.syncope.core.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.validation.entity.PlainSchemaCheck;

@Entity
@Table(name = JPAPlainSchema.TABLE)
@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
@PlainSchemaCheck
public class JPAPlainSchema extends AbstractSchema implements PlainSchema {

    private static final long serialVersionUID = -8621028596062054739L;

    public static final String TABLE = "PlainSchema";

    @OneToOne(fetch = FetchType.EAGER)
    private JPAAnyTypeClass anyTypeClass;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AttrSchemaType type = AttrSchemaType.String;

    @NotNull
    private String mandatoryCondition = Boolean.FALSE.toString();

    private Boolean multivalue = false;

    private Boolean uniqueConstraint = false;

    private Boolean readonly = false;

    @Column(nullable = true)
    private String conversionPattern;

    @Column(nullable = true)
    @Lob
    private String enumerationValues;

    @Column(nullable = true)
    @Lob
    private String enumerationKeys;

    @Column(nullable = true)
    private String secretKey;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private CipherAlgorithm cipherAlgorithm;

    @Column(nullable = true)
    private String mimeType;

    @OneToOne
    private JPAImplementation validator;

    @Override
    public AnyTypeClass getAnyTypeClass() {
        return anyTypeClass;
    }

    @Override
    public void setAnyTypeClass(final AnyTypeClass anyTypeClass) {
        checkType(anyTypeClass, JPAAnyTypeClass.class);
        this.anyTypeClass = (JPAAnyTypeClass) anyTypeClass;
    }

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
    public Implementation getValidator() {
        return validator;
    }

    @Override
    public void setValidator(final Implementation validator) {
        checkType(validator, JPAImplementation.class);
        checkImplementationType(validator, IdRepoImplementationType.VALIDATOR);
        this.validator = (JPAImplementation) validator;
    }

    @Override
    public String getEnumerationValues() {
        return enumerationValues;
    }

    @Override
    public void setEnumerationValues(final String enumerationValues) {
        this.enumerationValues = enumerationValues;
    }

    @Override
    public String getEnumerationKeys() {
        return enumerationKeys;
    }

    @Override
    public void setEnumerationKeys(final String enumerationKeys) {
        this.enumerationKeys = enumerationKeys;
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

}
