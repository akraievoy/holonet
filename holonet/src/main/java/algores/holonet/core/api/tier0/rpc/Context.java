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
import org.akraievoy.base.Die;
import org.akraievoy.base.introspect.Introspect;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Context {
  final Network parentNetwork;

  final RemotingHandler handler;
  final Map<Class, Object> serviceProxies = new HashMap<Class, Object>();

  final Stack<Call> calls = new Stack<Call>();
  int armedCalls = 0;

  public Context(Network parentNetwork) {
    this.parentNetwork = parentNetwork;

    handler = new RemotingHandler(this);
  }

  @SuppressWarnings({"unchecked"})
  public <E> E setupCall(final Node newRpcSource, final Address newRpcTarget, final Class<E> service) {
    Die.ifFalse("someone created an rpc proxy and then left it with no call", armedCalls == calls.size());
    calls.push(new Call(newRpcSource, newRpcTarget, service));

    Object cachedProxy = serviceProxies.get(service);
    if (cachedProxy == null) {
      cachedProxy = Proxy.newProxyInstance(getClass().getClassLoader(), Introspect.getDeepInterfaces(service), handler);
      serviceProxies.put(service, cachedProxy);
    }

    return (E) cachedProxy;
  }

  public Node lookupTarget() throws CommunicationException {
    final Call call = getCall();
    Die.ifNull("call", call);

    Die.ifFalse("trying to use rpc proxy twice", armedCalls < calls.size());
    armedCalls++;

    parentNetwork.registerRpcCall(call.getSource(), call.getTarget());

    final Node node = parentNetwork.getEnv().getNode(call.getTarget());

    parentNetwork.getInterceptor().registerRpcCallResult(node != null);
    if (node == null) {
      call.getSource().getServices().getRouting().registerCommunicationFailure(call.getTarget());
      throw new CommunicationException("Node for address " + call.getTarget() + " is not alive.");
    }

    parentNetwork.registerRpcCall(call.getSource(), call.getTarget());

    return node;
  }

  public Call getCall() {
    return calls.peek();
  }

  //	LATER test on this
  public void onCallCompletion() {
    armedCalls--;
    calls.pop();
  }
}