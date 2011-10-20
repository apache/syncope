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

import org.syncope.core.util.AttributableUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.validation.ValidationException;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.client.mod.AbstractAttributableMod;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.AbstractDerSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractVirAttr;
import org.syncope.core.persistence.beans.AbstractVirSchema;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.AttrDAO;
import org.syncope.core.persistence.dao.AttrValueDAO;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.persistence.dao.DerAttrDAO;
import org.syncope.core.persistence.dao.DerSchemaDAO;
import org.syncope.core.persistence.dao.MembershipDAO;
import org.syncope.core.persistence.dao.PolicyDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.dao.VirAttrDAO;
import org.syncope.core.persistence.dao.VirSchemaDAO;
import org.syncope.core.persistence.propagation.PropagationByResource;
import org.syncope.core.util.JexlUtil;
import org.syncope.types.PropagationOperation;
import org.syncope.types.SyncopeClientExceptionType;

public abstract class AbstractAttributableDataBinder {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            AbstractAttributableDataBinder.class);

    @Autowired
    protected ConfDAO confDAO;

    @Autowired
    protected RoleDAO roleDAO;

    @Autowired
    protected SchemaDAO schemaDAO;

    @Autowired
    protected DerSchemaDAO derivedSchemaDAO;

    @Autowired
    protected VirSchemaDAO virtualSchemaDAO;

    @Autowired
    protected AttrDAO attributeDAO;

    @Autowired
    protected DerAttrDAO derAttrDAO;

    @Autowired
    protected VirAttrDAO virAttrDAO;

    @Autowired
    protected AttrValueDAO attributeValueDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected ResourceDAO resourceDAO;

    @Autowired
    protected MembershipDAO membershipDAO;

    @Autowired
    protected PolicyDAO policyDAO;

    @Autowired
    private JexlUtil jexlUtil;

    private <T extends AbstractSchema> T getSchema(
            final String schemaName, final Class<T> reference) {

        T schema = schemaDAO.find(schemaName, reference);

        // safely ignore invalid schemas from AttributeTO
        // see http://code.google.com/p/syncope/issues/detail?id=17
        if (schema == null) {
            LOG.debug("Ignoring invalid schema {}", schemaName);
        } else if (schema.isReadonly()) {
            schema = null;

            LOG.debug("Ignoring virtual or readonly schema {}", schemaName);
        }

        return schema;
    }

    private <T extends AbstractDerSchema> T getDerivedSchema(
            final String derSchemaName, final Class<T> reference) {

        T derivedSchema = derivedSchemaDAO.find(derSchemaName, reference);

        if (derivedSchema == null) {
            LOG.debug("Ignoring invalid derived schema {}", derSchemaName);
        }

        return derivedSchema;
    }

    private <T extends AbstractVirSchema> T getVirtualSchema(
            final String virSchemaName, final Class<T> reference) {

        T virtualSchema = virtualSchemaDAO.find(virSchemaName, reference);

        if (virtualSchema == null) {
            LOG.debug("Ignoring invalid virtual schema {}", virSchemaName);
        }

        return virtualSchema;
    }

    private ExternalResource getResource(final String resourceName) {
        ExternalResource resource = resourceDAO.find(resourceName);

        if (resource == null) {
            LOG.debug("Ignoring invalid resource {} ", resourceName);
        }

        return resource;
    }

    protected void fillAttribute(final List<String> values,
            final AttributableUtil attributableUtil,
            final AbstractSchema schema,
            final AbstractAttr attribute,
            final SyncopeClientException invalidValues) {

        // if the schema is multivalue, all values are considered for
        // addition, otherwise only the fist one - if provided - is
        // considered
        List<String> valuesProvided = schema.isMultivalue()
                ? values
                : (values.isEmpty()
                ? Collections.EMPTY_LIST
                : Collections.singletonList(
                values.iterator().next()));

        for (String value : valuesProvided) {
            if (value == null || value.isEmpty()) {
                LOG.debug("Null value for {}, ignoring", schema.getName());
            } else {
                try {
                    attribute.addValue(value, attributableUtil);
                } catch (ValidationException e) {
                    LOG.error("Invalid value for attribute "
                            + schema.getName() + ": " + value, e);

                    invalidValues.addElement(schema.getName() + ": " + value);
                }
            }
        }
    }

    private boolean evaluateMandatoryCondition(
            final String mandatoryCondition,
            final List<? extends AbstractAttr> attributes) {

        JexlContext jexlContext = new MapContext();
        jexlUtil.addAttributesToContext(attributes, jexlContext);

        return Boolean.parseBoolean(
                jexlUtil.evaluate(
                mandatoryCondition, jexlContext));
    }

    private boolean evaluateMandatoryCondition(
            final ExternalResource resource,
            final List<? extends AbstractAttr> attributes,
            final String intAttrName,
            final AttributableUtil attributableUtil) {

        List<SchemaMapping> mappings = resource.getMappings(intAttrName,
                attributableUtil.intMappingType());

        boolean result = false;

        SchemaMapping mapping;
        for (Iterator<SchemaMapping> itor = mappings.iterator();
                itor.hasNext() && !result;) {

            mapping = itor.next();
            result |= evaluateMandatoryCondition(
                    mapping.getMandatoryCondition(),
                    attributes);
        }

        return result;
    }

    private boolean evaluateMandatoryCondition(
            final Set<ExternalResource> resources,
            final List<? extends AbstractAttr> attributes,
            final String intAttrName,
            final AttributableUtil attributableUtil) {

        boolean result = false;

        ExternalResource resource;
        for (Iterator<ExternalResource> itor = resources.iterator();
                itor.hasNext() && !result;) {

            resource = itor.next();
            if (resource.isForceMandatoryConstraint()) {
                result |= evaluateMandatoryCondition(resource,
                        attributes, intAttrName, attributableUtil);
            }
        }

        return result;
    }

    private SyncopeClientException checkMandatory(
            final AttributableUtil attributableUtil,
            final AbstractAttributable attributable) {

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        LOG.debug("Check mandatory constraint among resources {}",
                attributable.getExternalResources());
        // Check if there is some mandatory schema defined for which no value
        // has been provided
        List<AbstractSchema> allSchemas =
                schemaDAO.findAll(attributableUtil.schemaClass());

        for (AbstractSchema schema : allSchemas) {
            if (attributable.getAttribute(schema.getName()) == null
                    && !schema.isReadonly()
                    && (evaluateMandatoryCondition(
                    schema.getMandatoryCondition(),
                    attributable.getAttributes())
                    || evaluateMandatoryCondition(
                    attributable.getExternalResources(),
                    attributable.getAttributes(),
                    schema.getName(),
                    attributableUtil))) {

                LOG.error("Mandatory schema " + schema.getName()
                        + " not provided with values");

                requiredValuesMissing.addElement(schema.getName());
            }
        }

        return requiredValuesMissing;
    }

    public PropagationByResource fillVirtual(
            final AbstractAttributable attributable,
            final Set<String> vAttrsToBeRemoved,
            final Set<String> vAttrsToBeAdded,
            final AttributableUtil attributableUtil) {

        PropagationByResource propByRes = new PropagationByResource();

        AbstractVirSchema virtualSchema;
        AbstractVirAttr virtualAttribute;

        // 1. virtual attributes to be removed
        for (String vAttrToBeRemoved : vAttrsToBeRemoved) {
            virtualSchema = getVirtualSchema(vAttrToBeRemoved,
                    attributableUtil.virtualSchemaClass());

            if (virtualSchema != null) {
                virtualAttribute = attributable.getVirtualAttribute(
                        virtualSchema.getName());

                if (virtualAttribute == null) {
                    LOG.debug("No virtual attribute found for schema {}",
                            virtualSchema.getName());
                } else {
                    virAttrDAO.delete(virtualAttribute);
                }

                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (virtualSchema.getName().equals(
                            mapping.getIntAttrName())
                            && mapping.getIntMappingType()
                            == attributableUtil.virtualIntMappingType()
                            && mapping.getResource() != null
                            && attributable.getExternalResources().
                            contains(mapping.getResource())) {

                        propByRes.add(PropagationOperation.UPDATE,
                                mapping.getResource());

                        if (mapping.isAccountid() && virtualAttribute != null
                                && !virtualAttribute.getValues().isEmpty()) {

                            propByRes.addOldAccountId(
                                    mapping.getResource().getName(),
                                    virtualAttribute.getValues().get(0));
                        }
                    }
                }
            }
        }

        LOG.debug("Virtual attributes to be removed:\n{}", propByRes);

        // 2. virtual attributes to be added
        for (String vAttrToBeAdded : vAttrsToBeAdded) {
            virtualSchema = getVirtualSchema(vAttrToBeAdded,
                    attributableUtil.virtualSchemaClass());

            if (virtualSchema != null) {
                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (virtualSchema.getName().equals(
                            mapping.getIntAttrName())
                            && mapping.getIntMappingType()
                            == attributableUtil.virtualIntMappingType()
                            && mapping.getResource() != null
                            && attributable.getExternalResources().
                            contains(mapping.getResource())) {

                        propByRes.add(PropagationOperation.UPDATE,
                                mapping.getResource());
                    }
                }

                virtualAttribute = attributableUtil.newVirtualAttribute();
                virtualAttribute.setVirtualSchema(virtualSchema);
                virtualAttribute.setOwner(attributable);
                attributable.addVirtualAttribute(virtualAttribute);
            }
        }

        LOG.debug("Virtual attributes to be added:\n{}", propByRes);

        return propByRes;
    }

    protected PropagationByResource fill(
            final AbstractAttributable attributable,
            final AbstractAttributableMod attributableMod,
            final AttributableUtil attributableUtil,
            final SyncopeClientCompositeErrorException compositeErrorException)
            throws SyncopeClientCompositeErrorException {

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientException invalidValues = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidValues);

        // 1. resources to be removed
        ExternalResource resource;
        for (String resourceToBeRemoved :
                attributableMod.getResourcesToBeRemoved()) {

            resource = getResource(resourceToBeRemoved);

            if (resource != null) {
                propByRes.add(PropagationOperation.DELETE, resource);

                attributable.removeExternalResource(resource);

                if (attributableUtil == attributableUtil.USER) {
                    resource.removeUser((SyncopeUser) attributable);
                }
                if (attributableUtil == attributableUtil.ROLE) {
                    resource.removeRole((SyncopeRole) attributable);
                }
            }
        }

        LOG.debug("Resources to be removed:\n{}", propByRes);

        // 2. resources to be added
        for (String resourceToBeAdded :
                attributableMod.getResourcesToBeAdded()) {

            resource = getResource(resourceToBeAdded);

            if (resource != null) {
                propByRes.add(PropagationOperation.CREATE, resource);

                attributable.addExternalResource(resource);

                if (attributableUtil == attributableUtil.USER) {
                    resource.addUser((SyncopeUser) attributable);
                }
                if (attributableUtil == attributableUtil.ROLE) {
                    resource.addRole((SyncopeRole) attributable);
                }
            }
        }

        LOG.debug("Resources to be added:\n{}", propByRes);

        AbstractSchema schema;
        AbstractAttr attribute;
        AbstractDerSchema derivedSchema;
        AbstractDerAttr derivedAttribute;

        // 3. attributes to be removed
        for (String attributeToBeRemoved :
                attributableMod.getAttributesToBeRemoved()) {

            schema = getSchema(
                    attributeToBeRemoved, attributableUtil.schemaClass());

            if (schema != null) {
                attribute = attributable.getAttribute(schema.getName());
                if (attribute == null) {
                    LOG.debug("No attribute found for schema {}", schema);
                } else {
                    String newValue = null;
                    for (AttributeMod mod : attributableMod.
                            getAttributesToBeUpdated()) {

                        if (schema.getName().equals(mod.getSchema())) {
                            newValue = mod.getValuesToBeAdded().
                                    iterator().next();
                        }
                    }

                    if (!schema.isUniqueConstraint()
                            || (!attribute.getUniqueValue().getStringValue().
                            equals(newValue))) {

                        attributable.removeAttribute(attribute);
                        attributeDAO.delete(attribute.getId(),
                                attributableUtil.attributeClass());
                    }
                }

                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (schema.getName().equals(mapping.getIntAttrName())
                            && mapping.getIntMappingType()
                            == attributableUtil.intMappingType()
                            && mapping.getResource() != null
                            && attributable.getExternalResources().
                            contains(mapping.getResource())) {

                        propByRes.add(PropagationOperation.UPDATE,
                                mapping.getResource());

                        if (mapping.isAccountid() && attribute != null
                                && !attribute.getValuesAsStrings().isEmpty()) {

                            propByRes.addOldAccountId(
                                    mapping.getResource().getName(),
                                    attribute.getValuesAsStrings().
                                    iterator().next());
                        }
                    }
                }
            }
        }

        LOG.debug("Attributes to be removed:\n{}", propByRes);

        // 4. attributes to be updated
        Set<Long> valuesToBeRemoved;
        List<String> valuesToBeAdded;
        for (AttributeMod attributeMod :
                attributableMod.getAttributesToBeUpdated()) {

            schema = getSchema(attributeMod.getSchema(),
                    attributableUtil.schemaClass());

            if (schema != null) {
                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (schema.getName().equals(mapping.getIntAttrName())
                            && mapping.getIntMappingType()
                            == attributableUtil.intMappingType()
                            && mapping.getResource() != null
                            && attributable.getExternalResources().
                            contains(mapping.getResource())) {

                        propByRes.add(PropagationOperation.UPDATE,
                                mapping.getResource());
                    }
                }

                attribute = attributable.getAttribute(schema.getName());
                if (attribute == null) {
                    attribute = attributableUtil.newAttribute();
                    attribute.setSchema(schema);
                    attribute.setOwner(attributable);

                    attributable.addAttribute(attribute);
                }

                // 1.1 remove values
                valuesToBeRemoved = new HashSet<Long>();
                for (String valueToBeRemoved :
                        attributeMod.getValuesToBeRemoved()) {

                    if (attribute.getSchema().isUniqueConstraint()) {
                        if (valueToBeRemoved.equals(attribute.getUniqueValue().
                                getValueAsString())) {

                            valuesToBeRemoved.add(
                                    attribute.getUniqueValue().getId());
                        }
                    } else {
                        for (AbstractAttrValue mav : attribute.getValues()) {
                            if (valueToBeRemoved.equals(
                                    mav.getValueAsString())) {

                                valuesToBeRemoved.add(mav.getId());
                            }
                        }
                    }
                }
                for (Long attributeValueId : valuesToBeRemoved) {
                    attributeValueDAO.delete(attributeValueId,
                            attributableUtil.attributeValueClass());
                }

                // 1.2 add values
                valuesToBeAdded = attributeMod.getValuesToBeAdded();
                if (valuesToBeAdded != null && !valuesToBeAdded.isEmpty()
                        && (!schema.isUniqueConstraint()
                        || attribute.getUniqueValue() == null
                        || !valuesToBeAdded.iterator().next().equals(
                        attribute.getUniqueValue().getValueAsString()))) {

                    fillAttribute(attributeMod.getValuesToBeAdded(),
                            attributableUtil, schema, attribute,
                            invalidValues);
                }

                // if no values are in, the attribute can be safely removed
                if (attribute.getValuesAsStrings().isEmpty()) {
                    attributeDAO.delete(attribute);
                }
            }
        }

        if (!invalidValues.getElements().isEmpty()) {
            compositeErrorException.addException(invalidValues);
        }

        LOG.debug("Attributes to be updated:\n{}", propByRes);

        // 5. derived attributes to be removed
        for (String derivedAttributeToBeRemoved :
                attributableMod.getDerivedAttributesToBeRemoved()) {

            derivedSchema = getDerivedSchema(derivedAttributeToBeRemoved,
                    attributableUtil.derivedSchemaClass());

            if (derivedSchema != null) {

                derivedAttribute = attributable.getDerivedAttribute(
                        derivedSchema.getName());

                if (derivedAttribute == null) {
                    LOG.debug("No derived attribute found for schema {}",
                            derivedSchema.getName());
                } else {
                    derAttrDAO.delete(derivedAttribute);
                }

                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (derivedSchema.getName().equals(
                            mapping.getIntAttrName())
                            && mapping.getIntMappingType()
                            == attributableUtil.derivedIntMappingType()
                            && mapping.getResource() != null
                            && attributable.getExternalResources().
                            contains(mapping.getResource())) {

                        propByRes.add(PropagationOperation.UPDATE,
                                mapping.getResource());

                        if (mapping.isAccountid() && derivedAttribute != null
                                && !derivedAttribute.getValue(
                                attributable.getAttributes()).isEmpty()) {

                            propByRes.addOldAccountId(
                                    mapping.getResource().getName(),
                                    derivedAttribute.getValue(
                                    attributable.getAttributes()));
                        }
                    }
                }
            }
        }

        LOG.debug("Derived attributes to be removed:\n{}",
                propByRes);

        // 6. derived attributes to be added
        for (String derivedAttributeToBeAdded :
                attributableMod.getDerivedAttributesToBeAdded()) {

            derivedSchema = getDerivedSchema(derivedAttributeToBeAdded,
                    attributableUtil.derivedSchemaClass());

            if (derivedSchema != null) {
                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (derivedSchema.getName().equals(
                            mapping.getIntAttrName())
                            && mapping.getIntMappingType()
                            == attributableUtil.derivedIntMappingType()
                            && mapping.getResource() != null
                            && attributable.getExternalResources().
                            contains(mapping.getResource())) {

                        propByRes.add(PropagationOperation.UPDATE,
                                mapping.getResource());
                    }
                }

                derivedAttribute = attributableUtil.newDerivedAttribute();
                derivedAttribute.setDerivedSchema(derivedSchema);
                derivedAttribute.setOwner(attributable);
                attributable.addDerivedAttribute(derivedAttribute);
            }
        }

        LOG.debug("Derived attributes to be added:\n{}",
                propByRes);

        // 7. virtual attributes: for users this is delegated to 
        // PropagationManager
        if (AttributableUtil.USER != attributableUtil) {
            fillVirtual(attributable,
                    attributableMod.getVirtualAttributesToBeRemoved(),
                    attributableMod.getVirtualAttributesToBeAdded(),
                    attributableUtil);
        }

        // Finally, check if mandatory values are missing
        SyncopeClientException requiredValuesMissing =
                checkMandatory(attributableUtil, attributable);
        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return propByRes;
    }

    public void fillVirtual(final AbstractAttributable attributable,
            final List<AttributeTO> vAttrs,
            final AttributableUtil attributableUtil) {

        AbstractVirSchema virtualSchema;
        AbstractVirAttr virtualAttribute;
        for (AttributeTO attributeTO : vAttrs) {
            virtualSchema = getVirtualSchema(attributeTO.getSchema(),
                    attributableUtil.virtualSchemaClass());

            if (virtualSchema != null) {
                virtualAttribute = attributableUtil.newVirtualAttribute();
                virtualAttribute.setVirtualSchema(virtualSchema);
                virtualAttribute.setOwner(attributable);
                virtualAttribute.setValues(attributeTO.getValues());
                attributable.addVirtualAttribute(virtualAttribute);
            }
        }
    }

    protected void fill(final AbstractAttributable attributable,
            final AbstractAttributableTO attributableTO,
            final AttributableUtil attributableUtil,
            final SyncopeClientCompositeErrorException compositeErrorException)
            throws SyncopeClientCompositeErrorException {

        // 1. attributes
        SyncopeClientException invalidValues = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidValues);

        AbstractSchema schema;
        AbstractAttr attribute;

        // Only consider attributeTO with values
        for (AttributeTO attributeTO : attributableTO.getAttributes()) {
            if (attributeTO.getValues() != null
                    && !attributeTO.getValues().isEmpty()) {

                schema = getSchema(attributeTO.getSchema(),
                        attributableUtil.schemaClass());

                if (schema != null) {
                    attribute = attributable.getAttribute(schema.getName());
                    if (attribute == null) {
                        attribute = attributableUtil.newAttribute();
                        attribute.setSchema(schema);
                    }

                    fillAttribute(attributeTO.getValues(),
                            attributableUtil,
                            schema,
                            attribute,
                            invalidValues);

                    if (!attribute.getValuesAsStrings().isEmpty()) {
                        attributable.addAttribute(attribute);
                        attribute.setOwner(attributable);
                    }
                }
            }
        }

        if (!invalidValues.getElements().isEmpty()) {
            compositeErrorException.addException(invalidValues);
        }

        SyncopeClientException requiredValuesMissing =
                checkMandatory(attributableUtil, attributable);
        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        // 2. derived attributes
        AbstractDerSchema derivedSchema;
        AbstractDerAttr derivedAttribute;
        for (AttributeTO attributeTO : attributableTO.getDerivedAttributes()) {

            derivedSchema = getDerivedSchema(attributeTO.getSchema(),
                    attributableUtil.derivedSchemaClass());

            if (derivedSchema != null) {
                derivedAttribute = attributableUtil.newDerivedAttribute();
                derivedAttribute.setDerivedSchema(derivedSchema);
                derivedAttribute.setOwner(attributable);
                attributable.addDerivedAttribute(derivedAttribute);
            }
        }

        // 3. virtual attributes: for users this is delegated to 
        // PropagationManager
        if (AttributableUtil.USER != attributableUtil) {
            fillVirtual(attributable, attributableTO.getVirtualAttributes(),
                    attributableUtil);
        }

        // 4. resources
        ExternalResource resource;
        for (String resourceName : attributableTO.getResources()) {
            resource = getResource(resourceName);

            if (resource != null) {
                attributable.addExternalResource(resource);

                switch (attributableUtil) {
                    case USER:
                        resource.addUser((SyncopeUser) attributable);
                        break;

                    case ROLE:
                        resource.addRole((SyncopeRole) attributable);
                        break;

                    default:
                }
            }
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }
    }

    public void fillTO(
            final AbstractAttributableTO abstractAttributableTO,
            final Collection<? extends AbstractAttr> attributes,
            final Collection<? extends AbstractDerAttr> derivedAttributes,
            final Collection<? extends AbstractVirAttr> virtualAttributes,
            final Collection<ExternalResource> resources) {

        AttributeTO attributeTO;
        for (AbstractAttr attribute : attributes) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(attribute.getSchema().getName());
            attributeTO.setValues(attribute.getValuesAsStrings());
            attributeTO.setReadonly(attribute.getSchema().isReadonly());

            abstractAttributableTO.addAttribute(attributeTO);
        }

        for (AbstractDerAttr derivedAttribute : derivedAttributes) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(
                    derivedAttribute.getDerivedSchema().getName());
            attributeTO.addValue(derivedAttribute.getValue(attributes));
            attributeTO.setReadonly(true);

            abstractAttributableTO.addDerivedAttribute(attributeTO);
        }

        for (AbstractVirAttr virtualAttribute : virtualAttributes) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(
                    virtualAttribute.getVirtualSchema().getName());
            attributeTO.setValues(virtualAttribute.getValues());
            attributeTO.setReadonly(false);

            abstractAttributableTO.addVirtualAttribute(attributeTO);
        }

        for (ExternalResource resource : resources) {
            abstractAttributableTO.addResource(resource.getName());
        }
    }
}
