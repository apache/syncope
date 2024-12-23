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
import org.apache.syncope.common.lib.request.AnyCR
import org.apache.syncope.common.lib.request.AnyUR
import org.apache.syncope.common.lib.to.LinkedAccountTO
import org.apache.syncope.common.lib.to.RealmTO
import org.apache.syncope.common.lib.to.EntityTO
import org.apache.syncope.common.lib.to.OrgUnit
import org.apache.syncope.common.lib.to.Provision
import org.apache.syncope.common.lib.to.ProvisioningReport
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask
import org.apache.syncope.core.provisioning.api.job.JobExecutionException
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningActions
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile
import org.apache.syncope.core.provisioning.api.pushpull.InboundActions
import org.identityconnectors.framework.common.objects.LiveSyncDelta

@CompileStatic
class MyInboundActions implements InboundActions {

  @Override
  Set<String> moreAttrsToGet(ProvisioningProfile profile, OrgUnit orgUnit) {
    return Set.of();
  }

  @Override
  Set<String> moreAttrsToGet(ProvisioningProfile profile, Provision provision) {
    return Set.of();
  }
  
  @Override
  LiveSyncDelta preprocess(ProvisioningProfile profile, LiveSyncDelta delta) {
    return delta;
  }
  
  @Override
  void beforeProvision(
    ProvisioningProfile profile,
    LiveSyncDelta delta,
    AnyCR anyCR) throws JobExecutionException {

  }

  @Override
  void beforeProvision(
    ProvisioningProfile profile,
    LiveSyncDelta delta,
    RealmTO realmTO) throws JobExecutionException {

  }

  @Override
  void beforeAssign(
    ProvisioningProfile profile,
    LiveSyncDelta delta,
    AnyCR anyCR) throws JobExecutionException {

  }

  @Override
  void beforeAssign(
    ProvisioningProfile profile,
    LiveSyncDelta delta,
    RealmTO realmTO) throws JobExecutionException {

  }

  @Override
  void beforeUnassign(
    ProvisioningProfile profile,
    LiveSyncDelta delta,
    EntityTO entity) throws JobExecutionException {

  }

  @Override
  void beforeDeprovision(
    ProvisioningProfile profile,
    LiveSyncDelta delta,
    EntityTO entity) throws JobExecutionException {

  }

  @Override
  void beforeUnlink(
    ProvisioningProfile profile,
    LiveSyncDelta delta,
    EntityTO entity) throws JobExecutionException {

  }

  @Override
  void beforeLink(
    ProvisioningProfile profile,
    LiveSyncDelta delta,
    EntityTO entity) throws JobExecutionException {

  }

  @Override
  void beforeUpdate(
    ProvisioningProfile profile,
    LiveSyncDelta delta,
    EntityTO entity,
    AnyUR anyUR) throws JobExecutionException {

  }

  @Override
  void beforeDelete(
    ProvisioningProfile profile,
    LiveSyncDelta delta,
    EntityTO entity) throws JobExecutionException {

  }

  @Override
  void after(
    ProvisioningProfile profile,
    LiveSyncDelta delta,
    EntityTO entity,
    ProvisioningReport result) throws JobExecutionException {

    // do nothing
  }

  @Override
  IgnoreProvisionException onError(
    ProvisioningProfile profile,
    LiveSyncDelta delta,
    Exception e) throws JobExecutionException {

    return null;
  }
}
