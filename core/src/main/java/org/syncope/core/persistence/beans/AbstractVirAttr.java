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
package org.syncope.core.persistence.beans;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.propagation.ConnectorFacadeProxy;
import org.syncope.core.util.ApplicationContextManager;
import org.syncope.core.util.JexlUtil;
import org.syncope.types.IntMappingType;

@MappedSuperclass
public abstract class AbstractVirAttr extends AbstractBaseBean {

    private static final long serialVersionUID = 5023204776925954907L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @Transient
    protected List<String> values;

    public Long getId() {
        return id;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    protected <T extends AbstractAttributable> List<Object> retrieveValues(
            final T attributable, final String attributeName,
            final IntMappingType intMappingType) {

        LOG.debug("{}: retrieving external values for {}",
                new Object[]{attributable, attributeName});

        List<Object> virAttrValues;

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();
        ConnInstanceLoader connInstanceLoader =
                context.getBean(ConnInstanceLoader.class);
        if (connInstanceLoader == null) {
            LOG.error("Could not get to ConnInstanceLoader");
            return null;
        } else {
            virAttrValues = new ArrayList<Object>();
        }

        JexlUtil jexlUtil = context.getBean(JexlUtil.class);

        for (ExternalResource resource : attributable.getExternalResources()) {
            LOG.debug("Retrieving attribute mapped on {}", resource);

            Set<String> attributeNames = new HashSet<String>();

            String accountLink = resource.getAccountLink();
            String accountId = null;

            for (SchemaMapping mapping : resource.getMappings()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Processing mapping."
                            + "\n\tID: " + mapping.getId()
                            + "\n\tSource: " + mapping.getIntAttrName()
                            + "\n\tDestination: " + mapping.getExtAttrName()
                            + "\n\tType: " + mapping.getIntMappingType()
                            + "\n\tMandatory condition: "
                            + mapping.getMandatoryCondition()
                            + "\n\tAccountId: " + mapping.isAccountid()
                            + "\n\tPassword: " + mapping.isPassword());
                }

                if (attributeName.equals(mapping.getIntAttrName())
                        && mapping.getIntMappingType() == intMappingType) {

                    attributeNames.add(mapping.getExtAttrName());
                }

                if (mapping.isAccountid()) {
                    try {
                        accountId = attributable.getAttribute(
                                mapping.getIntAttrName()).
                                getValuesAsStrings().get(0);
                    } catch (NullPointerException e) {
                        // ignore exception
                        LOG.debug("Invalid accountId specified", e);
                    }
                }
            }

            if (accountId == null && accountLink != null) {
                accountId = jexlUtil.evaluate(accountLink, attributable);
            }

            if (attributeNames != null && accountId != null) {
                LOG.debug("Get object attribute for entry {}", accountId);

                ConnectorFacadeProxy connector =
                        connInstanceLoader.getConnector(resource);

                try {
                    Set<Attribute> attributes = connector.getObjectAttributes(
                            ObjectClass.ACCOUNT,
                            new Uid(accountId),
                            null,
                            attributeNames);

                    LOG.debug("Retrieved {}", attributes);

                    for (Attribute attribute : attributes) {
                        virAttrValues.addAll(attribute.getValue());
                    }
                } catch (Exception e) {
                    LOG.warn("Error connecting to {}", resource.getName(), e);
                    // ignore exception and go ahead
                }
            }
        }

        return virAttrValues;
    }

    public abstract List<String> getValues();

    public abstract <T extends AbstractAttributable> T getOwner();

    public abstract <T extends AbstractAttributable> void setOwner(T owner);

    public abstract <T extends AbstractVirSchema> T getVirtualSchema();

    public abstract <T extends AbstractVirSchema> void setVirtualSchema(
            T derivedSchema);
}
