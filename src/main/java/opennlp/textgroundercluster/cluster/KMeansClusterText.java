package opennlp.textgroundercluster.cluster;

import gnu.trove.map.hash.TObjectFloatHashMap;
import opennlp.textgroundercluster.data.GeoDocument;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;


/**
 * Runs k-means clustering
 */
public class KMeansClusterText {
    private static final Logger sLogger = Logger.getLogger("KMeansClusterText.class");
    private static final String CENTROID_PATH = "CentroidPath";

    public static class KMeansMapperInit extends Mapper<Object, Text, IntWritable, Text> {

        private Text geoDoc = new Text();
        private IntWritable classNumber = new IntWritable(0);
        private GeoDocument geoDocument;
        private Random random = new Random(System.currentTimeMillis());

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            // Example of input line
            // USER_79321756	47.528139,-122.197916	new:2 boys:1 jk:2 crazy:1 rollin:1 peppers:1 salt:1
            int k = Integer.parseInt(context.getConfiguration().get("K"));
            try {
                geoDocument = new GeoDocument(value.toString());
                geoDoc.set(geoDocument.toString());
                int docCount = (int)context.getCounter(KMEANS_COUNTER.DOCUMENT_COUNT).getValue();
                //classNumber.set(docCount%k);
                classNumber.set(random.nextInt(k));
                context.write(classNumber, geoDoc);
                context.getCounter(KMEANS_COUNTER.DOCUMENT_COUNT).increment(1);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new InterruptedException("AIB:\n" + value.toString());
            }

            //sLogger.info("DocumentCount: " + context.getCounter(KMEANS_COUNTER.DOCUMENT_COUNT).getValue());
        }
    }

    public static class KMeansMapper extends Mapper<Object, Text, IntWritable, Text> {

        private Text geoDoc = new Text();
        private List<IntWritable> classes = new ArrayList<IntWritable>();
        private List<GeoDocument> centroids = new ArrayList<GeoDocument>();

        @Override
        protected void setup(org.apache.hadoop.mapreduce.Mapper<Object,Text,IntWritable,Text>.Context context) throws java.io.IOException, java.lang.InterruptedException {
            String centroidFile = context.getConfiguration().get(CENTROID_PATH);
            centroids.clear();
            int k = Integer.parseInt(context.getConfiguration().get("K"));
            for (int i = 0; i < k; i++) {
                String path = centroidFile + "." + i;
                sLogger.info("Reading centroids from file: " + path);
                BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
                String line = null;
                line = bufferedReader.readLine(); {
                    if(line == null) throw new FileNotFoundException(path);
                    centroids.add(new GeoDocument(line));
                    classes.add(new IntWritable(i));
                }
                bufferedReader.close();
            }
        }

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            int k = Integer.parseInt(context.getConfiguration().get("K"));

            String line = value.toString();
            GeoDocument geoDocument = new GeoDocument(line);
            GeoDocument closest = geoDocument.computeClosest(centroids);
            if(closest != null && closest.getClassNumber() != geoDocument.getClassNumber()) {
                geoDocument.setClassNumber(closest.getClassNumber());
                context.getCounter(KMEANS_COUNTER.DELTA_COUNT).increment(1);
            }

            geoDoc.set(geoDocument.toString());
            context.write(classes.get(geoDocument.getClassNumber()), geoDoc);
        }
    }


    public static class KMeansReducerInit extends Reducer<IntWritable, Text, IntWritable, Text> {
        private Text geoDoc = new Text();

        public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            sLogger.info("KmeansCluster.KMeansReducerInit.reduce");
            for (Text value : values) {
                GeoDocument geoDocument = new GeoDocument(value.toString());
                geoDocument.setClassNumber(key.get());
                geoDoc.set(geoDocument.toString());
                context.write(key, geoDoc);
            }
        }
    }

    public static class KMeansClusterReducer extends Reducer<IntWritable, Text, IntWritable, Text> {
        private Text geoDoc = new Text();

        public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            sLogger.info("KmeansCluster.KMeansClusterReducer.reduce");
            final int classNumber = key.get();
            sLogger.info("Reducing on key: " + classNumber);

            TObjectFloatHashMap<Integer> wordValueMap = new TObjectFloatHashMap<Integer>();
            GeoDocument geoDocument = new GeoDocument();
            int numberOfDocs = 0;
            for (Text value : values) {
                geoDocument.apply(value.toString());
                geoDocument.setClassNumber(classNumber);
                geoDoc.set(geoDocument.toString());
                context.write(key, geoDoc);
                
                for (Integer word : geoDocument.getWordValueMap().keySet()) {
                    if(wordValueMap.containsKey(word)) {
                        wordValueMap.put(word, wordValueMap.get(word) + geoDocument.getWordValueMap().get(word));
                    } else {
                        wordValueMap.put(word, geoDocument.getWordValueMap().get(word));
                    }
                }
                numberOfDocs ++;
            }
            
            for (Integer word : wordValueMap.keySet()) {
                Float total = wordValueMap.get(word)/numberOfDocs;
                //if (total < .001) continue;
                wordValueMap.put(word, wordValueMap.get(word)/numberOfDocs);
            }

            GeoDocument centroid = new GeoDocument(classNumber, "id", "geo", wordValueMap);
            centroid.computeNorm();
            String centroidPath = context.getConfiguration().get(CENTROID_PATH) + "." + key.toString();
            sLogger.info("Writing centroid:" + classNumber + " to file " + centroidPath);
            FileWriter fileWriter = new FileWriter(centroidPath, false);
            fileWriter.write(centroid.getClassNumber() + "\t" +  centroid.toString() + "\r\n");
            fileWriter.close();
        }
    }

    public static class KMeansOutputReducer extends Reducer<IntWritable, Text, IntWritable, Text> {
        private Text geoDoc = new Text();

        public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            sLogger.info("KmeansCluster.KMeansOutputReducer.reduce");
            final int classNumber = key.get();
            sLogger.info("Reducing on key: " + classNumber);

            for (Text value : values) {
                GeoDocument geoDocument = new GeoDocument(value.toString());
                geoDoc.set(geoDocument.toString());
                context.write(key, geoDoc);
            }
        }
    }


    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String inputFile = args[0];
        String centroidFile = args[1];
        String outputFile = args[2];
        int K = Integer.parseInt(args[3]);
        Integer iteration = 0;
        sLogger.info("InputFile: " + inputFile);
        sLogger.info("OutputFile: " + outputFile);
        sLogger.info("K: " + K);
        conf.setInt("K", K);
        conf.set(CENTROID_PATH, centroidFile);

        Job job = new Job(conf, "K-Means Cluster");
        job.setJarByClass(KMeansClusterText.class);
        job.setMapperClass(KMeansMapperInit.class);
        //job.setMapperClass(KMeansMapper.class);
        //job.setReducerClass(KMeansReducerInit.class);
        job.setReducerClass(KMeansClusterReducer.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(Text.class);
        job.setNumReduceTasks(8);
        FileInputFormat.addInputPath(job, new Path(inputFile));
        FileOutputFormat.setOutputPath(job, new Path(inputFile + "." + iteration));
        boolean result =  job.waitForCompletion(true);
        Counters counters = job.getCounters();
        final long documentCount = counters.findCounter(KMEANS_COUNTER.DOCUMENT_COUNT).getValue();
        System.out.println("Processed " + documentCount + " documents.");
        
        long deltaCount;
        do {
            Job clusterJob = new Job(conf, "K-Means Cluster");
            clusterJob.setJarByClass(KMeansClusterText.class);
            clusterJob.setMapperClass(KMeansMapper.class);
            clusterJob.setReducerClass(KMeansClusterReducer.class);
            clusterJob.setOutputKeyClass(IntWritable.class);
            clusterJob.setOutputValueClass(Text.class);
            clusterJob.setNumReduceTasks(8);
            FileInputFormat.addInputPath(clusterJob, new Path(inputFile + "." + iteration));
            FileOutputFormat.setOutputPath(clusterJob, new Path(inputFile + "." + (iteration + 1)));
            boolean clusterResult =  clusterJob.waitForCompletion(true);
            counters = clusterJob.getCounters();
            deltaCount = counters.findCounter(KMEANS_COUNTER.DELTA_COUNT).getValue();
            sLogger.info("Finished iteration: " + iteration);
            sLogger.info("Document Count: " + documentCount);
            sLogger.info("DELTA in clusters: " + deltaCount);
            iteration++;
        } while(deltaCount >  documentCount/100);// || iteration < K+1);

        Job job2 = new Job(conf, "K-Means Cluster");
        job2.setJarByClass(KMeansClusterText.class);
        job2.setMapperClass(KMeansMapper.class);
        job2.setReducerClass(KMeansOutputReducer.class);
        job2.setOutputKeyClass(IntWritable.class);
        job2.setOutputValueClass(Text.class);
        job2.setNumReduceTasks(K);
        //job2.setMapOutputValueClass(GeoDocument.class);
        //job2.setInputFormatClass(SequenceFileInputFormat.class);
        FileInputFormat.addInputPath(job2, new Path(inputFile + "." + iteration));
        FileOutputFormat.setOutputPath(job2, new Path(outputFile));
        result =  job2.waitForCompletion(true);

    }

    public static enum KMEANS_COUNTER {
        DOCUMENT_COUNT,
        DELTA_COUNT
    };
}
