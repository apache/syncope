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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.to.AttrRepoTO;

@FunctionalInterface
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "_class")
public interface AttrRepoConf extends BaseBean {

    interface Mapper {

        Map<String, Object> map(AttrRepoTO attrRepo, StubAttrRepoConf conf);

        Map<String, Object> map(AttrRepoTO attrRepo, LDAPAttrRepoConf conf);

        Map<String, Object> map(AttrRepoTO attrRepo, JDBCAttrRepoConf conf);

        Map<String, Object> map(AttrRepoTO attrRepo, SyncopeAttrRepoConf conf);

        Map<String, Object> map(AttrRepoTO attrRepo, AzureActiveDirectoryAttrRepoConf conf);

        Map<String, Object> map(AttrRepoTO attrRepo, OktaAttrRepoConf conf);
    }

    Map<String, Object> map(AttrRepoTO attrRepo, Mapper mapper);
}
