/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.syncope.buildtools;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import javax.naming.NamingException;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.name.DN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support for commands to load an LDIF from an URL into a DirContext.
 *
 * @see org.apache.directory.server.protocol.shared.store.LdifFileLoader
 */
public class LdifURLLoader {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(LdifURLLoader.class);

    /**
     * A handle on the top core session.
     */
    protected CoreSession coreSession;

    /**
     * The LDIF URL.
     */
    protected URL ldif;

    /**
     * The total count of entries loaded.
     */
    private int count;

    public LdifURLLoader(final CoreSession coreSession, final URL ldif) {
        this.coreSession = coreSession;
        this.ldif = ldif;
    }

    /**
     * Opens the LDIF file and loads the entries into the context.
     *
     * @return The count of entries created.
     */
    public int execute() {
        DN rdn = null;
        InputStream in = null;

        try {
            in = ldif.openStream();

            for (final LdifEntry ldifEntry : new LdifReader(in)) {
                final DN dn = ldifEntry.getDn();

                if (ldifEntry.isEntry()) {
                    final Entry entry = ldifEntry.getEntry();
                    try {
                        coreSession.lookup(dn);
                        LOG.info("Found {}, will not create.", rdn);
                    } catch (Exception e) {
                        try {
                            coreSession.add(new DefaultServerEntry(
                                    coreSession.getDirectoryService().getSchemaManager(), entry));
                            count++;
                            LOG.info("Created {}.", rdn);
                        } catch (NamingException ne) {
                            LOG.info("Could not create entry {}", entry, ne);
                        }
                    }
                } else {
                    //modify
                    final List<Modification> items = ldifEntry.getModificationItems();
                    try {
                        coreSession.modify(dn, items);
                        LOG.info("Modified: " + dn + " with modificationItems: " + items);
                    } catch (NamingException e) {
                        LOG.info("Could not modify: " + dn + " with modificationItems: " + items, e);
                    }
                }
            }
        } catch (FileNotFoundException fnfe) {
            LOG.error(I18n.err(I18n.ERR_173));
        } catch (Exception ioe) {
            LOG.error(I18n.err(I18n.ERR_174), ioe);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    LOG.error(I18n.err(I18n.ERR_175), e);
                }
            }
        }

        return count;
    }
}
