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

import java.util.List;
import org.apache.syncope.common.lib.to.AbstractAttributableTO;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;

public interface AttributableUtil {

    AttributableType getType();

    <T extends Attributable<?, ?, ?>> Class<T> attributableClass();

    <T extends PlainSchema> Class<T> plainSchemaClass();

    <T extends PlainSchema> T newPlainSchema();

    <T extends PlainAttr> Class<T> plainAttrClass();

    <T extends PlainAttr> T newPlainAttr();

    <T extends PlainAttrValue> Class<T> plainAttrValueClass();

    <T extends PlainAttrValue> T newPlainAttrValue();

    <T extends AttrTemplate<PlainSchema>> Class<T> plainAttrTemplateClass();

    <T extends PlainAttrValue> Class<T> plainAttrUniqueValueClass();

    <T extends PlainAttrValue> T newPlainAttrUniqueValue();

    <T extends DerSchema> Class<T> derSchemaClass();

    <T extends DerSchema> T newDerSchema();

    <T extends DerAttr> Class<T> derAttrClass();

    <T extends DerAttr> T newDerAttr();

    <T extends AttrTemplate<DerSchema>> Class<T> derAttrTemplateClass();

    <T extends VirSchema> Class<T> virSchemaClass();

    <T extends VirSchema> T newVirSchema();

    <T extends VirAttr> Class<T> virAttrClass();

    <T extends VirAttr> T newVirAttr();

    <T extends AttrTemplate<VirSchema>> Class<T> virAttrTemplateClass();

    <T extends MappingItem> T getAccountIdItem(ExternalResource resource);

    String getAccountLink(ExternalResource resource);

    <T extends MappingItem> List<T> getMappingItems(ExternalResource resource, MappingPurpose purpose);

    IntMappingType plainIntMappingType();

    IntMappingType derIntMappingType();

    IntMappingType virIntMappingType();

    <T extends MappingItem> Class<T> mappingItemClass();

    <T extends AbstractAttributableTO> T newAttributableTO();

    <T extends AbstractSubjectTO> T newSubjectTO();
}
