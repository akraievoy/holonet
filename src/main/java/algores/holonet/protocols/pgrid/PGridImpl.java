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

package algores.holonet.protocols.pgrid;

import algores.holonet.capi.Event;
import algores.holonet.core.CommunicationException;
import algores.holonet.core.SimulatorException;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.Range;
import algores.holonet.core.api.tier0.storage.StorageService;
import algores.holonet.core.api.tier1.overlay.OverlayServiceBase;
import com.google.common.base.Optional;

import java.util.*;

import static algores.holonet.core.api.tier0.routing.RoutingPackage.*;

public class PGridImpl extends OverlayServiceBase implements PGrid {
  protected static final boolean TRACE_OPERATIONS = false;
  protected static final int THRESH = 5;

  protected List<String> operations;
  protected static final String[] PADDING = new String[]{"    ", "   ", "  ", " ", "", "", "", ""};

  public PGridImpl() {
    if (TRACE_OPERATIONS) {
      operations = new ArrayList<String>();
    }
  }

  public PGridImpl copy() {
    return new PGridImpl();
  }

  protected PGridRouting getRouting() {
    return (PGridRouting) super.getRouting();
  }

  public void join(Address oldNode) throws SimulatorException {
    stabilizeInternal(oldNode);
    stabilize();
  }

  protected void stabilizeInternal(Address oldNode) throws CommunicationException {
    Address curNode = oldNode;
    Set<Address> replicas = new HashSet<Address>();

    while (curNode != null && !curNode.equals(owner.getAddress())) {
      final PGrid rpc = rpc(curNode);
      PGridRouting result1 = getRouting();
      final Range path = result1.getPath();
      final Set<Key> keys = owner.getServices().getStorage().getDataEntries().keySet();
      InviteResponse response = rpc.invite(path, keys);

      PGridRouting result = getRouting();

      result.update(eventToRoute(Event.HEART_BEAT, response.getOwnRoute()));

      if (response.getDelegated() == null || replicas.contains(response.getDelegated().getAddress())) {
        break;
      }

      curNode = response.getDelegated().getAddress();
      replicas.add(curNode);
    }
  }

  public String toString() {
    final String keyNum = String.valueOf(owner.getServices().getStorage().getKeys().size());
    PGridRouting result = getRouting();
    return PADDING[keyNum.length()] + keyNum + "@" + result.getPath().toString();
  }

  public static class InviteResponse {
    final RoutingEntry delegated;
    final RoutingEntry ownRoute;

    public InviteResponse(RoutingEntry delegated, RoutingEntry ownRoute) {
      this.delegated = delegated;
      this.ownRoute = ownRoute;
    }

    public RoutingEntry getDelegated() {
      return delegated;
    }

    public RoutingEntry getOwnRoute() {
      return ownRoute;
    }
  }

  public InviteResponse invite(Range rPath, Set<Key> pKeys) throws CommunicationException {
    PGridRouting result2 = getRouting();
    if (result2.getPath().isSame(rPath)) {  //	keys are same
      if (isSplitRequired(rPath, pKeys)) {
        PGridRouting result = getRouting();
        balSplit(result.getPath(), pKeys);
        return inviteResponse(replicaRoute());
      } else {
        balDataExchange(pKeys);
        return inviteResponse(null);
      }
    }

    PGridRouting result1 = getRouting();
    if (result1.getPath().isPrefixFor(rPath, false)) {
      PGridRouting result = getRouting();
      if (isSplitRequired(result.getPath(), pKeys)) {
        unbalSpitL(rPath, pKeys);
        return inviteResponse(replicaRoute());
      } else {
        unbalDataExchangeL(rPath, pKeys);
        return inviteResponse(null);
      }
    }

    PGridRouting result = getRouting();
    if (rPath.isPrefixFor(result.getPath(), false)) {
      if (isSplitRequired(rPath, pKeys)) {
        unbalSplitR(rPath, pKeys);
        return inviteResponse(replicaRoute());
      } else {
        unbalDataExchangeR(rPath, pKeys);
        return inviteResponse(null);
      }
    }

    return inviteResponse(route(rPath));
  }

  //	TODO inline that

  protected RoutingEntry route(Range rPath) {
    PGridRouting result = getRouting();
    return result.localLookup(rPath.getKey(), 1, true).get(0);
  }

  protected RoutingEntry replicaRoute() {
    PGridRouting result = getRouting();
    PGridRouting result1 = getRouting();
    return result1.replicaSet(result.ownRoute().getKey(), Byte.MAX_VALUE).get(0);
  }

  protected InviteResponse inviteResponse(RoutingEntry delegated) {
    final int entries = owner.getServices().getStorage().getDataEntries().size();
    PGridRouting result = getRouting();
    final RoutingEntry updatedOwnRoute = result.ownRoute().entryCount(entries);
    return new InviteResponse(delegated, updatedOwnRoute);
  }

  protected boolean isUnbalanced(final Range prefixedPath, Set<Key> prefixKeys) {
    return prefixedPath.count(prefixKeys) > THRESH / 2;
  }

  protected void balDataExchange(Set<Key> pKeys) throws CommunicationException {
    //noinspection ConstantConditions
    PGridRouting result2 = getRouting();
    final String operation = TRACE_OPERATIONS ? "{balDataExchange} path: " + result2.getPath() : null;
    if (TRACE_OPERATIONS) {
      operations.add(operation);
    }
    PGridRouting result = getRouting();
    PGridRouting result1 = getRouting();
    split(result1.getPath(), result.getPath(), false, operation);
  }

  protected void unbalDataExchangeL(Range rPath, Set<Key> pKeys) throws CommunicationException {
    //noinspection ConstantConditions
    PGridRouting result1 = getRouting();
    final String operation = TRACE_OPERATIONS ? "{unbalDataExchangeL} localPath: " + result1.getPath() + " rPath: " + rPath : null;
    if (TRACE_OPERATIONS) {
      operations.add(operation);
    }
    PGridRouting result = getRouting();
    split(result.getPath(), rPath, false, operation);
  }

  protected void unbalDataExchangeR(Range rPath, Set<Key> pKeys) throws CommunicationException {
    //noinspection ConstantConditions
    PGridRouting result1 = getRouting();
    final String operation = TRACE_OPERATIONS ? "{unbalDataExchangeR} localPath: " + result1.getPath() + " rPath: " + rPath : null;
    if (TRACE_OPERATIONS) {
      operations.add(operation);
    }
    PGridRouting result = getRouting();
    split(result.getPath(), rPath, false, operation);
  }

  protected boolean isSplitRequired(Range extendedPath, Set<Key> pKeys) {
    return extendedPath.getBits() < Key.BITNESS && pKeys.size() + owner.getServices().getStorage().getKeys().size() > THRESH;  //	LATER change 5 to sampled value
  }

  protected void unbalSplitR(Range rPath, Set<Key> pKeys) throws CommunicationException {
    //noinspection ConstantConditions
    PGridRouting result2 = getRouting();
    final String operation = TRACE_OPERATIONS ? "{unbalSplitR} localPath: " + result2.getPath() + " rPath: " + rPath : null;
    if (TRACE_OPERATIONS) {
      operations.add(operation);
    }
    PGridRouting result = getRouting();
    PGridRouting result1 = getRouting();
    split(result1.getPath(), rPath.expandWithComplement(result.getPath()), true, operation);
  }

  protected void unbalSpitL(Range rPath, Set<Key> pKeys) throws CommunicationException {
    //noinspection ConstantConditions
    PGridRouting result1 = getRouting();
    final String operation = TRACE_OPERATIONS ? "{unbalSpitL} localPath: " + result1.getPath() + " rPath: " + rPath : null;
    if (TRACE_OPERATIONS) {
      operations.add(operation);
    }
    PGridRouting result = getRouting();
    split(result.getPath().expandWithComplement(rPath), rPath, true, operation);
  }

  protected void balSplit(Range samePath, Set<Key> pKeys) throws CommunicationException {
    final Range[] localAndRemotePaths = selectBalSplitPaths(samePath, pKeys);

    //noinspection ConstantConditions
    final String operation = TRACE_OPERATIONS ? "{balSplit} localPath: " + localAndRemotePaths[0] + " remotePath: " + localAndRemotePaths[1] + " remove" : null;
    if (TRACE_OPERATIONS) {
      operations.add(operation);
    }
    split(localAndRemotePaths[0], localAndRemotePaths[1], true, operation);
  }

  protected Range[] selectBalSplitPaths(Range samePath, Set<Key> pKeys) {
    final Range path0 = samePath.append(false);  //	extended path, 0 appended
    final Range path1 = samePath.append(true);    //	extended path, 1 appended

    //	number of local entries for 0-path, local entries for 1-path, and the same for remotes
    final int local0 = owner.getServices().getStorage().getKeyCountForPath(path0.getKey(), path0.getBits());
    final int local1 = owner.getServices().getStorage().getKeyCountForPath(path1.getKey(), path1.getBits());
    final int remote0 = path0.count(pKeys);
    final int remote1 = path1.count(pKeys);

    //	as in both cases we're unable to balance load any further
    //	try to minimize number of entries moved around
    final int my0 = local1 + remote0;
    final int my1 = local0 + remote1;

    if (my0 < my1) {
      return new Range[]{path0, path1};
    } else {
      return new Range[]{path1, path0};
    }
  }

  // Progressive request part of a split operation.

  protected void split(Range localPath, Range remotePath, final boolean remove, String operation) throws CommunicationException {
    if (TRACE_OPERATIONS) {
      operations.add("--> {split} localPath: " + localPath + " remotePath: " + remotePath + (remove ? " remove" : ""));
    }
    PGridRouting routingLocal = getRouting();

    routingLocal.setPath(localPath);

    final StorageService storage = owner.getServices().getStorage();
    final Map<Key, Object> movedData = storage.filterTo(remotePath.getKey(), remotePath.getBits(), remove, new TreeMap<Key, Object>());
    routingLocal.ownRoute().entryCount(storage.getDataEntries().size());

    SplitData splitData = new SplitData(routingLocal.routes().routes(), movedData);

    //	entries moved from remote party stored in the same variable
    SplitData rData = rpc(getCaller()).splitCallback(remotePath, splitData, localPath, remove, operation);

    storage.putAll(rData.getData());
    //	TODO discern caller own route from the table-stored routes (live rpc and indirect discovery)
    final Collection<RoutingEntry> routes = rData.getRoutes();
    routingLocal.update(eventToRoutes(Event.DISCOVERED, routes));
  }

  /**
   * Reverse request part of a split operation.
   */
  public SplitData splitCallback(Range newLocalPath, SplitData sData, Range newRemotePath, final boolean remove, String operation) {
    if (TRACE_OPERATIONS) {
      operations.add("{splitCallback} newLocalPath: " + newLocalPath + " newRemotePath: " + newRemotePath + (remove ? " remove" : "") + " <- " + operation);
    }
    final StorageService storage = owner.getServices().getStorage();
    storage.putAll(sData.getData());

    PGridRouting routingLocal = getRouting();
    routingLocal.setPath(newLocalPath);
    final Collection<RoutingEntry> sDataRoutes = sData.getRoutes();
    routingLocal.update(eventToRoutes(Event.DISCOVERED, sDataRoutes));

    sData.getData().clear();
    sData.getRoutes().clear();

    storage.filterTo(newRemotePath.getKey(), newRemotePath.getBits(), remove, sData.getData());
    routingLocal.ownRoute().entryCount(storage.getDataEntries().size());
    sData.getRoutes().clear();
    sData.getRoutes().addAll(routingLocal.routes().routes());

    return sData;
  }

  public void leave() throws CommunicationException {
    PGridRouting result2 = getRouting();
    Map<Key, Object> dataEntries = owner.getServices().getStorage().filter(result2.getPath().getKey(), 0, false);
    Collection<RoutingEntry> allRoutes = getRouting().routes().routes();

    final List<RoutingEntry> replicas = getRouting().replicaSet(getRouting().getPath().getKey(), Byte.MAX_VALUE);

    for (RoutingEntry replica : replicas) {
      if (replica.getAddress().equals(owner.getAddress())) {
        continue;
      }
      try {
        PGridRouting result = getRouting();
        PGridRouting result1 = getRouting();
        rpc(replica.getAddress()).splitCallback(
            result1.getPath(),
            new SplitData(allRoutes, dataEntries),
            result.getPath(),
            false,
            "mergeWithReplica"
        );
        return;
      } catch (CommunicationException e) {
        //	ignored
      }
    }

    //	no replicas
    for (RoutingEntry complement : allRoutes) {
      if (complement.getAddress().equals(owner.getAddress())) {
        continue;
      }

      try {
        PGridRouting result = getRouting();
        PGridRouting result1 = getRouting();
        rpc(complement.getAddress()).splitCallback(
            result1.getPath().getCommonPrefixPath(complement.getRange()),
            new SplitData(allRoutes, dataEntries),
            result.getPath(),
            false,
            "mergeWithReplica"
        );
        return;
      } catch (CommunicationException e) {
        //	ignored
      }
    }
  }

  @SuppressWarnings({"ConstantConditions"})
  public void stabilize() throws CommunicationException {
    final Set<Address> processed = new HashSet<Address>();
    boolean more = true;
    while (more) {
      PGridRouting result = getRouting();
      PGridRouting result1 = getRouting();
      final List<RoutingEntry> routes = result1.replicaSet(result.getPath().getKey(), Byte.MAX_VALUE);
      more = false;

      for (RoutingEntry route : routes) {
        final Address address = route.getAddress();
        if (!processed.contains(address)) {
          more = true;
          stabilizeInternal(address);
          processed.add(address);
        }
      }
    }
  }

  public PGrid rpc(final Address target) throws CommunicationException {
    final Optional<PGrid> pgOpt =
        owner.getServices().getRpc().rpcTo(target, PGrid.class);
    if (pgOpt.isPresent()) {
      return pgOpt.get();
    } else {
      getRouting().registerCommunicationFailure(getCaller(), false);
      throw new CommunicationException(
          String.format("%s is offline", target)
      );
    }
  }

  protected static class SplitData {
    protected final Collection<RoutingEntry> routes;
    protected final Map<Key, Object> data;

    public SplitData(Collection<RoutingEntry> routes, Map<Key, Object> data) {
      this.routes = routes;
      this.data = data;
    }

    public Collection<RoutingEntry> getRoutes() {
      return routes;
    }

    public Map<Key, Object> getData() {
      return data;
    }
  }
}
