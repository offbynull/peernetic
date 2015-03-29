package com.offbynull.peernetic.core.gateways.udp;

import com.offbynull.peernetic.core.gateway.Serializer;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.Validate;

final class SerializerDecodeHandler extends AbstractDecodeHandler {

    private Serializer serializer;

    SerializerDecodeHandler(Serializer serializer) {
        Validate.notNull(serializer);
        this.serializer = serializer;
    }

    
    @Override
    protected Object decode(ByteBuf buf) {
        Validate.notNull(buf);
        
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        
        return serializer.deserialize(data);
    }
    
}