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
package org.apache.syncope.core.provisioning.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.identityconnectors.framework.common.objects.Attribute;

public interface MappingManager {

    /**
     * Get connObjectKey internal value.
     *
     * @param any any object
     * @param resource resource information
     * @param provision provision information
     * @return connObjectKey internal value
     */
    Optional<String> getConnObjectKeyValue(Any any, ExternalResource resource, Provision provision);

    /**
     * Get connObjectKey internal value.
     *
     * @param realm realm
     * @param orgUnit orgUnit information
     * @return connObjectKey internal value
     */
    Optional<String> getConnObjectKeyValue(Realm realm, OrgUnit orgUnit);

    /**
     * Get attribute values for the given {@link Item} and any object.
     *
     * @param resource resource information
     * @param provision provision information
     * @param mapItem mapping item
     * @param intAttrName int attr name
     * @param schemaType schema type
     * @param any any object
     * @param usernameAccountGetter function to get actual account instance for username
     * @param plainAttrGetter function to get PlainAttr instances
     * @return attribute values and their type
     */
    Pair<AttrSchemaType, List<PlainAttrValue>> getIntValues(
            ExternalResource resource,
            Provision provision,
            Item mapItem,
            IntAttrName intAttrName,
            AttrSchemaType schemaType,
            Any any,
            AccountGetter usernameAccountGetter,
            PlainAttrGetter plainAttrGetter);

    /**
     * Prepare attribute for sending to a connector instance.
     *
     * @param resource resource information
     * @param provision provision information
     * @param item mapping item
     * @param any given any object
     * @param password clear-text password
     * @param usernameAccountGetter function to get actual account instance for username
     * @param passwordAccountGetter function to get actual account instance for password
     * @param plainAttrGetter function to get PlainAttr instances
     * @return connObjectLink (if it is the case) + prepared attribute
     */
    Pair<String, Attribute> prepareAttr(
            ExternalResource resource,
            Provision provision,
            Item item,
            Any any,
            String password,
            AccountGetter usernameAccountGetter,
            AccountGetter passwordAccountGetter,
            PlainAttrGetter plainAttrGetter);

    /**
     * Prepare attributes for sending to a connector instance.
     *
     * @param any given any object
     * @param password clear-text password
     * @param changePwd whether password should be included for propagation attributes or not
     * @param enable whether any object must be enabled or not
     * @param resource resource information
     * @param provision provision information
     * @return connObjectLink + prepared attributes
     */
    Pair<String, Set<Attribute>> prepareAttrsFromAny(
            Any any,
            String password,
            boolean changePwd,
            Boolean enable,
            ExternalResource resource,
            Provision provision);

    /**
     * Prepare attributes for sending to a connector instance.
     *
     * @param user given user
     * @param account linked account
     * @param password user's clear-text password, to use as default value in case
     * @param changePwd whether password should be included for propagation attributes or not
     * @param provision provision information
     * @return prepared attributes
     */
    Set<Attribute> prepareAttrsFromLinkedAccount(
            User user, LinkedAccount account, String password, boolean changePwd, Provision provision);

    /**
     * Prepare attributes for sending to a connector instance.
     *
     * @param realm Realm
     * @param orgUnit provision information
     * @return connObjectLink + prepared attributes
     */
    Pair<String, Set<Attribute>> prepareAttrsFromRealm(Realm realm, OrgUnit orgUnit);

    /**
     * Set attribute values, according to the given {@link Item}, to any object from attribute received from
     * connector.
     *
     * @param <T> any object
     * @param mapItem mapping item
     * @param attr attribute received from connector
     * @param anyTO any object
     */
    <T extends AnyTO> void setIntValues(Item mapItem, Attribute attr, T anyTO);

    /**
     * Set attribute values, according to the given {@link Item}, to realm from attribute received from
     * connector.
     *
     * @param orgUnitItem mapping item
     * @param attr attribute received from connector
     * @param realmTO realm
     */
    void setIntValues(Item orgUnitItem, Attribute attr, RealmTO realmTO);

    /**
     * Checks if there is a mapping item in the given {@link Provision} for {@code mustChangePassword}.
     *
     * @param provision provision
     * @return if there is a mapping item in the given provision for {@code mustChangePassword}
     */
    boolean hasMustChangePassword(Provision provision);
}
