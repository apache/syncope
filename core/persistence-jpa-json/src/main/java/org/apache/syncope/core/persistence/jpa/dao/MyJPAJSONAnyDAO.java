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

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.entity.JSONPlainAttr;

public class MyJPAJSONAnyDAO extends AbstractJPAJSONAnyDAO {

    public MyJPAJSONAnyDAO(final PlainSchemaDAO plainSchemaDAO) {
        super(plainSchemaDAO);
    }

    @Override
    protected String queryBegin(final String table) {
        String view = StringUtils.containsIgnoreCase(table, AnyTypeKind.USER.name())
                ? "user_search"
                : StringUtils.containsIgnoreCase(table, AnyTypeKind.GROUP.name())
                ? "group_search"
                : "anyObject_search";
        return "SELECT DISTINCT id FROM " + view + ' ';
    }

    @Override
    protected String attrValueMatch(
            final AnyUtils anyUtils,
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final boolean ignoreCaseMatch) {

        Pair<String, Boolean> schemaInfo = schemaInfo(schema.getType(), ignoreCaseMatch);
        if (schemaInfo.getRight()) {
            return "plainSchema = ? "
                    + "AND "
                    + (schemaInfo.getRight() ? "LOWER(" : "")
                    + (schema.isUniqueConstraint()
                    ? "attrUniqueValue ->> '$." + schemaInfo.getLeft() + '\''
                    : schemaInfo.getLeft())
                    + (schemaInfo.getRight() ? ")" : "")
                    + " = "
                    + (schemaInfo.getRight() ? "LOWER(" : "")
                    + '?'
                    + (schemaInfo.getRight() ? ")" : "");
        } else {
            PlainAttr<?> container = anyUtils.newPlainAttr();
            container.setSchema(schema);
            if (attrValue instanceof PlainAttrUniqueValue) {
                container.setUniqueValue((PlainAttrUniqueValue) attrValue);
            } else {
                ((JSONPlainAttr) container).add(attrValue);
            }
            return "JSON_CONTAINS(plainAttrs, '" + POJOHelper.serialize(List.of(container)).replace("'", "''") + "')";
        }
    }
}
