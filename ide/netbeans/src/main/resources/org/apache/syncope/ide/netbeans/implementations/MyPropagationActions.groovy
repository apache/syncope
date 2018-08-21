
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
import groovy.transform.CompileStatic
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask
import org.apache.syncope.core.persistence.api.entity.task.TaskExec
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions
import org.identityconnectors.framework.common.objects.ConnectorObject

@CompileStatic
class MyPropagationActions implements PropagationActions {
  
  @Override
  void before(PropagationTask task, ConnectorObject beforeObj) {
    // do nothing
  }

  @Override
  void onError(PropagationTask task, TaskExec execution, Exception error) {
    // do nothing
  }

  @Override
  void after(PropagationTask task, TaskExec execution, ConnectorObject afterObj) {
    // do nothing
  }
}
