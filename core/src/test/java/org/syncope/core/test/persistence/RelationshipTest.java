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
package org.syncope.core.test.persistence;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.user.UserAttribute;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractAttributeValue;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.beans.user.UserDerivedSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.AttributeValueDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;

/**
 * TODO: complete refactor for http://code.google.com/p/syncope/issues/detail?id=7
 */
@Transactional
public class RelationshipTest extends AbstractTest {

    @Autowired
    SchemaDAO attributeSchemaDAO;
    @Autowired
    AttributeDAO attributeDAO;
    @Autowired
    AttributeValueDAO attributeValueDAO;
    @Autowired
    DerivedSchemaDAO derivedAttributeSchemaDAO;
    private Set<Long> loginDateAttributeIds;
    private Set<Long> allLoginDateAttributeValueIds;
    private Set<String> derivedAttributeSchemaNames;
    private Set<String> attributeSchemaNames;

    @Test
    @Rollback(false)
    public final void prepare() {
        if (true) {
            return;
        }

        // 1. AttributeSchema <-> Attribute <-> AttributeValue
        {
            UserSchema loginDateSchema =
                    attributeSchemaDAO.find("loginDate", UserSchema.class);
            assertNotNull(loginDateSchema);

            loginDateAttributeIds = new HashSet<Long>();
            allLoginDateAttributeValueIds = new HashSet<Long>();
            for (UserAttribute loginDateAttribute :
                    (Set<UserAttribute>) loginDateSchema.getAttributes()) {

                loginDateAttributeIds.add(loginDateAttribute.getId());

                for (AbstractAttributeValue loginDateAttributeValue :
                        loginDateAttribute.getAttributeValues()) {

                    allLoginDateAttributeValueIds.add(
                            loginDateAttributeValue.getId());
                }
            }

            attributeSchemaDAO.delete("loginDate", UserSchema.class);
        }

        // 2. AttributeSchema <-> DerivedAttributeSchema
        {
            UserSchema surnameSchema = attributeSchemaDAO.find("surname",
                    UserSchema.class);
            assertNotNull(surnameSchema);

            derivedAttributeSchemaNames = new HashSet<String>();
            for (AbstractDerivedSchema derivedAttributeSchema :
                    surnameSchema.getDerivedSchemas()) {

                derivedAttributeSchemaNames.add(
                        derivedAttributeSchema.getName());
            }

            attributeSchemaDAO.delete("surname", UserSchema.class);
        }

        // 3. DerivedAttributeSchema <-> AttributeSchema
        /*{
        DerivedAttributeSchema alternativeCNSchema =
        derivedAttributeSchemaDAO.find("icon2");
        assertNotNull(alternativeCNSchema);

        attributeSchemaNames = new HashSet<String>();
        for (AttributeSchema attributeSchema :
        alternativeCNSchema.getAttributeSchemas()) {

        attributeSchemaNames.add(attributeSchema.getName());
        }

        derivedAttributeSchemaDAO.delete("icon2");
        }*/
    }

    @AfterTransaction
    public final void verify() {
        if (true) {
            return;
        }

        // 1. AttributeSchema <-> Attribute <-> AttributeValue
        {
            assertNotNull(loginDateAttributeIds);
            assertNotNull(allLoginDateAttributeValueIds);
            assertNull(attributeSchemaDAO.find("loginDate", UserSchema.class));

            for (Long loginDateAttribute : loginDateAttributeIds) {
                assertNull(attributeDAO.find(loginDateAttribute,
                        UserAttribute.class));
            }
            for (Long attributeValue : allLoginDateAttributeValueIds) {
                assertNull(attributeValueDAO.find(attributeValue,
                        UserAttributeValue.class));
            }
        }

        // 2. AttributeSchema <-> DerivedAttributeSchema
        {
            assertNotNull(derivedAttributeSchemaNames);
            assertNull(attributeSchemaDAO.find("surname", UserSchema.class));

            AbstractDerivedSchema derivedAttributeSchema = null;
            for (String derivedAttributeSchemaName :
                    derivedAttributeSchemaNames) {

                derivedAttributeSchema =
                        derivedAttributeSchemaDAO.find(
                        derivedAttributeSchemaName, UserDerivedSchema.class);

                for (AbstractSchema attributeSchema :
                        derivedAttributeSchema.getSchemas()) {

                    assertTrue(!"surname".equals(attributeSchema.getName()));
                }
            }
        }

        // 3. DerivedAttributeSchema <-> AttributeSchema
        /*{
        assertNotNull(attributeSchemaNames);
        assertNull(derivedAttributeSchemaDAO.find("icon2"));

        AttributeSchema attributeSchema = null;
        for (String attributeSchemaName : attributeSchemaNames) {
        attributeSchema = attributeSchemaDAO.find(attributeSchemaName);

        for (DerivedAttributeSchema derivedAttributeSchema :
        attributeSchema.getDerivedAttributeSchemas()) {

        assertTrue(!"icon2".equals(
        derivedAttributeSchema.getName()));
        }
        }
        }*/
    }
}
