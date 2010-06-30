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
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttribute;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.validation.ValidationException;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class UserDataBinder {

    private static final Logger log = LoggerFactory.getLogger(
            UserDataBinder.class);
    private static final String[] ignoreProperties = {"attributes",
        "derivedAttributes", "roles"};
    private SyncopeUserDAO syncopeUserDAO;
    private SchemaDAO schemaDAO;

    @Autowired
    public UserDataBinder(SyncopeUserDAO syncopeUserDAO,
            SchemaDAO schemaDAO) {

        this.syncopeUserDAO = syncopeUserDAO;
        this.schemaDAO = schemaDAO;
    }

    public SyncopeUser createSyncopeUser(UserTO userTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);
        SyncopeClientException invalidSchemas = new SyncopeClientException();
        invalidSchemas.setType(SyncopeClientExceptionType.InvalidSchemas);
        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException();
        requiredValuesMissing.setType(
                SyncopeClientExceptionType.UserRequiredValuesMissing);
        SyncopeClientException invalidValues = new SyncopeClientException();
        invalidValues.setType(SyncopeClientExceptionType.UserInvalidValues);

        SyncopeUser user = new SyncopeUser();
        BeanUtils.copyProperties(userTO, user,
                (String[]) ArrayUtils.add(ignoreProperties, "id"));

        UserSchema schema = null;
        UserAttribute attribute = null;
        Set<String> valuesProvided = null;
        UserAttributeValue attributeValue = null;
        for (AttributeTO attributeTO : userTO.getAttributes()) {
            schema = schemaDAO.find(attributeTO.getSchema(), UserSchema.class);

            if (schema == null) {
                invalidSchemas.addAttributeName(attributeTO.getSchema());
            } else {
                attribute = new UserAttribute();
                attribute.setSchema(schema);
                attribute.setOwner(user);

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
                    attributeValue = new UserAttributeValue();

                    try {
                        attribute.addValue(value, attributeValue);
                    } catch (ValidationException e) {
                        log.error("Invalid value for attribute "
                                + schema.getName() + ": " + value, e);

                        invalidValues.addAttributeName(schema.getName());
                    }
                }

                if (!attribute.getAttributeValues().isEmpty()) {
                    user.addAttribute(attribute);
                }
            }
        }

        // Check if there is some mandatory schema defined for which no value
        // has been provided
        List<UserSchema> allUserSchemas = schemaDAO.findAll(UserSchema.class);
        for (UserSchema userSchema : allUserSchemas) {
            if (user.getAttribute(userSchema.getName()) == null
                    && userSchema.isMandatory()) {

                log.error("Mandatory schema " + userSchema.getName()
                        + " not provided with values");

                requiredValuesMissing.addAttributeName(userSchema.getName());
            }
        }

        // Throw composite exception if there is at least one attribute name set
        // in the composing exceptions
        if (!invalidSchemas.getAttributeNames().isEmpty()) {
            compositeErrorException.addException(invalidSchemas);
        }
        if (!requiredValuesMissing.getAttributeNames().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }
        if (!invalidValues.getAttributeNames().isEmpty()) {
            compositeErrorException.addException(invalidValues);
        }
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        // Everything went out fine, we can flush to the database
        user = syncopeUserDAO.save(user);
        syncopeUserDAO.getEntityManager().flush();
        return user;
    }

    public UserTO getUserTO(SyncopeUser user) {
        UserTO userTO = new UserTO();
        BeanUtils.copyProperties(user, userTO, ignoreProperties);

        AttributeTO attributeTO = null;
        for (AbstractAttribute attribute : user.getAttributes()) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(attribute.getSchema().getName());
            attributeTO.setValues(attribute.getStringAttributeValues());

            userTO.addAttribute(attributeTO);
        }

        for (AbstractDerivedAttribute derivedAttribute :
                user.getDerivedAttributes()) {

            attributeTO = new AttributeTO();
            attributeTO.setSchema(
                    derivedAttribute.getDerivedSchema().getName());
            attributeTO.setValues(Collections.singleton(
                    derivedAttribute.getValue(user.getAttributes())));

            userTO.addDerivedAttribute(attributeTO);
        }

        for (SyncopeRole role : user.getRoles()) {
            userTO.addRole(role.getId());
        }

        return userTO;
    }
}
