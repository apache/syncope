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
package org.apache.syncope.fit.buildtools;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Start and stop an embedded ApacheDS instance alongside with Servlet Context.
 */
public class ApacheDSStartStopListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(ApacheDSStartStopListener.class);

    private DirectoryService service;

    private LdapServer server;

    /**
     * Add a new partition to the server.
     *
     * @param partitionId The partition Id
     * @param partitionDn The partition DN
     * @param dnFactory the DN factory
     * @return The newly added partition
     * @throws Exception If the partition can't be added
     */
    private Partition addPartition(final String partitionId, final String partitionDn, final DnFactory dnFactory)
            throws Exception {

        // Create a new partition with the given partition id
        JdbmPartition partition = new JdbmPartition(service.getSchemaManager(), dnFactory);
        partition.setId(partitionId);
        partition.setPartitionPath(new File(service.getInstanceLayout().getPartitionsDirectory(), partitionId).toURI());
        partition.setSuffixDn(new Dn(partitionDn));
        service.addPartition(partition);

        return partition;
    }

    /**
     * Add a new set of index on the given attributes.
     *
     * @param partition The partition on which we want to add index
     * @param attrs The list of attributes to index
     */
    private void addIndex(final Partition partition, final String... attrs) {
        // Index some attributes on the apache partition
        Set<Index<?, String>> indexedAttributes = new HashSet<>();

        for (String attribute : attrs) {
            indexedAttributes.add(new JdbmIndex<String>(attribute, false));
        }

        ((JdbmPartition) partition).setIndexedAttributes(indexedAttributes);
    }

    /**
     * Initialize the schema manager and add the schema partition to directory service.
     *
     * @throws Exception if the schema LDIF files are not found on the classpath
     */
    private void initSchemaPartition() throws Exception {
        InstanceLayout instanceLayout = service.getInstanceLayout();

        File schemaPartitionDirectory = new File(instanceLayout.getPartitionsDirectory(), "schema");

        // Extract the schema on disk (a brand new one) and load the registries
        if (schemaPartitionDirectory.exists()) {
            LOG.debug("schema partition already exists, skipping schema extraction");
        } else {
            SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(instanceLayout.getPartitionsDirectory());
            extractor.extractOrCopy();
        }

        SchemaLoader loader = new LdifSchemaLoader(schemaPartitionDirectory);
        SchemaManager schemaManager = new DefaultSchemaManager(loader);

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse
        // and normalize their suffix Dn
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();
        if (!errors.isEmpty()) {
            throw new IllegalStateException(I18n.err(I18n.ERR_317, Exceptions.printErrors(errors)));
        }

        service.setSchemaManager(schemaManager);

        // Init the LdifPartition with schema
        LdifPartition schemaLdifPartition = new LdifPartition(schemaManager, service.getDnFactory());
        schemaLdifPartition.setPartitionPath(schemaPartitionDirectory.toURI());

        // The schema partition
        SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
        schemaPartition.setWrappedPartition(schemaLdifPartition);
        service.setSchemaPartition(schemaPartition);
    }

    /**
     * Initialize the server. It creates the partition, adds the index, and injects the context entries for the created
     * partitions.
     *
     * @param workDir the directory to be used for storing the data
     * @param loadDefaultContent if default content should be loaded
     * @throws Exception if there were some problems while initializing
     */
    private void initDirectoryService(final ServletContext servletContext, final File workDir,
            final boolean loadDefaultContent) throws Exception {

        // Initialize the LDAP service
        service = new DefaultDirectoryService();
        service.setInstanceLayout(new InstanceLayout(workDir));

        CacheService cacheService = new CacheService();
        cacheService.initialize(service.getInstanceLayout());

        service.setCacheService(cacheService);

        // first load the schema
        initSchemaPartition();

        // then the system partition
        // this is a MANDATORY partition
        // DO NOT add this via addPartition() method, trunk code complains about duplicate partition
        // while initializing
        JdbmPartition systemPartition = new JdbmPartition(service.getSchemaManager(), service.getDnFactory());
        systemPartition.setId("system");
        systemPartition.setPartitionPath(
                new File(service.getInstanceLayout().getPartitionsDirectory(), systemPartition.getId()).toURI());
        systemPartition.setSuffixDn(new Dn(ServerDNConstants.SYSTEM_DN));
        systemPartition.setSchemaManager(service.getSchemaManager());

        // mandatory to call this method to set the system partition
        // Note: this system partition might be removed from trunk
        service.setSystemPartition(systemPartition);

        // Disable the ChangeLog system
        service.getChangeLog().setEnabled(false);
        service.setDenormalizeOpAttrsEnabled(true);

        // Now we can create as many partitions as we need
        Partition ispPartition = addPartition("isp", "o=isp", service.getDnFactory());

        // Index some attributes on the apache partition
        addIndex(ispPartition, "objectClass", "ou", "uid");

        // And start the service
        service.startup();

        if (loadDefaultContent) {
            Resource contentLdif = WebApplicationContextUtils.getWebApplicationContext(servletContext).
                    getResource("classpath:/content.ldif");
            LdifInputStreamLoader contentLoader = new LdifInputStreamLoader(service.getAdminSession(),
                    contentLdif.getInputStream());
            int numEntries = contentLoader.execute();
            LOG.info("Successfully created {} entries", numEntries);
        }
    }

    /**
     * Startup ApacheDS embedded.
     *
     * @param sce ServletContext event
     */
    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        File workDir = (File) sce.getServletContext().getAttribute("javax.servlet.context.tempdir");
        workDir = new File(workDir, "server-work");

        final boolean loadDefaultContent = !workDir.exists();

        if (loadDefaultContent && !workDir.mkdirs()) {
            throw new RuntimeException("Could not create " + workDir.getAbsolutePath());
        }

        Entry result;
        try {
            initDirectoryService(sce.getServletContext(), workDir, loadDefaultContent);

            server = new LdapServer();
            server.setTransports(new TcpTransport(Integer.parseInt(
                    WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext()).
                    getBean("testds.port", String.class))));
            server.setDirectoryService(service);

            server.start();

            // store directoryService in context to provide it to servlets etc.
            sce.getServletContext().setAttribute(DirectoryService.JNDI_KEY, service);

            result = service.getAdminSession().lookup(new Dn("o=isp"));
        } catch (Exception e) {
            LOG.error("Fatal error in context init", e);
            throw new RuntimeException(e);
        }

        if (result == null) {
            throw new RuntimeException("Base DN not found");
        } else {
            LOG.info("ApacheDS startup completed succesfully");
        }
    }

    /**
     * Shutdown ApacheDS embedded.
     *
     * @param sce ServletContext event
     */
    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        try {
            if (server != null) {
                server.stop();
            }
            if (service != null) {
                service.shutdown();
            }
        } catch (Exception e) {
            LOG.error("Fatal error in context shutdown", e);
            throw new RuntimeException(e);
        }
    }
}
