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
package org.apache.syncope.core.spring.implementation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.macro.MacroActions;
import org.apache.syncope.core.spring.SpringTestConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { SpringTestConfiguration.class })
class GroovySandboxTest {

    @TempDir
    private Path tempDir;

    @Test
    void processBuilder() throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("processBuilder");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                getClass().getResourceAsStream("/ProcessBuilderMacroActions.groovy")));

        MacroActions actions = ImplementationManager.build(impl);

        SecurityException e = assertThrows(
                SecurityException.class, () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new java.lang.ProcessBuilder java.lang.String[]'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void bash() throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("bash");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                getClass().getResourceAsStream("/BashMacroActions.groovy")));

        MacroActions actions = ImplementationManager.build(impl);

        SecurityException e = assertThrows(
                SecurityException.class, () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new java.io.File java.lang.String'"));
    }

    @Test
    void staticMacroActions() throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("staticMacroActions");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                getClass().getResourceAsStream("/StaticMacroActions.groovy")));

        BeanCreationException e = assertThrows(BeanCreationException.class, () -> ImplementationManager.build(impl));
        SecurityException sec = (SecurityException) ExceptionUtils.getRootCause(e);
        assertTrue(sec.getMessage().startsWith("Insecure call to 'new java.lang.ProcessBuilder java.util.List'"));
    }

    @Test
    void pathOfFilesReadString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-read.txt");
        Files.writeString(testFile, "sandbox-read-ok");

        final Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("pathOfFilesReadString");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                getClass().getResourceAsStream("/PathOfFilesReadStringMacroActions.groovy")));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Path of java.lang.String java.lang.String[]'"));
    }

    @Test
    void pathOfFilesWriteString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-write.txt");

        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("pathOfFilesWriteString");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                getClass().getResourceAsStream("/PathOfFilesWriteStringMacroActions.groovy")));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Path of java.lang.String java.lang.String[]'"));
    }
}
