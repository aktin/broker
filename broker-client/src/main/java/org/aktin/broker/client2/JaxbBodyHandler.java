package org.aktin.broker.client2;

import java.io.InputStream;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Subscription;

import javax.xml.bind.JAXB;

/**
 * Body handler transforming the response body via JAXB to the desired class
 * @author R.W.Majeed
 *
 * @param <T> desired target type
 */
public class JaxbBodyHandler<T> implements BodyHandler<T> {
	private Class<T> type;

	private JaxbBodyHandler(Class<T> type) {
		this.type = type;
	}
	public static <T> JaxbBodyHandler<T> forType(Class<T> type){
		return new JaxbBodyHandler<T>(type);
	}
	@Override
	public BodySubscriber<T> apply(ResponseInfo responseInfo) {
		return new JaxbInputStreamSubscriber(BodySubscribers.ofInputStream());
	}

	private class JaxbInputStreamSubscriber implements BodySubscriber<T>{
		private BodySubscriber<InputStream> in;
		public JaxbInputStreamSubscriber(BodySubscriber<InputStream> in) {
			this.in = in;
		}
		@Override
		public void onSubscribe(Subscription subscription) {in.onSubscribe(subscription);}

		@Override
		public void onNext(List<ByteBuffer> item) {	in.onNext(item);}

		@Override
		public void onError(Throwable throwable) { in.onError(throwable);}

		@Override
		public void onComplete() {}

		@Override
		public CompletionStage<T> getBody() {
			return in.getBody().thenApply(stream -> JAXB.unmarshal(stream, type));
		}
		
	}

}
