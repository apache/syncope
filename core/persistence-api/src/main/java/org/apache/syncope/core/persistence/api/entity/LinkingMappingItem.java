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
package org.apache.syncope.core.persistence.api.entity;

import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;

/**
 * {@link MappingItem} implementation for usage within {@link VirSchema#asLinkingMappingItem()} implementations.
 */
public class LinkingMappingItem implements MappingItem {

    private static final long serialVersionUID = 327455459536715529L;

    private final VirSchema virSchema;

    public LinkingMappingItem(final VirSchema virSchema) {
        this.virSchema = virSchema;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public Mapping getMapping() {
        return virSchema.getProvision().getMapping();
    }

    @Override
    public void setMapping(final Mapping mapping) {
        // RO instance, nothing to do
    }

    @Override
    public String getExtAttrName() {
        return virSchema.getExtAttrName();
    }

    @Override
    public void setExtAttrName(final String extAttrName) {
        // RO instance, nothing to do
    }

    @Override
    public String getIntAttrName() {
        return virSchema.getKey();
    }

    @Override
    public void setIntAttrName(final String intAttrName) {
        // RO instance, nothing to do
    }

    @Override
    public String getMandatoryCondition() {
        return virSchema.getMandatoryCondition();
    }

    @Override
    public void setMandatoryCondition(final String condition) {
        // RO instance, nothing to do
    }

    @Override
    public MappingPurpose getPurpose() {
        return virSchema.isReadonly() ? MappingPurpose.PULL : MappingPurpose.BOTH;
    }

    @Override
    public void setPurpose(final MappingPurpose purpose) {
        // RO instance, nothing to do
    }

    @Override
    public boolean isConnObjectKey() {
        return false;
    }

    @Override
    public void setConnObjectKey(final boolean connObjectKey) {
        // RO instance, nothing to do
    }

    @Override
    public boolean isPassword() {
        return false;
    }

    @Override
    public void setPassword(final boolean password) {
        // RO instance, nothing to do
    }

    @Override
    public String getPropagationJEXLTransformer() {
        return null;
    }

    @Override
    public void setPropagationJEXLTransformer(final String propagationJEXLTransformer) {
        // RO instance, nothing to do
    }

    @Override
    public String getPullJEXLTransformer() {
        return null;
    }

    @Override
    public void setPullJEXLTransformer(final String pullJEXLTransformer) {
        // RO instance, nothing to do
    }

    @Override
    public List<String> getTransformerClassNames() {
        return Collections.emptyList();
    }
}
