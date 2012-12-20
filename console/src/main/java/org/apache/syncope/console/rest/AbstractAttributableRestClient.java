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
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.AbstractAttributableTO;
import org.apache.syncope.client.to.ConnObjectTO;

public abstract class AbstractAttributableRestClient extends BaseRestClient {

    public abstract Integer count();

    public abstract List<? extends AbstractAttributableTO> list(int page, int size);

    public abstract Integer searchCount(NodeCond searchCond);

    public abstract List<? extends AbstractAttributableTO> search(NodeCond searchCond, int page, int size);

    public abstract ConnObjectTO getRemoteObject(String resourceName, String objectId);

    public abstract AbstractAttributableTO delete(Long id);
}
