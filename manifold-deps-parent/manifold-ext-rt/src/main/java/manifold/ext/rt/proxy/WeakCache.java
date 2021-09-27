/*
 * Copyright (c) 2021 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.ext.rt.proxy;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

final class WeakCache<K, P, V> {

  private final ReferenceQueue<K> refQueue
    = new ReferenceQueue<>();
  // the key type is Object for supporting null key
  private final ConcurrentMap<Object, ConcurrentMap<Object, Supplier<V>>> map
    = new ConcurrentHashMap<>();
  private final ConcurrentMap<Supplier<V>, Boolean> reverseMap
    = new ConcurrentHashMap<>();
  private final BiFunction<K, P, ?> subKeyFactory;
  private final BiFunction<K, P, V> valueFactory;

  /**
   * Construct an instance of {@code WeakCache}
   *
   * @param subKeyFactory a function mapping a pair of
   *                      {@code (key, parameter) -> sub-key}
   * @param valueFactory  a function mapping a pair of
   *                      {@code (key, parameter) -> value}
   * @throws NullPointerException if {@code subKeyFactory} or
   *                              {@code valueFactory} is null.
   */
  public WeakCache(BiFunction<K, P, ?> subKeyFactory,
                   BiFunction<K, P, V> valueFactory) {
    this.subKeyFactory = Objects.requireNonNull(subKeyFactory);
    this.valueFactory = Objects.requireNonNull(valueFactory);
  }

  /**
   * Look-up the value through the cache. This always evaluates the
   * {@code subKeyFactory} function and optionally evaluates
   * {@code valueFactory} function if there is no entry in the cache for given
   * pair of (key, subKey) or the entry has already been cleared.
   *
   * @param key       possibly null key
   * @param parameter parameter used together with key to create sub-key and
   *                  value (should not be null)
   * @return the cached value (never null)
   * @throws NullPointerException if {@code parameter} passed in or
   *                              {@code sub-key} calculated by
   *                              {@code subKeyFactory} or {@code value}
   *                              calculated by {@code valueFactory} is null.
   */
  public V get(K key, P parameter) {
    Objects.requireNonNull(parameter);

    expungeStaleEntries();

    Object cacheKey = WeakCache.CacheKey.valueOf(key, refQueue);

    // lazily install the 2nd level valuesMap for the particular cacheKey
    ConcurrentMap<Object, Supplier<V>> valuesMap = map.get(cacheKey);
    if (valuesMap == null) {
      ConcurrentMap<Object, Supplier<V>> oldValuesMap
        = map.putIfAbsent(cacheKey,
        valuesMap = new ConcurrentHashMap<>());
      if (oldValuesMap != null) {
        valuesMap = oldValuesMap;
      }
    }

    // create subKey and retrieve the possible Supplier<V> stored by that
    // subKey from valuesMap
    Object subKey = Objects.requireNonNull(subKeyFactory.apply(key, parameter));
    Supplier<V> supplier = valuesMap.get(subKey);
    WeakCache.Factory factory = null;

    while (true) {
      if (supplier != null) {
        // supplier might be a Factory or a CacheValue<V> instance
        V value = supplier.get();
        if (value != null) {
          return value;
        }
      }
      // else no supplier in cache
      // or a supplier that returned null (could be a cleared CacheValue
      // or a Factory that wasn't successful in installing the CacheValue)

      // lazily construct a Factory
      if (factory == null) {
        factory = new WeakCache.Factory(key, parameter, subKey, valuesMap);
      }

      if (supplier == null) {
        supplier = valuesMap.putIfAbsent(subKey, factory);
        if (supplier == null) {
          // successfully installed Factory
          supplier = factory;
        }
        // else retry with winning supplier
      } else {
        if (valuesMap.replace(subKey, supplier, factory)) {
          // successfully replaced
          // cleared CacheEntry / unsuccessful Factory
          // with our Factory
          supplier = factory;
        } else {
          // retry with current supplier
          supplier = valuesMap.get(subKey);
        }
      }
    }
  }

  /**
   * Checks whether the specified non-null value is already present in this
   * {@code WeakCache}. The check is made using identity comparison regardless
   * of whether value's class overrides {@link Object#equals} or not.
   *
   * @param value the non-null value to check
   * @return true if given {@code value} is already cached
   * @throws NullPointerException if value is null
   */
  public boolean containsValue(V value) {
    Objects.requireNonNull(value);

    expungeStaleEntries();
    return reverseMap.containsKey(new WeakCache.LookupValue<>(value));
  }

  /**
   * Returns the current number of cached entries that
   * can decrease over time when keys/values are GC-ed.
   */
  public int size() {
    expungeStaleEntries();
    return reverseMap.size();
  }

  private void expungeStaleEntries() {
    WeakCache.CacheKey<K> cacheKey;
    while ((cacheKey = (WeakCache.CacheKey<K>)refQueue.poll()) != null) {
      cacheKey.expungeFrom(map, reverseMap);
    }
  }

  /**
   * A factory {@link Supplier} that implements the lazy synchronized
   * construction of the value and installment of it into the cache.
   */
  private final class Factory implements Supplier<V> {

    private final K key;
    private final P parameter;
    private final Object subKey;
    private final ConcurrentMap<Object, Supplier<V>> valuesMap;

    Factory(K key, P parameter, Object subKey,
            ConcurrentMap<Object, Supplier<V>> valuesMap) {
      this.key = key;
      this.parameter = parameter;
      this.subKey = subKey;
      this.valuesMap = valuesMap;
    }

    @Override
    public synchronized V get() { // serialize access
      // re-check
      Supplier<V> supplier = valuesMap.get(subKey);
      if (supplier != this) {
        // something changed while we were waiting:
        // might be that we were replaced by a CacheValue
        // or were removed because of failure ->
        // return null to signal WeakCache.get() to retry
        // the loop
        return null;
      }
      // else still us (supplier == this)

      // create new value
      V value = null;
      try {
        value = Objects.requireNonNull(valueFactory.apply(key, parameter));
      } finally {
        if (value == null) { // remove us on failure
          valuesMap.remove(subKey, this);
        }
      }
      // the only path to reach here is with non-null value
      assert value != null;

      // wrap value with CacheValue (WeakReference)
      WeakCache.CacheValue<V> cacheValue = new WeakCache.CacheValue<>(value);

      // put into reverseMap
      reverseMap.put(cacheValue, Boolean.TRUE);

      // try replacing us with CacheValue (this should always succeed)
      if (!valuesMap.replace(subKey, this, cacheValue)) {
        throw new AssertionError("Should not reach here");
      }

      // successfully replaced us with new CacheValue -> return the value
      // wrapped by it
      return value;
    }
  }

  /**
   * Common type of value suppliers that are holding a referent.
   * The {@link #equals} and {@link #hashCode} of implementations is defined
   * to compare the referent by identity.
   */
  private interface Value<V> extends Supplier<V> {}

  /**
   * An optimized {@link WeakCache.Value} used to look-up the value in
   * {@link WeakCache#containsValue} method so that we are not
   * constructing the whole {@link WeakCache.CacheValue} just to look-up the referent.
   */
  private static final class LookupValue<V> implements WeakCache.Value<V>
  {
    private final V value;

    LookupValue(V value) {
      this.value = value;
    }

    @Override
    public V get() {
      return value;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(value); // compare by identity
    }

    @Override
    public boolean equals(Object obj) {
      return obj == this ||
        obj instanceof WeakCache.Value &&
          this.value == ((WeakCache.Value<?>) obj).get();  // compare by identity
    }
  }

  /**
   * A {@link WeakCache.Value} that weakly references the referent.
   */
  private static final class CacheValue<V>
    extends WeakReference<V> implements WeakCache.Value<V>
  {
    private final int hash;

    CacheValue(V value) {
      super(value);
      this.hash = System.identityHashCode(value); // compare by identity
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      V value;
      return obj == this ||
        obj instanceof WeakCache.Value &&
          // cleared CacheValue is only equal to itself
          (value = get()) != null &&
          value == ((WeakCache.Value<?>) obj).get(); // compare by identity
    }
  }

  /**
   * CacheKey containing a weakly referenced {@code key}. It registers
   * itself with the {@code refQueue} so that it can be used to expunge
   * the entry when the {@link WeakReference} is cleared.
   */
  private static final class CacheKey<K> extends WeakReference<K> {

    // a replacement for null keys
    private static final Object NULL_KEY = new Object();

    static <K> Object valueOf(K key, ReferenceQueue<K> refQueue) {
      return key == null
        // null key means we can't weakly reference it,
        // so we use a NULL_KEY singleton as cache key
        ? NULL_KEY
        // non-null key requires wrapping with a WeakReference
        : new WeakCache.CacheKey<>(key, refQueue);
    }

    private final int hash;

    private CacheKey(K key, ReferenceQueue<K> refQueue) {
      super(key, refQueue);
      this.hash = System.identityHashCode(key);  // compare by identity
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      K key;
      return obj == this ||
        obj != null &&
          obj.getClass() == this.getClass() &&
          // cleared CacheKey is only equal to itself
          (key = this.get()) != null &&
          // compare key by identity
          key == ((WeakCache.CacheKey<K>) obj).get();
    }

    void expungeFrom(ConcurrentMap<?, ? extends ConcurrentMap<?, ?>> map,
                     ConcurrentMap<?, Boolean> reverseMap) {
      // removing just by key is always safe here because after a CacheKey
      // is cleared and enqueue-ed it is only equal to itself
      // (see equals method)...
      ConcurrentMap<?, ?> valuesMap = map.remove(this);
      // remove also from reverseMap if needed
      if (valuesMap != null) {
        for (Object cacheValue : valuesMap.values()) {
          reverseMap.remove(cacheValue);
        }
      }
    }
  }
}
