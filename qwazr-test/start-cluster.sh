#!/bin/sh

# This script launch 5 instance of QWAZR.
# 2 masters and 4 nodes.
# Each instance has its own data directory (master1, master2, node3, node4, node5, node6).

# First we launch the master
for i in 1 2
do
	java -jar ../qwazr/target/qwazr-1.0.0-SNAPSHOT-exec.jar \
		-l 127.0.0.1 -sp 9${i}91 -wp 9${i}90 -d master${i} >logs/log${i}.out 2>&1 &
done

# Then we launch the nodes
for i in 3 4 5 6
do
	java -jar ../qwazr/target/qwazr-1.0.0-SNAPSHOT-exec.jar \
		-l 127.0.0.1 -sp 9${i}91 -wp 9${i}90 -d node${i} >logs/log${i}.out 2>&1 &
done
