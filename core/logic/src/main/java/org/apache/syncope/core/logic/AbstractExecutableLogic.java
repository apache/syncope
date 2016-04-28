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
package org.apache.syncope.core.logic;

import java.util.Date;
import java.util.List;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;

public abstract class AbstractExecutableLogic<T extends AbstractBaseBean> extends AbstractJobLogic<T> {

    public abstract ExecTO execute(String key, Date startAt, boolean dryRun);

    public abstract int countExecutions(String key);

    public abstract List<ExecTO> listExecutions(String key, int page, int size, List<OrderByClause> orderByClauses);

    public abstract List<ExecTO> listRecentExecutions(int max);

    public abstract ExecTO deleteExecution(String executionKey);

    public abstract BulkActionResult deleteExecutions(
            String key, Date startedBefore, Date startedAfter, Date endedBefore, Date endedAfter);

    public abstract List<JobTO> listJobs();

    public abstract void actionJob(String key, JobAction action);
}
