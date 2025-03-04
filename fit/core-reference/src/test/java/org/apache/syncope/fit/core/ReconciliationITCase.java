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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.CSVPullSpec;
import org.apache.syncope.common.rest.api.beans.CSVPushSpec;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.common.rest.api.service.ReconciliationService;
import org.apache.syncope.fit.AbstractITCase;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
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
        ReconStatus status = RECONCILIATION_SERVICE.status(
                new ReconQuery.Builder(PRINTER, RESOURCE_NAME_DBSCRIPTED).anyKey(printer.getName()).build());
        assertNotNull(status);
        assertEquals(AnyTypeKind.ANY_OBJECT, status.getAnyTypeKind());
        assertEquals(printer.getKey(), status.getAnyKey());
        assertEquals(MatchType.ANY, status.getMatchType());
        assertNotNull(status.getOnSyncope());
        assertNull(status.getOnResource());

        // 4. push
        PushTaskTO pushTask = new PushTaskTO();
        pushTask.setPerformCreate(true);
        pushTask.setUnmatchingRule(UnmatchingRule.PROVISION);
        RECONCILIATION_SERVICE.push(new ReconQuery.Builder(PRINTER, RESOURCE_NAME_DBSCRIPTED).
                anyKey(printer.getKey()).build(), pushTask);

        // 5. verify that printer is now propagated
        assertEquals(1, jdbcTemplate.queryForList(
                "SELECT id FROM testPRINTER WHERE printername=?", printer.getName()).size());

        // 6. verify resource was not assigned
        printer = ANY_OBJECT_SERVICE.read(printer.getKey());
        assertTrue(printer.getResources().isEmpty());

        // 7. verify reconciliation status
        status = RECONCILIATION_SERVICE.status(
                new ReconQuery.Builder(PRINTER, RESOURCE_NAME_DBSCRIPTED).anyKey(printer.getName()).build());
        assertNotNull(status);
        assertNotNull(status.getOnSyncope());
        assertNotNull(status.getOnResource());

        // __ENABLE__ management depends on the actual connector...
        Attr enable = status.getOnSyncope().getAttr(OperationalAttributes.ENABLE_NAME).orElse(null);
        if (enable != null) {
            status.getOnSyncope().getAttrs().remove(enable);
        }
        // FIQL is always null for Syncope
        assertNull(status.getOnSyncope().getFiql());
        assertNotNull(status.getOnResource().getFiql());
        status.getOnResource().setFiql(null);
        assertEquals(status.getOnSyncope(), status.getOnResource());
    }

    @Test
    public void pull() {
        // 1. create printer, with no resources
        AnyObjectCR printerCR = AnyObjectITCase.getSample("reconciliation");
        printerCR.getResources().clear();
        AnyObjectTO printer = createAnyObject(printerCR).getEntity();
        assertNotNull(printer.getKey());
        assertNotEquals("Nowhere", printer.getPlainAttr("location").get().getValues().getFirst());

        // 2. add row into the external resource's table, with same name
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        jdbcTemplate.update(
                "INSERT INTO TESTPRINTER (id, printername, location, deleted, lastmodification) VALUES (?,?,?,?,?)",
                printer.getKey(), printer.getName(), "Nowhere", false, new Date());

        // 3. verify reconciliation status
        ReconStatus status = RECONCILIATION_SERVICE.status(
                new ReconQuery.Builder(PRINTER, RESOURCE_NAME_DBSCRIPTED).anyKey(printer.getName()).build());
        assertNotNull(status);
        assertNotNull(status.getOnSyncope());
        assertNotNull(status.getOnResource());
        assertNotEquals(status.getOnSyncope().getAttr("LOCATION"), status.getOnResource().getAttr("LOCATION"));

        // 4. pull
        PullTaskTO pullTask = new PullTaskTO();
        pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        pullTask.setPerformUpdate(true);
        RECONCILIATION_SERVICE.pull(new ReconQuery.Builder(PRINTER, RESOURCE_NAME_DBSCRIPTED).
                anyKey(printer.getName()).build(), pullTask);

        // 5. verify reconciliation result (and resource is still not assigned)
        printer = ANY_OBJECT_SERVICE.read(printer.getKey());
        assertEquals("Nowhere", printer.getPlainAttr("location").get().getValues().getFirst());
        assertTrue(printer.getResources().isEmpty());
    }

    @Test
    public void importSingle() {
        // 1. add row into the external resource's table
        String externalKey = UUID.randomUUID().toString();
        String externalName = "printer" + getUUIDString();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        jdbcTemplate.update(
                "INSERT INTO TESTPRINTER (id, printername, location, deleted, lastmodification) VALUES (?,?,?,?,?)",
                externalKey, externalName, "Nowhere", false, new Date());

        // 2. verify reconciliation status
        ReconStatus status = RECONCILIATION_SERVICE.status(
                new ReconQuery.Builder(PRINTER, RESOURCE_NAME_DBSCRIPTED).fiql("ID==" + externalKey).build());
        assertNotNull(status);
        assertNull(status.getAnyTypeKind());
        assertNull(status.getAnyKey());
        assertNull(status.getMatchType());
        assertNull(status.getOnSyncope());
        assertNotNull(status.getOnResource());
        assertEquals(externalKey, status.getOnResource().getAttr(Uid.NAME).get().getValues().getFirst());
        assertEquals(externalName, status.getOnResource().getAttr("PRINTERNAME").get().getValues().getFirst());

        // 3. pull
        PullTaskTO pullTask = new PullTaskTO();
        pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        pullTask.setPerformCreate(true);
        RECONCILIATION_SERVICE.pull(
                new ReconQuery.Builder(PRINTER, RESOURCE_NAME_DBSCRIPTED).fiql("ID==" + externalKey).build(), pullTask);

        // 4. verify reconciliation result
        AnyObjectTO printer = ANY_OBJECT_SERVICE.read(PRINTER, externalName);
        assertNotNull(printer);
    }

    @Test
    public void importCSV() {
        ReconciliationService service = ADMIN_CLIENT.getService(ReconciliationService.class);
        Client client = WebClient.client(service);
        client.type(RESTHeaders.TEXT_CSV);

        CSVPullSpec spec = new CSVPullSpec.Builder(AnyTypeKind.USER.name(), "username").build();
        InputStream csv = getClass().getResourceAsStream("/test1.csv");

        List<ProvisioningReport> results = service.pull(spec, csv);
        assertEquals(AnyTypeKind.USER.name(), results.getFirst().getAnyType());
        assertNotNull(results.getFirst().getKey());
        assertEquals("donizetti", results.getFirst().getName());
        assertEquals("donizetti", results.getFirst().getUidValue());
        assertEquals(ResourceOperation.CREATE, results.getFirst().getOperation());
        assertEquals(ProvisioningReport.Status.SUCCESS, results.getFirst().getStatus());

        UserTO donizetti = USER_SERVICE.read(results.getFirst().getKey());
        assertNotNull(donizetti);
        assertEquals("Gaetano", donizetti.getPlainAttr("firstname").get().getValues().getFirst());
        assertEquals(1, donizetti.getPlainAttr("loginDate").get().getValues().size());

        UserTO cimarosa = USER_SERVICE.read(results.get(1).getKey());
        assertNotNull(cimarosa);
        assertEquals("Domenico Cimarosa", cimarosa.getPlainAttr("fullname").get().getValues().getFirst());
        assertEquals(2, cimarosa.getPlainAttr("loginDate").get().getValues().size());
    }

    @Test
    public void exportCSV() throws IOException {
        ReconciliationService service = ADMIN_CLIENT.getService(ReconciliationService.class);
        Client client = WebClient.client(service);
        client.accept(RESTHeaders.TEXT_CSV);

        AnyQuery anyQuery = new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("*ini").query()).
                page(1).
                size(1000).
                orderBy("username ASC").
                build();

        CSVPushSpec spec = new CSVPushSpec.Builder(AnyTypeKind.USER.name()).ignorePaging(true).
                field("username").
                field("status").
                plainAttr("firstname").
                plainAttr("surname").
                plainAttr("email").
                plainAttr("loginDate").
                build();

        Response response = service.push(anyQuery, spec);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(
                "attachment; filename=" + SyncopeConstants.MASTER_DOMAIN + ".csv",
                response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION));

        PagedResult<UserTO> users = USER_SERVICE.search(anyQuery);
        assertNotNull(users);

        MappingIterator<Map<String, String>> reader = new CsvMapper().readerFor(Map.class).
                with(CsvSchema.emptySchema().withHeader()).readValues((InputStream) response.getEntity());

        int rows = 0;
        for (; reader.hasNext(); rows++) {
            Map<String, String> row = reader.next();

            assertEquals(users.getResult().get(rows).getUsername(), row.get("username"));
            assertEquals(users.getResult().get(rows).getStatus(), row.get("status"));

            switch (row.get("username")) {
                case "rossini":
                    assertEquals(spec.getNullValue(), row.get("email"));
                    assertTrue(row.get("loginDate").contains(spec.getArrayElementSeparator()));
                    break;

                case "verdi":
                    assertEquals("verdi@syncope.org", row.get("email"));
                    assertEquals(spec.getNullValue(), row.get("loginDate"));
                    break;

                case "bellini":
                    assertEquals(spec.getNullValue(), row.get("email"));
                    assertFalse(row.get("loginDate").contains(spec.getArrayElementSeparator()));
                    break;

                default:
                    break;
            }
        }
        assertEquals(rows, users.getTotalCount());
    }
}
