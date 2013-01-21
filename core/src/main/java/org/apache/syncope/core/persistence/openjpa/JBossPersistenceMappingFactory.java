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
package org.apache.syncope.core.persistence.openjpa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.lib.meta.ClassArgParser;
import org.apache.openjpa.lib.meta.ClasspathMetaDataIterator;
import org.apache.openjpa.lib.meta.FileMetaDataIterator;
import org.apache.openjpa.lib.meta.JarFileURLMetaDataIterator;
import org.apache.openjpa.lib.meta.MetaDataIterator;
import org.apache.openjpa.lib.meta.ResourceMetaDataIterator;
import org.apache.openjpa.lib.meta.URLMetaDataIterator;
import org.apache.openjpa.lib.meta.ZipFileMetaDataIterator;
import org.apache.openjpa.lib.meta.ZipStreamMetaDataIterator;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.persistence.PersistenceMetaDataFactory;
import org.apache.openjpa.persistence.jdbc.PersistenceMappingFactory;

/**
 * Temporary class used while issue OPENJPA-2229 gets fixed and reaches mainstream distribution.
 */
public class JBossPersistenceMappingFactory extends PersistenceMappingFactory {

    private static final Localizer _loc = Localizer.forPackage(PersistenceMetaDataFactory.class);

    /**
     * Scan for persistent type names using the given metadata iterator.
     */
    private void scan(MetaDataIterator mitr, ClassArgParser cparser, Set names,
            boolean mapNames, Object debugContext)
            throws IOException {
        Map map;
        try {
            map = cparser.mapTypeNames(mitr);
        } finally {
            mitr.close();
        }

        Map.Entry entry;
        for (Iterator itr = map.entrySet().iterator(); itr.hasNext();) {
            entry = (Map.Entry) itr.next();
            if (mapNames) {
                mapPersistentTypeNames(entry.getKey(), (String[]) entry.getValue());
            }
            List newNames = Arrays.asList((String[]) entry.getValue());
            if (log.isTraceEnabled()) {
                log.trace(_loc.get("scan-found-names", newNames, debugContext));
            }
            names.addAll(newNames);
        }
    }

    /**
     * Parse persistent type names.
     */
    @Override
    protected Set<String> parsePersistentTypeNames(ClassLoader loader)
            throws IOException {
        
        ClassArgParser cparser = newClassArgParser();
        String[] clss;
        Set<String> names = new HashSet<String>();
        if (files != null) {
            File file;
            for (Iterator itr = files.iterator(); itr.hasNext();) {
                file = (File) itr.next();
                if ((AccessController.doPrivileged(J2DoPrivHelper.isDirectoryAction(file))).booleanValue()) {
                    if (log.isTraceEnabled()) {
                        log.trace(_loc.get("scanning-directory", file));
                    }
                    scan(new FileMetaDataIterator(file, newMetaDataFilter()), cparser, names, true, file);
                } else if (file.getName().endsWith(".jar")) {
                    if (log.isTraceEnabled()) {
                        log.trace(_loc.get("scanning-jar", file));
                    }
                    try {
                        ZipFile zFile = AccessController.doPrivileged(J2DoPrivHelper.newZipFileAction(file));
                        scan(new ZipFileMetaDataIterator(zFile, newMetaDataFilter()), cparser, names, true, file);
                    } catch (PrivilegedActionException pae) {
                        throw (IOException) pae.getException();
                    }
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace(_loc.get("scanning-file", file));
                    }
                    clss = cparser.parseTypeNames(new FileMetaDataIterator(file));
                    List<String> newNames = Arrays.asList(clss);
                    if (log.isTraceEnabled()) {
                        log.trace(_loc.get("scan-found-names", newNames, file));
                    }
                    names.addAll(newNames);
                    File f = AccessController.doPrivileged(J2DoPrivHelper.getAbsoluteFileAction(file));
                    try {
                        mapPersistentTypeNames(AccessController.doPrivileged(J2DoPrivHelper.toURLAction(f)), clss);
                    } catch (PrivilegedActionException pae) {
                        throw (FileNotFoundException) pae.getException();
                    }
                }
            }
        }
        URL url;
        if (urls != null) {
            for (Iterator itr = urls.iterator(); itr.hasNext();) {
                url = (URL) itr.next();
                if ("file".equals(url.getProtocol())) {
                    File file = AccessController.doPrivileged(J2DoPrivHelper.getAbsoluteFileAction(
                            new File(url.getFile())));
                    if (files != null && files.contains(file)) {
                        continue;
                    } else if ((AccessController.doPrivileged(J2DoPrivHelper.isDirectoryAction(file))).booleanValue()) {
                        if (log.isTraceEnabled()) {
                            log.trace(_loc.get("scanning-directory", file));
                        }
                        scan(new FileMetaDataIterator(file, newMetaDataFilter()), cparser, names, true, file);
                        continue;
                    }
                }
                // OPENJPA-2229 - begin
                if ("vfs".equals(url.getProtocol())) {
                    if (log.isTraceEnabled()) {
                        log.trace(_loc.get("scanning-vfs-url", url));
                    }

                    URLConnection conn = url.openConnection();
                    Object vfsContent = conn.getContent();
                    try {
                        Class virtualFileClass = Class.forName("org.jboss.vfs.VirtualFile");
                        Method getPhysicalFile = virtualFileClass.getDeclaredMethod("getPhysicalFile");
                        File file = (File) getPhysicalFile.invoke(vfsContent);
                        scan(new FileMetaDataIterator(file, newMetaDataFilter()), cparser, names, true, file);
                    } catch (Exception e) {
                        log.error(_loc.get("while-scanning-vfs-url", url), e);
                    }

                    continue;
                }
                // OPENJPA-2229 - end
                if ("jar".equals(url.getProtocol())) {
                    if (url.getPath().endsWith("!/")) {
                        if (log.isTraceEnabled()) {
                            log.trace(_loc.get("scanning-jar-url", url));
                        }
                        scan(new ZipFileMetaDataIterator(url, newMetaDataFilter()), cparser, names, true, url);
                    } else {
                        if (log.isTraceEnabled()) {
                            log.trace(_loc.get("scanning-jar-url", url));
                        }
                        scan(new JarFileURLMetaDataIterator(url, newMetaDataFilter()), cparser, names, true, url);
                    }
                } else if (url.getPath().endsWith(".jar")) {
                    if (log.isTraceEnabled()) {
                        log.trace(_loc.get("scanning-jar-at-url", url));
                    }
                    try {
                        InputStream is = (InputStream) AccessController.doPrivileged(
                                J2DoPrivHelper.openStreamAction(url));
                        scan(new ZipStreamMetaDataIterator(new ZipInputStream(is), newMetaDataFilter()),
                                cparser, names, true, url);
                    } catch (PrivilegedActionException pae) {
                        throw (IOException) pae.getException();
                    }
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace(_loc.get("scanning-url", url));
                    }
                    clss = cparser.parseTypeNames(new URLMetaDataIterator(url));
                    List<String> newNames = Arrays.asList(clss);
                    if (log.isTraceEnabled()) {
                        log.trace(_loc.get("scan-found-names", newNames, url));
                    }
                    names.addAll(newNames);
                    mapPersistentTypeNames(url, clss);
                }
            }
        }
        if (rsrcs != null) {
            String rsrc;
            MetaDataIterator mitr;
            for (Iterator itr = rsrcs.iterator(); itr.hasNext();) {
                rsrc = (String) itr.next();
                if (rsrc.endsWith(".jar")) {
                    url = AccessController.doPrivileged(
                            J2DoPrivHelper.getResourceAction(loader, rsrc));
                    if (url != null) {
                        if (log.isTraceEnabled()) {
                            log.trace(_loc.get("scanning-jar-stream-url", url));
                        }
                        try {
                            InputStream is = (InputStream) AccessController.doPrivileged(
                                    J2DoPrivHelper.openStreamAction(url));
                            scan(new ZipStreamMetaDataIterator(new ZipInputStream(is), newMetaDataFilter()), cparser,
                                    names, true, url);
                        } catch (PrivilegedActionException pae) {
                            throw (IOException) pae.getException();
                        }
                    }
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace(_loc.get("scanning-resource", rsrc));
                    }
                    mitr = new ResourceMetaDataIterator(rsrc, loader);
                    OpenJPAConfiguration conf = repos.getConfiguration();
                    Map peMap = null;
                    if (conf instanceof OpenJPAConfigurationImpl) {
                        peMap = ((OpenJPAConfigurationImpl) conf).getPersistenceEnvironment();
                    }
                    URL puUrl = peMap == null ? null : (URL) peMap.get(PERSISTENCE_UNIT_ROOT_URL);
                    List<String> mappingFileNames =
                            peMap == null ? null : (List<String>) peMap.get(MAPPING_FILE_NAMES);
                    List<URL> jars = peMap == null ? null : (List<URL>) peMap.get(JAR_FILE_URLS);
                    String puUrlString = puUrl == null ? null : puUrl.toString();
                    if (log.isTraceEnabled()) {
                        log.trace(_loc.get("pu-root-url", puUrlString));
                    }

                    List<URL> mitrUrls = new ArrayList<URL>(3);
                    while (mitr.hasNext()) {
                        url = (URL) mitr.next();
                        String urlString = url.toString();
                        if (log.isTraceEnabled()) {
                            log.trace(_loc.get("resource-url", urlString));
                        }
                        if (peMap != null) {
                            //OPENJPA-2102: decode the URL to remove such things a spaces (' ') encoded as '%20'
                            if (puUrlString != null && decode(urlString).indexOf(decode(puUrlString)) != -1) {
                                mitrUrls.add(url);
                            }
                            if (mappingFileNames != null && !mappingFileNames.isEmpty()) {
                                for (String mappingFileName : mappingFileNames) {
                                    if (log.isTraceEnabled()) {
                                        log.trace(_loc.get("mapping-file-name", mappingFileName));
                                    }
                                    if (urlString.indexOf(mappingFileName) != -1) {
                                        mitrUrls.add(url);
                                    }
                                }
                            }

                            if (jars != null && !jars.isEmpty()) {
                                for (URL jarUrl : jars) {
                                    if (log.isTraceEnabled()) {
                                        log.trace(_loc.get("jar-file-url", jarUrl));
                                    }
                                    if (urlString.indexOf(jarUrl.toString()) != -1) {
                                        mitrUrls.add(url);
                                    }
                                }
                            }
                        } else {
                            mitrUrls.add(url);
                        }
                    }
                    mitr.close();

                    for (Object obj : mitrUrls) {
                        url = (URL) obj;
                        clss = cparser.parseTypeNames(new URLMetaDataIterator(url));
                        List<String> newNames = Arrays.asList(clss);
                        if (log.isTraceEnabled()) {
                            log.trace(_loc.get("scan-found-names", newNames, rsrc));
                        }
                        names.addAll(newNames);
                        mapPersistentTypeNames(url, clss);
                    }
                }
            }
        }
        if (cpath != null) {
            String[] dirs = (String[]) cpath.toArray(new String[cpath.size()]);
            scan(new ClasspathMetaDataIterator(dirs, newMetaDataFilter()), cparser, names, true, dirs);
        }
        if (types != null) {
            names.addAll(types);
        }

        if (log.isTraceEnabled()) {
            log.trace(_loc.get("parse-found-names", names));
        }

        return names;
    }
}
