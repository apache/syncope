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
package org.apache.syncope.core.misc;

import org.apache.syncope.core.misc.policy.InvalidPasswordPolicySpecException;
import org.apache.syncope.core.misc.security.PasswordGenerator;
import org.apache.syncope.core.misc.security.SecureRandomUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.mod.AnyMod;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PasswordPolicySpec;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheValue;
import org.apache.syncope.core.misc.security.Encryptor;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.misc.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplate;
import org.identityconnectors.common.Base64;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConnObjectUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ConnObjectUtils.class);

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private PasswordGenerator pwdGen;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    private final Encryptor encryptor = Encryptor.getInstance();

    /**
     * Virtual attribute cache.
     */
    @Autowired
    private VirAttrCache virAttrCache;

    /**
     * Build a UserTO / GroupTO / AnyObjectTO out of connector object attributes and schema mapping.
     *
     * @param obj connector object
     * @param syncTask synchronization task
     * @param provision provision information
     * @param anyUtils utils
     * @param <T> any object
     * @return UserTO for the user to be created
     */
    @Transactional(readOnly = true)
    public <T extends AnyTO> T getAnyTO(
            final ConnectorObject obj, final SyncTask syncTask, final Provision provision, final AnyUtils anyUtils) {

        T anyTO = getAnyTOFromConnObject(obj, syncTask, provision, anyUtils);

        // (for users) if password was not set above, generate
        if (anyTO instanceof UserTO && StringUtils.isBlank(((UserTO) anyTO).getPassword())) {
            final UserTO userTO = (UserTO) anyTO;

            List<PasswordPolicySpec> ppSpecs = new ArrayList<>();

            Realm realm = realmDAO.find(userTO.getRealm());
            if (realm != null) {
                for (Realm ancestor : realmDAO.findAncestors(realm)) {
                    if (ancestor.getPasswordPolicy() != null
                            && ancestor.getPasswordPolicy().getSpecification(PasswordPolicySpec.class) != null) {

                        ppSpecs.add(ancestor.getPasswordPolicy().getSpecification(PasswordPolicySpec.class));
                    }
                }
            }

            for (String resName : userTO.getResources()) {
                ExternalResource resource = resourceDAO.find(resName);
                if (resource != null && resource.getPasswordPolicy() != null
                        && resource.getPasswordPolicy().getSpecification(PasswordPolicySpec.class) != null) {

                    ppSpecs.add(resource.getPasswordPolicy().getSpecification(PasswordPolicySpec.class));
                }
            }

            String password;
            try {
                password = pwdGen.generate(ppSpecs);
            } catch (InvalidPasswordPolicySpecException e) {
                LOG.error("Could not generate policy-compliant random password for {}", userTO, e);

                password = SecureRandomUtils.generateRandomPassword(16);
            }
            userTO.setPassword(password);
        }

        return anyTO;
    }

    /**
     * Build an UserMod out of connector object attributes and schema mapping.
     *
     * @param key any object to be updated
     * @param obj connector object
     * @param original any object to get diff from
     * @param syncTask synchronization task
     * @param provision provision information
     * @param anyUtils utils
     * @param <T> any object
     * @return modifications for the any object to be updated
     */
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public <T extends AnyMod> T getAnyMod(final Long key, final ConnectorObject obj,
            final AnyTO original, final SyncTask syncTask, final Provision provision, final AnyUtils anyUtils) {

        AnyTO updated = getAnyTOFromConnObject(obj, syncTask, provision, anyUtils);
        updated.setKey(key);

        if (AnyTypeKind.USER == anyUtils.getAnyTypeKind()) {
            // update password if and only if password is really changed
            User user = userDAO.authFind(key);
            if (StringUtils.isBlank(((UserTO) updated).getPassword())
                    || encryptor.verify(((UserTO) updated).getPassword(),
                            user.getCipherAlgorithm(), user.getPassword())) {

                ((UserTO) updated).setPassword(null);
            }
            return (T) AnyOperations.diff(((UserTO) updated), ((UserTO) original), true);
        } else if (AnyTypeKind.GROUP == anyUtils.getAnyTypeKind()) {
            return (T) AnyOperations.diff(((GroupTO) updated), ((GroupTO) original), true);
        }

        return null;
    }

    private <T extends AnyTO> T getAnyTOFromConnObject(final ConnectorObject obj,
            final SyncTask syncTask, final Provision provision, final AnyUtils anyUtils) {

        T anyTO = anyUtils.newAnyTO();

        // 1. fill with data from connector object
        anyTO.setRealm(syncTask.getDestinatioRealm().getFullPath());
        for (MappingItem item : anyUtils.getMappingItems(provision, MappingPurpose.SYNCHRONIZATION)) {
            Attribute attr = obj.getAttributeByName(item.getExtAttrName());

            AttrTO attrTO;
            switch (item.getIntMappingType()) {
                case UserId:
                case GroupId:
                    break;

                case Password:
                    if (anyTO instanceof UserTO && attr != null && attr.getValue() != null
                            && !attr.getValue().isEmpty()) {

                        ((UserTO) anyTO).setPassword(getPassword(attr.getValue().get(0)));
                    }
                    break;

                case Username:
                    if (anyTO instanceof UserTO) {
                        ((UserTO) anyTO).setUsername(attr == null || attr.getValue().isEmpty()
                                || attr.getValue().get(0) == null
                                        ? null
                                        : attr.getValue().get(0).toString());
                    }
                    break;

                case GroupName:
                    if (anyTO instanceof GroupTO) {
                        ((GroupTO) anyTO).setName(attr == null || attr.getValue().isEmpty()
                                || attr.getValue().get(0) == null
                                        ? null
                                        : attr.getValue().get(0).toString());
                    }
                    break;

                case GroupOwnerSchema:
                    if (anyTO instanceof GroupTO && attr != null) {
                        // using a special attribute (with schema "", that will be ignored) for carrying the
                        // GroupOwnerSchema value
                        attrTO = new AttrTO();
                        attrTO.setSchema(StringUtils.EMPTY);
                        if (attr.getValue().isEmpty() || attr.getValue().get(0) == null) {
                            attrTO.getValues().add(StringUtils.EMPTY);
                        } else {
                            attrTO.getValues().add(attr.getValue().get(0).toString());
                        }

                        ((GroupTO) anyTO).getPlainAttrs().add(attrTO);
                    }
                    break;

                case UserPlainSchema:
                case GroupPlainSchema:
                    attrTO = new AttrTO();
                    attrTO.setSchema(item.getIntAttrName());

                    PlainSchema schema = plainSchemaDAO.find(item.getIntAttrName());

                    for (Object value : attr == null || attr.getValue() == null
                            ? Collections.emptyList()
                            : attr.getValue()) {

                        AttrSchemaType schemaType = schema == null ? AttrSchemaType.String : schema.getType();
                        if (value != null) {
                            final PlainAttrValue attrValue = anyUtils.newPlainAttrValue();
                            switch (schemaType) {
                                case String:
                                    attrValue.setStringValue(value.toString());
                                    break;

                                case Binary:
                                    attrValue.setBinaryValue((byte[]) value);
                                    break;

                                default:
                                    try {
                                        attrValue.parseValue(schema, value.toString());
                                    } catch (ParsingValidationException e) {
                                        LOG.error("While parsing provided value {}", value, e);
                                        attrValue.setStringValue(value.toString());
                                        schemaType = AttrSchemaType.String;
                                    }
                                    break;
                            }
                            attrTO.getValues().add(attrValue.getValueAsString(schemaType));
                        }
                    }

                    anyTO.getPlainAttrs().add(attrTO);
                    break;

                case UserDerivedSchema:
                case GroupDerivedSchema:
                    attrTO = new AttrTO();
                    attrTO.setSchema(item.getIntAttrName());
                    anyTO.getDerAttrs().add(attrTO);
                    break;

                case UserVirtualSchema:
                case GroupVirtualSchema:
                    attrTO = new AttrTO();
                    attrTO.setSchema(item.getIntAttrName());

                    for (Object value : attr == null || attr.getValue() == null
                            ? Collections.emptyList()
                            : attr.getValue()) {

                        if (value != null) {
                            attrTO.getValues().add(value.toString());
                        }
                    }

                    anyTO.getVirAttrs().add(attrTO);
                    break;

                default:
            }
        }

        // 2. add data from defined template (if any)
        AnyTemplate anyTypeTemplate = syncTask.getTemplate(provision.getAnyType());
        if (anyTypeTemplate != null) {
            AnyTO template = anyTypeTemplate.get();

            if (template.getRealm() != null) {
                anyTO.setRealm(template.getRealm());
            }

            if (template instanceof UserTO) {
                if (StringUtils.isNotBlank(((UserTO) template).getUsername())) {
                    String evaluated = JexlUtils.evaluate(((UserTO) template).getUsername(), anyTO);
                    if (StringUtils.isNotBlank(evaluated)) {
                        ((UserTO) anyTO).setUsername(evaluated);
                    }
                }

                if (StringUtils.isNotBlank(((UserTO) template).getPassword())) {
                    String evaluated = JexlUtils.evaluate(((UserTO) template).getPassword(), anyTO);
                    if (StringUtils.isNotBlank(evaluated)) {
                        ((UserTO) anyTO).setPassword(evaluated);
                    }
                }
            }
            if (template instanceof GroupTO) {
                if (StringUtils.isNotBlank(((GroupTO) template).getName())) {
                    String evaluated = JexlUtils.evaluate(((GroupTO) template).getName(), anyTO);
                    if (StringUtils.isNotBlank(evaluated)) {
                        ((GroupTO) anyTO).setName(evaluated);
                    }
                }

                if (((GroupTO) template).getUserOwner() != null) {
                    final User userOwner = userDAO.find(((GroupTO) template).getUserOwner());
                    if (userOwner != null) {
                        ((GroupTO) anyTO).setUserOwner(userOwner.getKey());
                    }
                }
                if (((GroupTO) template).getGroupOwner() != null) {
                    final Group groupOwner = groupDAO.find(((GroupTO) template).getGroupOwner());
                    if (groupOwner != null) {
                        ((GroupTO) anyTO).setGroupOwner(groupOwner.getKey());
                    }
                }
            }

            fillFromTemplate(anyTO, template);

            for (String resource : template.getResources()) {
                anyTO.getResources().add(resource);
            }
        }

        return anyTO;
    }

    /**
     * Extract password value from passed value (if instance of GuardedString or GuardedByteArray).
     *
     * @param pwd received from the underlying connector
     * @return password value
     */
    public String getPassword(final Object pwd) {
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
     * Get connector object TO from a connector object.
     *
     * @param connObject connector object.
     * @return connector object TO.
     */
    public ConnObjectTO getConnObjectTO(final ConnectorObject connObject) {
        final ConnObjectTO connObjectTO = new ConnObjectTO();

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

            connObjectTO.getPlainAttrs().add(attrTO);
        }

        return connObjectTO;
    }

    /**
     * Query connected external resources for values to populated virtual attributes associated with the given owner.
     *
     * @param any any object
     */
    public void retrieveVirAttrValues(final Any<?, ?, ?> any) {
        ConfigurableApplicationContext context = ApplicationContextProvider.getApplicationContext();
        ConnectorFactory connFactory = context.getBean(ConnectorFactory.class);

        IntMappingType type = any.getType().getKind() == AnyTypeKind.USER
                ? IntMappingType.UserVirtualSchema
                : any.getType().getKind() == AnyTypeKind.GROUP
                        ? IntMappingType.GroupVirtualSchema
                        : IntMappingType.AnyVirtualSchema;

        Map<String, ConnectorObject> resources = new HashMap<>();

        // -----------------------
        // Retrieve virtual attribute values if and only if they have not been retrieved yet
        // -----------------------
        for (VirAttr<?> virAttr : any.getVirAttrs()) {
            // reset value set
            if (virAttr.getValues().isEmpty()) {
                retrieveVirAttrValue(any, virAttr, type, resources, connFactory);
            }
        }
        // -----------------------
    }

    private void retrieveVirAttrValue(
            final Any<?, ?, ?> any,
            final VirAttr<?> virAttr,
            final IntMappingType type,
            final Map<String, ConnectorObject> externalResources,
            final ConnectorFactory connFactory) {

        String schemaName = virAttr.getSchema().getKey();
        VirAttrCacheValue virAttrCacheValue = virAttrCache.get(any.getType().getKey(), any.getKey(), schemaName);

        LOG.debug("Retrieve values for virtual attribute {} ({})", schemaName, type);

        if (virAttrCache.isValidEntry(virAttrCacheValue)) {
            // cached ...
            LOG.debug("Values found in cache {}", virAttrCacheValue);
            virAttr.getValues().clear();
            virAttr.getValues().addAll(new ArrayList<>(virAttrCacheValue.getValues()));
        } else {
            // not cached ...
            LOG.debug("Need one or more remote connections");

            VirAttrCacheValue toBeCached = new VirAttrCacheValue();

            AnyUtils anyUtils = anyUtilsFactory.getInstance(any);
            Collection<ExternalResource> targetResources = getTargetResources(virAttr, type, anyUtils, any.getType());

            for (ExternalResource resource : targetResources) {
                Provision provision = resource.getProvision(any.getType());
                LOG.debug("Search values into {},{}", resource, provision);

                if (provision != null) {
                    try {
                        List<MappingItem> mappings = anyUtils.getMappingItems(provision, MappingPurpose.BOTH);

                        ConnectorObject connectorObject;
                        if (externalResources.containsKey(resource.getKey())) {
                            connectorObject = externalResources.get(resource.getKey());
                        } else {
                            LOG.debug("Perform connection to {}", resource.getKey());
                            String connObjectKey = anyUtils.getConnObjectKeyItem(provision) == null
                                    ? null
                                    : MappingUtils.getConnObjectKeyValue(any, provision);

                            if (StringUtils.isBlank(connObjectKey)) {
                                throw new IllegalArgumentException("No ConnObjectKey found for " + resource.getKey());
                            }

                            Connector connector = connFactory.getConnector(resource);

                            OperationOptions oo =
                                    connector.getOperationOptions(MappingUtils.getMatchingMappingItems(mappings, type));

                            connectorObject =
                                    connector.getObject(provision.getObjectClass(), new Uid(connObjectKey), oo);
                            externalResources.put(resource.getKey(), connectorObject);
                        }

                        if (connectorObject != null) {
                            // ask for searched virtual attribute value
                            Collection<MappingItem> virAttrMappings =
                                    MappingUtils.getMatchingMappingItems(mappings, schemaName, type);

                            // the same virtual attribute could be mapped with one or more external attribute 
                            for (MappingItem mapping : virAttrMappings) {
                                Attribute attribute = connectorObject.getAttributeByName(mapping.getExtAttrName());

                                if (attribute != null && attribute.getValue() != null) {
                                    for (Object obj : attribute.getValue()) {
                                        if (obj != null) {
                                            virAttr.getValues().add(obj.toString());
                                        }
                                    }
                                }
                            }

                            toBeCached.setResourceValues(resource.getKey(), new HashSet<>(virAttr.getValues()));

                            LOG.debug("Retrieved values {}", virAttr.getValues());
                        }
                    } catch (Exception e) {
                        LOG.error("Error reading connector object from {}", resource.getKey(), e);

                        if (virAttrCacheValue != null) {
                            toBeCached.forceExpiring();
                            LOG.debug("Search for a cached value (even expired!) ...");
                            final Set<String> cachedValues = virAttrCacheValue.getValues(resource.getKey());
                            if (cachedValues != null) {
                                LOG.debug("Use cached value {}", cachedValues);
                                virAttr.getValues().addAll(cachedValues);
                                toBeCached.setResourceValues(resource.getKey(), new HashSet<>(cachedValues));
                            }
                        }
                    }
                }
            }

            virAttrCache.put(any.getType().getKey(), any.getKey(), schemaName, toBeCached);
        }
    }

    private Collection<ExternalResource> getTargetResources(
            final VirAttr<?> attr, final IntMappingType type, final AnyUtils anyUtils, final AnyType anyType) {

        Iterable<? extends ExternalResource> iterable = attr.getOwner() instanceof User
                ? userDAO.findAllResources((User) attr.getOwner())
                : attr.getOwner() instanceof AnyObject
                        ? anyObjectDAO.findAllResources((AnyObject) attr.getOwner())
                        : attr.getOwner() instanceof Group
                                ? ((Group) attr.getOwner()).getResources()
                                : Collections.<ExternalResource>emptySet();
        return getTargetResources(attr, type, anyUtils, iterable, anyType);
    }

    private Collection<ExternalResource> getTargetResources(final VirAttr<?> attr, final IntMappingType type,
            final AnyUtils anyUtils, final Iterable<? extends ExternalResource> ownerResources, final AnyType anyType) {

        return CollectionUtils.select(ownerResources, new Predicate<ExternalResource>() {

            @Override
            public boolean evaluate(final ExternalResource resource) {
                return resource.getProvision(anyType) != null
                        && !MappingUtils.getMatchingMappingItems(
                                anyUtils.getMappingItems(resource.getProvision(anyType), MappingPurpose.BOTH),
                                attr.getSchema().getKey(), type).isEmpty();
            }
        });
    }

    private void fillFromTemplate(final AnyTO anyTO, final AnyTO template) {
        Map<String, AttrTO> currentAttrMap = anyTO.getPlainAttrMap();
        for (AttrTO templateAttr : template.getPlainAttrs()) {
            if (templateAttr.getValues() != null && !templateAttr.getValues().isEmpty()
                    && (!currentAttrMap.containsKey(templateAttr.getSchema())
                    || currentAttrMap.get(templateAttr.getSchema()).getValues().isEmpty())) {

                anyTO.getPlainAttrs().add(evaluateAttrTemplate(anyTO, templateAttr));
            }
        }

        currentAttrMap = anyTO.getDerAttrMap();
        for (AttrTO templateDerAttr : template.getDerAttrs()) {
            if (!currentAttrMap.containsKey(templateDerAttr.getSchema())) {
                anyTO.getDerAttrs().add(templateDerAttr);
            }
        }

        currentAttrMap = anyTO.getVirAttrMap();
        for (AttrTO templateVirAttr : template.getVirAttrs()) {
            if (templateVirAttr.getValues() != null && !templateVirAttr.getValues().isEmpty()
                    && (!currentAttrMap.containsKey(templateVirAttr.getSchema())
                    || currentAttrMap.get(templateVirAttr.getSchema()).getValues().isEmpty())) {

                anyTO.getVirAttrs().add(evaluateAttrTemplate(anyTO, templateVirAttr));
            }
        }
    }

    private AttrTO evaluateAttrTemplate(final AnyTO anyTO, final AttrTO template) {
        AttrTO result = new AttrTO();
        result.setSchema(template.getSchema());

        if (template.getValues() != null && !template.getValues().isEmpty()) {
            for (String value : template.getValues()) {
                String evaluated = JexlUtils.evaluate(value, anyTO);
                if (StringUtils.isNotBlank(evaluated)) {
                    result.getValues().add(evaluated);
                }
            }
        }

        return result;
    }

    /**
     * Transform a
     * <code>Collection</code> of {@link Attribute} instances into a {@link Map}. The key to each element in the map is
     * the <i>name</i> of an
     * <code>Attribute</code>. The value of each element in the map is the
     * <code>Attribute</code> instance with that name. <br/> Different from the original because: <ul> <li>map keys are
     * transformed toUpperCase()</li> <li>returned map is mutable</li> </ul>
     *
     * @param attributes set of attribute to transform to a map.
     * @return a map of string and attribute.
     *
     * @see org.identityconnectors.framework.common.objects.AttributeUtil#toMap(java.util.Collection)
     */
    public Map<String, Attribute> toMap(final Collection<? extends Attribute> attributes) {
        final Map<String, Attribute> map = new HashMap<>();
        for (Attribute attr : attributes) {
            map.put(attr.getName().toUpperCase(), attr);
        }
        return map;
    }
}
