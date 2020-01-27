package io.siggi.http.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class LazyHashMap<K, V> extends HashMap<K, V> {

	protected abstract void prepare();
	private boolean didPrep = false;

	private void prep() {
		if (didPrep) {
			return;
		}
		didPrep = true;
		prepare();
	}

	@Override
	public V remove(Object arg0) {
		prep();
		return super.remove(arg0);
	}

	@Override
	public boolean remove(Object arg0, Object arg1) {
		prep();
		return super.remove(arg0, arg1);
	}

	@Override
	public V get(Object arg0) {
		prep();
		return super.get(arg0);
	}

	@Override
	public V put(K arg0, V arg1) {
		prep();
		return super.put(arg0, arg1);
	}

	@Override
	public java.util.Collection<V> values() {
		prep();
		return super.values();
	}

	@Override
	public void clear() {
		prep();
		super.clear();
	}

	@Override
	public boolean isEmpty() {
		prep();
		return super.isEmpty();
	}

	@Override
	public boolean replace(K arg0, V arg1, V arg2) {
		prep();
		return super.replace(arg0, arg1, arg2);
	}

	@Override
	public V replace(K arg0, V arg1) {
		prep();
		return super.replace(arg0, arg1);
	}

	@Override
	public void replaceAll(java.util.function.BiFunction<? super K, ? super V, ? extends V> arg0) {
		prep();
		super.replaceAll(arg0);
	}

	@Override
	public int size() {
		prep();
		return super.size();
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		prep();
		return super.entrySet();
	}

	@Override
	public void putAll(java.util.Map<? extends K, ? extends V> arg0) {
		prep();
		super.putAll(arg0);
	}

	@Override
	public V putIfAbsent(K arg0, V arg1) {
		prep();
		return super.putIfAbsent(arg0, arg1);
	}

	@Override
	public java.util.Set<K> keySet() {
		prep();
		return super.keySet();
	}

	@Override
	public boolean containsValue(Object arg0) {
		prep();
		return super.containsValue(arg0);
	}

	@Override
	public boolean containsKey(Object arg0) {
		prep();
		return super.containsKey(arg0);
	}

	@Override
	public V getOrDefault(Object arg0, V arg1) {
		prep();
		return super.getOrDefault(arg0, arg1);
	}

	@Override
	public void forEach(java.util.function.BiConsumer<? super K, ? super V> arg0) {
		prep();
		super.forEach(arg0);
	}

	@Override
	public V computeIfAbsent(K arg0, java.util.function.Function<? super K, ? extends V> arg1) {
		prep();
		return super.computeIfAbsent(arg0, arg1);
	}

	@Override
	public V computeIfPresent(K arg0, java.util.function.BiFunction<? super K, ? super V, ? extends V> arg1) {
		prep();
		return super.computeIfPresent(arg0, arg1);
	}

	@Override
	public V compute(K arg0, java.util.function.BiFunction<? super K, ? super V, ? extends V> arg1) {
		prep();
		return super.compute(arg0, arg1);
	}

	@Override
	public V merge(K arg0, V arg1, java.util.function.BiFunction<? super V, ? super V, ? extends V> arg2) {
		prep();
		return super.merge(arg0, arg1, arg2);
	}

	@Override
	@SuppressWarnings(value = {"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
	public Object clone() {
		prep();
		HashMap<K, V> n = new HashMap<K, V>();
		n.putAll(this);
		return n;
	}
}
