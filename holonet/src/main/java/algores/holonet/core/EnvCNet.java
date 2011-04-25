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
import org.akraievoy.base.runner.api.RefCtx;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.WeightedEventModel;
import org.akraievoy.cnet.gen.vo.WeightedEventModelBase;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.ref.RefVertexData;
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

  protected WeightedEventModel nodeUse = new WeightedEventModelBase();
  protected WeightedEventModel nodeSel = new WeightedEventModelBase();

  protected List<Node> nodeIndex = new ArrayList<Node>();
  protected Set<Node> nodes = new HashSet<Node>();

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

  protected boolean refSet(final RefCtx[] refs) {
    for (RefCtx ref : refs) {
      if (ref == null || ref.getValue() == null) {
        return false;
      }
    }
    return true;
  }

  public void init() {
    if (!refSet(new RefCtx[]{locX, locY, density, dist, req})) {
      log.info("activating fallback to EnvSimple: not all refs set");
      fallback = new EnvSimple();
      return;
    }

    final VertexData density = this.density.getValue();
    final int size = density.getSize();

    for (int i = 0; i < size; i++) {
      nodeUse.add(i, density.get(i));
      nodeSel.add(i, density.get(i));
    }
  }

  public Address createNetworkAddress(EntropySource eSource) {
    if (fallback != null) {
      return fallback.createNetworkAddress(eSource);
    }

    final int nodeIdx = nodeSel.generate(eSource, true, null);

    return new AddressCNet(nodeIdx);
  }

  public Node getNode(Address address) {
    if (fallback != null) {
      return fallback.getNode(address);
    }

    final AddressCNet addrCNet = (AddressCNet) address;
    final int idx = addrCNet.getNodeIdx();

    while (nodeIndex.size() <= idx) {
      nodeIndex.add(null);
    }

    return nodeIndex.get(idx);
  }

  public void putNode(Node newNode, Address address) {
    if (fallback != null) {
      fallback.putNode(newNode, address);
      return;
    }

    final AddressCNet addrCNet = (AddressCNet) address;

    final int idx = addrCNet.getNodeIdx();

    while (nodeIndex.size() <= idx) {
      nodeIndex.add(null);
    }
    /*final Node oldNode = */
    nodeIndex.set(idx, newNode);
    nodes.add(newNode);
  }

  public void removeNode(Address address) {
    if (fallback != null) {
      fallback.removeNode(address);
      return;
    }

    final AddressCNet addrCNet = (AddressCNet) address;

    final int idx = addrCNet.getNodeIdx();

    final Node prevNode = nodeIndex.set(idx, null);
    nodes.remove(prevNode);

    //	return this slot to event model
    final VertexData density = this.density.getValue();
    nodeSel.add(idx, density.get(idx));
  }

  public Collection<Node> getAllNodes() {
    if (fallback != null) {
      return fallback.getAllNodes();
    }

    return nodes;
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
