/*
 * This file is part of MKey
 * https://github.com/perbone/mkey/
 * 
 * Copyright 2013-2017 Paulo Perbone
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.perbone.mkey;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete implementation for the {@link Cache} interface.
 * 
 * @author Paulo Perbone <pauloperbone@yahoo.com>
 * @since 0.1.0
 * 
 * @see {@link CacheBuilder}
 * @see {@link EvictionPolicy}
 * @see {@link GarbagePolicy}
 */
final class CacheImpl implements Cache
{
    private static final Logger logger = LoggerFactory.getLogger(CacheImpl.class);

    private static String HOUSEKEEPER_THREAD_NAME_PREFIX = "mkey-cache-housekeeper-";

    private static long HOUSEKEEPER_DELAY_MILLISECONDS = 5000L;

    public static String getHousekeeperThreadNamePrefix()
    {
        return HOUSEKEEPER_THREAD_NAME_PREFIX;
    }

    public static void setHousekeeperThreadNamePrefix(final String prefix) throws IllegalArgumentException
    {
        if (prefix == null)
            throw new IllegalArgumentException("thread name prefix must not be null");

        if (prefix.trim().length() == 0)
            throw new IllegalArgumentException("thread name prefix must not be empty");

        HOUSEKEEPER_THREAD_NAME_PREFIX = prefix;
    }

    public static long getHousekeeperDelay(final TimeUnit unit)
    {
        return unit.convert(HOUSEKEEPER_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    public static void setHousekeeperDelay(final long delay, final TimeUnit unit) throws IllegalArgumentException
    {
        if (delay <= 0)
            throw new IllegalArgumentException("delay must be grater than zero");

        if (unit == null)
            throw new IllegalArgumentException("delay unit must not be null");

        HOUSEKEEPER_DELAY_MILLISECONDS = TimeUnit.MILLISECONDS.convert(delay, unit);

        housekeeper.interrupt(); // Just in case it is currently sleeping in the previously delay
    }

    private static Thread housekeeper;

    private static final List<WeakReference<CacheImpl>> caches = new CopyOnWriteArrayList<>();

    static
    {
        /* Anonymous thread for the cache housekeeper */
        housekeeper = new Thread() {
            @Override
            public void run()
            {
                for (;;)
                {
                    try
                    {
                        List<WeakReference<CacheImpl>> zombies = new ArrayList<>();

                        for (WeakReference<CacheImpl> ref : caches)
                        {
                            CacheImpl cache = ref.get();

                            if (cache == null)
                            {
                                zombies.add(ref);
                                continue;
                            }

                            if (cache.evictionPolicy != EvictionPolicy.NONE)
                                CacheImpl.doEvict(cache);

                            if (cache.garbagePolicy != GarbagePolicy.NONE)
                                CacheImpl.doGC(cache);

                            // TODO housekeeping for zombies entries inside table that can happen
                            // due high concurrency when putting new values @see #put()
                        }

                        for (WeakReference<CacheImpl> ref : zombies)
                            caches.remove(ref);

                        zombies.clear();

                        Thread.sleep(HOUSEKEEPER_DELAY_MILLISECONDS);
                    }
                    catch (final InterruptedException e)
                    {
                        Thread.interrupted(); // Clear it
                    }
                    catch (final Exception e)
                    {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        };

        housekeeper.setName(HOUSEKEEPER_THREAD_NAME_PREFIX + housekeeper.getId());
        housekeeper.setDaemon(true);
        housekeeper.start();
    }

    static final class Entry
    {
        final Object id;
        final Object value;
        final Object[] keys;
        final long creation;
        final AtomicLong lastAccess;
        final AtomicLong count;

        Entry(final Object id, final Object value, final Object[] keys)
        {
            this.id = id;
            this.value = value;
            this.keys = keys;
            this.creation = System.nanoTime();
            this.lastAccess = new AtomicLong(creation);
            this.count = new AtomicLong(0);
        }

        /**
         * Update the access stat.
         */
        void updateAccess()
        {
            lastAccess.set(System.nanoTime());
            count.incrementAndGet();
        }

        /**
         * Gets the current entry life time.
         * 
         * @param unit
         *            the unit for the time to be converted to
         * 
         * @return the converted life time
         */
        long lifeTime(final TimeUnit unit)
        {
            return unit.convert(System.nanoTime() - creation, TimeUnit.NANOSECONDS);
        }

        /**
         * Gets the elapsed time since the last access to this entry payload.
         * 
         * @return the converted last access time
         */
        long elapsedTimeSinceLastAccess(final TimeUnit unit)
        {
            return unit.convert(System.nanoTime() - lastAccess.get(), TimeUnit.NANOSECONDS);
        }
    }

    /* Construction parameters */
    @SuppressWarnings("unused")
    private final boolean statistics;
    private final long hardLimitSize;
    private final EvictionPolicy evictionPolicy;
    private final GarbagePolicy garbagePolicy;
    private final long ttl;
    private final TimeUnit ttlUnit;
    private final long accessTimeout;
    private final TimeUnit accessTimeoutUnit;
    private final long accessCount;

    /* Main internal structures */
    private final ConcurrentMap<Object, Entry> table = new ConcurrentHashMap<Object, Entry>();
    private final ConcurrentMap<Object, Object> index = new ConcurrentHashMap<Object, Object>();
    private final AtomicLong size = new AtomicLong(0);

    /* Eviction policy LRU structure */
    private final ConcurrentSkipListSet<Entry> lru = new ConcurrentSkipListSet<Entry>(new Comparator<Entry>() {
        @Override
        public int compare(Entry e1, Entry e2)
        {
            if (e1 == e2) // FIXME equals?
                return 0;
            if (e1.count.get() == e2.count.get())
                return e1.creation < e2.creation ? -1 : 1;
            else
                return e1.count.get() < e2.count.get() ? -1 : 1;
        }
    });

    /**
     * Creates a new cache object.
     * <p>
     * This operation is package protected so only the {@link CacheBuilder} can call it.
     * 
     * @param statistics
     *            flag that controls with the cache will have cache statistics
     * @param hardLimitSize
     *            the hard limit size (capacity) of the internal memory buffer
     * @param evictionPolicy
     *            the eviction policy for the eviction process. If NONE is used, the hardLimitSize
     *            will be used as the final capacity and when this value is reached the cache no
     *            longer will accept entry additions.
     * @param garbagePolicy
     * @param ttl
     * @param ttlUnit
     * @param accessTimeout
     * @param accessTimeoutUnit
     * @param accessCount
     */
    CacheImpl(final boolean statistics, final long hardLimitSize, final EvictionPolicy evictionPolicy,
            final GarbagePolicy garbagePolicy, long ttl, TimeUnit ttlUnit, final long accessTimeout,
            final TimeUnit accessTimeoutUnit, final long accessCount)
    {
        this.statistics = statistics;
        this.hardLimitSize = hardLimitSize;
        this.evictionPolicy = evictionPolicy;
        this.garbagePolicy = garbagePolicy;
        this.ttl = ttl;
        this.ttlUnit = ttlUnit;
        this.accessTimeout = accessTimeout;
        this.accessTimeoutUnit = accessTimeoutUnit;
        this.accessCount = accessCount;

        caches.add(new WeakReference<CacheImpl>(this));
    }

    @Override
    public boolean isEmpty()
    {
        return size.get() == 0;
    }

    /**
     * Number of entries of this cache.
     * 
     * @return the number of entries
     */
    @Override
    public long size()
    {
        return size.get();
    }

    @Override
    public boolean contains(final Object key)
    {
        if (key == null)
            throw new IllegalArgumentException("Key must not be null");

        final Object id = index.get(key);

        if (id == null)
            return false;

        final Entry entry = table.get(id);

        if (entry == null)
            return false;
        else
            return isAlive(entry);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final Object key)
    {
        if (key == null)
            throw new IllegalArgumentException("Key must not be null");

        final Object id = index.get(key);

        if (id == null)
            return null;

        final Entry entry = table.get(id);

        if (entry == null)
            return null;

        if (!isAlive(entry))
        {
            delete(entry);
            return null;
        }

        entry.updateAccess();
        updateEvictionInfo(entry);

        return (T) entry.value;
    }

    @Override
    public <T> void put(final T value, final Object... keys) throws IllegalArgumentException, IllegalStateException
    {
        if (value == null)
            throw new IllegalArgumentException("Value must not be null");

        if (keys.length == 0)
            throw new IllegalArgumentException("Keys must not be missing");

        for (Object key : keys)
        {
            if (key == null)
                throw new IllegalArgumentException("Keys must not contain null");
        }

        deleteEntries(keys); // Remove all previous entries indexed by the given keys
        insert(value, keys);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T remove(final Object key)
    {
        final Object id = index.get(key);

        if (id == null)
            return null;

        Entry entry = table.get(id);

        if (entry == null)
            return null;

        if (!delete(entry))
            return null;

        return (T) entry.value;
    }

    @Override
    public void clear() throws UnsupportedOperationException
    {
        table.clear();
        index.clear();
        size.set(0);
    }

    @Override
    public void evict()
    {
        doEvict(this);
    }

    @Override
    public void gc()
    {
        doGC(this);
    }

    private List<Entry> deleteEntries(final Object[] keys)
    {
        final List<Entry> entries = new ArrayList<Entry>();

        for (Object key : keys)
        {
            Object id = index.get(key);

            if (id == null)
                continue;

            Entry entry = table.get(id);

            if (entry == null)
                continue;

            if (delete(entry))
                entries.add(entry);
        }

        return entries;
    }

    private <T> void insert(final T value, final Object... keys)
    {
        final Object id = new Object();

        final Entry entry = new Entry(id, value, keys);

        table.put(id, entry);

        for (Object key : keys)
        {
            Object previousId = index.put(key, id);
            if (previousId != null)
            {
                // Can happen due high concurrency so we have the opportunity for housekeeping the
                // table
                table.remove(previousId);
            }
        }

        size.incrementAndGet();

        updateEvictionInfo(entry);
    }

    private boolean delete(final Entry entry)
    {
        if (table.remove(entry.id) == null)
            return false;

        for (Object k : entry.keys)
        {
            index.remove(k);
        }

        size.decrementAndGet();

        clearEvictionInfo(entry);

        return true;
    }

    private void updateEvictionInfo(final Entry entry)
    {
        if (evictionPolicy == EvictionPolicy.LRU)
        {
            lru.remove(entry);
            lru.add(entry);
        }
    }

    private void clearEvictionInfo(final Entry entry)
    {
        if (evictionPolicy == EvictionPolicy.LRU)
        {
            lru.remove(entry);
        }
    }

    private boolean isAlive(final Entry entry)
    {
        if (garbagePolicy == GarbagePolicy.NONE)
            return true;

        /* TIME_TO_LIVE policy */
        if (garbagePolicy == GarbagePolicy.TIME_TO_LIVE && entry.lifeTime(ttlUnit) > ttl)
            return false;

        /* ACCESS_TIMEOUT policy */
        if (garbagePolicy == GarbagePolicy.ACCESS_TIMEOUT
                && entry.elapsedTimeSinceLastAccess(accessTimeoutUnit) > accessTimeout)
            return false;

        /* ACCESS_COUNT policy */
        if (garbagePolicy == GarbagePolicy.ACCESS_COUNT && entry.count.get() > accessCount)
            return false;

        return true;
    }

    /**
     * Enforces garbage collection upon the given cache
     * 
     * @param cache
     *            the cache object to collect
     */
    private static void doGC(final CacheImpl cache)
    {
        if (cache.garbagePolicy == GarbagePolicy.NONE)
            return;

        // TODO syncronize per cache
        for (Entry entry : cache.table.values())
        {
            if (!cache.isAlive(entry))
            {
                cache.delete(entry);
            }
        }
    }

    /**
     * Enforces eviction upon the given cache when the limit is reached.
     * <p>
     * Currently only LRU policy is supported.
     * 
     * @param cache
     *            the cache object to perform eviction
     */
    private static void doEvict(final CacheImpl cache)
    {
        // TODO syncronize per cache
        if (cache.evictionPolicy == EvictionPolicy.LRU)
        {
            /*
             * Will run only this snapshot size to avoid infinite loop due concurrent mutations.
             */
            for (long i = 0L, snapshot = cache.size(); i < snapshot && cache.size() > cache.hardLimitSize; i++)
            {
                Entry evicted = cache.lru.pollFirst();

                if (evicted == null)
                    break;

                cache.delete(evicted);
            }
        }
    }
}