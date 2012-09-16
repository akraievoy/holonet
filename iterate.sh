#!/bin/bash
mvn ${0} clean package
mkdir $HOME/holonet.release-1.3-standalone/
mv holonet.release/target/holonet.release-1.3-standalone.zip $HOME/holonet.release-1.3-standalone/
cd $HOME/holonet.release-1.3-standalone/
rm -rfv data logs
7z x -y holonet.release-1.3-standalone.zip
chmod u+x *.sh
./holonet.runner.sh
cd $HOME/working/holonet.git
