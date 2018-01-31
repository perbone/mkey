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

import java.util.concurrent.TimeUnit;

/**
 * Builder pattern used to create a new {@link Cache} object.
 * 
 * @author Paulo Perbone <pauloperbone@yahoo.com>
 * @since 0.1.0
 */
public final class CacheBuilder
{
    private boolean statistics = false;

    private long hardLimitSize = 1000;

    private EvictionPolicy evictionPolicy = EvictionPolicy.NONE;

    private GarbagePolicy garbagePolicy = GarbagePolicy.NONE;

    private long ttl = 60;

    private TimeUnit ttlUnit = TimeUnit.SECONDS;

    private long accessTimeout = 60;

    private TimeUnit accessTimeoutUnit = TimeUnit.SECONDS;

    private long accessCount = 1;

    private boolean noCache = false;

    public static CacheBuilder newInstance()
    {
        return new CacheBuilder();
    }

    private CacheBuilder()
    {
    }

    public Cache build()
    {
        if (noCache)
        {
            return new NoCacheImpl();
        }
        else
        {
            return new CacheImpl(statistics, hardLimitSize, evictionPolicy, garbagePolicy, ttl, ttlUnit, accessTimeout,
                    accessTimeoutUnit, accessCount);
        }
    }

    public boolean getStatistics()
    {
        return statistics;
    }

    public CacheBuilder statistics()
    {
        this.statistics = true;

        return this;
    }

    public long getHardLimitSize()
    {
        return hardLimitSize;
    }

    public CacheBuilder hardLimitSize(final long hardLimitSize)
    {
        if (hardLimitSize <= 0)
            throw new IllegalArgumentException("Hard limit size must be greater than zero");

        this.hardLimitSize = hardLimitSize;

        return this;
    }

    public EvictionPolicy getEvictionPolicy()
    {
        return evictionPolicy;
    }

    public CacheBuilder evictionPolicy(final EvictionPolicy evictionPolicy)
    {
        if (evictionPolicy == null)
            throw new IllegalArgumentException("Eviction policy must not be null");

        this.evictionPolicy = evictionPolicy;

        return this;
    }

    public GarbagePolicy getGarbagePolicy()
    {
        return garbagePolicy;
    }

    public CacheBuilder garbagePolicy(final GarbagePolicy garbagePolicy)
    {
        if (garbagePolicy == null)
            throw new IllegalArgumentException("Garbage policy must not be null");

        this.garbagePolicy = garbagePolicy;

        return this;
    }

    public long getTimeToLive()
    {
        return ttl;
    }

    public TimeUnit getTimeToLiveUnit()
    {
        return ttlUnit;
    }

    public CacheBuilder timeToLive(final long ttl, final TimeUnit unit)
    {
        if (ttl <= 0)
            throw new IllegalArgumentException("Time to live must be greater than zero");

        if (unit == null)
            throw new IllegalArgumentException("Time to live unit must not be null");

        this.ttl = ttl;
        this.ttlUnit = unit;

        return this;
    }

    public long getAccessTimeout()
    {
        return accessTimeout;
    }

    public TimeUnit getAccessTimeoutUnit()
    {
        return accessTimeoutUnit;
    }

    public CacheBuilder accessTimeout(final long accessTimeout, final TimeUnit unit)
    {
        if (accessTimeout <= 0)
            throw new IllegalArgumentException("Access timeout must be greater than zero");

        if (accessTimeoutUnit == null)
            throw new IllegalArgumentException("Access timeout unit must not be null");

        this.accessTimeout = accessTimeout;
        this.accessTimeoutUnit = unit;

        return this;
    }

    public long getAccessCount()
    {
        return accessCount;
    }

    public CacheBuilder accessCount(final long accessCount)
    {
        if (accessCount <= 0)
            throw new IllegalArgumentException("Access count must be greater than zero");

        this.accessCount = accessCount;

        return this;
    }

    public boolean getNoCache()
    {
        return noCache;
    }

    public CacheBuilder noCache()
    {
        this.noCache = true;

        return this;
    }
}