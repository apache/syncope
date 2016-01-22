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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.provisioning.api.data.AnyTypeDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnyTypeDataBinderImpl implements AnyTypeDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(AnyTypeDataBinder.class);

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public AnyType create(final AnyTypeTO anyTypeTO) {
        AnyType anyType = entityFactory.newEntity(AnyType.class);
        update(anyType, anyTypeTO);
        return anyType;
    }

    @Override
    public void update(final AnyType anyType, final AnyTypeTO anyTypeTO) {
        if (anyType.getKey() == null) {
            anyType.setKey(anyTypeTO.getKey());
        }
        if (anyType.getKind() == null) {
            anyType.setKind(anyTypeTO.getKind());
        }
        if (anyType.getKind() != anyTypeTO.getKind()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
            sce.getElements().add(AnyTypeKind.class.getSimpleName() + " cannot be changed");
            throw sce;
        }

        anyType.getClasses().clear();
        for (String anyTypeClassName : anyTypeTO.getClasses()) {
            AnyTypeClass anyTypeClass = anyTypeClassDAO.find(anyTypeClassName);
            if (anyTypeClass == null) {
                LOG.debug("Invalid " + AnyTypeClass.class.getSimpleName() + "{}, ignoring...", anyTypeClassName);
            } else {
                anyType.add(anyTypeClass);
            }
        }
    }

    @Override
    public AnyTypeTO getAnyTypeTO(final AnyType anyType) {
        AnyTypeTO anyTypeTO = new AnyTypeTO();

        anyTypeTO.setKey(anyType.getKey());
        anyTypeTO.setKind(anyType.getKind());
        for (AnyTypeClass anyTypeClass : anyType.getClasses()) {
            anyTypeTO.getClasses().add(anyTypeClass.getKey());
        }

        return anyTypeTO;
    }

}
