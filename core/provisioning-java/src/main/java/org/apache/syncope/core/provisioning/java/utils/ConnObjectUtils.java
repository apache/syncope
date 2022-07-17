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
package org.apache.syncope.core.provisioning.java.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

public class ConnObjectUtils {

    protected static final Logger LOG = LoggerFactory.getLogger(ConnObjectUtils.class);

    protected static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    public static SyncToken toSyncToken(final String syncToken) {
        return Optional.ofNullable(syncToken).map(st -> POJOHelper.deserialize(st, SyncToken.class)).orElse(null);
    }

    public static String toString(final SyncToken syncToken) {
        return Optional.ofNullable(syncToken).map(POJOHelper::serialize).orElse(null);
    }

    /**
     * Extract password value from passed value (if instance of GuardedString or GuardedByteArray).
     *
     * @param pwd received from the underlying connector
     * @return password value
     */
    public static String getPassword(final Object pwd) {
        StringBuilder result = new StringBuilder();

        if (pwd instanceof GuardedString) {
            result.append(SecurityUtil.decrypt((GuardedString) pwd));
        } else if (pwd instanceof GuardedByteArray) {
            result.append(Arrays.toString(SecurityUtil.decrypt((GuardedByteArray) pwd)));
        } else if (pwd instanceof String) {
            result.append((String) pwd);
        } else {
            result.append(pwd.toString());
        }

        return result.toString();
    }

    /**
     * Builds {@link ConnObject} out of a collection of {@link Attribute} instances.
     *
     * @param fiql FIQL expression to uniquely identify the given Connector Object
     * @param attrs attributes
     * @return transfer object
     */
    public static ConnObject getConnObjectTO(final String fiql, final Set<Attribute> attrs) {
        ConnObject connObjectTO = new ConnObject();
        connObjectTO.setFiql(fiql);

        if (!CollectionUtils.isEmpty(attrs)) {
            connObjectTO.getAttrs().addAll(attrs.stream().map(attr -> {
                Attr attrTO = new Attr();
                attrTO.setSchema(attr.getName());

                if (!CollectionUtils.isEmpty(attr.getValue())) {
                    attr.getValue().stream().filter(Objects::nonNull).forEach(value -> {
                        if (value instanceof GuardedString || value instanceof GuardedByteArray) {
                            attrTO.getValues().add(getPassword(value));
                        } else if (value instanceof byte[]) {
                            attrTO.getValues().add(Base64.getEncoder().encodeToString((byte[]) value));
                        } else {
                            attrTO.getValues().add(value.toString());
                        }
                    });
                }

                return attrTO;
            }).collect(Collectors.toList()));
        }

        return connObjectTO;
    }

    protected final TemplateUtils templateUtils;

    protected final RealmDAO realmDAO;

    protected final UserDAO userDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final PasswordGenerator passwordGenerator;

    protected final MappingManager mappingManager;

    protected final AnyUtilsFactory anyUtilsFactory;

    public ConnObjectUtils(
            final TemplateUtils templateUtils,
            final RealmDAO realmDAO,
            final UserDAO userDAO,
            final ExternalResourceDAO resourceDAO,
            final PasswordGenerator passwordGenerator,
            final MappingManager mappingManager,
            final AnyUtilsFactory anyUtilsFactory) {

        this.templateUtils = templateUtils;
        this.realmDAO = realmDAO;
        this.userDAO = userDAO;
        this.resourceDAO = resourceDAO;
        this.passwordGenerator = passwordGenerator;
        this.mappingManager = mappingManager;
        this.anyUtilsFactory = anyUtilsFactory;
    }

    /**
     * Build a UserCR / GroupCR / AnyObjectCR out of connector object attributes and schema mapping.
     *
     * @param obj connector object
     * @param pullTask pull task
     * @param anyTypeKind any type kind
     * @param provision provision information
     * @param generatePasswordIfPossible whether password value shall be generated, in case not found from
     * connector object and allowed by resource configuration
     * @param <C> create request type
     * @return create request
     */
    @Transactional(readOnly = true)
    public <C extends AnyCR> C getAnyCR(
            final ConnectorObject obj,
            final PullTask pullTask,
            final AnyTypeKind anyTypeKind,
            final Provision provision,
            final boolean generatePasswordIfPossible) {

        AnyTO anyTO = getAnyTOFromConnObject(obj, pullTask, anyTypeKind, provision);
        C anyCR = anyUtilsFactory.getInstance(anyTypeKind).newAnyCR();
        EntityTOUtils.toAnyCR(anyTO, anyCR);

        // (for users) if password was not set above, generate if resource is configured for that
        if (anyCR instanceof UserCR
                && StringUtils.isBlank(((UserCR) anyCR).getPassword())
                && generatePasswordIfPossible && pullTask.getResource().isRandomPwdIfNotProvided()) {

            UserCR userCR = (UserCR) anyCR;
            List<PasswordPolicy> passwordPolicies = new ArrayList<>();

            Realm realm = realmDAO.findByFullPath(userCR.getRealm());
            if (realm != null) {
                realmDAO.findAncestors(realm).stream().
                        filter(ancestor -> ancestor.getPasswordPolicy() != null).
                        forEach(ancestor -> passwordPolicies.add(ancestor.getPasswordPolicy()));
            }

            userCR.getResources().stream().
                    map(resourceDAO::find).
                    filter(r -> r != null && r.getPasswordPolicy() != null).
                    forEach(r -> passwordPolicies.add(r.getPasswordPolicy()));

            userCR.setPassword(passwordGenerator.generate(passwordPolicies));
        }

        return anyCR;
    }

    public RealmTO getRealmTO(final ConnectorObject obj, final OrgUnit orgUnit) {
        RealmTO realmTO = new RealmTO();

        MappingUtils.getPullItems(orgUnit.getItems().stream()).
                forEach(item -> mappingManager.setIntValues(
                item, obj.getAttributeByName(item.getExtAttrName()), realmTO));

        return realmTO;
    }

    /**
     * Build {@link AnyUR} out of connector object attributes and schema mapping.
     *
     * @param key any object to be updated
     * @param obj connector object
     * @param original any object to get diff from
     * @param pullTask pull task
     * @param anyTypeKind any type kind
     * @param provision provision information
     * @param <U> any object
     * @return modifications for the any object to be updated
     */
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public <U extends AnyUR> U getAnyUR(
            final String key,
            final ConnectorObject obj,
            final AnyTO original,
            final PullTask pullTask,
            final AnyTypeKind anyTypeKind,
            final Provision provision) {

        AnyTO updated = getAnyTOFromConnObject(obj, pullTask, anyTypeKind, provision);
        updated.setKey(key);

        U anyUR;
        switch (provision.getAnyType()) {
            case "USER":
                UserTO originalUser = (UserTO) original;
                UserTO updatedUser = (UserTO) updated;

                if (StringUtils.isBlank(updatedUser.getUsername())) {
                    updatedUser.setUsername(originalUser.getUsername());
                }

                // update password if and only if password is really changed
                User user = userDAO.authFind(key);
                if (StringUtils.isBlank(updatedUser.getPassword())
                        || ENCRYPTOR.verify(updatedUser.getPassword(),
                                user.getCipherAlgorithm(), user.getPassword())) {

                    updatedUser.setPassword(null);
                }

                updatedUser.setSecurityQuestion(originalUser.getSecurityQuestion());

                if (!mappingManager.hasMustChangePassword(provision)) {
                    updatedUser.setMustChangePassword(originalUser.isMustChangePassword());
                }

                anyUR = (U) AnyOperations.diff(updatedUser, originalUser, true);
                break;

            case "GROUP":
                GroupTO originalGroup = (GroupTO) original;
                GroupTO updatedGroup = (GroupTO) updated;

                if (StringUtils.isBlank(updatedGroup.getName())) {
                    updatedGroup.setName(originalGroup.getName());
                }
                updatedGroup.setUserOwner(originalGroup.getUserOwner());
                updatedGroup.setGroupOwner(originalGroup.getGroupOwner());
                updatedGroup.setUDynMembershipCond(originalGroup.getUDynMembershipCond());
                updatedGroup.getADynMembershipConds().putAll(originalGroup.getADynMembershipConds());
                updatedGroup.getTypeExtensions().addAll(originalGroup.getTypeExtensions());

                anyUR = (U) AnyOperations.diff(updatedGroup, originalGroup, true);
                break;

            default:
                AnyObjectTO originalAnyObject = (AnyObjectTO) original;
                AnyObjectTO updatedAnyObject = (AnyObjectTO) updated;

                if (StringUtils.isBlank(updatedAnyObject.getName())) {
                    updatedAnyObject.setName(originalAnyObject.getName());
                }

                anyUR = (U) AnyOperations.diff(updatedAnyObject, originalAnyObject, true);
        }

        if (anyUR != null) {
            // ensure not to include incidental realm changes in the patch
            anyUR.setRealm(null);

            // SYNCOPE-1343, remove null or empty values from the patch plain attributes
            AnyOperations.cleanEmptyAttrs(updated, anyUR);
        }
        return anyUR;
    }

    protected <T extends AnyTO> T getAnyTOFromConnObject(
            final ConnectorObject obj,
            final PullTask pullTask,
            final AnyTypeKind anyTypeKind,
            final Provision provision) {

        T anyTO = anyUtilsFactory.getInstance(anyTypeKind).newAnyTO();
        anyTO.setType(provision.getAnyType());
        anyTO.getAuxClasses().addAll(provision.getAuxClasses());

        // 1. fill with data from connector object
        anyTO.setRealm(pullTask.getDestinationRealm().getFullPath());
        MappingUtils.getPullItems(provision.getMapping().getItems().stream()).forEach(
                item -> mappingManager.setIntValues(item, obj.getAttributeByName(item.getExtAttrName()), anyTO));

        // 2. add data from defined template (if any)
        templateUtils.apply(anyTO, pullTask.getTemplate(provision.getAnyType()));

        return anyTO;
    }
}
