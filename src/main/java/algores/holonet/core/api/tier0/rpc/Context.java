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

import algores.holonet.core.CommunicationException;
import algores.holonet.core.Network;
import algores.holonet.core.Node;
import algores.holonet.core.api.Address;
import com.google.common.base.Optional;
import org.akraievoy.base.introspect.Introspect;

import java.lang.reflect.Proxy;
import java.util.*;

public class Context {
  final Network parentNetwork;

  final RemotingHandler handler;
  final Map<Class, Object> serviceProxies = new HashMap<Class, Object>();

  private final List<Call> activeRequests = new ArrayList<Call>();
  private int servedRequests = 0;
  private Node targetNode;

  public Context(Network parentNetwork) {
    this.parentNetwork = parentNetwork;

    handler = new RemotingHandler(this);
  }

  public <E> Optional<E> setupCall(final Node newRpcSource, final Address newRpcTarget, final Class<E> service) {
    if (activeRequests.size() != servedRequests) {
      //  someone created an rpc proxy and then left it with no call
      throw new IllegalStateException(
          String.format(
              "activeRequests.size(%d) > servedRequests(%d)",
              activeRequests.size(),
              servedRequests
          )
      );
    }

    final Call request = new Call(newRpcSource, newRpcTarget, service);

    @SuppressWarnings("unchecked")
    E cachedProxy = (E) serviceProxies.get(service);
    if (cachedProxy == null) {
      @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
      final E newProxy =
          (E) Proxy.newProxyInstance(
              getClass().getClassLoader(),
              Introspect.getDeepInterfaces(service),
              handler
          );
      cachedProxy = newProxy;
      serviceProxies.put(service, cachedProxy);
    }

    parentNetwork.registerRpcCall(request.getSource(), request.getTarget());
    targetNode = parentNetwork.getEnv().getNode(request.getTarget());
    parentNetwork.getInterceptor().registerRpcCallResult(
        request.getSource().getAddress(),
        request.getTarget(),
        targetNode != null
    );

    if (targetNode != null) {
      activeRequests.add(request);
      return Optional.of(cachedProxy);
    } else {
      return Optional.absent();
    }
  }

  public Node onCallStarted() throws CommunicationException {
    if (activeRequests.isEmpty()) {
      //  proxy not set up properly before call
      throw new IllegalStateException(
          "activeRequests.isEmpty"
      );
    }

    if (activeRequests.size() <= servedRequests) {
      //  trying to use RPC proxy twice
      throw new IllegalStateException(
          String.format(
              "activeRequests.size(%d) <= servedRequests(%d)",
              activeRequests.size(),
              servedRequests
          )
      );
    }

    if (targetNode == null) {
      throw new IllegalStateException(
          "targetNode == null"
      );
    }

    servedRequests++;
    final Call call = getActiveRequest().get();
    parentNetwork.registerRpcCall(call.getSource(), call.getTarget());

    return targetNode;
  }

  public void onCallCompleted() {
    Call call = activeRequests.get(activeRequests.size() - 1);
    parentNetwork.getInterceptor().registerRpcCallResult(
        call.getSource().getAddress(),
        call.getTarget(),
        true
    );
    activeRequests.remove(activeRequests.size() - 1);
    servedRequests--;
  }

  public Optional<Call> getActiveRequest() {
    if (activeRequests.isEmpty()) {
      return Optional.absent();
    }
    return Optional.of(activeRequests.get(activeRequests.size() - 1));
  }
}