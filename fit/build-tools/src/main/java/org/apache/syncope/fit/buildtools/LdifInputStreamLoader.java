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

import java.io.InputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.protocol.shared.store.LdifLoadFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdifInputStreamLoader {

    /**
     * The log for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(LdifInputStreamLoader.class);

    /**
     * A handle on the top core session.
     */
    protected CoreSession coreSession;

    /**
     * The LDIF input stream file containing LDIFs to load.
     */
    protected InputStream ldif;

    /**
     * the total count of entries loaded
     */
    private int count;

    /**
     * Creates a new instance of LdifFileLoader.
     *
     * @param coreSession the context to load the entries into.
     * @param ldif the file of LDIF entries to load.
     */
    public LdifInputStreamLoader(final CoreSession coreSession, final InputStream ldif) {
        this(coreSession, ldif, null);
    }

    /**
     * Creates a new instance of LdifFileLoader.
     *
     * @param coreSession core session
     * @param ldif LDIF content
     * @param filters filters
     */
    public LdifInputStreamLoader(
            final CoreSession coreSession, final InputStream ldif, final List<? extends LdifLoadFilter> filters) {

        this.coreSession = coreSession;
        this.ldif = ldif;
    }

    /**
     * Opens the LDIF file and loads the entries into the context.
     *
     * @return The count of entries created.
     */
    public int execute() {
        try {
            try {
                for (LdifEntry ldifEntry : new LdifReader(ldif)) {
                    Dn dn = ldifEntry.getDn();

                    if (ldifEntry.isEntry()) {
                        Entry entry = ldifEntry.getEntry();

                        try {
                            coreSession.lookup(dn);
                            LOG.debug("Found {}, will not create.", dn);
                        } catch (Exception e) {
                            try {
                                coreSession.add(
                                        new DefaultEntry(coreSession.getDirectoryService().getSchemaManager(), entry));
                                count++;
                                LOG.debug("Created {}.", dn);
                            } catch (LdapException e1) {
                                LOG.error("Could not create entry " + entry, e1);
                            }
                        }
                    } else {
                        //modify
                        List<Modification> items = ldifEntry.getModifications();

                        try {
                            coreSession.modify(dn, items);
                            LOG.debug("Modified: " + dn + " with modificationItems: " + items);
                        } catch (LdapException e) {
                            LOG.debug("Could not modify: " + dn + " with modificationItems: " + items, e);
                        }
                    }
                }
            } finally {
                IOUtils.closeQuietly(ldif);
            }
        } catch (Exception ioe) {
            LOG.error(I18n.err(I18n.ERR_174), ioe);
        }

        return count;
    }
}
