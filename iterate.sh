#!/bin/bash
set -o nounset
set -o errexit

mvn clean package
mkdir $HOME/holonet.release-1.3-standalone/ | echo err status suppressed
mv holonet.release/target/holonet.release-1.3-standalone.zip $HOME/holonet.release-1.3-standalone/
cd $HOME/holonet.release-1.3-standalone/
rm -rfv data logs
7z x -y holonet.release-1.3-standalone.zip
chmod u+x *.sh
if [ -r ~/holonet.release-1.3-standalone.7z ]; then
  rm -rf data/h2
  7z x ~/holonet.release-1.3-standalone.7z -o.. -i!holonet.release-1.3-standalone/data/h2/
  echo '---done reloading old database---'
fi
./holonet.runner.sh
cd $HOME/working/holonet.git
