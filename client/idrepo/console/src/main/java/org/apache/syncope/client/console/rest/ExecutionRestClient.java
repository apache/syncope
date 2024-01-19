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
package org.apache.syncope.client.console.rest;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.client.ui.commons.rest.RestClient;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

public interface ExecutionRestClient extends RestClient {

    void startExecution(String executionCollectorKey, Date startAt);

    void deleteExecution(String executionKey);

    List<ExecTO> listRecentExecutions(int max);

    List<ExecTO> listExecutions(String taskKey, int page, int size, SortParam<String> sort);

    long countExecutions(String taskKey);

    Map<String, String> batch(BatchRequest batchRequest);
}
