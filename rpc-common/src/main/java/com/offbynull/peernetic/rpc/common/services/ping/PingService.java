/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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

package com.offbynull.peernetic.rpc.common.services.ping;

/**
 * Ping service.
 * @author Kasra Faghihi
 */
public interface PingService {
    /**
     * Service ID.
     */
    int SERVICE_ID = 2001;
    
    /**
     * Ping.
     * @param value ping value (likely set to current time)
     * @return the same value passed in for the {@code ping} parameter
     */
    long ping(long value);
}
