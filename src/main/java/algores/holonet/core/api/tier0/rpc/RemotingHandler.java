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

package algores.holonet.core.api.tier0.rpc;

import algores.holonet.core.Node;
import org.akraievoy.base.Die;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * This class solely handles aspect of tracking emulated RPC calls.
 * <p/>
 * This was done via reflection and dynamic proxies (questionable approach actually).
 * Main idea is to generate a dynamic proxy that will redirect method calls to an instance of this class.
 */
public class RemotingHandler implements InvocationHandler {
  private static final Logger log = LoggerFactory.getLogger(RemotingHandler.class);

  final Context ctx;

  public RemotingHandler(Context ctx) {
    this.ctx = ctx;
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getDeclaringClass().equals(Object.class)) {
      //	some built-in method may be called from debugger, ignore that
      if (method.getReturnType().equals(String.class)) {
        return "*PROXIED*";
      }
      if (method.getReturnType().equals(Integer.TYPE)) {
        return 13;
      }
      if (method.getReturnType().equals(Boolean.TYPE)) {
        return true;
      }
      throw Die.unexpected("method", method.toString());
    }

    try {
      Node target = ctx.onCallStarted();
      return method.invoke(resolveService(target.getServices()), args);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    } catch (UndeclaredThrowableException e) {
      throw e.getCause();
    } finally {
      try {
        ctx.onCallCompleted();
      } catch (Exception e) {
        log.debug("unusual exception on ctx.onCallCompleted()", e);
      }
    }
  }

  @SuppressWarnings({"unchecked"})
  protected Object resolveService(ServiceRegistry target) {
    return target.resolveService(ctx.getActiveRequest().get().getService());
  }
}
