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

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;

abstract class AbstractJPAJSONAnySearchDAO extends JPAAnySearchDAO {

    protected static final FastDateFormat DATE_FORMAT =
            FastDateFormat.getInstance(SyncopeConstants.DEFAULT_DATE_PATTERN);

    @Override
    SearchSupport buildSearchSupport(final AnyTypeKind kind) {
        return new SearchSupport(kind);
    }

    protected void appendOp(final StringBuilder query, final AttributeCond.Type condType, final boolean not) {
        switch (condType) {
            case LIKE:
            case ILIKE:
                if (not) {
                    query.append("NOT ");
                }
                query.append(" LIKE ");
                break;

            case GE:
                if (not) {
                    query.append('<');
                } else {
                    query.append(">=");
                }
                break;

            case GT:
                if (not) {
                    query.append("<=");
                } else {
                    query.append('>');
                }
                break;

            case LE:
                if (not) {
                    query.append('>');
                } else {
                    query.append("<=");
                }
                break;

            case LT:
                if (not) {
                    query.append(">=");
                } else {
                    query.append('<');
                }
                break;

            case EQ:
            case IEQ:
            default:
                if (not) {
                    query.append('!');
                }
                query.append('=');
        }
    }
}
