# Textgrounder Cluster
===================

Package for clustering textgrounder data and combining the results of running textgrounder's geolocate on the individual clusters.

## Introduction

This project is intended to work with textgrounder and data prepared for textgrounder [https://github.com/utcompling/textgrounder].  The basic process is to take data set prepared for textgrounder and divide it into a number of smaller, related data sets.  Textgrounder's geolocate is run on each of the data sets.  The results from the individual runs are recombined to produce a final result.  This project is designed to run in Hadoop which is very fast at counting and clustering.


## Setup

Clone the repository

        git clone https://github.com/utcompling/textgrounder-cluster.git

Set the TEXTGROUNDER_CLUSTER variable.

        export TEXTGROUNDER_CLUSTER=$HOME/utcompling/textgrounder-cluster

Build with sbt

        ./sbt assembly

## Clustering the data

The cluster.py script will cluster the training data in the specified directory into a number of clusters.  The training data needs to have the word training in the file name.

        src/main/python/cluster.py [TARGET_DIRECTORY] [NUMBER_OF_CLUSTERS]


The training schema file is not currently used.  The first column is assumed to be the id and the last column is assumed to be the counts.  Need to update the code to use the schema.

The process creates a tfidf directory with the Document Frequency Count file and the training data converted to tfidf.  If these files already exist, these parts of the process will be skipped.  This is done because they are independent of the value of k.  This allows the user to try multiple values of k without re-computing the tfidf scores.

The process also creates a cluster-k directory.  If one already exists for the value of k specified, then the process will halt and not overwrite the directory.  Within this directory is a subdirectory for each of the clusters (0 to k-1).  Each subdirectory contains one cluster of training data and symbolic links to the other data files in the original directory.  Textgrounder can now be run on each of the subdirectories.

