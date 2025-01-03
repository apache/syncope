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

import java.util.Map;
import org.apache.syncope.common.lib.AbstractAzureActiveDirectoryConf;
import org.apache.syncope.common.lib.to.AttrRepoTO;

public class AzureActiveDirectoryAttrRepoConf extends AbstractAzureActiveDirectoryConf implements AttrRepoConf {

    private static final long serialVersionUID = -2365294132437794196L;

    /**
     * Whether attribute repository should consider the underlying attribute names in a case-insensitive manner.
     */
    private boolean caseInsensitive;

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    public void setCaseInsensitive(final boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    @Override
    public Map<String, Object> map(final AttrRepoTO attrRepo, final Mapper mapper) {
        return mapper.map(attrRepo, this);
    }
}
