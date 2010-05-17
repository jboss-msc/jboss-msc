i=0
while [ $i -le 120000 ]
do
	java  -XX:MaxPermSize=512M -Xms2g -Xmx2g -classpath "./target/test-classes:./target/classes:$HOME/.m2/repository/org/jboss/modules/jboss-modules/1.0.0.Beta1-SNAPSHOT/jboss-modules-1.0.0.Beta1-SNAPSHOT.jar:$HOME/.m2/repository/org/jboss/jboss-vfs/3.0.0.CR5/jboss-vfs-3.0.0.CR5.jar:$HOME/.m2/repository/org/jboss/logging/jboss-logging/3.0.0.Beta2/jboss-logging-3.0.0.Beta2.jar:$HOME/.m2/repository/org/jboss/threads/jboss-threads/2.0.0.CR4/jboss-threads-2.0.0.CR4.jar:$HOME/.m2/repository/junit/junit/4.7/junit-4.7.jar" org.jboss.msc.bench.NoDepsSleepyStartBench $i $1
	i=$(( $i + 5000 ))

done
i=200000
while [ $i -le 1000000 ]
do
	java  -XX:MaxPermSize=512M -Xms2g -Xmx2g -classpath "./target/test-classes:./target/classes:$HOME/.m2/repository/org/jboss/modules/jboss-modules/1.0.0.Beta1-SNAPSHOT/jboss-modules-1.0.0.Beta1-SNAPSHOT.jar:$HOME/.m2/repository/org/jboss/jboss-vfs/3.0.0.CR5/jboss-vfs-3.0.0.CR5.jar:$HOME/.m2/repository/org/jboss/logging/jboss-logging/3.0.0.Beta2/jboss-logging-3.0.0.Beta2.jar:$HOME/.m2/repository/org/jboss/threads/jboss-threads/2.0.0.CR4/jboss-threads-2.0.0.CR4.jar:$HOME/.m2/repository/junit/junit/4.7/junit-4.7.jar" org.jboss.msc.bench.NoDepsSleepyStartBench $i $1
	i=$(( $i + 100000 ))

done
