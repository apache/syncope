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
package org.apache.syncope.persistence.api.entity.role;

import java.util.List;
import java.util.Set;
import org.apache.syncope.persistence.api.entity.AccountPolicy;
import org.apache.syncope.persistence.api.entity.AttrTemplate;
import org.apache.syncope.persistence.api.entity.Entitlement;
import org.apache.syncope.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.persistence.api.entity.Schema;
import org.apache.syncope.persistence.api.entity.Subject;
import org.apache.syncope.persistence.api.entity.user.User;

public interface Role extends Subject<RPlainAttr, RDerAttr, RVirAttr> {

    String getName();

    Role getParent();

    boolean addEntitlement(Entitlement entitlement);

    boolean removeEntitlement(Entitlement entitlement);

    /**
     * Get all inherited attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    List<? extends RPlainAttr> findLastInheritedAncestorPlainAttrs();

    /**
     * Get all inherited derived attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    List<? extends RDerAttr> findLastInheritedAncestorDerAttrs();

    /**
     * Get all inherited virtual attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    List<? extends RVirAttr> findLastInheritedAncestorVirAttrs();

    /**
     * Get first valid account policy.
     *
     * @return parent account policy if isInheritAccountPolicy is 'true' and parent is not null, local account policy
     * otherwise.
     */
    AccountPolicy getAccountPolicy();

    <T extends AttrTemplate<K>, K extends Schema> List<T> findInheritedTemplates(Class<T> reference);

    <T extends AttrTemplate<K>, K extends Schema> T getAttrTemplate(
            Class<T> reference, String schemaName);

    <T extends AttrTemplate<K>, K extends Schema> List<K> getAttrTemplateSchemas(Class<T> reference);

    <T extends AttrTemplate<K>, K extends Schema> List<T> getAttrTemplates(Class<T> reference);

    Set<? extends Entitlement> getEntitlements();

    /**
     * Get first valid password policy.
     *
     * @return parent password policy if isInheritPasswordPolicy is 'true' and parent is not null, local password policy
     * otherwise
     */
    PasswordPolicy getPasswordPolicy();

    Role getRoleOwner();

    User getUserOwner();

    boolean isInheritAccountPolicy();

    boolean isInheritAttrs();

    boolean isInheritDerAttrs();

    boolean isInheritOwner();

    boolean isInheritPasswordPolicy();

    boolean isInheritTemplates();

    boolean isInheritVirAttrs();

    void setAccountPolicy(AccountPolicy accountPolicy);

    void setInheritAccountPolicy(boolean condition);

    void setInheritAttrs(boolean inheritAttrs);

    void setInheritDerAttrs(boolean inheritDerAttrs);

    void setInheritOwner(boolean inheritOwner);

    void setInheritPasswordPolicy(boolean condition);

    void setInheritTemplates(boolean condition);

    void setInheritVirAttrs(boolean inheritVirAttrs);

    void setName(String name);

    void setParent(Role parent);

    void setPasswordPolicy(PasswordPolicy passwordPolicy);

    void setRoleOwner(Role roleOwner);

    void setUserOwner(User userOwner);

    @Override
    boolean addPlainAttr(RPlainAttr attr);

    @Override
    boolean addDerAttr(RDerAttr attr);

    @Override
    boolean addVirAttr(RVirAttr attr);

    @Override
    RPlainAttr getPlainAttr(String plainSchemaName);

    @Override
    List<? extends RPlainAttr> getPlainAttrs();

    @Override
    RDerAttr getDerAttr(String derSchemaName);

    @Override
    List<? extends RDerAttr> getDerAttrs();

    @Override
    RVirAttr getVirAttr(String virSchemaName);

    @Override
    List<? extends RVirAttr> getVirAttrs();

    @Override
    boolean removePlainAttr(RPlainAttr attr);

    @Override
    boolean removeDerAttr(RDerAttr derAttr);

    @Override
    boolean removeVirAttr(RVirAttr virAttr);
}
