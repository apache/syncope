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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxInterceptor;
import org.kohsuke.groovy.sandbox.GroovyInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class GroovySandbox {

    protected static final Logger LOG = LoggerFactory.getLogger(GroovySandbox.class);

    protected final Whitelist whitelist;

    public GroovySandbox(final Whitelist whitelist) {
        this.whitelist = whitelist;
    }

    @Around("execution(* *(..))")
    public Object around(final ProceedingJoinPoint joinPoint) throws Throwable {
        GroovyInterceptor interceptor = new SandboxInterceptor(whitelist);
        try {
            interceptor.register();

            return joinPoint.proceed();
        } finally {
            interceptor.unregister();
        }
    }
}
