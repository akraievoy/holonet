/*
 Copyright 2011 Anton Kraievoy akraievoy@gmail.com
 This file is part of Holonet.

 Holonet is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Holonet is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Holonet. If not, see <http://www.gnu.org/licenses/>.
 */

package org.akraievoy.base.runner.api;

import org.akraievoy.base.ref.Ref;

/**
 * Context-aware Ref implementation: offers caching, automatic value retrieval and storage.
 * <p/>
 * This is a generic abstraction, Spring context use concrete implementations for various value types.
 */
public abstract class RefCtx<T> implements ContextInjectable, Ref<T> {
  protected final Class<T> classOfT;

  private String path;
  private Context ctx;
  private boolean cached = true;
  private T vCache;

  protected RefCtx(final Class<T> classOfT) {
    this(null, classOfT);
  }

  protected RefCtx(T vCache, final Class<T> classOfT) {
    this.vCache = vCache;
    this.classOfT = classOfT;
  }

  public void setCtx(Context ctx) {
    this.ctx = ctx;

    //	TODO spooky effects: don't read DB in setter!
    if (isPersistable()) {
      if (!ctx.containsKey(path)) {
        final Object value = getVCache();
        if (value != null) {
          ctxPut(value);
        }
      } else {
        setVCache(ctxGet());
      }
    }
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }

  public boolean isCached() {
    return cached;
  }

  public void setCached(boolean cached) {
    this.cached = cached;
  }

  protected T getVCache() {
    return vCache;
  }

  protected void setVCache(T valueObj) {
    this.vCache = valueObj;
  }

  protected boolean isPersistable() {
    return ctx != null && path != null && path.trim().length() > 0;
  }

  protected T ctxGet() {
    return ctx.get(this.path, classOfT, false);
  }

  protected void ctxPut(Object value) {
    ctx.put(path, value, false);
  }

  /**
   * Queries context (checking the cache) and stores the result as transient value before returning.
   *
   * @return may be null
   */
  public T getValue() {
    if (isPersistable()) {
      if (!isCached() || getVCache() == null) {
        setVCache(ctxGet());
      }
    }

    return getVCache();
  }

  /**
   * Stores the value within context, if context is set.
   *
   * @param value may be null, does not erase anything previously stored in DB
   */
  public void setValue(T value) {
    setVCache(value);

    if (isPersistable()) {
      ctxPut(value);
    }
  }
}
