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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.fit.AbstractITCase;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class ReconciliationITCase extends AbstractITCase {

    @Test
    public void push() {
        // 1. create printer, with no resources
        AnyObjectCR printerCR = AnyObjectITCase.getSample("reconciliation");
        printerCR.getResources().clear();
        AnyObjectTO printer = createAnyObject(printerCR).getEntity();
        assertNotNull(printer.getKey());

        // 2. verify no printer with that name is on the external resource's db
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        assertEquals(0, jdbcTemplate.queryForList(
                "SELECT id FROM testPRINTER WHERE printername=?", printer.getName()).size());

        // 3. verify reconciliation status
        ReconStatus status =
                reconciliationService.status(AnyTypeKind.ANY_OBJECT, printer.getName(), "resource-db-scripted");
        assertNotNull(status);
        assertNotNull(status.getOnSyncope());
        assertNull(status.getOnResource());

        // 4. push
        PushTaskTO pushTask = new PushTaskTO();
        pushTask.setPerformCreate(true);
        pushTask.setUnmatchingRule(UnmatchingRule.PROVISION);
        reconciliationService.push(AnyTypeKind.ANY_OBJECT, printer.getKey(), "resource-db-scripted", pushTask);

        // 5. verify that printer is now propagated
        assertEquals(1, jdbcTemplate.queryForList(
                "SELECT id FROM testPRINTER WHERE printername=?", printer.getName()).size());

        // 6. verify resource was not assigned
        printer = anyObjectService.read(printer.getKey());
        assertTrue(printer.getResources().isEmpty());

        // 7. verify reconciliation status
        status = reconciliationService.status(AnyTypeKind.ANY_OBJECT, printer.getName(), "resource-db-scripted");
        assertNotNull(status);
        assertNotNull(status.getOnSyncope());
        assertNotNull(status.getOnResource());

        // __ENABLE__ management depends on the actual connector...
        Attr enable = status.getOnSyncope().getAttr(OperationalAttributes.ENABLE_NAME).orElse(null);
        if (enable != null) {
            status.getOnSyncope().getAttrs().remove(enable);
        }
        assertEquals(status.getOnSyncope(), status.getOnResource());
    }

    @Test
    public void pull() {
        // 1. create printer, with no resources
        AnyObjectCR printerCR = AnyObjectITCase.getSample("reconciliation");
        printerCR.getResources().clear();
        AnyObjectTO printer = createAnyObject(printerCR).getEntity();
        assertNotNull(printer.getKey());
        assertNotEquals("Nowhere", printer.getPlainAttr("location").get().getValues().get(0));

        // 2. create table into the external resource's db, with same name
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        jdbcTemplate.update(
                "INSERT INTO TESTPRINTER (id, printername, location, deleted, lastmodification) VALUES (?,?,?,?,?)",
                printer.getKey(), printer.getName(), "Nowhere", false, new Date());

        // 3. verify reconciliation status
        ReconStatus status =
                reconciliationService.status(AnyTypeKind.ANY_OBJECT, printer.getName(), "resource-db-scripted");
        assertNotNull(status);
        assertNotNull(status.getOnSyncope());
        assertNotNull(status.getOnResource());
        assertNotEquals(status.getOnSyncope().getAttr("LOCATION"), status.getOnResource().getAttr("LOCATION"));

        // 4. pull
        PullTaskTO pullTask = new PullTaskTO();
        pullTask.setPerformUpdate(true);
        reconciliationService.pull(AnyTypeKind.ANY_OBJECT, printer.getName(), "resource-db-scripted", pullTask);

        // 5. verify reconciliation result (and resource is still not assigned)
        printer = anyObjectService.read(printer.getKey());
        assertEquals("Nowhere", printer.getPlainAttr("location").get().getValues().get(0));
        assertTrue(printer.getResources().isEmpty());
    }
}
