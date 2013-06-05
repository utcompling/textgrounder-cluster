# Textgrounder Cluster
===================

Package for clustering textgrounder data and combining the results of running textgrounder's geolocate on the individual clusters.

## Introduction

This project is intended to work with textgrounder and data prepared for textgrounder.  The basic process is to take data set prepared for textgrounder and divide it into a number of smaller, related data sets.  Textgrounder's geolocate is run on each of the data sets.  The results from the individual runs are recombined to produce a final result.  The overall process has yet to be automated so each step must be manually initiated.  This project is designed to run in Hadoop which is very fast at counting and clustering.


## Setup

Clone the repository

> git clone https://github.com/eskiles/textgrounder-cluster.git

Add the base directory to the path.

> export TEXTGROUNDER_CLUSTER=$HOME/utcompling/textgrounder-cluster

Build with sbt

> ./sbt assembly


## TF-IDF

Data prepared for textgrounder uses word counts.  These counts are converted to TF-IDF which is better for clustering.  The TF-IDF process assumes that the first column is the id and the last column is the count data.  This needs to be updated to read the schema file.

Go to the data directory.
> hadoop fs -put cophir-00000-training.data.txt cophir-00000-training.data.txt
> hadoop jar $TEXTGROUNDER_CLUSTER/target/textgrounder-cluster-assembly.jar opennlp.textgroundercluster.tfidf.DocumentFrequencyCount cophir-00000-training.data.txt documentFrequencyCount.txt
> mkdir tfidf
> hadoop fs -getmerge documentFrequencyCount.txt tfidf/documentFrequencyCount.txt
> java -cp $TEXTGROUNDER_CLUSTER/target/textgrounder-cluster-assembly.jar opennlp.textgroundercluster.tfidf.ConvertCountToTFIDF cophir-00000-training.data.txt tfidf/cophir-00000-training.data.tfidf tfidf/documentFrequencyCount.txt
> hadoop fs -put tfidf/cophir-00000-training.data.tfidf cophir-00000-training.data.tfidf

## Clustering

The TF-IDF data set can now be clustered.  Run KMeansClusterText to cluster the data.  The input parameters include the input file to cluster, a centroid file for storing the centroids, the output file for storing the results, and a value for K.

Run KMeansClusterText with 3 clusters on the train data

> mkdir cluster-3
> cd cluster-3
> hadoop jar $TEXTGROUNDER_CLUSTER/target/textgrounder-cluster-assembly.jar opennlp.textgroundercluster.cluster.KMeansClusterText cophir-00000-training.data.tfidf /scratch/01546/eskiles/corpora/cophir/cluster-3/centroid cophir-00000-training.data.cluster-3 3
> hadoop fs -getmerge cophir-00000-training.data.cluster-3 cophir-00000-training.data.cluster-3

separate the original files by id

> cat cophir-00000-training.data.tfidf.cluster-3 | awk '{ if ($1 =="0") print $2 }' > cophir-00000-training.data.class-0.id
> cat cophir-00000-training.data.tfidf.cluster-3 | awk '{ if ($1 =="1") print $2 }' > cophir-00000-training.data.class-1.id
> cat cophir-00000-training.data.tfidf.cluster-3 | awk '{ if ($1 =="2") print $2 }' > cophir-00000-training.data.class-2.id

> java -cp $TEXTGROUNDER_CLUSTER/target/textgrounder-cluster-assembly.jar opennlp.textgroundercluster.cluster.FindById cophir-00000-training.data.txt class-0/cophir-00000-training.data.txt cophir-00000-training.data.class-0.id
> java -cp $TEXTGROUNDER_CLUSTER/target/textgrounder-cluster-assembly.jar opennlp.textgroundercluster.cluster.FindById cophir-00000-training.data.txt class-1/cophir-00000-training.data.txt cophir-00000-training.data.class-1.id
> java -cp $TEXTGROUNDER_CLUSTER/target/textgrounder-cluster-assembly.jar opennlp.textgroundercluster.cluster.FindById cophir-00000-training.data.txt class-2/cophir-00000-training.data.txt cophir-00000-training.data.class-2.id



