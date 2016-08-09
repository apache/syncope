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
package org.apache.syncope.client.cli.commands;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.sql.DataSource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.migrate.MigrateCommand;
import org.apache.syncope.core.persistence.jpa.content.ContentLoaderHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public class MigrateTest {

    private static final String ROOT_ELEMENT = "dataset";

    private static String BASE_PATH;

    @BeforeClass
    public static void before() {
        Properties props = new Properties();
        InputStream propStream = null;
        try {
            propStream = MigrateTest.class.getResourceAsStream("/test.properties");
            props.load(propStream);

            BASE_PATH = props.getProperty("testClasses");
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            IOUtils.closeQuietly(propStream);
        }
        assertNotNull(BASE_PATH);
    }

    @Test
    public void conf() throws Exception {
        // 1. migrate
        String[] args = new String[4];
        args[0] = "migrate";
        args[1] = "--conf";
        args[2] = BASE_PATH + File.separator + "content12.xml";
        args[3] = BASE_PATH + File.separator + "MasterContent.xml";

        new MigrateCommand().execute(new Input(args));

        // 2. initialize db as persistence-jpa does
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:syncopedb;DB_CLOSE_DELAY=-1", "sa", null);

        new ResourceDatabasePopulator(new ClassPathResource("/schema20.sql")).execute(dataSource);

        // 3. attempt to set initial content from the migrated MasterContent.xml
        SAXParserFactory factory = SAXParserFactory.newInstance();
        InputStream in = null;
        try {
            in = new FileInputStream(args[3]);

            SAXParser parser = factory.newSAXParser();
            parser.parse(in, new ContentLoaderHandler(dataSource, ROOT_ELEMENT, false));
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
