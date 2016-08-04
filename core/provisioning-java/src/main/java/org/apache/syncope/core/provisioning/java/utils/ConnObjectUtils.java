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
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.utils.policy.InvalidPasswordRuleConf;
import org.identityconnectors.common.Base64;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConnObjectUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ConnObjectUtils.class);

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    @Autowired
    private TemplateUtils templateUtils;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private PasswordGenerator passwordGenerator;

    @Autowired
    private MappingManager mappingManager;

    /**
     * Extract password value from passed value (if instance of GuardedString or GuardedByteArray).
     *
     * @param pwd received from the underlying connector
     * @return password value
     */
    public static String getPassword(final Object pwd) {
        final StringBuilder result = new StringBuilder();

        if (pwd instanceof GuardedString) {
            ((GuardedString) pwd).access(new GuardedString.Accessor() {

                @Override
                public void access(final char[] clearChars) {
                    result.append(clearChars);
                }
            });
        } else if (pwd instanceof GuardedByteArray) {
            ((GuardedByteArray) pwd).access(new GuardedByteArray.Accessor() {

                @Override
                public void access(final byte[] clearBytes) {
                    result.append(new String(clearBytes));
                }
            });
        } else if (pwd instanceof String) {
            result.append((String) pwd);
        } else {
            result.append(pwd.toString());
        }

        return result.toString();
    }

    /**
     * Build a UserTO / GroupTO / AnyObjectTO out of connector object attributes and schema mapping.
     *
     * @param obj connector object
     * @param pullTask pull task
     * @param provision provision information
     * @param anyUtils utils
     * @param <T> any object
     * @return UserTO for the user to be created
     */
    @Transactional(readOnly = true)
    public <T extends AnyTO> T getAnyTO(
            final ConnectorObject obj, final PullTask pullTask, final Provision provision, final AnyUtils anyUtils) {

        T anyTO = getAnyTOFromConnObject(obj, pullTask, provision, anyUtils);

        // (for users) if password was not set above, generate
        if (anyTO instanceof UserTO && StringUtils.isBlank(((UserTO) anyTO).getPassword())) {
            UserTO userTO = (UserTO) anyTO;

            List<PasswordRuleConf> ruleConfs = new ArrayList<>();

            Realm realm = realmDAO.findByFullPath(userTO.getRealm());
            if (realm != null) {
                for (Realm ancestor : realmDAO.findAncestors(realm)) {
                    if (ancestor.getPasswordPolicy() != null) {
                        ruleConfs.addAll(ancestor.getPasswordPolicy().getRuleConfs());
                    }
                }
            }

            for (String resName : userTO.getResources()) {
                ExternalResource resource = resourceDAO.find(resName);
                if (resource != null && resource.getPasswordPolicy() != null) {
                    ruleConfs.addAll(resource.getPasswordPolicy().getRuleConfs());
                }
            }

            String password;
            try {
                password = passwordGenerator.generate(ruleConfs);
            } catch (InvalidPasswordRuleConf e) {
                LOG.error("Could not generate policy-compliant random password for {}", userTO, e);

                password = SecureRandomUtils.generateRandomPassword(16);
            }
            userTO.setPassword(password);
        }

        return anyTO;
    }

    /**
     * Build {@link AnyPatch} out of connector object attributes and schema mapping.
     *
     * @param key any object to be updated
     * @param obj connector object
     * @param original any object to get diff from
     * @param pullTask pull task
     * @param provision provision information
     * @param anyUtils utils
     * @param <T> any object
     * @return modifications for the any object to be updated
     */
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public <T extends AnyPatch> T getAnyPatch(final String key, final ConnectorObject obj,
            final AnyTO original, final PullTask pullTask, final Provision provision, final AnyUtils anyUtils) {

        AnyTO updated = getAnyTOFromConnObject(obj, pullTask, provision, anyUtils);
        updated.setKey(key);

        if (null != anyUtils.getAnyTypeKind()) {
            switch (anyUtils.getAnyTypeKind()) {
                case USER:
                    // update password if and only if password is really changed
                    User user = userDAO.authFind(key);
                    if (StringUtils.isBlank(((UserTO) updated).getPassword())
                            || ENCRYPTOR.verify(((UserTO) updated).getPassword(),
                                    user.getCipherAlgorithm(), user.getPassword())) {

                        ((UserTO) updated).setPassword(null);
                    }
                    return (T) AnyOperations.diff(((UserTO) updated), ((UserTO) original), true);

                case GROUP:
                    return (T) AnyOperations.diff(((GroupTO) updated), ((GroupTO) original), true);

                case ANY_OBJECT:
                    return (T) AnyOperations.diff(((AnyObjectTO) updated), ((AnyObjectTO) original), true);

                default:
            }
        }

        return null;
    }

    private <T extends AnyTO> T getAnyTOFromConnObject(final ConnectorObject obj,
            final PullTask pullTask, final Provision provision, final AnyUtils anyUtils) {

        T anyTO = anyUtils.newAnyTO();
        anyTO.setType(provision.getAnyType().getKey());

        // 1. fill with data from connector object
        anyTO.setRealm(pullTask.getDestinatioRealm().getFullPath());
        for (MappingItem item : MappingUtils.getPullMappingItems(provision)) {
            mappingManager.setIntValues(item, obj.getAttributeByName(item.getExtAttrName()), anyTO, anyUtils);
        }

        // 2. add data from defined template (if any)
        templateUtils.apply(anyTO, pullTask.getTemplate(provision.getAnyType()));

        return anyTO;
    }

    /**
     * Get connector object TO from a connector object.
     *
     * @param connObject connector object.
     * @return connector object TO.
     */
    public ConnObjectTO getConnObjectTO(final ConnectorObject connObject) {
        final ConnObjectTO connObjectTO = new ConnObjectTO();

        if (connObject != null) {
            for (Attribute attr : connObject.getAttributes()) {
                AttrTO attrTO = new AttrTO();
                attrTO.setSchema(attr.getName());

                if (attr.getValue() != null) {
                    for (Object value : attr.getValue()) {
                        if (value != null) {
                            if (value instanceof GuardedString || value instanceof GuardedByteArray) {
                                attrTO.getValues().add(getPassword(value));
                            } else if (value instanceof byte[]) {
                                attrTO.getValues().add(Base64.encode((byte[]) value));
                            } else {
                                attrTO.getValues().add(value.toString());
                            }
                        }
                    }
                }

                connObjectTO.getAttrs().add(attrTO);
            }
        }

        return connObjectTO;
    }
}
