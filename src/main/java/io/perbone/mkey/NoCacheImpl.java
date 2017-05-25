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

/**
 * This is a Null Object pattern implementation of the {@link Cache} interface.
 * 
 * @author Paulo Perbone <pauloperbone@yahoo.com>
 * @since 0.1.0
 */
final class NoCacheImpl implements Cache
{
    NoCacheImpl()
    {
        // do nothing
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public long size()
    {
        return 0;
    }

    @Override
    public boolean contains(final Object key)
    {
        return false;
    }

    @Override
    public <T> T get(final Object key)
    {
        return null;
    }

    @Override
    public <T> void put(final T value, final Object... keys) throws IllegalArgumentException, IllegalStateException
    {
        // do nothing
    }

    @Override
    public <T> T remove(final Object key)
    {
        return null;
    }

    @Override
    public void clear() throws UnsupportedOperationException
    {
        // do nothing
    }

    @Override
    public void evict()
    {
        // do nothing
    }

    @Override
    public void gc()
    {
        // do nothing
    }
}
