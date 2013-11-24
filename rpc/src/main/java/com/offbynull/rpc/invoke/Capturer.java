package com.offbynull.rpc.invoke;

import com.offbynull.rpc.invoke.Deserializer.DeserializerResult;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.lang3.Validate;

/**
 * Provides the ability to proxy a non-final class or interface such that method invokations are processed by some external source.
 * Invokations are sent to the external source as a serialized byte array, and each invokation waits for the external source to give back
 * either a result to be returned by the invokation or a {@link Throwable} to be thrown from the invokation. {@link CapturerHandler} is the
 * processing mechanism.
 * @author Kasra F
 * @param <T> proxy type
 */
public final class Capturer<T> {
    private Class<T> cls;
    private Serializer serializer;
    private Deserializer deserializer;
    private List<Filter> inFilters;
    private List<Filter> outFilters;

    /**
     * Constructs a {@link Capturer} object with {@link XStreamBinarySerializerDeserializer} for serialization and
     * {@link CompressionFilter} for filters.
     * @param cls class type to proxy
     * @throws NullPointerException if any arguments are {@code null}
     */
    public Capturer(Class<T> cls) {
        this(cls,
                new XStreamBinarySerializerDeserializer(),
                new XStreamBinarySerializerDeserializer(),
                new Filter[] { new CompressionFilter() },
                new Filter[] { new CompressionFilter() });
    }

    /**
     * Constructs a {@link Capturer} object.
     * @param cls class type to proxy
     * @param serializer serializer to use for invokation data
     * @param deserializer serializer to use for result data
     * @param inFilters filters serialized invokation data
     * @param outFilters filters serialized result data
     * @throws NullPointerException if any arguments are {@code null}, or if any arrays/collections contain {@code null}
     */
    public Capturer(Class<T> cls,
            Serializer serializer, Deserializer deserializer,
            Filter[] inFilters, Filter[] outFilters) {
        Validate.notNull(cls);
        Validate.notNull(serializer);
        Validate.notNull(deserializer);
        Validate.noNullElements(inFilters);
        Validate.noNullElements(outFilters);
        
        this.cls = cls;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.inFilters = new ArrayList<>(Arrays.asList(inFilters));
        this.outFilters = new ArrayList<>(Arrays.asList(outFilters));
    }
    
    /**
     * Creates a proxy object.
     * @param callback callback to notify when a method's been invoked on the returned proxy object
     * @return proxy object
     * @throws NullPointerException if any arguments are {@code null}
     */
    public T createInstance(final CapturerHandler callback) {
        Validate.notNull(callback);
        
        return  (T) Enhancer.create(cls, new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                String name = method.getName();
                Class<?>[] paramTypes = method.getParameterTypes();

                InvokeData invokeData = new InvokeData(name, args, paramTypes);

                // Serialize and filter input
                byte[] inData;
                try {
                    inData = serializer.serializeMethodCall(invokeData);
                    
                    for (Filter filter : inFilters) {
                        inData = filter.modify(inData);
                    }
                } catch (IOException ioe) {
                    callback.invokationFailed(ioe);
                    throw ioe;
                }
                
                // Call
                byte[] outData = callback.invokationTriggered(inData);
                
                // Filter and deserialize output
                DeserializerResult dr;
                try {
                    for (Filter filter : outFilters) {
                        outData = filter.unmodify(outData);
                    }
                    
                    dr = deserializer.deserialize(outData);
                } catch (IOException ioe) {
                    callback.invokationFailed(ioe);
                    throw ioe;
                }

                if (dr.getType() == SerializationType.METHOD_RETURN) {
                    return dr.getResult();
                } else if (dr.getType() == SerializationType.METHOD_THROW) {
                    throw (Throwable) dr.getResult();
                }
                
                throw new IOException("Expected "
                        + SerializationType.METHOD_RETURN + " or "
                        + SerializationType.METHOD_THROW + " but found "
                        + dr);
            }
        });
    }
}