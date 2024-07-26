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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;

public class MaJPAJSONAnyDAO extends AbstractJPAJSONAnyDAO {

    public MaJPAJSONAnyDAO(final PlainSchemaDAO plainSchemaDAO) {
        super(plainSchemaDAO);
    }

    @Override
    protected String queryBegin(final String table) {
        throw new UnsupportedOperationException("This method shall never be called");
    }

    @Override
    protected String attrValueMatch(
            final AnyUtils anyUtils,
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final boolean ignoreCaseMatch) {

        throw new UnsupportedOperationException("This method shall never be called");
    }

    @Override
    protected String plainAttrQuery(
            final String table,
            final AnyUtils anyUtils,
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final boolean ignoreCaseMatch,
            final List<Object> queryParams) {

        queryParams.add(schema.getKey());
        queryParams.add(attrValue.getStringValue());
        queryParams.add(attrValue.getBooleanValue());
        queryParams.add(Optional.ofNullable(attrValue.getDateValue()).
                map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format).orElse(null));
        queryParams.add(attrValue.getLongValue());
        queryParams.add(attrValue.getDoubleValue());

        SearchViewSupport svs = new SearchViewSupport(anyUtils.anyTypeKind());
        return "SELECT DISTINCT any_id FROM "
                + (schema.isUniqueConstraint() ? svs.uniqueAttr().name : svs.attr().name)
                + " WHERE schema_id = ? AND ((stringValue IS NOT NULL"
                + " AND "
                + (ignoreCaseMatch ? "LOWER(" : "BINARY ") + "stringValue" + (ignoreCaseMatch ? ")" : "")
                + " = "
                + (ignoreCaseMatch ? "LOWER(" : "") + "?" + (ignoreCaseMatch ? ")" : "") + ')'
                + " OR (booleanValue IS NOT NULL AND booleanValue = ?)"
                + " OR (dateValue IS NOT NULL AND dateValue = ?)"
                + " OR (longValue IS NOT NULL AND longValue = ?)"
                + " OR (doubleValue IS NOT NULL AND doubleValue = ?))";
    }
}
