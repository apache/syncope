/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.propagation;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttribute;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.propagation.ResourceOperations.Type;
import org.syncope.core.persistence.util.ApplicationContextManager;
import org.syncope.types.SchemaType;

public class PropagationManager {

    private static final Logger log =
            LoggerFactory.getLogger(PropagationManager.class);

    /**
     * Create the user on every associated resource.
     * Exceptions will be ignored.
     * @param user to be created.
     * @return a set of provisioned resources.
     * @throws PropagationException
     */
    public Set<String> create(SyncopeUser user) throws PropagationException {
        return create(user, null);
    }

    /**
     * Create the user on every associated resource.
     * It is possible to ask for a synchronous provisioning for some resources
     * specifying a set of resource names.
     * Exceptions won't be ignored and the process will be stopped if the
     * creation fails onto a synchronous resource.
     * @param user to be created.
     * @param syncResourceNames to ask for a synchronous or asynchronous provisioning.
     * @return a set of provisioned resources.
     * @throws PropagationException
     */
    public Set<String> create(SyncopeUser user, Set<String> syncResourceNames)
            throws PropagationException {

        Set<TargetResource> resources = new HashSet<TargetResource>();
        for (TargetResource resource : user.getTargetResources()) {
            resources.add(resource);
        }
        for (Membership membership : user.getMemberships()) {
            resources.addAll(membership.getSyncopeRole().getTargetResources());
        }

        ResourceOperations resourceOperations = new ResourceOperations();
        resourceOperations.set(Type.CREATE, resources);

        return provision(user, resourceOperations, syncResourceNames);
    }

    /**
     * Performs update on each resource associated to the user.
     * It is possible to ask for a synchronous provisioning for some resources
     * specifying a set of resource names.
     * Exceptions won't be ignored and the process will be stoppend if the
     * provisioning fails onto a synchronous resource.
     * @param user to be updated.
     * @param affectedResources resources affected by this update
     * @param syncResourceNames to ask for a synchronous or asynchronous update.
     * @return a set of updated resources.
     * @throws PropagationException
     */
    public Set<String> update(SyncopeUser user,
            ResourceOperations resourceOperations,
            Set<String> syncResourceNames)
            throws PropagationException {

        return provision(user, resourceOperations, syncResourceNames);
    }

    /**
     * Implementation of the provisioning feature.
     * @param user
     * @param syncResourceNames
     * @param merge
     * @return provisioned resources
     * @throws PropagationException
     */
    private Set<String> provision(SyncopeUser user,
            ResourceOperations resourceOperations,
            Set<String> syncResourceNames)
            throws PropagationException {

        // set of provisioned resources
        Set<String> provisioned = new HashSet<String>();

        // Avoid duplicates - see javadoc
        resourceOperations.purge();

        // Resource to be provisioned synchronously
        ResourceOperations syncOperations = new ResourceOperations();

        // Resource to be provisioned asynchronously
        ResourceOperations asyncOperations = new ResourceOperations();

        if (syncResourceNames == null) {
            syncResourceNames = Collections.EMPTY_SET;
        }

        for (Type type : ResourceOperations.Type.values()) {
            for (TargetResource resource : resourceOperations.get(type)) {
                if (syncResourceNames.contains(resource.getName())) {
                    syncOperations.add(type, resource);
                } else {
                    asyncOperations.add(type, resource);
                }
            }
        }

        // synchronous propagation ...
        if (log.isDebugEnabled()) {
            log.debug(
                    "Synchronous provisioning of " +
                    syncOperations + " with user " + user);
        }

        for (Type type : ResourceOperations.Type.values()) {
            for (TargetResource resource : syncOperations.get(type)) {
                try {

                    propagate(user, resource, type);
                    provisioned.add(resource.getName());

                } catch (Throwable t) {
                    log.error(
                            "Exception during provision on resource " +
                            resource.getName(), t);

                    throw new PropagationException(
                            "Exception during provision on resource " +
                            resource.getName(), resource.getName(), t);
                }
            }
        }

        // asynchronous propagation ...
        if (log.isDebugEnabled()) {
            log.debug(
                    "Asynchronous provisioning of " +
                    asyncOperations + " with user " + user);
        }

        for (Type type : ResourceOperations.Type.values()) {
            for (TargetResource resource : asyncOperations.get(type)) {
                try {

                    propagate(user, resource, type);
                    provisioned.add(resource.getName());

                } catch (Throwable t) {
                    log.error(
                            "Exception during provision on resource " +
                            resource.getName(), t);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "Provisioned " + provisioned + " with user " + user.getId());
        }

        return provisioned;
    }

    /**
     * Propagate provision/update the resource indicated.
     * @param user to be created.
     * @param resource to be provisioned.
     * @param type to be performed on the specified resource
     * @throws NoSuchBeanDefinitionException if the connector bean doesn't
     * exist.
     * @throws IllegalStateException if propagation fails.
     */
    private void propagate(SyncopeUser user, TargetResource resource, Type type)
            throws NoSuchBeanDefinitionException, IllegalStateException {

        ConnectorInstance connectorInstance = resource.getConnector();

        ConnectorFacade connector =
                getConnectorFacade(connectorInstance.getId().toString());

        if (connector == null) {
            log.error(
                    "Connector instance bean " +
                    connectorInstance.getId().toString() + " not found");

            throw new NoSuchBeanDefinitionException(
                    "Connector instance bean not found");
        }

        // get password
        String password = user.getPassword();

        // get mapping
        List<SchemaMapping> mappings = resource.getMappings();

        // set of user attributes
        Set<Attribute> attrs = new HashSet<Attribute>();

        // cast to be applied on SchemaType
        Class castToBeApplied = null;

        // account id
        String accountId = null;

        // resource field
        String field = null;
        // resource field values
        Set objValues = null;

        // syncope attribute schema name
        String schema = null;
        // syncope user attribute
        UserAttribute userAttribute = null;
        // syncope user attribute schema type
        SchemaType schemaType = null;
        // syncope user attribute values
        List<UserAttributeValue> values = null;

        for (SchemaMapping mapping : mappings) {

            try {
                // get field name on target resource
                field = mapping.getField();

                // get schema name on syncope
                schema = mapping.getUserSchema().getName();

                // get defined type for user attribute
                schemaType = mapping.getUserSchema().getType();

                if (log.isDebugEnabled()) {
                    log.debug(
                            "\nDefine mapping for: " +
                            "\n* Field " + field +
                            "\n* is accountId " + mapping.isAccountid() +
                            "\n* is password " + mapping.isPassword() +
                            "\n* is nullable " + mapping.isNullable() +
                            "\n* Schema " + schema +
                            "\n* Type " + schemaType.getClassName());
                }

                // get user attribute object
                userAttribute = user.getAttribute(schema);

                if (userAttribute == null) {
                    throw new Exception(
                            "Invalid user attribute " + userAttribute);
                }

                // -----------------------------
                // Retrieve user attribute values
                // -----------------------------
                objValues = new HashSet();

                values = userAttribute.getAttributeValues();

                for (UserAttributeValue value : values) {

                    castToBeApplied = Class.forName(schemaType.getClassName());

                    if (!FrameworkUtil.isSupportedAttributeType(
                            castToBeApplied)) {

                        castToBeApplied = String.class;
                        objValues.add(value.getValueAsString());

                    } else {

                        objValues.add(value.getValue());

                    }
                }
                // -----------------------------

                if (mapping.isAccountid()) {
                    accountId = objValues.iterator().next().toString();
                    attrs.add(new Name(accountId));
                }

                if (mapping.isPassword()) {
                    attrs.add(AttributeBuilder.buildPassword(
                            password.toCharArray()));
                }

                Object objValue = null;

                if (!objValues.isEmpty())
                    objValue = objValues.iterator().next();

                if (!mapping.isPassword() &&
                        !mapping.isAccountid()) {

                    if (mapping.getUserSchema().isMultivalue()) {
                        attrs.add(AttributeBuilder.build(
                                field,
                                objValues));
                    } else {
                        attrs.add(AttributeBuilder.build(
                                field,
                                castToBeApplied.cast(objValue)));
                    }
                }

            } catch (ClassNotFoundException e) {

                if (log.isWarnEnabled()) {
                    log.warn("Unsupported attribute type " +
                            schemaType.getClassName(), e);
                }

            } catch (Throwable t) {
                if (log.isWarnEnabled()) {
                    log.warn("Attribute '" + schema + "' processing failed", t);
                }
            }
        }

        Uid userUid = null;
        switch (type) {
            case CREATE:
                userUid = connector.create(ObjectClass.ACCOUNT, attrs, null);
                break;

            case UPDATE:
                userUid = connector.update(ObjectClass.ACCOUNT,
                        new Uid(accountId), attrs, null);
                break;

            case DELETE:
                connector.delete(ObjectClass.ACCOUNT, new Uid(accountId), null);
                break;

        }



        if (userUid == null && type != Type.DELETE) {
            log.error(
                    "Error creating / updating user onto resource " + resource);

            throw new IllegalStateException("Error creating user");
        }

    }

    private ConnectorFacade getConnectorFacade(String id) {

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) context.getBeanFactory();

        return (ConnectorFacade) beanFactory.getBean(id);
    }
}
