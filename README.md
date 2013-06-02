# Textgrounder Cluster
===================

Package for clustering textgrounder data and combining the results of running textgrounder's geolocate on the individual clusters.

## Introduction

This project is intended to work with textgrounder and data prepared for textgrounder.  The basic process is to take data set prepared for textgrounder and divide it into a number of smaller, related data sets.  Textgrounder's geolocate is run on each of the data sets.  The results from the individual runs are recombined to produce a final result.  The overall process has yet to be automated so each step must be manually initiated.  This project is designed to run in Hadoop which is very fast at counting and clustering.

## TF-IDF

Data prepared for textgrounder uses word counts.  These counts are converted to TF-IDF which is better for clustering.  
.. detail the commands for converting counts to TF-IDF

## Clustering

The TF-IDF data set can now be clustered.  Run KMeansClusterText to cluster the data.  The input parameters include the input file to cluster, a centroid file for storing the centroids, the output file for storing the results, and a value for K.

Run KMeansClusterText with 3 clusters on the train data

  hadoop jar textgrounderCluster.jar KMeansClusterText train centroid train-cluster 3

The output directory now has several files in it

  ls
  > centroid.0
  > centroid.1
  > centroid.2
  > train
  > train-cluster.0
  > train-cluster.1
  > train-cluster.2

## Classify
