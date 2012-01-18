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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.openjpa.datacache.AbstractDataCache;
import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.datacache.DataCachePCData;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.OpenJPAId;

/**
 * A plug-in {@link DataCache L2 Cache} for OpenJPA to use EhCache.
 * <p/>
 * This cache maintains a set of Ehcaches per-class basis. The name of the
 * NamedCache for a persistence class C is determined by the
 * {@link org.apache.openjpa.persistence.DataCache @DataCache} annotation in the
 * class C. If no name is specified in @DataCache annotation then a default name
 * is used. The default name is
 * <code>openjpa</code> but can be configured via this plug-in's
 * <code>DefaultName</code> property unless
 * <code>UseDefaultForUnnamedCaches</code> is set to
 * <code>false</code>.
 *
 * @author Pinaki Poddar
 * @author Craig Andrews
 * @author Greg Luck
 */
public class EhCacheDataCache extends AbstractDataCache implements DataCache {

    /**
     *
     */
    protected static final Localizer LOCALIZER =
            Localizer.forPackage(EhCacheDataCache.class);

    private static final long serialVersionUID = -713343580941141830L;

    /**
     *
     */
    protected final Map<Class, Ehcache> caches = new HashMap<Class, Ehcache>();

    /**
     *
     */
    protected boolean useDefaultForUnnamedCaches;

    /**
     *
     */
    protected String defaultName = "openjpa";

    /**
     *
     */
    protected ReentrantLock writeLock = new ReentrantLock();

    /**
     * Asserts if default name will be used for the Ehcache for classes which do
     * not specify explicitly a name in its @DataCache annotation. The default
     * value for this flag is
     * <code>true</code>
     */
    public boolean isUseDefaultForUnnamedCaches() {
        return useDefaultForUnnamedCaches;
    }

    /**
     * Sets if default name will be used for the Ehcache for classes which do
     * not specify explicitly a name in its @DataCache annotation. The default
     * value for this flag is
     * <code>true</code>
     */
    public void setUseDefaultForUnnamedCaches(boolean flag) {
        this.useDefaultForUnnamedCaches = flag;
    }

    /**
     * Gets the default name for the Ehcache used for classes which do not
     * specify explicitly a name in its @DataCache annotation. The default name
     * is
     * <code>openjpa</code>
     */
    public String getDefaultName() {
        return defaultName;
    }

    /**
     * Sets the default name for the Ehcache used for classes which do not
     * specify explicitly a name in its @DataCache annotation. The default name
     * is
     * <code>openjpa</code>
     */
    public void setDefaultName(String defaultName) {
        this.defaultName = defaultName;
    }

    /**
     * Clears all entries from the cache
     */
    @Override
    protected void clearInternal() {
        for (Ehcache cache : caches.values()) {
            cache.removeAll();
        }
    }

    /**
     *
     * @param oid
     * @return
     */
    @Override
    protected DataCachePCData getInternal(Object oid) {
        Element result = null;
        if (oid instanceof OpenJPAId) {
            Class cls = ((OpenJPAId) oid).getType();
            Ehcache cache = findCache(cls);
            if (cache == null) {
                return null;
            } else {
                result = cache.get(oid);
            }
        } else {
            for (Ehcache cache : caches.values()) {
                result = cache.get(oid);
                if (result != null) {
                    break;
                }
            }
        }
        if (result == null) {
            return null;
        } else {
            return (DataCachePCData) result.getObjectValue();
        }
    }

    /**
     * Pinning is is not implemented in this version
     *
     * @param oid
     * @return
     */
    @Override
    protected boolean pinInternal(Object oid) {
        return false;
    }

    /**
     *
     * @param oid
     * @param pc
     * @return
     */
    @Override
    protected DataCachePCData putInternal(Object oid, DataCachePCData pc) {
        Ehcache cache = findCache(pc.getType());
        if (cache != null) {
            cache.put(new Element(oid, pc));
        }
        return pc;
    }

    /**
     *
     * @param cls
     * @param subclasses
     */
    @Override
    protected void removeAllInternal(Class cls, boolean subclasses) {
        for (Map.Entry<Class, Ehcache> entry : caches.entrySet()) {
            if (subclasses) {
                if (cls.isAssignableFrom(entry.getKey())) {
                    entry.getValue().removeAll();
                }
            } else {
                if (entry.getKey() == cls) {
                    entry.getValue().removeAll();
                }
            }
        }
    }

    /**
     *
     * @param oid
     * @return
     */
    @Override
    protected DataCachePCData removeInternal(Object oid) {
        DataCachePCData result = getInternal(oid);
        Class cls = determineClassFromObjectId(oid);
        if (caches.containsKey(cls)) {
            caches.get(cls).remove(oid);
        }
        return result;
    }

    /**
     * Pinning and unpinning are not implemented in this version
     *
     * @param oid
     * @return
     */
    @Override
    protected boolean unpinInternal(Object oid) {
        return false;
    }

    /**
     *
     */
    public void writeLock() {
        writeLock.lock();
    }

    /**
     *
     */
    public void writeUnlock() {
        writeLock.unlock();
    }

    /**
     * Find an Ehcache for the given Class. Makes all the following attempt in
     * order to find a cache and if every attempt fails returns null:
     * <p/>
     * <LI>NamedCache for the given class has been obtained before <LI>Meta-data
     * for the given class annotated for a
     * {@link org.apache.openjpa.persistence.DataCache DataCache}. <LI>{@link #setUseDefaultForUnnamedCaches(boolean) Configured}
     * to use default cache.
     */
    protected Ehcache findCache(Class clazz) {
        Ehcache cache = caches.get(clazz);
        if (cache == null) {
            ClassMetaData meta = conf.getMetaDataRepositoryInstance().
                    getCachedMetaData(clazz);
            String name = null;
            if (meta != null) {
                name = meta.getDataCacheName();
            }
            if ((name == null || "default".equals(name))
                    && !isUseDefaultForUnnamedCaches()) {
                name = clazz.getName();
            } else if (isUseDefaultForUnnamedCaches()) {
                name = getDefaultName();
            }

            cache = CacheManager.getInstance().getEhcache(name);
            if (cache == null) {
                cache = getOrCreateCache(name);
            }

            //if (cache != null) {
            caches.put(clazz, cache);
            /*
             * } else if (name == null) { throw new
             * UserException(LOCALIZER.get("no-cache-name", clazz)); } else {
             * throw new UserException(LOCALIZER.get("no-cache", clazz, name));
            }
             */
        }
        return cache;
    }

    /**
     * Gets a cache. If the cache does not exist it is created using a hardcoded
     * default.
     *
     * @param name
     * @return
     */
    protected synchronized Ehcache getOrCreateCache(String name) {
        CacheManager cacheManager = CacheManager.getInstance();
        Ehcache ehCache = cacheManager.getEhcache(name);
        if (ehCache == null) {
            cacheManager.addCache(name);
            ehCache = cacheManager.getEhcache(name);
        }
        return ehCache;
    }

    /**
     *
     * @param oid
     * @return
     */
    protected Class determineClassFromObjectId(Object oid) {
        if (oid instanceof OpenJPAId) {
            return ((OpenJPAId) oid).getType();
        }
        return null;
    }
}
