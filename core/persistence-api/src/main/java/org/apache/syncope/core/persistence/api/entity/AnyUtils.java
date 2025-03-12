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

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;

public interface AnyUtils {

    AnyTypeKind anyTypeKind();

    <T extends Any> Class<T> anyClass();

    Optional<Field> getField(String name);

    <T extends AnyTO> T newAnyTO();

    <C extends AnyCR> C newAnyCR();

    <U extends AnyUR> U newAnyUR(String key);

    <A extends Any> AnyDAO<A> dao();

    Set<ExternalResource> getAllResources(Any any);

    void addAttr(PlainAttrValidationManager validator, String key, PlainSchema schema, String value);

    void removeAttr(String key, PlainSchema schema);
}
