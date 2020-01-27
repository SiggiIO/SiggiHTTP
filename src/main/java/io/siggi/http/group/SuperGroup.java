package io.siggi.http.group;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * A group of groups, designed for grouping together websockets so you can send
 * messages to a large group of websockets at the same time, but can be used for
 * other purposes too, fully thread safe.
 *
 * @author Siggi
 * @param <T> the type of object you want to group
 */
public final class SuperGroup<T> {

	private final Map<String, Group<T>> groups = new HashMap<>();
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	public SuperGroup() {
	}

	/**
	 * Add an object to a group. If the group does not exist, it will be created
	 * automatically. If you're currently inside a forEach for the group you're
	 * adding to, the addition will take place after exiting forEach.
	 *
	 * @param groupName
	 * @param object
	 * @return
	 */
	public Group addToGroup(String groupName, T object) {
		if (groupName == null || object == null) {
			throw new NullPointerException();
		}
		Group g;
		readLock.lock();
		try {
			g = groups.get(groupName);
			if (g != null) {
				if (g.add(object)) {
					return g;
				}
			}
		} finally {
			readLock.unlock();
		}
		writeLock.lock();
		try {
			g = groups.get(groupName);
			if (g == null) {
				groups.put(groupName, g = new Group(this, groupName));
			}
			if (!g.add(object)) {
				groups.put(groupName, g = new Group(this, groupName));
				g.add(object);
			}
			return g;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Do something for each object in a group.
	 *
	 * @param groupName the group name
	 * @param consumer what to do with each object
	 */
	public void forEach(String groupName, Consumer<T> consumer) {
		AutoReleaseLock arl = new AutoReleaseLock(readLock);
		readLock.lock();
		try {
			Group<T> g = groups.get(groupName);
			if (g != null) {
				g.forEach(arl, consumer);
			}
		} finally {
			arl.unlock();
		}
	}

	/**
	 * Remove an object from a group. If this results in the group becoming
	 * empty, the group will be deleted. If you're currently inside a forEach
	 * for the group you're removing from, the removal will take place after
	 * exiting forEach.
	 *
	 * @param groupName the group to remove it from
	 * @param object the object to remove.
	 */
	public void remove(String groupName, T object) {
		if (groupName == null || object == null) {
			throw new NullPointerException();
		}
		Group<T> g;
		readLock.lock();
		try {
			g = groups.get(groupName);
		} finally {
			readLock.unlock();
		}
		if (g == null) {
			return;
		}
		g.remove(object);
	}

	void removeGroup(Group<T> group) {
		writeLock.lock();
		try {
			String n = group.getName();
			Group<T> chk = groups.get(n);
			if (group == chk) {
				groups.remove(n);
			}
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Get a snapshot of the current groups, however the group objects
	 * themselves are not snapshots.
	 *
	 * @return snapshot of groups
	 */
	public Map<String, Group> getGroups() {
		readLock.lock();
		try {
			Map<String, Group> g = new HashMap<>();
			g.putAll(groups);
			return Collections.unmodifiableMap(g);
		} finally {
			readLock.unlock();
		}
	}
}
