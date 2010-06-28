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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.SyncopeUserDAO;

@Component
public class UserDataBinder {

    private static final Logger log = LoggerFactory.getLogger(
            UserDataBinder.class);
    private static final String[] ignoreSchemaProperties = {"attributes",
        "derivedAttributes", "roles"};
    private SyncopeUserDAO syncopeUserDAO;

    @Autowired
    public UserDataBinder(SyncopeUserDAO syncopeUserDAO) {

        this.syncopeUserDAO = syncopeUserDAO;
    }

    public SyncopeUser createSyncopeUser(UserTO userTO) {

        SyncopeUser user = new SyncopeUser();
        //BeanUtils.copyProperties(userTO, user, ignoreSchemaProperties);

        return syncopeUserDAO.save(user);
    }

    public UserTO getUserTO(SyncopeUser user) {
        UserTO userTO = new UserTO();
        BeanUtils.copyProperties(user, userTO, ignoreSchemaProperties);

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
