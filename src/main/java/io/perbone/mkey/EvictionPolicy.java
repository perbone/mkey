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
 * @author Paulo Perbone <pauloperbone@yahoo.com>
 * @since 0.1.0
 */
public enum EvictionPolicy
{
    /** No eviction; will use hard limit as capacity */
    NONE,

    /** */
    CLOCK,

    /** */
    FIFO,

    /** */
    GREEDY_DUAL_SIZE,

    /** */
    LANDLORD,

    /** LFU eviction implementation based on http://dhruvbird.com/lfu.pdf */
    LFU,

    /** */
    LIFO,

    /** */
    LRU,

    /** */
    LRU2,

    /** */
    LIRS,

    /** */
    MRU,

    /** */
    RANDOM,

    /** */
    THRESHOLD
}