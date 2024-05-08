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
import java.util.Set
import org.apache.syncope.common.lib.to.ProvisioningReport
import org.apache.syncope.core.persistence.api.entity.Entity
import org.apache.syncope.core.provisioning.api.job.JobExecutionException
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile
import org.apache.syncope.core.provisioning.api.pushpull.PushActions

@CompileStatic
class MyPushActions implements PushActions {
  
  @Override
  Set<String> moreAttrsToGet(ProvisioningProfile profile, Entity entity) {
    return Set.of();
  }

  @Override 
  Entity beforeAssign(
    ProvisioningProfile profile,
    Entity entity) throws JobExecutionException {

    return entity;
  }

  @Override 
  Entity beforeProvision(
    ProvisioningProfile profile,
    Entity entity) throws JobExecutionException {

    return entity;
  }

  @Override 
  Entity beforeUpdate(
    ProvisioningProfile profile,
    Entity entity) throws JobExecutionException {

    return entity;
  }

  @Override 
  Entity beforeLink(
    ProvisioningProfile profile,
    Entity entity) throws JobExecutionException {

    return entity;
  }

  @Override 
  Entity beforeUnlink(
    ProvisioningProfile profile,
    Entity entity) throws JobExecutionException {

    return entity;
  }

  @Override 
  Entity beforeUnassign(
    ProvisioningProfile profile,
    Entity entity) throws JobExecutionException {

    return entity;
  }

  @Override 
  Entity beforeDeprovision(
    ProvisioningProfile profile,
    Entity entity) throws JobExecutionException {

    return entity;
  }

  @Override 
  Entity beforeDelete(
    ProvisioningProfile profile,
    Entity entity) throws JobExecutionException {

    return entity;
  }

  @Override 
  void onError(
    ProvisioningProfile profile,
    Entity entity,
    ProvisioningReport result,
    Exception error) throws JobExecutionException {

    // do nothing
  }

  @Override 
  void after(
    ProvisioningProfile profile,
    Entity entity,
    ProvisioningReport result) throws JobExecutionException {

    // do nothing
  }
}
