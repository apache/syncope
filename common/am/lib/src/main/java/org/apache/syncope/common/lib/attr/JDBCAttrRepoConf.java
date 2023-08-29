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
package org.apache.syncope.common.lib.attr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.AbstractJDBCConf;
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apache.syncope.common.lib.types.CaseCanonicalizationMode;

public class JDBCAttrRepoConf extends AbstractJDBCConf implements AttrRepoConf {

    private static final long serialVersionUID = -4474060002361453868L;

    public enum QueryType {
        AND,
        OR

    }

    /**
     * Designed to work against a table where there is a mapping of one row to one user.
     */
    private boolean singleRow = true;

    /**
     * If the SQL should only be run if all attributes listed in the mappings exist in the query.
     */
    private boolean requireAllAttributes = true;

    /**
     * When constructing the final person object from the attribute repository,
     * indicate how the username should be canonicalized.
     */
    private CaseCanonicalizationMode caseCanonicalization = CaseCanonicalizationMode.NONE;

    /**
     * Indicates how multiple attributes in a query should be concatenated together.
     */
    private QueryType queryType = QueryType.AND;

    /**
     * Used only when there is a mapping of many rows to one user.
     * This is done using a key-value structure where the key is the
     * name of the "attribute name" column the value is the name of the "attribute value" column.
     */
    private final Map<String, String> columnMappings = new HashMap<>(0);

    /**
     * Username attribute(s) to use when running the SQL query.
     */
    private final List<String> username = new ArrayList<>(0);

    /**
     * Collection of attributes, used to build the SQL query, that should go through
     * a case canonicalization process defined as {@code key->value}.
     */
    private final List<String> caseInsensitiveQueryAttributes = new ArrayList<>(0);

    /**
     * Define a {@code Map} of query attribute names to data-layer attribute names to use when building the query.
     * The key is always the name of the query attribute that is defined by CAS and passed internally,
     * and the value is the database column that should map.
     */
    private final Map<String, String> queryAttributes = new HashMap<>(0);

    public boolean isSingleRow() {
        return singleRow;
    }

    public void setSingleRow(final boolean singleRow) {
        this.singleRow = singleRow;
    }

    public boolean isRequireAllAttributes() {
        return requireAllAttributes;
    }

    public void setRequireAllAttributes(final boolean requireAllAttributes) {
        this.requireAllAttributes = requireAllAttributes;
    }

    public CaseCanonicalizationMode getCaseCanonicalization() {
        return caseCanonicalization;
    }

    public void setCaseCanonicalization(final CaseCanonicalizationMode caseCanonicalization) {
        this.caseCanonicalization = caseCanonicalization;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public void setQueryType(final QueryType queryType) {
        this.queryType = queryType;
    }

    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }

    public List<String> getUsername() {
        return username;
    }

    public List<String> getCaseInsensitiveQueryAttributes() {
        return caseInsensitiveQueryAttributes;
    }

    public Map<String, String> getQueryAttributes() {
        return queryAttributes;
    }

    @Override
    public Map<String, Object> map(final AttrRepoTO attrRepo, final Mapper mapper) {
        return mapper.map(attrRepo, this);
    }
}
