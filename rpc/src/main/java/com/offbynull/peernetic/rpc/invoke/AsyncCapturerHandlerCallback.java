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
package com.offbynull.peernetic.rpc.invoke;

/**
 * Interface used to signal that the response for a async proxy call has arrived.
 * @author Kasra Faghihi
 */
public interface AsyncCapturerHandlerCallback {
    /**
     * Response arrived.
     * @param response serialized response
     */
    void responseArrived(byte[] response);
    /**
     * Response failed.
     * @param err error
     */
    void responseFailed(Throwable err);
}
