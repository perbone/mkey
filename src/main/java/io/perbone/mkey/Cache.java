/*
 * This file is part of MKey
 * https://github.com/perbone/mkey/
 * 
 * Copyright 2013-2018 Paulo Perbone
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

/**
 * Interface used to interact with the second-level cache. If a cache is not in use, the methods of
 * this interface have no effect, except for <code>contains</code>, which returns false.
 * 
 * @author Paulo Perbone <pauloperbone@yahoo.com>
 * @since 0.1.0
 * 
 * @see {@link CacheBuilder}
 * @see {@link EvictionPolicy}
 * @see {@link GarbagePolicy}
 */
public interface Cache
{
    /**
     * Tells whether or not this cache contains entries.
     * 
     * @return <tt>true</tt> if this cache contains no entries; <tt>false</tt> otherwise
     */
    boolean isEmpty();

    /**
     * Number of entries of this cache.
     * 
     * @return the number of entries
     */
    long size();

    /**
     * Tells whether or not this cache contains a entry for the specified key.
     * 
     * @param key
     *            the entry key
     * @return <tt>true</tt> if this map contains a mapping for the specified key; <tt>false</tt>
     *         otherwise
     */
    boolean contains(Object key);

    /**
     * Returns the entry to witch the specified key is mapped or {@code null} if this cache contains
     * no entry for the key.
     * 
     * @param key
     *            the entry key
     * 
     * @return the entry for the key or {@code null}
     */
    <T> T get(Object key);

    /**
     * Associate the specified entry with the specified keys in this cache. If the cache previously
     * contained an entry for any of the keys, the old entries are replaced by the specified one.
     * 
     * @param value
     *            the cache entry
     * @param keys
     *            the entry keys to associate with
     */
    <T> void put(T value, Object... keys);

    /**
     * Removes the entry for the specified key.
     * 
     * @param key
     *            the cache entry key
     * @return the previous entry associated with key, or {@code null} if there was no mapping for
     *         key
     */
    <T> T remove(Object key);

    /**
     * Remove all entries from this cache.
     * 
     * @throws UnsupportedOperationException
     *             if the <tt>clear</tt> operation is not supported by this cache implementation
     */
    void clear() throws UnsupportedOperationException;

    /**
     * Runs the eviction within this cache.
     */
    void evict();

    /**
     * Runs the garbage collector within this cache.
     */
    void gc();
}