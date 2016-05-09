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
import org.apache.syncope.common.lib.to.ExecTO;

public interface ExecutionRestClient extends RestClient {

    void startExecution(String executionCollectorKey, Date start);

    void deleteExecution(String executionKey);

    List<ExecTO> listRecentExecutions(final int max);

    List<ExecTO> listExecutions(final String taskKey, final int page, final int size);

    int countExecutions(final String taskKey);
}
