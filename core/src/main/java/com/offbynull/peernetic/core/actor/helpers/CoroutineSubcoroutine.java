/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import org.apache.commons.lang3.Validate;

// a subcoroutine that wraps a normal coroutine
public final class CoroutineSubcoroutine implements Subcoroutine<Void> {

    private final String sourceId;
    private final Coroutine coroutine;

    public CoroutineSubcoroutine(String sourceId, Coroutine coroutine) {
        Validate.notNull(sourceId);
        Validate.notNull(coroutine);
        this.sourceId = sourceId;
        this.coroutine = coroutine;
    }
    
    @Override
    public String getSourceId() {
        return sourceId;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        coroutine.run(cnt);
        return null;
    }
}