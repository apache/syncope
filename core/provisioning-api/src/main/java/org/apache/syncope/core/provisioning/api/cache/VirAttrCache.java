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
package org.apache.syncope.core.provisioning.api.cache;

/**
 * Virtual Attribute Value cache.
 */
public interface VirAttrCache {

    /**
     * Force entry expiring.
     *
     * @param type any object
     * @param key any object key
     * @param schemaKey virtual attribute schema
     */
    void expire(String type, String key, String schemaKey);

    /**
     * Retrieve cached value. Return null in case of virtual attribute not cached.
     *
     * @param type any object
     * @param key any object key
     * @param schemaKey virtual attribute schema.
     * @return cached values or null if virtual attribute is not cached.
     */
    VirAttrCacheValue get(String type, String key, String schemaKey);

    /**
     * Cache entry is valid if and only if value exist and it is not expired.
     *
     * @param value cache entry value.
     * @return TRUE if the value is valid; FALSE otherwise.
     */
    boolean isValidEntry(VirAttrCacheValue value);

    /**
     * Cache virtual attribute values.
     *
     * @param type any object
     * @param key any object key
     * @param schemaName virtual attribute name
     * @param value virtual attribute values
     */
    void put(String type, String key, String schemaName, VirAttrCacheValue value);

}
