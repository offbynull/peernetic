package com.offbynull.rpc.invoke;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.MethodUtils;

/**
 * Invokes methods on an object based on serialized data. See {@link Capturer}.
 * @author Kasra F
 */
public final class Invoker implements Closeable {

    private Object object;
    private ExecutorService executor;
    private Serializer serializer;
    private Deserializer deserializer;
    private List<Filter> inFilters;
    private List<Filter> outFilters;


    /**
     * Constructs an {@link Invoker} object with {@link XStreamBinarySerializerDeserializer} for serialization and {@link CompressionFilter}
     * for filters.
     * @param object object to invoke on
     * @param executor executor to use for invokations
     * @throws NullPointerException if {@code object} is {@code null}
     */
    public Invoker(Object object, ExecutorService executor) {
        this(object, executor,
                new XStreamBinarySerializerDeserializer(),
                new XStreamBinarySerializerDeserializer(),
                new Filter[] { new CompressionFilter() },
                new Filter[] { new CompressionFilter() });
    }
    
    /**
     * Constructs a {@link Invoker} object.
     * @param object object to invoke on
     * @param executor executor to use for invokations
     * @param serializer serializer to use for invokation data
     * @param deserializer serializer to use for result data
     * @param inFilters filters serialized invokation data
     * @param outFilters filters serialized result data
     * @throws NullPointerException if any arguments other than {@code executor} are {@code null}, or if any arrays/collections contain
     * {@code null}
     */
    public Invoker(Object object, ExecutorService executor,
            Serializer serializer, Deserializer deserializer,
            Filter[] inFilters, Filter[] outFilters) {
        Validate.notNull(object);
        Validate.notNull(serializer);
        Validate.notNull(deserializer);
        Validate.noNullElements(inFilters);
        Validate.noNullElements(outFilters);
        
        this.object = object;
        this.executor = executor;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.inFilters = new ArrayList<>(Arrays.asList(inFilters));
        this.outFilters = new ArrayList<>(Arrays.asList(outFilters));
    }
    
    /**
     * Invoke a method.
     * @param data serialized invokation data
     * @param callback listener
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void invoke(final byte[] data, final InvokerListener callback) {
        invoke(data, callback, Collections.emptyMap());
    }
    
    /**
     * Invoke a method.
     * @param data serialized invokation data
     * @param callback listener
     * @param info extra information to pass in to invokation
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void invoke(final byte[] data, final InvokerListener callback, Map<? extends Object, ? extends Object> info) {
        Validate.notNull(data);
        Validate.notNull(callback);
        Validate.notNull(info);
        
        final Map<Object, Object> sharedDataCopy = new HashMap<>(info);
        
        Runnable r = new Runnable() {
            @Override
            public void run() {
                byte[] inData = data;
                
                // Filter and deserialize input
                InvokeData invokeData;
                try {
                    for (Filter filter : inFilters) {
                        inData = filter.unmodify(inData);
                    }
                    
                    Deserializer.DeserializerResult dr =
                            deserializer.deserialize(inData);
                    
                    if (dr.getType() != SerializationType.METHOD_CALL) {
                        throw new IOException("Expected "
                                + SerializationType.METHOD_CALL + " but found"
                                + dr);
                    }
                    
                    invokeData = (InvokeData) dr.getResult();
                } catch (IOException ioe) {
                    callback.invokationFailed(ioe);
                    return;
                }

                // Set shared data map
                InvokeThreadInformation.setInvokeThreadInfo(sharedDataCopy);
                
                // Call and serialize
                byte[] outData;
                try {
                    Object ret = MethodUtils.invokeMethod(object,
                            invokeData.getMethodName(),
                            invokeData.getArguments());
                    
                    outData = serializer.serializeMethodReturn(ret);
                } catch (NoSuchMethodException | IllegalAccessException ex) {
                    callback.invokationFailed(ex);
                    return;
                } catch (InvocationTargetException ex) {
                    outData = serializer.serializeMethodThrow(ex.getCause());
                } finally {
                    InvokeThreadInformation.removeInvokeThreadInfo();
                }
                
                // Filter output
                try {
                    for (Filter filter : outFilters) {
                        outData = filter.modify(outData);
                    }
                } catch (IOException ioe) {
                    callback.invokationFailed(ioe);
                    return;
                }
                
                // Send
                callback.invokationFinised(outData);
            }
        };
        
        if (executor == null) {
            r.run();
        } else {
            executor.execute(r);
        }
    }

    @Override
    public void close() throws IOException {
        executor.shutdownNow();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            Thread.interrupted(); // just in case this is interrupted exception
            throw new IOException(ex);
        }
    }
}