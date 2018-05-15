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
package org.apache.syncope.core.persistence.api.entity.resource;

import java.util.List;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncToken;

public interface Provision extends Entity {

    ExternalResource getResource();

    void setResource(ExternalResource resource);

    AnyType getAnyType();

    void setAnyType(AnyType anyType);

    ObjectClass getObjectClass();

    void setObjectClass(ObjectClass objectClass);

    boolean add(AnyTypeClass anyTypeClass);

    List<? extends AnyTypeClass> getAuxClasses();

    SyncToken getSyncToken();

    String getSerializedSyncToken();

    void setSyncToken(SyncToken syncToken);

    boolean isIgnoreCaseMatch();

    void setIgnoreCaseMatch(boolean ignoreCaseMatch);

    PlainSchema getUidOnCreate();

    void setUidOnCreate(PlainSchema uidOnCreate);

    Mapping getMapping();

    void setMapping(Mapping mapping);
}
