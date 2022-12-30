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

import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.util.Optional;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.core.persistence.api.entity.AnyTemplate;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.ApplicationContextProvider;

@MappedSuperclass
public abstract class AbstractAnyTemplate extends AbstractGeneratedKeyEntity implements AnyTemplate {

    private static final long serialVersionUID = -5280310945358790780L;

    @ManyToOne
    private JPAAnyType anyType;

    @Lob
    private String template;

    @Override
    public AnyType getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final AnyType anyType) {
        checkType(anyType, JPAAnyType.class);
        this.anyType = (JPAAnyType) anyType;
    }

    @Override
    public AnyTO get() {
        return template == null
                ? anyType == null
                        ? null
                        : ApplicationContextProvider.getApplicationContext().getBean(AnyUtilsFactory.class).
                                getInstance(anyType.getKind()).newAnyTO()
                : anyType == null
                        ? null
                        : POJOHelper.deserialize(template, anyType.getKind().getTOClass());
    }

    @Override
    public void set(final AnyTO template) {
        this.template = Optional.ofNullable(template).map(POJOHelper::serialize).orElse(null);
    }
}
