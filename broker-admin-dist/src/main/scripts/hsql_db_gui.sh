#!/usr/bin/env bash




"$JAVA_HOME"/bin/java -cp lib/\* org.hsqldb.util.DatabaseManagerSwing --rcfile sqltool.rc --urlid broker

# For command line interface, you can use the sqltool:
# "$JAVA_HOME"/bin/java -cp lib/\* org.hsqldb.cmdline.SqlTool --rcFile=sqltool.rc broker

