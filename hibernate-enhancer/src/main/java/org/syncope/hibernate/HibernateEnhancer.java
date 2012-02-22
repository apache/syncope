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
package org.syncope.hibernate;

import java.lang.reflect.Field;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import javax.persistence.Entity;
import javax.persistence.Lob;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

/**
 * Add Hibernate's @Type to each entity String field labeled @Lob, in order to
 * enable PostgreSQL's LOB support.
 */
public final class HibernateEnhancer {

    /**
     * Private empty constructor: this is an utility class!
     */
    private HibernateEnhancer() {
    }

    public static void main(final String[] args)
            throws Exception {

        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Expecting classpath as single argument");
        }

        ClassPool classPool = ClassPool.getDefault();
        classPool.appendClassPath(args[0]);

        PathMatchingResourcePatternResolver resResolver =
                new PathMatchingResourcePatternResolver(
                classPool.getClassLoader());
        CachingMetadataReaderFactory cachingMetadataReaderFactory =
                new CachingMetadataReaderFactory();

        for (Resource resource : resResolver.getResources(
                "classpath*:org/syncope/core/**/*.class")) {

            MetadataReader metadataReader =
                    cachingMetadataReaderFactory.getMetadataReader(resource);
            if (metadataReader.getAnnotationMetadata().
                    isAnnotated(Entity.class.getName())) {

                Class entity = Class.forName(
                        metadataReader.getClassMetadata().getClassName());
                classPool.appendClassPath(new ClassClassPath(entity));
                CtClass ctClass =
                        ClassPool.getDefault().get(entity.getName());
                if (ctClass.isFrozen()) {
                    ctClass.defrost();
                }
                ClassFile classFile = ctClass.getClassFile();
                ConstPool constPool = classFile.getConstPool();

                for (Field field : entity.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Lob.class)) {
                        AnnotationsAttribute typeAttr =
                                new AnnotationsAttribute(
                                constPool, AnnotationsAttribute.visibleTag);
                        Annotation typeAnnot = new Annotation(
                                "org.hibernate.annotations.Type", constPool);
                        typeAnnot.addMemberValue("type", new StringMemberValue(
                                "org.hibernate.type.StringClobType",
                                constPool));
                        typeAttr.addAnnotation(typeAnnot);

                        CtField lobField = ctClass.getDeclaredField(field.
                                getName());
                        lobField.getFieldInfo().addAttribute(typeAttr);
                    }
                }

                ctClass.writeFile(args[0]);
            }
        }
    }
}
