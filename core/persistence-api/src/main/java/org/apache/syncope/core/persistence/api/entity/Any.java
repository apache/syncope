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

import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import java.util.List;

public interface Any<P extends PlainAttr<?>> extends AnnotatedEntity {

    AnyType getType();

    void setType(AnyType type);

    Realm getRealm();

    void setRealm(Realm realm);

    String getStatus();

    void setStatus(String status);

    String getWorkflowId();

    void setWorkflowId(String workflowId);

    boolean add(P attr);

    boolean remove(P attr);

    P getPlainAttr(String plainSchemaName);

    List<? extends P> getPlainAttrs();

    boolean add(ExternalResource resource);

    List<String> getResourceKeys();

    List<? extends ExternalResource> getResources();

    boolean add(AnyTypeClass auxClass);

    List<? extends AnyTypeClass> getAuxClasses();
}
