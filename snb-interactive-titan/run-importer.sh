#!/bin/bash

HOSTNAME="127.0.0.1"
GRAPH=validation
# INPUT="/u4/apacaci/Projects/jimmy/ldbc_snb_datagen/datasets/sf1_noupdate/social_network"
INPUT="/u4/apacaci/Projects/jimmy/ldbc_snb_interactive_validation/sparksee/validation_set"
REPORT_PERIOD="10"
BATCH_SIZE="100"
BACKEND=cassandra

mvn exec:java -Dexec.mainClass="net.ellitron.ldbcsnbimpls.interactive.titan.TitanGraphLoader" -Dexec.cleanupDaemonThreads=false -Dexec.args="-locator $HOSTNAME -backend $BACKEND -graphName $GRAPH -input $INPUT -batchSize $BATCH_SIZE -progReportPeriod $REPORT_PERIOD"
