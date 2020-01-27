package io.siggi.http.group;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public final class Group<T> {

	private final SuperGroup<T> superGroup;
	private final String groupName;

	private boolean closed = false;
	private final Set<T> objects = new HashSet<>();
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();
	private final ThreadLocal<Set<T>> localAdditions = new ThreadLocal();
	private final ThreadLocal<Set<T>> localRemovals = new ThreadLocal();

	Group(SuperGroup<T> superGroup, String groupName) {
		this.superGroup = superGroup;
		this.groupName = groupName;
	}

	boolean add(T object) {
		if (localAdditions.get() != null) {
			localAdditions.get().add(object);
			localRemovals.get().remove(object);
			return true;
		}
		writeLock.lock();
		try {
			if (closed) {
				return false;
			}
			objects.add(object);
			return true;
		} finally {
			writeLock.unlock();
		}
	}

	void remove(T object) {
		if (localRemovals.get() != null) {
			localRemovals.get().add(object);
			localAdditions.get().remove(object);
			return;
		}
		writeLock.lock();
		try {
			remove0(object);
		} finally {
			writeLock.unlock();
		}
	}

	private void remove0(T object) {
		objects.remove(object);
		if (objects.isEmpty()) {
			closed = true;
			superGroup.removeGroup(this);
		}
	}

	boolean forEach(AutoReleaseLock arl, Consumer<T> consumer) {
		Set<T> additions = new HashSet<>();
		Set<T> removals = new HashSet<>();
		try {
			try {
				localAdditions.set(additions);
				localRemovals.set(removals);
				readLock.lock();
				if (closed) {
					return false;
				}
				arl.unlock();
				try {
					objects.forEach(consumer);
					return true;
				} finally {
					readLock.unlock();
				}
			} finally {
				localAdditions.remove();
				localRemovals.remove();
			}
		} finally {
			if (!removals.isEmpty()) {
				writeLock.lock();
				try {
					for (T object : additions) {
						objects.add(object);
					}
					for (T object : removals) {
						remove0(object);
					}
				} finally {
					writeLock.unlock();
				}
			}
		}
	}

	public SuperGroup getSuperGroup() {
		return superGroup;
	}

	public String getName() {
		return groupName;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (other instanceof Group) {
			Group o = (Group) other;
			return o.superGroup == superGroup && o.groupName.equals(groupName);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 97 * hash + this.superGroup.hashCode();
		hash = 97 * hash + this.groupName.hashCode();
		return hash;
	}
}
