/*
 Copyright 2012 Anton Kraievoy akraievoy@gmail.com
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

package org.akraievoy.holonet.exp

import data.{DhtSim, OverlayGO, OverlayEnum, DlaGenImages}

trait RegistryData {
  lazy val experiments: Seq[Experiment] = Seq(
    DlaGenImages.experiment,
    OverlayEnum.experiment,
    OverlayGO.experiment1physDataset,
    OverlayGO.experiment2overlayDataset,
    OverlayGO.experiment3genetics,
    DhtSim.experiment1seeds,
    DhtSim.experiment2paramSpace,
    DhtSim.experiment3attack,
    DhtSim.experiment3attackChained,
    DhtSim.experiment3attackDestab,
    DhtSim.experiment3attackDestabChained,
    DhtSim.experiment3destab,
    DhtSim.experiment3destabChained,
    DhtSim.experiment3static,
    DhtSim.experiment3staticChained
  )

}
