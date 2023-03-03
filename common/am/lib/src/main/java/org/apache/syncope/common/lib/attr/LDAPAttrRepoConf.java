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

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.common.lib.AbstractLDAPConf;
import org.apache.syncope.common.lib.to.AttrRepoTO;

public class LDAPAttrRepoConf extends AbstractLDAPConf implements AttrRepoConf {

    private static final long serialVersionUID = -471527731042579422L;

    /**
     * Whether all existing attributes should be passed
     * down to the query builder map and be used in the construction
     * of the filter.
     */
    private boolean useAllQueryAttributes = true;

    /**
     * Define a {@code Map} of query attribute names to data-layer attribute names to use when building the query.
     * The key is always the name of the query attribute that is defined by CAS and passed internally,
     * and the value is the column/field that should map.
     */
    private final Map<String, String> queryAttributes = new HashMap<>(0);

    public boolean isUseAllQueryAttributes() {
        return useAllQueryAttributes;
    }

    public void setUseAllQueryAttributes(final boolean useAllQueryAttributes) {
        this.useAllQueryAttributes = useAllQueryAttributes;
    }

    public Map<String, String> getQueryAttributes() {
        return queryAttributes;
    }

    @Override
    public Map<String, Object> map(final AttrRepoTO attrRepo, final Mapper mapper) {
        return mapper.map(attrRepo, this);
    }
}
