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
package org.apache.syncope.console.rest;

import java.util.List;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.ConnObjectTO;

public abstract class AbstractAttributableRestClient extends BaseRestClient {

    private static final long serialVersionUID = 1962529678091410544L;

    public abstract Integer count();

    public abstract List<? extends AbstractAttributableTO> list(int page, int size);

    public abstract Integer searchCount(NodeCond searchCond) throws InvalidSearchConditionException;

    public abstract List<? extends AbstractAttributableTO> search(NodeCond searchCond, int page, int size)
            throws InvalidSearchConditionException;

    public abstract ConnObjectTO getConnectorObject(String resourceName, Long id);

    public abstract AbstractAttributableTO delete(Long id);
}
