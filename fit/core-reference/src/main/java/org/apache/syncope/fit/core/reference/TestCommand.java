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
package org.apache.syncope.fit.core.reference;

import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.core.logic.AnyObjectLogic;
import org.apache.syncope.core.logic.RealmLogic;
import org.apache.syncope.core.provisioning.api.macro.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class TestCommand implements Command<TestCommandArgs> {

    private static final Logger LOG = LoggerFactory.getLogger(TestCommand.class);

    @Autowired
    private RealmLogic realmLogic;

    @Autowired
    private AnyObjectLogic anyObjectLogic;

    private Optional<RealmTO> getRealm(final String fullPath) {
        return realmLogic.search(null, Set.of(fullPath), Pageable.unpaged()).get().
                filter(realm -> fullPath.equals(realm.getFullPath())).findFirst();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public String run(final TestCommandArgs args) {
        // 1. create new Realm
        RealmTO realm = new RealmTO();
        realm.setName(args.getRealmName());
        realm.setParent(getRealm(args.getParentRealm()).map(RealmTO::getKey).orElse(null));
        realm = realmLogic.create(args.getParentRealm(), realm).getEntity();
        LOG.info("Realm created: {}", realm.getFullPath());

        // 2. create new PRINTER
        AnyObjectTO anyObject = anyObjectLogic.create(
                new AnyObjectCR.Builder(realm.getFullPath(), "PRINTER", args.getPrinterName()).
                        plainAttr(new Attr.Builder("location").value("location").build()).
                        build(),
                false).getEntity();
        LOG.info("PRINTER created: {}", anyObject.getName());

        return "Realm created: " + realm.getFullPath() + "; PRINTER created: " + anyObject.getName();
    }
}
