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
package org.apache.syncope.client.console.tasks;

import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.wicket.PageReference;

/**
 * Modal window with Task form (to stop and start execution).
 */
public class SchedTaskDetails extends AbstractSchedTaskDetails<SchedTaskTO> {

    private static final long serialVersionUID = -2501860242590060867L;

    public SchedTaskDetails(final SchedTaskTO taskTO, final PageReference pageRef) {
        super(taskTO, pageRef);
    }

//    @Override
//    public void submitAction(final SchedTaskTO taskTO) {
//        if (taskTO.getId() > 0) {
//            taskRestClient.updateSchedTask(taskTO);
//        } else {
//            taskRestClient.createSchedTask(taskTO);
//        }
//    }
}
