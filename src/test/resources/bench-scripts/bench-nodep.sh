i=0
while [ $i -le 120000 ]
do
	java  -Xms2g -Xmx2g -classpath "./target/test-classes:./target/classes:~/.m2/repository/org/jboss/modules/jboss-modules/1.0.0.Beta1-SNAPSHOT/jboss-modules-1.0.0.Beta1-SNAPSHOT.jar:~/.m2/repository/org/jboss/jboss-vfs/3.0.0.CR5/jboss-vfs-3.0.0.CR5.jar:~/.m2/repository/org/jboss/logging/jboss-logging/3.0.0.Beta2/jboss-logging-3.0.0.Beta2.jar:~/.m2/repository/org/jboss/threads/jboss-threads/2.0.0.CR4/jboss-threads-2.0.0.CR4.jar:~/.m2/repository/junit/junit/4.7/junit-4.7.jar" org.jboss.msc.bench.NoDepBench $i
	i=$(( $i + 5000 ))

done
i=200000
while [ $i -le 1000000 ]
do
	java  -Xms2g -Xmx2g -classpath "./target/test-classes:./target/classes:~/.m2/repository/org/jboss/modules/jboss-modules/1.0.0.Beta1-SNAPSHOT/jboss-modules-1.0.0.Beta1-SNAPSHOT.jar:~/.m2/repository/org/jboss/jboss-vfs/3.0.0.CR5/jboss-vfs-3.0.0.CR5.jar:~/.m2/repository/org/jboss/logging/jboss-logging/3.0.0.Beta2/jboss-logging-3.0.0.Beta2.jar:~/.m2/repository/org/jboss/threads/jboss-threads/2.0.0.CR4/jboss-threads-2.0.0.CR4.jar:~/.m2/repository/junit/junit/4.7/junit-4.7.jar" org.jboss.msc.bench.NoDepBench $i
	i=$(( $i + 100000 ))

done
