package org.aktin.broker.client2;

import java.io.InputStream;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpResponse.ResponseInfo;
import java.util.function.Supplier;

import jakarta.xml.bind.JAXB;

/**
 * Body handler transforming the response body via JAXB to the desired class
 * @author R.W.Majeed
 *
 * @param <T> desired target type
 */
public class JaxbBodyHandler<T> implements BodyHandler<Supplier<T>> {
	private Class<T> type;

	public JaxbBodyHandler(Class<T> type) {
		this.type = type;
	}
	public static <T> JaxbBodyHandler<T> forType(Class<T> type){
		return new JaxbBodyHandler<T>(type);
	}
	@Override
	public BodySubscriber<Supplier<T>> apply(ResponseInfo responseInfo) {
		return asJAXB(type);
	}
	public static <W> BodySubscriber<Supplier<W>> asJAXB(Class<W> targetType) {
        BodySubscriber<InputStream> upstream = BodySubscribers.ofInputStream();

        return BodySubscribers.mapping(upstream,inputStream -> toSupplierOfType(inputStream, targetType));
    }
	public static <W> Supplier<W> toSupplierOfType(InputStream in, Class<W> type) {
        return () -> JAXB.unmarshal(in, type);
    }
}
