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

import groovy.grape.GrabAnnotationTransformation;
import groovy.lang.GroovyClassLoader;
import java.io.Reader;
import java.util.Set;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.jenkinsci.plugins.scriptsecurity.sandbox.blacklists.Blacklist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.RejectASTTransformsCustomizer;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxInterceptor;
import org.kohsuke.groovy.sandbox.GroovyInterceptor;
import org.kohsuke.groovy.sandbox.SandboxTransformer;
import org.springframework.util.function.ThrowingSupplier;

public class GroovySandboxScriptEngineImpl extends GroovyScriptEngineImpl {

    private static final GroovyClassLoader GROOVY_CLASSLOADER;

    static {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.addCompilationCustomizers(new RejectASTTransformsCustomizer(), new SandboxTransformer());
        cc.setDisabledGlobalASTTransformations(Set.of(GrabAnnotationTransformation.class.getName()));

        GROOVY_CLASSLOADER = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), cc);
    }

    protected final Blacklist blackList;

    public GroovySandboxScriptEngineImpl(final Blacklist blackList) {
        super(GROOVY_CLASSLOADER);
        this.blackList = blackList;
    }

    protected Object sandboxEval(final ThrowingSupplier<Object> eval) {
        GroovyInterceptor interceptor = null;
        try {
            interceptor = new SandboxInterceptor(blackList);
            interceptor.register();
        } catch (NoClassDefFoundError noClassDefFound) {
            // ignore
        }

        try {
            return eval.get();
        } finally {
            if (interceptor != null) {
                try {
                    interceptor.unregister();
                } catch (NoClassDefFoundError noClassDefFound) {
                    // ignore
                }
            }
        }
    }

    @Override
    public Object eval(final Reader reader, final ScriptContext ctx) throws ScriptException {
        return sandboxEval(() -> super.eval(reader, ctx));
    }

    @Override
    public Object eval(final String script, final ScriptContext ctx) throws ScriptException {
        return sandboxEval(() -> super.eval(script, ctx));
    }
}
