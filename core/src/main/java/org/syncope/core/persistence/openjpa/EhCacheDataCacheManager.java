/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.openjpa;

import net.sf.ehcache.Ehcache;
import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.datacache.DataCacheManager;
import org.apache.openjpa.datacache.DataCacheManagerImpl;

/**
 * @author Craig Andrews
 * @author Greg Luck
 */
public class EhCacheDataCacheManager
        extends DataCacheManagerImpl implements DataCacheManager {

    @Override
    public DataCache getDataCache(String name, boolean create) {
        DataCache cache = super.getDataCache(name, create);
        if (cache == null) {
            cache = getSystemDataCache();
        }
        return cache;
    }

    @Override
    public EhCacheDataCache getSystemDataCache() {
        return ((EhCacheDataCache) super.getSystemDataCache());
    }

    public Ehcache getEhCache(Class cls) {
        return getSystemDataCache().findCache(cls);
    }
}