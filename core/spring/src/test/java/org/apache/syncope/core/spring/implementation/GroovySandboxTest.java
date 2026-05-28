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
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                Objects.requireNonNull(getClass().getResourceAsStream("/ProcessBuilderMacroActions.groovy"))));

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
                Objects.requireNonNull(getClass().getResourceAsStream("/BashMacroActions.groovy"))));

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
                Objects.requireNonNull(getClass().getResourceAsStream("/StaticMacroActions.groovy"))));

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
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/PathOfFilesReadStringMacroActions.groovy"))));

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
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/PathOfFilesWriteStringMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Path of java.lang.String java.lang.String[]'"));
    }

    @Test
    void pathOfUriFilesReadString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-read-uri.txt");
        Files.writeString(testFile, "sandbox-read-uri-ok");

        final Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("pathOfUriFilesReadString");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/PathOfUriFilesReadStringMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toUri().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Path of java.net.URI'"));
    }

    @Test
    void pathsGetFilesReadString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-paths-get-read.txt");
        Files.writeString(testFile, "sandbox-paths-get-read-ok");

        final Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("pathsGetFilesReadString");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/PathsGetFilesReadStringMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Paths get java.lang.String java.lang.String[]'"));
    }

    @Test
    void pathsGetUriFilesReadString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-paths-get-uri-read.txt");
        Files.writeString(testFile, "sandbox-paths-get-uri-read-ok");

        final Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("pathsGetUriFilesReadString");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/PathsGetUriFilesReadStringMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toUri().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Paths get java.net.URI'"));
    }

    @Test
    void fileSystemsReadString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-filesystems-read.txt");
        Files.writeString(testFile, "sandbox-filesystems-read-ok");

        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("fileSystemsReadString");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/FileSystemsReadStringMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.FileSystems getDefault'"));
    }

    @Test
    void fileSystemProviderReadString() throws Exception {
        final Path testFile = tempDir.resolve("sandbox-filesystem-provider-read.txt");
        Files.writeString(testFile, "sandbox-filesystem-provider-read-ok");

        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("fileSystemProviderReadString");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/FileSystemProviderReadStringMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder(testFile.toUri().toString())));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.spi.FileSystemProvider installedProviders'"));
    }

    @Test
    void filesCreateTempFile() throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("filesCreateTempFile");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/FilesCreateTempFileMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Files createTempFile "
                + "java.lang.String java.lang.String java.nio.file.attribute.FileAttribute[]'"));
    }

    @Test
    void filesCreateTempDirectory() throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("filesCreateTempDirectory");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/FilesCreateTempDirectoryMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.nio.file.Files createTempDirectory "
                + "java.lang.String java.nio.file.attribute.FileAttribute[]'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void processBuilderStartPipeline() throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("processBuilderStartPipeline");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/ProcessBuilderStartPipelineMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        @SuppressWarnings("unchecked")
        final Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of(
                        "builders",
                        List.of(new ProcessBuilder("/bin/sh", "-c", "printf sandbox-start-pipeline-ok")));

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.lang.ProcessBuilder startPipeline java.util.List'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void runtimeExec() throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("runtimeExec");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/RuntimeExecMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        @SuppressWarnings("unchecked")
        final Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("runtime", Runtime.getRuntime());

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(ctx, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'method java.lang.Runtime exec java.lang.String[]'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void beansExpressionRuntimeExec() throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("beansExpressionRuntimeExec");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/BeansExpressionRuntimeExecMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'new java.beans.Expression java.lang.Object java.lang.String java.lang.Object[]'"));
    }

    @Test
    void beansStatementSystemExit() throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("beansStatementSystemExit");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/BeansStatementSystemExitMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'new java.beans.Statement java.lang.Object java.lang.String java.lang.Object[]'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void groovyShellRuntimeExec() throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("groovyShellRuntimeExec");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/GroovyShellRuntimeExecMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains("Insecure call to 'new groovy.lang.GroovyShell'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void evalRuntimeExec() throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("evalRuntimeExec");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/EvalRuntimeExecMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod groovy.util.Eval me java.lang.String'"));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void methodHandlesRuntimeExec() throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("methodHandlesRuntimeExec");
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/MethodHandlesRuntimeExecMacroActions.groovy"))));

        final MacroActions actions = ImplementationManager.build(impl);

        final SecurityException e = assertThrows(
                SecurityException.class,
                () -> actions.afterAll(null, new StringBuilder()));
        assertTrue(e.getMessage().contains(
                "Insecure call to 'staticMethod java.lang.invoke.MethodHandles publicLookup'"));
    }
}
