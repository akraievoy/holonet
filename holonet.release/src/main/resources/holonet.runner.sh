java -server -Xmx2560m -cp `find jar -iname "*.jar" -printf '%p:'``find lib -iname "*.jar" -printf '%p:'`classes org.akraievoy.base.runner.Main >logs/output.log 2>logs/error.log
