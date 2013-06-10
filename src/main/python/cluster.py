#!/usr/bin/env python

#####
##### cluster.py
#####

# Runs the textgrounder-cluster modules to cluster training data

import sys, os
from optparse import OptionParser
from subprocess import call

# Global definitions for the jar and classes to run
# Jar file
tgJar = "$TEXTGROUNDER_CLUSTER/target/textgrounder-cluster-assembly.jar"
# Class Files
DocumentFrequencyCount = "opennlp.textgroundercluster.tfidf.DocumentFrequencyCount"
ConvertCountToTFIDF = "opennlp.textgroundercluster.tfidf.ConvertCountToTFIDF"
KMeansClusterText = "opennlp.textgroundercluster.cluster.KMeansClusterText"
FindById = "opennlp.textgroundercluster.cluster.FindById"

#Base Dir
inputDir = ""
schemaPath = ""
trainingDataFile = ""
trainingDataPath = ""
filesToLink = []


# TFIDF Files
tfidfDir = "" 
DocumentFrequencyCountFile = "DocumentFrequencyCount.txt"
DocumentFrequencyCountFilePath = ""  # full path to the DF Count file
tfidfDataFile = ""
tfidfDataPath = ""


# Cluster Files
clusterDir = ""
clusterFile = ""
clusterPath = ""
clusterSubdirs = []
centroidFile = "centroid"
centroidPath = ""
idFile = "idFile"
idPath = ""



def runCommand(command):
	print command
	return_code = call(command, shell=True)
	if (return_code) != 0:
		if (return_code) < 0:
			print ("Killed by signal " + str(return_code))
			print ("On Command: " + command)
			sys.exit()
		else:
			print ("Command failed with return code " + str(return_code))
			print ("On Command: " + command)
			sys.exit()

def doWordCount(tfidfDir, tfidfDataPath, trainingDataPath, trainingDataFile, DocumentFrequencyCountFile, DocumentFrequencyCountFilePath):
	print("### WORD COUNT ###")
	if not os.path.exists(tfidfDir):
		os.makedirs(tfidfDir)

	command = "hadoop fs -put " +  trainingDataPath + " " +  trainingDataFile
	runCommand(command)

	command = "hadoop jar " + tgJar + " " + DocumentFrequencyCount + " " + trainingDataFile + " " +  DocumentFrequencyCountFile
	runCommand(command)

	command = "hadoop fs -getmerge " +  DocumentFrequencyCountFile + " " +  DocumentFrequencyCountFilePath
	runCommand(command)

def doTfidfConversion(trainingDataPath, tfidfDataPath, DocumentFrequencyCountFilePath):
	print("### TFIDF CONVERSION ###")
	command = "java -cp " + tgJar  + " " +  ConvertCountToTFIDF + " " + trainingDataPath + " " + tfidfDataPath + " " + DocumentFrequencyCountFilePath
	runCommand(command)

def doCluster(inputDir, clusterDir, tfidfDataPath, tfidfDataFile, trainingDataFile, trainingDataPath, k):
	print("### CLUSTER ###")
	if (os.path.exists(clusterDir)):
		print ("ERROR:  A cluster for this data and value of k already exists.")
		exit()
	else:
		os.makedirs(clusterDir)

	centroidPath = os.path.join(clusterDir, centroidFile)
	clusterFile = trainingDataFile + ".cluster-" + str(k)
	clusterPath = os.path.join(clusterDir, clusterFile)
	idPath = os.path.join(clusterDir, idFile)
	print("Centroid path: " + centroidPath)
	print("Cluster file: " + clusterFile)
	print("ID Path: " + idPath)
	
	for i in range(k):
		clusterSubdirs.insert(i, os.path.join(clusterDir, "cluster-" + str(i)))
		print("Cluster subdir: " + clusterSubdirs[i])
		os.makedirs(clusterSubdirs[i])

	command = "hadoop fs -put " + tfidfDataPath + " " + tfidfDataFile
	runCommand(command)

	command = "hadoop jar " + tgJar + " " + KMeansClusterText + " " + tfidfDataFile + " " + centroidPath + " " + clusterFile + " " + str(k)
	runCommand(command)

	command = "hadoop fs -getmerge " + clusterFile + " " + clusterPath
	runCommand(command)

	for i in range(k):
		command = "cat " + clusterPath + " | awk '{ if ($1 ==\"" + str(i) + "\") print $2 }' > " + idPath
		runCommand(command)
		command = "java -cp " + tgJar + " " + FindById + " " +  trainingDataPath + " " + os.path.join(clusterSubdirs[i], trainingDataFile) + " " + idPath
		runCommand(command)
		for fileToLink in filesToLink:
			command = "ln -s " + os.path.join(inputDir, fileToLink) + " " + os.path.join(clusterSubdirs[i], fileToLink)
			runCommand(command)

def main():
	parser = OptionParser()
	parser.add_option("-i", "--input-dir", dest="input_dir", metavar="DIR", help="Path to the training data.")
	parser.add_option("-k", "--clusters", dest="k", type="int", help="Number of clusters.")
	(opts, args) = parser.parse_args()
	inputDir = opts.input_dir
	k = opts.k

	if (not os.path.exists(inputDir)):
		exit("Could not find path to training data.")
	if (k < 2):
		exit("k must be greater than 1.")

	clusterDir = os.path.join(inputDir, "cluster-" + str(k))
	tfidfDir = os.path.join(inputDir, "tfidf")
	DocumentFrequencyCountFilePath = os.path.join(tfidfDir, DocumentFrequencyCountFile)

	trainingDataFile = ""
	for file in os.listdir(inputDir):
		if os.path.isfile(os.path.join(inputDir,file)):
			print ("Found file: " + file)
			if "training" in file:
				if "schema" in file:
					schemaPath = os.path.join(inputDir, file)
					filesToLink.append(file)
				else:
					trainingDataFile = file
			else:
				filesToLink.append(file)
		else:
			print("Ignoring directory: " + file)

	if trainingDataFile == "":
		print("ERROR:  Could not find training data.")
		exit()

	trainingDataPath = os.path.join(inputDir, trainingDataFile)
	tfidfDataFile = trainingDataFile + ".tfidf"
	tfidfDataPath = os.path.join(tfidfDir, tfidfDataFile)

	print ("Input dir is: " + inputDir)
	print ("Schema file is: " + schemaPath)
	print ("Training data file is: " + trainingDataPath)
	print ("Tfidf dir is: " + tfidfDir)
	print ("Tfidf data file is: " + tfidfDataPath)
	print ("Document freq file is: " + DocumentFrequencyCountFilePath)
	print ("Cluster dir is: " + clusterDir)

	if os.path.exists(DocumentFrequencyCountFilePath):
		print ("Skipping document frequency counting")
	else:
		doWordCount(tfidfDir, tfidfDataPath, trainingDataPath, trainingDataFile, DocumentFrequencyCountFile, DocumentFrequencyCountFilePath)

	if os.path.exists(tfidfDataPath):
		print ("Skipping tfidf conversion")
	else:
		doTfidfConversion(trainingDataPath, tfidfDataPath, DocumentFrequencyCountFilePath)

	doCluster(inputDir, clusterDir, tfidfDataPath, tfidfDataFile, trainingDataFile, trainingDataPath, k)



if  __name__ =='__main__':
    main()


