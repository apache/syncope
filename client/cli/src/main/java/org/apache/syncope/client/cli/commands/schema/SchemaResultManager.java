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
package org.apache.syncope.client.cli.commands.schema;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.client.cli.view.Table;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;

public class SchemaResultManager extends CommonsResultManager {

    public void toView(final String schemaTypeString, final List<? extends AbstractSchemaTO> schemaTOs) {
        switch (SchemaType.valueOf(schemaTypeString)) {
            case PLAIN:
                printPlainSchemasDetailed(schemaTOs);
                break;
            case DERIVED:
                fromListDerived(schemaTOs);
                break;
            case VIRTUAL:
                fromListVirtual(schemaTOs);
                break;
            default:
                break;
        }
    }

    private void printPlainSchemasDetailed(final List<? extends AbstractSchemaTO> schemaTOs) {
        System.out.println("");
        for (final AbstractSchemaTO schemaTO : schemaTOs) {
            printPlanSchemaDetailed((PlainSchemaTO) schemaTO);
        }
    }

    private void printPlanSchemaDetailed(final PlainSchemaTO schemaTO) {
        System.out.println(" > SCHEMA KEY: " + schemaTO.getKey());
        System.out.println("    type: " + schemaTO.getType().toString());
        System.out.println("    any type class: " + schemaTO.getAnyTypeClass());
        System.out.println("    conversion pattern: " + schemaTO.getConversionPattern());
        System.out.println("    mandatory condition: " + schemaTO.getMandatoryCondition());
        System.out.println("    mime type: " + schemaTO.getMimeType());
        System.out.println("    validator class: " + schemaTO.getValidatorClass());
        System.out.println("    cipher algorithm: " + (schemaTO.getCipherAlgorithm() == null
                ? "" : schemaTO.getCipherAlgorithm().getAlgorithm()));
        System.out.println("");
    }

    public void printPlainSchemas(final List<? extends AbstractSchemaTO> schemaTOs) {
        final Table.TableBuilder tableBuilder =
                new Table.TableBuilder("plain schemas").header("schema key").header("type").header("mandatory");
        for (final AbstractSchemaTO schemaTO : schemaTOs) {
            tableBuilder.rowValues(Arrays.asList(
                    ((PlainSchemaTO) schemaTO).getKey(),
                    ((PlainSchemaTO) schemaTO).getType().toString(),
                    ((PlainSchemaTO) schemaTO).getMandatoryCondition()));
        }
        tableBuilder.build().print();
    }

    public void fromListDerived(final List<? extends AbstractSchemaTO> schemaTOs) {
        final Table.TableBuilder tableBuilder =
                new Table.TableBuilder("derived schemas").header("schema key").header("expression");
        for (final AbstractSchemaTO schemaTO : schemaTOs) {
            tableBuilder.rowValues(Arrays.asList(
                    ((DerSchemaTO) schemaTO).getKey(),
                    ((DerSchemaTO) schemaTO).getExpression()));
        }
        tableBuilder.build().print();
    }

    public void fromListVirtual(final List<? extends AbstractSchemaTO> schemaTOs) {
        final Table.TableBuilder tableBuilder =
                new Table.TableBuilder("virtual schemas").header("schema key").header("readonly");
        for (final AbstractSchemaTO schemaTO : schemaTOs) {
            tableBuilder.rowValues(Arrays.asList(
                    ((VirSchemaTO) schemaTO).getKey(),
                    String.valueOf(((VirSchemaTO) schemaTO).isReadonly())));
        }
        tableBuilder.build().print();
    }

    public void printDetails(final Map<String, String> details) {
        printDetails("Schemas details", details);
    }
}
