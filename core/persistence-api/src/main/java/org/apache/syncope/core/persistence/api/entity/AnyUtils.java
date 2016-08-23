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

import java.util.Set;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AllowedSchemas;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;

public interface AnyUtils {

    AnyTypeKind getAnyTypeKind();

    <T extends Any<?>> Class<T> anyClass();

    boolean isFieldName(String name);

    <T extends PlainAttr<?>> Class<T> plainAttrClass();

    <T extends PlainAttr<?>> T newPlainAttr();

    <T extends PlainAttrValue> Class<T> plainAttrValueClass();

    <T extends PlainAttrValue> T newPlainAttrValue();

    <T extends PlainAttrValue> Class<T> plainAttrUniqueValueClass();

    <T extends PlainAttrValue> T newPlainAttrUniqueValue();

    <T extends PlainAttrValue> T clonePlainAttrValue(T src);
    
    <T extends AnyTO> T newAnyTO();

    Set<ExternalResource> getAllResources(Any<?> any);

    <S extends Schema> AllowedSchemas<S> getAllowedSchemas(Any<?> any, Class<S> reference);
}
