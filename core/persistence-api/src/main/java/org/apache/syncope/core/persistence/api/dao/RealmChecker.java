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
package org.apache.syncope.core.persistence.api.dao;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class RealmChecker {

    protected static final Logger LOG = LoggerFactory.getLogger(RealmChecker.class);

    protected final PlainSchemaDAO plainSchemaDAO;

    public RealmChecker(final PlainSchemaDAO plainSchemaDAO) {
        this.plainSchemaDAO = plainSchemaDAO;
    }

    @Transactional(readOnly = true)
    public void checkBeforeSave(final Realm realm) {
        Set<String> allowed = realm.getAnyTypeClasses().stream().
                flatMap(atc -> atc.getPlainSchemas().stream()).map(PlainSchema::getKey).collect(Collectors.toSet());

        for (PlainAttr attr : realm.getPlainAttrs()) {
            String schema = Optional.ofNullable(attr).map(PlainAttr::getSchema).orElse(null);
            if (schema != null && !allowed.contains(schema)) {
                throw new InvalidEntityException(
                        realm.getClass(),
                        EntityViolationType.InvalidPlainAttr.propertyPath("plainAttrs"),
                        schema + " not allowed for this instance");
            }
        }

        // check UNIQUE constraints
        realm.getPlainAttrs().stream().
                filter(attr -> attr.getUniqueValue() != null).
                forEach(attr -> {
                    if (plainSchemaDAO.existsPlainAttrUniqueValue(
                            realm.getKey(),
                            plainSchemaDAO.findById(attr.getSchema()).
                                    orElseThrow(() -> new NotFoundException("PlainSchema " + attr.getSchema())),
                            attr.getUniqueValue())) {

                        throw new DuplicateException("Duplicate value found for "
                                + attr.getSchema() + "=" + attr.getUniqueValue().getValueAsString());
                    } else {
                        LOG.debug("No duplicate value found for {}={}",
                                attr.getSchema(), attr.getUniqueValue().getValueAsString());
                    }
                });
    }
}
