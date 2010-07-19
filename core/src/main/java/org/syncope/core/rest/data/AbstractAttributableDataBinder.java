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
package org.syncope.core.rest.data;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractAttributeValue;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.Resource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.AttributeValueDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.validation.ValidationException;
import org.syncope.types.SyncopeClientExceptionType;

class AbstractAttributableDataBinder {

    protected static final Logger log = LoggerFactory.getLogger(
            AbstractAttributableDataBinder.class);
    protected SyncopeRoleDAO syncopeRoleDAO;
    protected SchemaDAO schemaDAO;
    protected DerivedSchemaDAO derivedSchemaDAO;
    protected AttributeValueDAO attributeValueDAO;
    protected SyncopeUserDAO syncopeUserDAO;
    protected ResourceDAO resourceDAO;

    protected <T extends AbstractAttributable> T fillAbstractAttributable(
            T abstractAttributable,
            AbstractAttributableTO abstractAttributableTO,
            AttributableUtil attributableUtil,
            SyncopeClientCompositeErrorException compositeErrorException)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);
        SyncopeClientException invalidValues = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidValues);
        SyncopeClientException invalidUniques = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidUniques);

        // 1. attributes
        AbstractSchema schema = null;
        AbstractAttribute attribute = null;
        Set<String> valuesProvided = null;
        AbstractAttributeValue attributeValue = null;
        for (AttributeTO attributeTO : abstractAttributableTO.getAttributes()) {
            schema = schemaDAO.find(attributeTO.getSchema(),
                    attributableUtil.getSchemaClass());

            // safely ignore invalid schemas from AttributeTO
            // see http://code.google.com/p/syncope/issues/detail?id=17
            if (schema == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring invalid schema "
                            + attributeTO.getSchema());
                }
            } else if (schema.isVirtual()) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring virtual schema" + schema.getName());
                }
            } else {
                attribute = attributableUtil.newAttribute();
                attribute.setSchema(schema);
                attribute.setOwner(abstractAttributable);

                // if the schema is multivale, all values are considered for
                // addition, otherwise only the fist one - if provided - is
                // considered
                valuesProvided = schema.isMultivalue()
                        ? attributeTO.getValues()
                        : (attributeTO.getValues().isEmpty()
                        ? Collections.EMPTY_SET
                        : Collections.singleton(
                        attributeTO.getValues().iterator().next()));
                for (String value : valuesProvided) {
                    attributeValue = attributableUtil.newAttributeValue();

                    try {
                        attributeValue = attribute.addValue(value,
                                attributeValue);
                    } catch (ValidationException e) {
                        log.error("Invalid value for attribute "
                                + schema.getName() + ": " + value, e);

                        invalidValues.addElement(schema.getName());
                    }

                    // if the schema is uniquevalue, check the uniqueness
                    if (schema.isUniquevalue()
                            && attributeValueDAO.existingAttributeValue(
                            attributeValue)) {

                        log.error("Unique value schema " + schema.getName()
                                + " with no unique value: "
                                + attributeValue.getValueAsString());

                        invalidUniques.addElement(schema.getName());
                        attribute.setAttributeValues(Collections.EMPTY_SET);
                    }
                }

                if (!attribute.getAttributeValues().isEmpty()) {
                    abstractAttributable.addAttribute(attribute);
                }
            }
        }

        // 2. derived attributes
        AbstractDerivedSchema derivedSchema = null;
        AbstractDerivedAttribute derivedAttribute = null;
        for (AttributeTO attributeTO :
                abstractAttributableTO.getDerivedAttributes()) {

            derivedSchema = derivedSchemaDAO.find(attributeTO.getSchema(),
                    attributableUtil.getDerivedSchemaClass());

            if (derivedSchema == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring invalid derivedschema "
                            + attributeTO.getSchema());
                }
            } else {
                derivedAttribute = attributableUtil.newDerivedAttribute();
                derivedAttribute.setDerivedSchema(derivedSchema);
                derivedAttribute.setOwner(abstractAttributable);
                abstractAttributable.addDerivedAttribute(derivedAttribute);
            }
        }
        // Check if there is some mandatory schema defined for which no value
        // has been provided
        List<AbstractSchema> allUserSchemas =
                schemaDAO.findAll(attributableUtil.getSchemaClass());
        for (AbstractSchema userSchema : allUserSchemas) {
            if (abstractAttributable.getAttribute(userSchema.getName()) == null
                    && userSchema.isMandatory()) {

                log.error("Mandatory schema " + userSchema.getName()
                        + " not provided with values");

                requiredValuesMissing.addElement(userSchema.getName());
            }
        }

        // 3. resources
        Resource resource = null;
        for (String resourceName : abstractAttributableTO.getResources()) {
            resource = resourceDAO.find(resourceName);

            if (resource == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring invalid resource " + resourceName);
                }
            } else {
                abstractAttributable.addResource(resource);

                if (attributableUtil == attributableUtil.USER) {
                    resource.addUser((SyncopeUser) abstractAttributable);
                }
                if (attributableUtil == attributableUtil.ROLE) {
                    resource.addRole((SyncopeRole) abstractAttributable);
                }
            }
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }
        if (!invalidValues.getElements().isEmpty()) {
            compositeErrorException.addException(invalidValues);
        }
        if (!invalidUniques.getElements().isEmpty()) {
            compositeErrorException.addException(invalidUniques);
        }
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return abstractAttributable;
    }

    protected <T extends AbstractAttributableTO> T getAbstractAttributableTO(
            T abstractAttributableTO,
            AbstractAttributable abstractAttributable) {

        AttributeTO attributeTO = null;
        for (AbstractAttribute attribute :
                abstractAttributable.getAttributes()) {

            attributeTO = new AttributeTO();
            attributeTO.setSchema(attribute.getSchema().getName());
            attributeTO.setValues(attribute.getAttributeValuesAsStrings());

            abstractAttributableTO.addAttribute(attributeTO);
        }

        for (AbstractDerivedAttribute derivedAttribute :
                abstractAttributable.getDerivedAttributes()) {

            attributeTO = new AttributeTO();
            attributeTO.setSchema(
                    derivedAttribute.getDerivedSchema().getName());
            attributeTO.addValue(derivedAttribute.getValue(
                    abstractAttributable.getAttributes()));

            abstractAttributableTO.addDerivedAttribute(attributeTO);
        }

        for (Resource resource : abstractAttributable.getResources()) {
            abstractAttributableTO.addResource(resource.getName());
        }

        return abstractAttributableTO;
    }
}
