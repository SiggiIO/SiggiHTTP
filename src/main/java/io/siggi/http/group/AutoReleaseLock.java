package io.siggi.http.group;

import java.util.concurrent.locks.Lock;

class AutoReleaseLock {

	private final Lock lock;
	private boolean didUnlock = false;

	public AutoReleaseLock(Lock lock) {
		this.lock = lock;
	}

	public void unlock() {
		if (didUnlock) {
			return;
		}
		didUnlock = true;
		lock.unlock();
	}
}
