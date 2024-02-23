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
package org.apache.syncope.core.persistence.jpa.dao;

import jakarta.persistence.Entity;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.common.dao.AbstractAnyMatchDAO;

public class JPAAnyMatchDAO extends AbstractAnyMatchDAO {

    public JPAAnyMatchDAO(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final RealmDAO realmDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator,
            final EntityFactory entityFactory) {

        super(userDAO, groupDAO, anyObjectDAO, realmDAO, plainSchemaDAO, anyUtilsFactory, validator, entityFactory);
    }

    @Override
    protected void relationshipFieldMatches(
            final PropertyDescriptor pd,
            final AnyCond cond,
            final PlainSchema schema) {

        if (pd.getPropertyType().getAnnotation(Entity.class) != null) {
            Method relMethod = null;
            try {
                relMethod = ClassUtils.getPublicMethod(pd.getPropertyType(), "getKey", new Class<?>[0]);
            } catch (Exception e) {
                LOG.error("Could not find {}#getKey", pd.getPropertyType(), e);
            }

            if (relMethod != null && String.class.isAssignableFrom(relMethod.getReturnType())) {
                cond.setSchema(cond.getSchema() + "_id");
                schema.setType(AttrSchemaType.String);
            }
        }
    }
}
