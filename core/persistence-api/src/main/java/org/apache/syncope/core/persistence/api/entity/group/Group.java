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
package org.apache.syncope.core.persistence.api.entity.group;

import java.util.List;
import org.apache.syncope.core.persistence.api.entity.AttrTemplate;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.user.User;

public interface Group extends Subject<GPlainAttr, GDerAttr, GVirAttr> {

    String getName();

    <T extends AttrTemplate<K>, K extends Schema> T getAttrTemplate(Class<T> reference, String schemaName);

    <T extends AttrTemplate<K>, K extends Schema> List<K> getAttrTemplateSchemas(Class<T> reference);

    <T extends AttrTemplate<K>, K extends Schema> List<T> getAttrTemplates(Class<T> reference);

    Group getGroupOwner();

    User getUserOwner();

    void setName(String name);

    void setGroupOwner(Group groupOwner);

    void setUserOwner(User userOwner);

    @Override
    boolean addPlainAttr(GPlainAttr attr);

    @Override
    boolean removePlainAttr(GPlainAttr attr);

    @Override
    boolean addDerAttr(GDerAttr attr);

    @Override
    boolean removeDerAttr(GDerAttr derAttr);

    @Override
    boolean addVirAttr(GVirAttr attr);

    @Override
    boolean removeVirAttr(GVirAttr virAttr);

    @Override
    GPlainAttr getPlainAttr(String plainSchemaName);

    @Override
    List<? extends GPlainAttr> getPlainAttrs();

    @Override
    GDerAttr getDerAttr(String derSchemaName);

    @Override
    List<? extends GDerAttr> getDerAttrs();

    @Override
    GVirAttr getVirAttr(String virSchemaName);

    @Override
    List<? extends GVirAttr> getVirAttrs();

}
