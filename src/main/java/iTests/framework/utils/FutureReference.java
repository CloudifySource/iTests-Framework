package iTests.framework.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author moran
 * @param <V>
 */
public class FutureReference<V> implements Future<V> {

	private final CountDownLatch latch = new CountDownLatch(1);
	private final AtomicReference<V> ref = new AtomicReference<V>();
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return latch.getCount() == 0;
	}

	@Override
	public V get() throws InterruptedException {
		latch.await();
		return ref.get();
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		if (latch.await(timeout, unit)) {
			return ref.get();
		}
		throw new TimeoutException();
	}
	
	public void set(V val) {
		ref.set(val);
		latch.countDown();
	}

}
