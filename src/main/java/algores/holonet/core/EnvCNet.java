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

package algores.holonet.core;

import algores.holonet.core.api.API;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.Range;
import com.google.common.base.Optional;
import gnu.trove.TIntArrayList;
import org.akraievoy.base.ref.Ref;
import org.akraievoy.cnet.metrics.api.Metric;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.WeightedEventModel;
import org.akraievoy.cnet.gen.vo.WeightedEventModelBase;
import org.akraievoy.cnet.metrics.domain.MetricEDataRouteLen;
import org.akraievoy.cnet.metrics.domain.MetricRoutesFloydWarshall;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.IndexCodec;
import org.akraievoy.cnet.net.vo.VertexData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class EnvCNet implements Env {
  private static final Logger log = LoggerFactory.getLogger(EnvCNet.class);

  protected Ref<VertexData> locX;
  protected Ref<VertexData> locY;
  protected Ref<VertexData> density;

  protected Ref<? extends EdgeData> dist;
  protected Ref<? extends EdgeData> req;
  protected Ref<? extends EdgeData> overlay;
  protected Ref<EdgeData> overlayDist = new RefObject<EdgeData>();
  protected double overlayDistDiameter;

  protected WeightedEventModel nodeModel = new WeightedEventModelBase(Optional.of("nodes"));
  protected WeightedEventModel requestModel = new WeightedEventModelBase(Optional.of("requests"));
  protected IndexCodec requestCodec = new IndexCodec(false);

  protected HashMap<Integer, Node> addressIdxToNode = new HashMap<Integer, Node>(256, 0.25f);
  protected final EnvMappings mappings = new EnvMappings();

  protected EnvSimple fallback = null;

  public void setDensity(Ref<VertexData> density) {
    this.density = density;
  }

  public void setDist(Ref<? extends EdgeData> dist) {
    this.dist = dist;
  }

  public void setLocX(Ref<VertexData> locX) {
    this.locX = locX;
  }

  public void setLocY(Ref<VertexData> locY) {
    this.locY = locY;
  }

  public void setReq(Ref<? extends EdgeData> req) {
    this.req = req;
  }

  public void setOverlay(Ref<? extends EdgeData> overlay) {
    this.overlay = overlay;
  }

  protected int refSet(final Ref<?>[] refs) {
    for (int i = 0; i < refs.length; i++) {
      Ref<?> ref = refs[i];
      if (ref == null || ref.getValue() == null) {
        return i;
      }
    }
    return -1;
  }

  public void init() {
    int unsetRefIndex = refSet(
        new Ref<?>[]{locX, locY, density, dist, req, overlay}
    );
    if (unsetRefIndex >= 0) {
      log.warn(
          "activating fallback to EnvSimple: ref {} not set",
          new String[]{"locX", "locY", "density", "dist", "req", "overlay"}[unsetRefIndex]
      );
      fallback = new EnvSimple();
      return;
    }

    final MetricEDataRouteLen metricEDataRouteLen =
        new MetricEDataRouteLen(new MetricRoutesFloydWarshall());
    metricEDataRouteLen.getRoutes().setDistSource(dist);
    metricEDataRouteLen.getRoutes().setSource(overlay);

    overlayDist.setValue(Metric.fetch(metricEDataRouteLen));
    final double[] overlayDistDiam = { 0.0 };
    overlayDist.getValue().visitNonDef(new EdgeData.EdgeVisitor() {
      @Override
      public void visit(int from, int into, double e) {
        overlayDistDiam[0] = Math.max(overlayDistDiam[0], e);
      }
    });
    overlayDistDiameter = overlayDistDiam[0];

    final VertexData density = this.density.getValue();
    final int size = density.getSize();

    for (int i = 0; i < size; i++) {
      nodeModel.add(i, density.get(i));
    }
    renewRequestModel();
  }

  protected void renewRequestModel() {
    requestModel.clear();
    if (req.getValue().getNonDefCount() == 0) {
      throw new IllegalStateException("empty request network");
    }
    req.getValue().visitNonDef(new EdgeData.EdgeVisitor() {
      public void visit(int from, int into, double e) {
        if (addressIdxToNode.containsKey(from) && addressIdxToNode.containsKey(into)) {
          requestModel.add(requestCodec.fi2id(from, into), e);
        }
      }
    });
  }

  @Override
  public double apply(
      Address localAddress, Key target,
      Address curAddress, Range curRange
  ) {
    if (fallback != null) {
      return fallback.apply(localAddress, target, curAddress, curRange);
    }

    final AddressCNet local = (AddressCNet) localAddress;
    final AddressCNet curr = (AddressCNet) curAddress;

    final EdgeData overlayEdges = overlayDist.getValue();
    final double overlayEdge =
        overlayEdges.get(local.getNodeIdx(), curr.getNodeIdx());
    final double overlayEdgeDist = overlayEdge / overlayDistDiameter;

    return 1 + overlayEdgeDist/8;
  }

  private final SortedMap<Address, List<Address>> seedLinksCache = new TreeMap<Address, List<Address>>();

  @Override
  public List<Address> seedLinks(Address localAddress) {
    if (fallback != null) {
      return fallback.seedLinks(localAddress);
    }

    final List<Address> cachedSeedLinks = seedLinksCache.get(localAddress);
    if (cachedSeedLinks != null) {
      return cachedSeedLinks;
    }

    final ArrayList<Address> seedLinks = new ArrayList<Address>();
    for (Node node : addressIdxToNode.values()) {
      seedLinks.add(node.getAddress());
    }
    for (Iterator<Address> slIt = seedLinks.iterator(); slIt.hasNext(); ) {
      if (!seedLink(localAddress, slIt.next())) {
        slIt.remove();
      }
    }

    seedLinksCache.put(localAddress, seedLinks);

    return seedLinks;
  }

  @Override
  public boolean seedLink(Address from, Address into) {
    if (fallback != null) {
      return fallback.seedLink(from, into);
    }

    return overlay.getValue().conn(
        ((AddressCNet) from).getNodeIdx(),
        ((AddressCNet) into).getNodeIdx()
    );
  }

  public Address createNetworkAddress(EntropySource eSource) {
    if (fallback != null) {
      return fallback.createNetworkAddress(eSource);
    }

    final int nodeIdx = nodeModel.generate(eSource, true, null);

    return new AddressCNet(nodeIdx);
  }

  @Override
  public SortedMap<Key, Node> keyToNode() {
    final TreeMap<Key, Node> keyToNode = new TreeMap<Key, Node>();
    for (Map.Entry<Integer, Node> addrToNode : this.addressIdxToNode.entrySet()) {
      keyToNode.put(
          addrToNode.getValue().getKey(),
          addrToNode.getValue()
      );
    }
    return keyToNode;
  }

  @Override
  public int indexOf(Address address) {
    if (fallback != null) {
      return fallback.indexOf(address);
    }

    return ((AddressCNet) address).getNodeIdx();
  }

  public Node getNode(Address address) {
    if (fallback != null) {
      return fallback.getNode(address);
    }

    final AddressCNet addrCNet = (AddressCNet) address;
    final int idx = addrCNet.getNodeIdx();

    return addressIdxToNode.get(idx);
  }

  public void putNode(Node newNode, Address address) {
    if (fallback != null) {
      fallback.putNode(newNode, address);
      return;
    }

    final AddressCNet addrCNet = (AddressCNet) address;

    final int idx = addrCNet.getNodeIdx();

    addressIdxToNode.put(idx, newNode);
    renewRequestModel();
    seedLinksCache.clear();
  }

  public void removeNode(Address address) {
    if (fallback != null) {
      fallback.removeNode(address);
      return;
    }

    final AddressCNet addrCNet = (AddressCNet) address;
    final int idx = addrCNet.getNodeIdx();
    addressIdxToNode.remove(idx);
    renewRequestModel();

    //	return this slot to event model
    final VertexData density = this.density.getValue();
    nodeModel.add(idx, density.get(idx));
    seedLinksCache.clear();
  }

  public Collection<Node> getAllNodes() {
    if (fallback != null) {
      return fallback.getAllNodes();
    }

    return addressIdxToNode.values();
  }

  public EnvMappings getMappings() {
    return mappings;
  }

  @Override
  public Optional<RequestPair> generateRequestPair(EntropySource entropy) {
    if (fallback != null) {
      return fallback.generateRequestPair(entropy);
    }
    if (requestModel.getSize() == 0) {
      return Optional.absent();
    }

    final int pairId = requestModel.generate(entropy, false, null);

    final int clientId = requestCodec.id2leading(pairId);
    final int serverId = requestCodec.id2trailing(pairId);

    return Optional.of(
        new RequestPair(
          addressIdxToNode.get(clientId),
          addressIdxToNode.get(serverId)
        )
    );
  }

  protected static final NumberFormat nf = createNumberFormat();

  protected static NumberFormat createNumberFormat() {
    final NumberFormat nf = DecimalFormat.getNumberInstance();

    nf.setMaximumFractionDigits(3);
    nf.setMinimumFractionDigits(3);

    return nf;
  }

  protected class AddressCNet implements Address {
    protected final int nodeIdx;
    protected Key key = null;
    protected String toString;

    public AddressCNet(int nodeIdx) {
      this.nodeIdx = nodeIdx;
    }

    public double getDistance(Address address) {
      final AddressCNet addrCNet = (AddressCNet) address;

      return dist.getValue().get(nodeIdx, addrCNet.nodeIdx);
    }

    public Address getAddress() {
      return this;
    }

    public String toString() {
      if (toString == null) {
        toString = "(" + nf.format(locX.getValue().get(nodeIdx)) + ":" + nf.format(locY.getValue().get(nodeIdx)) + ")";
      }
      return toString;
    }

    public synchronized Key getKey() {
      if (key == null) {
        key = API.createKey(this);
      }
      return key;
    }

    public int compareTo(algores.holonet.capi.Address that) {
      final AddressCNet addrCNet = (AddressCNet) that;

      //  FIXME should this order be compatible with order of keys?
      return this.nodeIdx - addrCNet.nodeIdx;
    }

    public int getNodeIdx() {
      return nodeIdx;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      AddressCNet that = (AddressCNet) o;

      if (nodeIdx != that.nodeIdx) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return nodeIdx;
    }
  }
}
