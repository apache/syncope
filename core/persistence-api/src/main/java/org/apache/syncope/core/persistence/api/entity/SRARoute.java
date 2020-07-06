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
package org.apache.syncope.core.persistence.api.entity;

import java.net.URI;
import java.util.List;
import org.apache.syncope.common.lib.types.SRARouteFilter;
import org.apache.syncope.common.lib.types.SRARoutePredicate;
import org.apache.syncope.common.lib.types.SRARouteType;

public interface SRARoute extends Entity {

    String getName();

    void setName(String name);

    URI getTarget();

    void setTarget(URI target);

    URI getError();

    void setError(URI error);

    SRARouteType getType();

    void setType(SRARouteType type);

    boolean isLogout();

    void setLogout(boolean logout);

    URI getPostLogout();

    void setPostLogout(URI postLogout);

    boolean isCsrf();

    void setCsrf(boolean csrf);

    int getOrder();

    void setOrder(int order);

    List<SRARouteFilter> getFilters();

    void setFilters(List<SRARouteFilter> filters);

    List<SRARoutePredicate> getPredicates();

    void setPredicates(List<SRARoutePredicate> predicates);
}
