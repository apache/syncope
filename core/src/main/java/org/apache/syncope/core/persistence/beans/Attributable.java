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
package org.apache.syncope.core.persistence.beans;

import java.util.List;
import java.util.Set;

/**
 * Entity interface. It has been introduced to specify a dynamic proxy for AbstractAttributable instances in order to 
 * manage virtual attributes during propagation.
 */
public interface Attributable {

    <T extends AbstractAttr> T getAttribute(final String schemaName);

    <T extends AbstractDerAttr> T getDerivedAttribute(final String derivedSchemaName);

    <T extends AbstractVirAttr> T getVirtualAttribute(final String virtualSchemaName);

    Long getId();

    <T extends AbstractAttr> boolean addAttribute(T attribute);

    <T extends AbstractAttr> boolean removeAttribute(T attribute);

    List<? extends AbstractAttr> getAttributes();

    void setAttributes(List<? extends AbstractAttr> attributes);

    <T extends AbstractDerAttr> boolean addDerivedAttribute(T derivedAttribute);

    <T extends AbstractDerAttr> boolean removeDerivedAttribute(T derivedAttribute);

    List<? extends AbstractDerAttr> getDerivedAttributes();

    void setDerivedAttributes(List<? extends AbstractDerAttr> derivedAttributes);

    <T extends AbstractVirAttr> boolean addVirtualAttribute(T virtualAttributes);

    <T extends AbstractVirAttr> boolean removeVirtualAttribute(T virtualAttribute);

    List<? extends AbstractVirAttr> getVirtualAttributes();

    void setVirtualAttributes(List<? extends AbstractVirAttr> virtualAttributes);

    boolean addResource(final ExternalResource resource);

    boolean removeResource(final ExternalResource resource);

    Set<ExternalResource> getResources();

    Set<String> getResourceNames();

    void setResources(final Set<ExternalResource> resources);
}
