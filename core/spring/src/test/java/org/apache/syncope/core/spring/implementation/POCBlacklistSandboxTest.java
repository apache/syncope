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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.provisioning.api.macro.MacroActions;
import org.apache.syncope.core.spring.SpringTestConfiguration;
import org.apache.syncope.core.spring.security.DefaultEncryptorManager;
import org.apache.syncope.core.spring.security.DummyImplementationLookup;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.jenkinsci.plugins.scriptsecurity.sandbox.blacklists.Blacklist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { POCBlacklistSandboxTest.TmpConfiguration.class })
class POCBlacklistSandboxTest {

    @TempDir
    private Path tempDir;

    @EnableAspectJAutoProxy(proxyTargetClass = false)
    @Configuration(proxyBeanMethods = false)
    static class TmpConfiguration {

        @Bean
        ApplicationContextProvider applicationContextProvider() {
            return new ApplicationContextProvider();
        }

        @Bean
        EncryptorManager encryptorManager() {
            SecurityProperties securityProperties = new SecurityProperties();
            securityProperties.setAesSecretKey(SpringTestConfiguration.AES_SECRET_KEY);
            return new DefaultEncryptorManager(securityProperties);
        }

        @Primary
        @Bean
        ImplementationLookup implementationLookup() {
            return new DummyImplementationLookup();
        }

        @Bean
        Blacklist groovyBlackList() throws IOException {
            Path blacklist = Files.createTempFile("tmp-groovy-", ".blacklist");
            blacklist.toFile().deleteOnExit();
            try (Reader reader = Files.newBufferedReader(blacklist)) {
                return new Blacklist(reader);
            }
        }
    }

    private MacroActions actions(final String key, final String resource) throws Exception {
        Implementation impl = mock(Implementation.class);
        when(impl.getKey()).thenReturn("tmp-" + key);
        when(impl.getEngine()).thenReturn(ImplementationEngine.GROOVY);
        when(impl.getBody()).thenReturn(IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream(resource))));

        return ImplementationManager.build(impl);
    }

    // Add to tmp-groovy.blacklist:
    // staticMethod java.nio.file.Path of java.lang.String java.lang.String[]
    @Test
    void pocPathOfStringReadsFile() throws Exception {
        Path testFile = tempDir.resolve("tmp-path-of-string.txt");
        Files.writeString(testFile, "tmp-path-of-string-ok");

        StringBuilder output = actions("pathOfString", "/PathOfFilesReadStringMacroActions.groovy").
                afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString()));

        assertTrue(output.toString().contains("tmp-path-of-string-ok"));
    }

    // Add to tmp-groovy.blacklist:
    // staticMethod java.nio.file.Path of java.net.URI
    @Test
    void pocPathOfUriReadsFile() throws Exception {
        Path testFile = tempDir.resolve("tmp-path-of-uri.txt");
        Files.writeString(testFile, "tmp-path-of-uri-ok");

        StringBuilder output = actions("pathOfUri", "/PathOfUriFilesReadStringMacroActions.groovy").
                afterAll(null, new StringBuilder(testFile.toUri().toString()));

        assertTrue(output.toString().contains("tmp-path-of-uri-ok"));
    }

    // Add to tmp-groovy.blacklist:
    // staticMethod java.nio.file.Paths get java.lang.String java.lang.String[]
    @Test
    void pocPathsGetStringReadsFile() throws Exception {
        Path testFile = tempDir.resolve("tmp-paths-get-string.txt");
        Files.writeString(testFile, "tmp-paths-get-string-ok");

        StringBuilder output = actions("pathsGetString", "/PathsGetFilesReadStringMacroActions.groovy").
                afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString()));

        assertTrue(output.toString().contains("tmp-paths-get-string-ok"));
    }

    // Add to tmp-groovy.blacklist:
    // staticMethod java.nio.file.Paths get java.net.URI
    @Test
    void pocPathsGetUriReadsFile() throws Exception {
        Path testFile = tempDir.resolve("tmp-paths-get-uri.txt");
        Files.writeString(testFile, "tmp-paths-get-uri-ok");

        StringBuilder output = actions("pathsGetUri", "/PathsGetUriFilesReadStringMacroActions.groovy").
                afterAll(null, new StringBuilder(testFile.toUri().toString()));

        assertTrue(output.toString().contains("tmp-paths-get-uri-ok"));
    }

    // Add to tmp-groovy.blacklist:
    // staticMethod java.nio.file.FileSystems getDefault
    // method java.nio.file.FileSystem getPath java.lang.String java.lang.String[]
    @Test
    void pocFileSystemsReadsFile() throws Exception {
        Path testFile = tempDir.resolve("tmp-filesystems.txt");
        Files.writeString(testFile, "tmp-filesystems-ok");

        StringBuilder output = actions("fileSystems", "/FileSystemsReadStringMacroActions.groovy").
                afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString()));

        assertTrue(output.toString().contains("tmp-filesystems-ok"));
    }

    // Add to tmp-groovy.blacklist:
    // staticMethod java.nio.file.spi.FileSystemProvider installedProviders
    // method java.nio.file.spi.FileSystemProvider getPath java.net.URI
    @Test
    void pocFileSystemProviderReadsFile() throws Exception {
        Path testFile = tempDir.resolve("tmp-filesystem-provider.txt");
        Files.writeString(testFile, "tmp-filesystem-provider-ok");

        StringBuilder output = actions("fileSystemProvider", "/FileSystemProviderReadStringMacroActions.groovy").
                afterAll(null, new StringBuilder(testFile.toUri().toString()));

        assertTrue(output.toString().contains("tmp-filesystem-provider-ok"));
    }

    // Add to tmp-groovy.blacklist:
    // staticMethod java.nio.file.Files createTempFile java.lang.String java.lang.String java.nio.file.attribute.FileAttribute[]
    @Test
    void pocFilesCreateTempFileWritesFile() throws Exception {
        StringBuilder output = actions("filesCreateTempFile", "/FilesCreateTempFileMacroActions.groovy").
                afterAll(null, new StringBuilder());

        assertTrue(output.toString().contains("sandbox-files-create-temp-ok"));
    }

    // Add to tmp-groovy.blacklist:
    // staticMethod java.nio.file.Files createTempDirectory java.lang.String java.nio.file.attribute.FileAttribute[]
    @Test
    void pocFilesCreateTempDirectoryCreatesDirectory() throws Exception {
        StringBuilder output = actions("filesCreateTempDirectory", "/FilesCreateTempDirectoryMacroActions.groovy").
                afterAll(null, new StringBuilder());

        assertTrue(Files.isDirectory(Path.of(output.toString())));
    }

    // Add to tmp-groovy.blacklist:
    // staticMethod java.lang.ProcessBuilder startPipeline java.util.List
    @EnabledOnOs(OS.LINUX)
    @Test
    void pocProcessBuilderStartPipelineExecutesCommand() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of(
                        "builders",
                        List.of(new ProcessBuilder("/bin/sh", "-c", "printf tmp-start-pipeline-ok")));

        StringBuilder output = actions("processBuilderStartPipeline", "/ProcessBuilderStartPipelineMacroActions.groovy").
                afterAll(ctx, new StringBuilder());

        assertEquals("tmp-start-pipeline-ok", output.toString());
    }

    // Add to tmp-groovy.blacklist:
    // method java.lang.Runtime exec java.lang.String[]
    @EnabledOnOs(OS.LINUX)
    @Test
    void pocRuntimeExecExecutesCommand() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("runtime", Runtime.getRuntime());

        StringBuilder output = actions("runtimeExec", "/RuntimeExecMacroActions.groovy").
                afterAll(ctx, new StringBuilder());

        assertEquals("sandbox-runtime-exec-ok", output.toString());
    }

    // Add to tmp-groovy.blacklist:
    // new java.beans.Expression java.lang.Object java.lang.String java.lang.Object[]
    // method java.beans.Expression getValue
    @EnabledOnOs(OS.LINUX)
    @Test
    void pocBeansExpressionExecutesCommand() throws Exception {
        StringBuilder output = actions("beansExpression", "/BeansExpressionRuntimeExecMacroActions.groovy").
                afterAll(null, new StringBuilder());

        assertTrue(output.toString().contains("sandbox-beans-expression-ok"));
    }

    // Add to tmp-groovy.blacklist:
    // new java.beans.Statement java.lang.Object java.lang.String java.lang.Object[]
    @Test
    void pocBeansStatementCanBuildSystemExitStatement() throws Exception {
        StringBuilder output = actions("beansStatement", "/BeansStatementSystemExitMacroActions.groovy").
                afterAll(null, new StringBuilder());

        assertTrue(output.toString().contains("exit"));
    }

    // Add to tmp-groovy.blacklist:
    // new groovy.lang.GroovyShell
    // method groovy.lang.GroovyShell evaluate java.lang.String
    @EnabledOnOs(OS.LINUX)
    @Test
    void pocGroovyShellExecutesNestedCode() throws Exception {
        StringBuilder output = actions("groovyShell", "/GroovyShellRuntimeExecMacroActions.groovy").
                afterAll(null, new StringBuilder());

        assertEquals("sandbox-groovy-shell-ok", output.toString());
    }

    // Add to tmp-groovy.blacklist:
    // staticMethod groovy.util.Eval me java.lang.String
    @EnabledOnOs(OS.LINUX)
    @Test
    void pocEvalExecutesNestedCode() throws Exception {
        StringBuilder output = actions("eval", "/EvalRuntimeExecMacroActions.groovy").
                afterAll(null, new StringBuilder());

        assertEquals("sandbox-eval-ok", output.toString());
    }

    // Add to tmp-groovy.blacklist:
    // staticMethod java.lang.invoke.MethodHandles publicLookup
    // method java.lang.invoke.MethodHandles$Lookup findStatic java.lang.Class java.lang.String java.lang.invoke.MethodType
    // method java.lang.invoke.MethodHandle invokeWithArguments java.lang.Object[]
    @EnabledOnOs(OS.LINUX)
    @Test
    void pocMethodHandlesExecutesCommand() throws Exception {
        StringBuilder output = actions("methodHandles", "/MethodHandlesRuntimeExecMacroActions.groovy").
                afterAll(null, new StringBuilder());

        assertEquals("sandbox-method-handles-ok", output.toString());
    }

    // Add to tmp-groovy.blacklist, one by one:
    // method java.lang.Runtime exec java.lang.String
    // method java.lang.Runtime exec java.lang.String java.lang.String[]
    // method java.lang.Runtime exec java.lang.String java.lang.String[] java.io.File
    // method java.lang.Runtime exec java.lang.String[]
    // method java.lang.Runtime exec java.lang.String[] java.lang.String[]
    // method java.lang.Runtime exec java.lang.String[] java.lang.String[] java.io.File
    @EnabledOnOs(OS.LINUX)
    @Test
    void pocRuntimeExecOverloadsExecuteCommands() throws Exception {
        StringBuilder output = actions("runtimeExecOverloads", "/RuntimeExecOverloadsMacroActions.groovy").
                afterAll(null, new StringBuilder());

        assertEquals(
                "runtime-string|runtime-string-env|runtime-string-env-dir|"
                + "runtime-array|runtime-array-env|runtime-array-env-dir|",
                output.toString());
    }

    // Add to tmp-groovy.blacklist, one by one:
    // staticMethod groovy.util.Eval me java.lang.String
    // staticMethod groovy.util.Eval me java.lang.String java.lang.Object java.lang.String
    // staticMethod groovy.util.Eval x java.lang.Object java.lang.String
    // staticMethod groovy.util.Eval xy java.lang.Object java.lang.Object java.lang.String
    // staticMethod groovy.util.Eval xyz java.lang.Object java.lang.Object java.lang.Object java.lang.String
    @Test
    void pocEvalOverloadsExecuteNestedCode() throws Exception {
        StringBuilder output = actions("evalOverloads", "/EvalOverloadsMacroActions.groovy").
                afterAll(null, new StringBuilder());

        assertEquals("eval-me0|eval-me1|eval-x|eval-xy|eval-xyz|", output.toString());
    }

    // Add to tmp-groovy.blacklist, one by one:
    // new groovy.lang.GroovyShell
    // method groovy.lang.GroovyShell evaluate java.lang.String
    // method groovy.lang.GroovyShell evaluate java.lang.String java.lang.String
    // method groovy.lang.GroovyShell evaluate java.lang.String java.lang.String java.lang.String
    // method groovy.lang.GroovyShell evaluate java.io.Reader
    // method groovy.lang.GroovyShell evaluate java.io.Reader java.lang.String
    // method groovy.lang.GroovyShell evaluate java.io.File
    // method groovy.lang.GroovyShell evaluate java.net.URI
    // method groovy.lang.GroovyShell parse java.lang.String
    // method groovy.lang.GroovyShell parse java.io.Reader
    // method groovy.lang.GroovyShell run java.lang.String java.lang.String java.util.List
    // method groovy.lang.GroovyShell run java.io.Reader java.lang.String java.util.List
    @Test
    void pocGroovyShellOverloadsEvaluateParseAndRunNestedCode() throws Exception {
        Path script = tempDir.resolve("tmp-groovy-shell-overload.groovy");
        Files.writeString(script, "return 'shell-evaluate-file|'");
        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("scriptFile", script.toFile());

        StringBuilder output = actions("groovyShellOverloads", "/GroovyShellOverloadsMacroActions.groovy").
                afterAll(ctx, new StringBuilder());

        assertEquals(
                "shell-evaluate-string|shell-evaluate-string-name|shell-evaluate-string-name-codebase|"
                + "shell-evaluate-reader|shell-evaluate-reader-name|shell-evaluate-file|shell-evaluate-file|"
                + "shell-parse-string|shell-parse-reader|shell-run-string-list|shell-run-reader-list|",
                output.toString());
    }

    // Add to tmp-groovy.blacklist, one by one:
    // new groovy.lang.GroovyClassLoader
    // method groovy.lang.GroovyClassLoader parseClass java.lang.String
    // method groovy.lang.GroovyClassLoader parseClass java.lang.String java.lang.String
    // method groovy.lang.GroovyClassLoader parseClass java.io.Reader java.lang.String
    @Test
    void pocGroovyClassLoaderOverloadsCompileNestedCode() throws Exception {
        StringBuilder output = actions("groovyClassLoaderOverloads", "/GroovyClassLoaderOverloadsMacroActions.groovy").
                afterAll(null, new StringBuilder());

        assertEquals("gcl-string|gcl-string-name|gcl-reader-name|", output.toString());
    }

    // Add to tmp-groovy.blacklist, one by one:
    // new javax.script.ScriptEngineManager
    // method javax.script.ScriptEngineManager getEngineByName java.lang.String
    // method javax.script.ScriptEngineManager getEngineByExtension java.lang.String
    // method javax.script.ScriptEngine eval java.lang.String
    // method javax.script.ScriptEngine eval java.io.Reader
    @Test
    void pocScriptEngineOverloadsEvaluateNestedCode() throws Exception {
        StringBuilder output = actions("scriptEngineOverloads", "/ScriptEngineOverloadsMacroActions.groovy").
                afterAll(null, new StringBuilder());

        assertEquals("script-engine-string|script-engine-reader|script-engine-extension|", output.toString());
    }

    // Add to tmp-groovy.blacklist, one by one:
    // staticMethod java.nio.file.FileSystems getFileSystem java.net.URI
    // staticMethod java.nio.file.FileSystems newFileSystem java.net.URI java.util.Map
    // staticMethod java.nio.file.FileSystems newFileSystem java.net.URI java.util.Map java.lang.ClassLoader
    // staticMethod java.nio.file.FileSystems newFileSystem java.nio.file.Path
    // staticMethod java.nio.file.FileSystems newFileSystem java.nio.file.Path java.lang.ClassLoader
    // staticMethod java.nio.file.FileSystems newFileSystem java.nio.file.Path java.util.Map
    // staticMethod java.nio.file.FileSystems newFileSystem java.nio.file.Path java.util.Map java.lang.ClassLoader
    @Test
    void pocFileSystemsOverloadsReachFilesystemApis() throws Exception {
        Path file = tempDir.resolve("tmp-filesystems-overloads.txt");
        Files.writeString(file, "tmp-filesystems-overloads-ok");
        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("path", file);

        StringBuilder output = actions("fileSystemsOverloads", "/FileSystemsOverloadsMacroActions.groovy").
                afterAll(ctx, new StringBuilder());

        assertEquals(
                "fs-get-filesystem|fs-new-uri-map|fs-new-uri-map-loader|"
                + "fs-new-path|fs-new-path-loader|fs-new-path-map|fs-new-path-map-loader|",
                output.toString());
    }

    // Add to tmp-groovy.blacklist, one by one:
    // method java.nio.file.spi.FileSystemProvider getFileSystem java.net.URI
    // method java.nio.file.spi.FileSystemProvider newFileSystem java.net.URI java.util.Map
    // method java.nio.file.spi.FileSystemProvider newFileSystem java.nio.file.Path java.util.Map
    @Test
    void pocFileSystemProviderOverloadsReachProviderApis() throws Exception {
        Path file = tempDir.resolve("tmp-filesystem-provider-overloads.txt");
        Files.writeString(file, "tmp-filesystem-provider-overloads-ok");
        @SuppressWarnings("unchecked")
        Map<String, java.io.Serializable> ctx =
                (Map<String, java.io.Serializable>) (Map<?, ?>) Map.of("path", file);

        StringBuilder output = actions("fileSystemProviderOverloads", "/FileSystemProviderOverloadsMacroActions.groovy").
                afterAll(ctx, new StringBuilder());

        assertEquals("fsp-get-filesystem|fsp-new-uri-map|fsp-new-path-map|", output.toString());
    }

    // Add to tmp-groovy.blacklist, one by one:
    // staticMethod java.lang.invoke.MethodHandles lookup
    // staticMethod java.lang.invoke.MethodHandles publicLookup
    // staticMethod java.lang.invoke.MethodHandles reflectAs java.lang.Class java.lang.invoke.MethodHandle
    // method java.lang.invoke.MethodHandle invokeWithArguments java.lang.Object[]
    // method java.lang.invoke.MethodHandle invokeWithArguments java.util.List
    // method java.lang.invoke.MethodHandles$Lookup bind java.lang.Object java.lang.String java.lang.invoke.MethodType
    // method java.lang.invoke.MethodHandles$Lookup findConstructor java.lang.Class java.lang.invoke.MethodType
    // method java.lang.invoke.MethodHandles$Lookup findStatic java.lang.Class java.lang.String java.lang.invoke.MethodType
    // method java.lang.invoke.MethodHandles$Lookup findVirtual java.lang.Class java.lang.String java.lang.invoke.MethodType
    @Test
    void pocMethodHandlesOverloadsInvokeIndirectly() throws Exception {
        StringBuilder output = actions("methodHandlesOverloads", "/MethodHandlesOverloadsMacroActions.groovy").
                afterAll(null, new StringBuilder());

        assertEquals("7|MH-VIRTUAL|mh-constructor|MH-BIND|toString|", output.toString());
    }

    // Add to tmp-groovy.blacklist, one by one:
    // new java.beans.Expression java.lang.Object java.lang.String java.lang.Object[]
    // new java.beans.Expression java.lang.Object java.lang.Object java.lang.String java.lang.Object[]
    // new java.beans.Statement java.lang.Object java.lang.String java.lang.Object[]
    // method java.beans.Expression execute
    // method java.beans.Expression getValue
    // method java.beans.Expression setValue java.lang.Object
    // method java.beans.Statement execute
    @Test
    void pocBeansOverloadsInvokeIndirectly() throws Exception {
        StringBuilder output = actions("beansOverloads", "/BeansOverloadsMacroActions.groovy").
                afterAll(null, new StringBuilder());

        assertEquals("beans-expression|beans-preset|beans-set-value|beans-statement-execute|", output.toString());
    }

    // Add to tmp-groovy.blacklist:
    // staticMethod java.nio.file.Path of java.lang.String java.lang.String[]
    @Test
    void pocPathOfStringWritesFile() throws Exception {
        Path testFile = tempDir.resolve("tmp-path-of-string-write.txt");
        assertFalse(Files.exists(testFile));

        actions("pathOfStringWrite", "/PathOfFilesWriteStringMacroActions.groovy").
                afterAll(null, new StringBuilder(testFile.toAbsolutePath().toString()));

        assertEquals("sandbox-write-ok", Files.readString(testFile));
    }
}
