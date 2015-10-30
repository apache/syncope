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
import java.util.LinkedList;
import java.util.Map;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.client.cli.view.Table;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;

public class SchemaResultManager extends CommonsResultManager {

    public void printSchemas(final LinkedList<? extends AbstractSchemaTO> schemaTOs) {
        final Table.TableBuilder tableBuilder
                = new Table.TableBuilder("plain schemas").header("schema key").header("type").header("mandatory");
        for (final AbstractSchemaTO schemaTO : schemaTOs) {
            tableBuilder.rowValues(new LinkedList(Arrays.asList(
                    ((PlainSchemaTO) schemaTO).getKey(),
                    ((PlainSchemaTO) schemaTO).getType().toString(),
                    ((PlainSchemaTO) schemaTO).getMandatoryCondition())));
        }
        tableBuilder.build().print();
    }

    public void printSchemasWithDetails(final LinkedList<? extends AbstractSchemaTO> schemaTOs) {
        final Table.TableBuilder tableBuilder
                = new Table.TableBuilder("plain schema details ")
                .header("schema key")
                .header("type class")
                .header("pattern")
                .header("enum keys")
                .header("enum values")
                .header("mandatory condition")
                .header("mime type")
                .header("secret key")
                .header("class validator")
                .header("chiper")
                .header("type");
        for (final AbstractSchemaTO schemaTO : schemaTOs) {
            tableBuilder.rowValues(new LinkedList(Arrays.asList(
                    ((PlainSchemaTO) schemaTO).getKey(),
                    ((PlainSchemaTO) schemaTO).getAnyTypeClass(),
                    ((PlainSchemaTO) schemaTO).getConversionPattern(),
                    ((PlainSchemaTO) schemaTO).getEnumerationKeys(),
                    ((PlainSchemaTO) schemaTO).getEnumerationValues(),
                    ((PlainSchemaTO) schemaTO).getMandatoryCondition(),
                    ((PlainSchemaTO) schemaTO).getMimeType(),
                    ((PlainSchemaTO) schemaTO).getSecretKey(),
                    ((PlainSchemaTO) schemaTO).getValidatorClass(),
                    ((PlainSchemaTO) schemaTO).getCipherAlgorithm(),
                    ((PlainSchemaTO) schemaTO).getType().toString())));
        }
        tableBuilder.build().print();
    }

    public void fromListDerived(final LinkedList<? extends AbstractSchemaTO> schemaTOs) {
        final Table.TableBuilder tableBuilder
                = new Table.TableBuilder("derived schemas").header("schema key").header("expression");
        for (final AbstractSchemaTO schemaTO : schemaTOs) {
            tableBuilder.rowValues(new LinkedList(Arrays.asList(
                    ((DerSchemaTO) schemaTO).getKey(),
                    ((DerSchemaTO) schemaTO).getExpression())));
        }
        tableBuilder.build().print();
    }

    public void fromListVirtual(final LinkedList<? extends AbstractSchemaTO> schemaTOs) {
        final Table.TableBuilder tableBuilder
                = new Table.TableBuilder("virtual schemas").header("schema key").header("readonly");
        for (final AbstractSchemaTO schemaTO : schemaTOs) {
            tableBuilder.rowValues(new LinkedList(Arrays.asList(
                    ((VirSchemaTO) schemaTO).getKey(),
                    String.valueOf(((VirSchemaTO) schemaTO).isReadonly()))));
        }
        tableBuilder.build().print();
    }

    public void printDetails(final Map<String, String> details) {
        printDetails("Schemas details", details);
    }
}
