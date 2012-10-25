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
import org.akraievoy.base.runner.api.RefCtx;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.WeightedEventModel;
import org.akraievoy.cnet.gen.vo.WeightedEventModelBase;
import org.akraievoy.cnet.metrics.api.MetricResultFetcher;
import org.akraievoy.cnet.metrics.domain.MetricEDataRouteLen;
import org.akraievoy.cnet.metrics.domain.MetricRoutesFloydWarshall;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.ref.RefVertexData;
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

  protected RefVertexData locX;
  protected RefVertexData locY;
  protected RefVertexData density;

  protected RefEdgeData dist;
  protected RefEdgeData req;
  protected RefEdgeData overlay;
  protected RefEdgeData overlayDist = new RefEdgeData();
  protected double overlayDistDiameter;

  protected WeightedEventModel nodeModel = new WeightedEventModelBase(Optional.of("nodes"));
  protected WeightedEventModel requestModel = new WeightedEventModelBase(Optional.of("requests"));
  protected IndexCodec requestCodec = new IndexCodec(false);

  protected SortedMap<Integer, Node> nodeIndex = new TreeMap<Integer, Node>();
  protected final EnvMappings mappings = new EnvMappings();

  protected EnvSimple fallback = null;

  public void setDensity(RefVertexData density) {
    this.density = density;
  }

  public void setDist(RefEdgeData dist) {
    this.dist = dist;
  }

  public void setLocX(RefVertexData locX) {
    this.locX = locX;
  }

  public void setLocY(RefVertexData locY) {
    this.locY = locY;
  }

  public void setReq(RefEdgeData req) {
    this.req = req;
  }

  public void setOverlay(RefEdgeData overlay) {
    this.overlay = overlay;
  }

  protected boolean refSet(final RefCtx[] refs) {
    for (RefCtx ref : refs) {
      if (ref == null || ref.getValue() == null) {
        return false;
      }
    }
    return true;
  }

  public void init() {
    if (!refSet(new RefCtx[]{locX, locY, density, dist, req, overlay})) {
      log.warn("activating fallback to EnvSimple: not all refs set");
      fallback = new EnvSimple();
      return;
    }

    final MetricEDataRouteLen metricEDataRouteLen =
        new MetricEDataRouteLen(new MetricRoutesFloydWarshall());
    metricEDataRouteLen.getRoutes().setDistSource(dist);
    metricEDataRouteLen.getRoutes().setSource(overlay);

    overlayDist.setValue(
        (EdgeData) MetricResultFetcher.fetch(metricEDataRouteLen)
    );
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
    req.getValue().visitNonDef(new EdgeData.EdgeVisitor() {
      public void visit(int from, int into, double e) {
        if (nodeIndex.containsKey(from) && nodeIndex.containsKey(into)) {
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

    return 1+overlayEdgeDist/8;
  }

  public Address createNetworkAddress(EntropySource eSource) {
    if (fallback != null) {
      return fallback.createNetworkAddress(eSource);
    }

    final int nodeIdx = nodeModel.generate(eSource, true, null);

    return new AddressCNet(nodeIdx);
  }

  public Node getNode(Address address) {
    if (fallback != null) {
      return fallback.getNode(address);
    }

    final AddressCNet addrCNet = (AddressCNet) address;
    final int idx = addrCNet.getNodeIdx();

    return nodeIndex.get(idx);
  }

  public void putNode(Node newNode, Address address) {
    if (fallback != null) {
      fallback.putNode(newNode, address);
      return;
    }

    final AddressCNet addrCNet = (AddressCNet) address;

    final int idx = addrCNet.getNodeIdx();

    nodeIndex.put(idx, newNode);
    renewRequestModel();
  }

  public void removeNode(Address address) {
    if (fallback != null) {
      fallback.removeNode(address);
      return;
    }

    final AddressCNet addrCNet = (AddressCNet) address;
    final int idx = addrCNet.getNodeIdx();
    nodeIndex.remove(idx);
    renewRequestModel();

    //	return this slot to event model
    final VertexData density = this.density.getValue();
    nodeModel.add(idx, density.get(idx));
  }

  public Collection<Node> getAllNodes() {
    if (fallback != null) {
      return fallback.getAllNodes();
    }

    return nodeIndex.values();
  }

  public EnvMappings getMappings() {
    return mappings;
  }

  @Override
  public RequestPair generateRequestPair(EntropySource entropy) {
    final int pairId = requestModel.generate(entropy, false, null);

    final int clientId = requestCodec.id2leading(pairId);
    final int serverId = requestCodec.id2trailing(pairId);

    return new RequestPair(
        nodeIndex.get(clientId),
        nodeIndex.get(serverId)
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

    public void init(EntropySource eSource) {

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

      return this.nodeIdx - addrCNet.nodeIdx;
    }

    public int getNodeIdx() {
      return nodeIdx;
    }
  }
}
