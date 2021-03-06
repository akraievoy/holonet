# General

*Package*: holonet
*Copyright/Maintenance* 2011 Anton Kraievoy akraievoy@gmail.com

Project issues are tracked in Pivotal Tracker:
  https://www.pivotaltracker.com/projects/279361

# Features of this project:

 - an API for complex networks, aiming to be performant, but not yet profiled/compared with competition;
 - several metrics implemented over the former API, most notable are EigenGap/EigenVector metrics;
 - several random models to generate complex networks;
 - configurable/chainable Spring-based experiments: runner module with Swing-based experiment selector;
 - experiment configuration allows to profile a metric against several input parameters (multi-axis parameter spaces);
 - persistence of experiment results into an H2 database;
 - tab-separated-values export for any scalar metric for any experiment; 
 - configurable Pajek exports for network/vector metrics for any experiment;
 - DHT simulation environment with environment modelled via random complex network models;
 - DHT API layering coherent with Common API description;
 - several versions of Chord protocol, P-Grid protocol still on the way;
 - a simple framework to run Genetic Optimization over a network with metric-based constraints;
 
Let me know if:

 * you want to use the code (I'll gladly add javadocs to unclear sites, revise your feature requests or whatsoever);
 * you think I've missed you or someone else in the copyright notices;
 * you want to re-license this code under any other terms. 

# Trivia

## Sample of license headers

    /*
        This file is part of holonet.

        holonet is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        holonet is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with holonet. If not, see <http://www.gnu.org/licenses/>.
    */

Sample of copyright header.

    /*
        Copyright ${year} ${author} ${email}
    */

#Intro

## Description of the modules:

 * base-runner: loader for Spring defitions of experiments and runner for them
 * base-runner-spring: small container project for custom Spring handlers
 * base-db: custom jdbs wrapper which should be removed
 * build.versioning: numbering the builds for GIT
 * cnet: value objects for complex networks (no so large actually, up to 1k nodes)
 * holonet: common API for DHT data stores, Chord, P-Grid
 * holonet.release: when you do mvn clean package in the project root, here (in target dir) you receive project distro
 * holonet.runner: some bridging across holonet + cnet modules : base-runner

## Experiment definitions are located in:
  https://github.com/akraievoy/holonet/tree/master/holonet.release/src/main/resources/data/import

# Running the project
FIXME

