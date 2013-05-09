package org.akraievoy.holonet.exp

import java.util.concurrent.Callable

package object store {

  trait CachePimps {

    import com.google.common.cache.CacheLoader
    import com.google.common.cache.Cache

    implicit def functionToCacheLoader[K, V](f: K => V) =
      new CacheLoader[K, V] {
        def load(key: K) = f(key)
      }

    implicit def functionToCallable[K, V](f: () => V) =
      new Callable[V] {
        def call() = f()
      }

    implicit def pimpCache[K, V](cache: Cache[K, V]) = {
      new PimpedCache(cache)
    }

    class PimpedCache[K, V](cache: Cache[K, V]) {
      def getOption(k: K): Option[V] = {
        Option(cache.getIfPresent(k))
      }

      def getOrLoad(k: K)(f: K => _ <: V): V = {
        val callable: Callable[_ <: V] = () => f(k)
        cache.get(k, callable)
      }
    }
  }
}
