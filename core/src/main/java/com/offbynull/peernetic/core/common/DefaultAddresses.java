/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.core.common;

import com.offbynull.peernetic.core.shuttle.Address;

/**
 * Default addresses for gateways and actors.
 * @author Kasra Faghihi
 */
public final class DefaultAddresses {

    /**
     * Default address to log gateway as String.
     */
    public static final String DEFAULT_LOG = "log";
    
    /**
     * Default address to timer gateway as String.
     */
    public static final String DEFAULT_TIMER = "timer";
    
    /**
     * Default address to direct gateway as String.
     */
    public static final String DEFAULT_GATEWAY = "gateway";
    
    /**
     * Default address to log gateway.
     */
    public static final Address DEFAULT_LOG_ADDRESS = Address.of(DEFAULT_LOG);

    /**
     * Default address to timer gateway.
     */
    public static final Address DEFAULT_TIMER_ADDRESS = Address.of(DEFAULT_TIMER);

    /**
     * Default address to gateway gateway.
     */
    public static final Address DEFAULT_GATEWAY_ADDRESS = Address.of(DEFAULT_GATEWAY);

    private DefaultAddresses() {
        // do nothing
    }
}
