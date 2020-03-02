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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.rest.api.beans.CSVPullSpec;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.rest.api.beans.CSVPushSpec;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ReconciliationLogicTest extends AbstractTest {

    @Autowired
    private ReconciliationLogic reconciliationLogic;

    @Autowired
    private UserLogic userLogic;

    @Test
    public void pullFromCSV() {
        CSVPullSpec spec = new CSVPullSpec.Builder(AnyTypeKind.USER.name(), "username").build();
        InputStream csv = getClass().getResourceAsStream("/test1.csv");

        List<ProvisioningReport> results = AuthContextUtils.callAsAdmin(SyncopeConstants.MASTER_DOMAIN, () -> {
            return reconciliationLogic.pull(spec, csv);
        });
        assertEquals(2, results.size());

        assertEquals(AnyTypeKind.USER.name(), results.get(0).getAnyType());
        assertNotNull(results.get(0).getKey());
        assertEquals("donizetti", results.get(0).getName());
        assertEquals("donizetti", results.get(0).getUidValue());
        assertEquals(ResourceOperation.CREATE, results.get(0).getOperation());
        assertEquals(ProvisioningReport.Status.SUCCESS, results.get(0).getStatus());

        AuthContextUtils.callAsAdmin(SyncopeConstants.MASTER_DOMAIN, () -> {
            UserTO donizetti = userLogic.read(results.get(0).getKey());
            assertNotNull(donizetti);
            assertEquals("Gaetano", donizetti.getPlainAttr("firstname").get().getValues().get(0));
            assertEquals(1, donizetti.getPlainAttr("loginDate").get().getValues().size());

            UserTO cimarosa = userLogic.read(results.get(1).getKey());
            assertNotNull(cimarosa);
            assertEquals("Domenico Cimarosa", cimarosa.getPlainAttr("fullname").get().getValues().get(0));
            assertEquals(2, cimarosa.getPlainAttr("loginDate").get().getValues().size());

            return null;
        });
    }

    @Test
    public void pushToCSV() throws IOException {
        Pair<Integer, List<UserTO>> search = AuthContextUtils.callAsAdmin(SyncopeConstants.MASTER_DOMAIN,
                () -> userLogic.search(null, 1, 100, List.of(), SyncopeConstants.ROOT_REALM, false));
        assertNotNull(search);

        CSVPushSpec spec = new CSVPushSpec.Builder(AnyTypeKind.USER.name()).ignorePaging(true).
                field("username").
                field("status").
                plainAttr("firstname").
                plainAttr("surname").
                plainAttr("email").
                plainAttr("loginDate").
                build();

        PipedInputStream in = new PipedInputStream();
        PipedOutputStream os = new PipedOutputStream(in);

        List<ProvisioningReport> results = AuthContextUtils.callAsAdmin(SyncopeConstants.MASTER_DOMAIN, () -> {
            return reconciliationLogic.push(null, 1, 1, List.of(), SyncopeConstants.ROOT_REALM, spec, os);
        });
        assertEquals(search.getLeft(), results.size());

        MappingIterator<Map<String, String>> reader =
                new CsvMapper().readerFor(Map.class).with(CsvSchema.emptySchema().withHeader()).readValues(in);

        for (int i = 0; i < results.size() && reader.hasNext(); i++) {
            Map<String, String> row = reader.next();

            assertEquals(results.get(i).getName(), row.get("username"));
            assertEquals(search.getRight().stream().filter(user -> row.get("username").equals(user.getUsername())).
                    findFirst().get().getStatus(),
                    row.get("status"));

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
    }
}
