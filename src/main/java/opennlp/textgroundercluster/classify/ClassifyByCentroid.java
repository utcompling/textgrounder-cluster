package opennlp.textgroundercluster.classify;

import opennlp.textgroundercluster.data.GeoDocument;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ClassifyByCentroid {
    private static String CENTROID_FILE = "CENTROID FILE";

    public static class ClassifyMapper extends Mapper<Object, Text, IntWritable, Text> {

        private Text geoDoc = new Text();
        private List<IntWritable> classes = new ArrayList<IntWritable>();
        private List<GeoDocument> centroids = new ArrayList<GeoDocument>();

        @Override
        protected void setup(org.apache.hadoop.mapreduce.Mapper<Object,Text,IntWritable,Text>.Context context) throws java.io.IOException, java.lang.InterruptedException {
            String centroidInFile = context.getConfiguration().get(CENTROID_FILE);
            System.out.println("Reading centroids from file: " + centroidInFile);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(centroidInFile));
            String line = null;
            centroids.clear();
            int classNumber = 0;
            while ((line = bufferedReader.readLine()) != null) {
                centroids.add(new GeoDocument(line));
                classes.add(new IntWritable(classNumber));
                classNumber++;
            }
            //System.out.println("Found " + centroids.size() + " centroids.");
            bufferedReader.close();
        }

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            GeoDocument geoDocument = new GeoDocument(line);
            //System.out.println("Read document for user: " + geoDocument.getId());
            GeoDocument closest = geoDocument.computeClosest(centroids);
            if(closest.getClassNumber() != geoDocument.getClassNumber()) {
                System.out.println("Updating class assignment from " + geoDocument.getClassNumber() +
                        " to " + closest.getClassNumber());
                geoDocument.setClassNumber(closest.getClassNumber());
            }

            geoDoc.set(geoDocument.toString());
            context.write(classes.get(geoDocument.getClassNumber()), geoDoc);
        }
    }

    public static class ClassifyReducer extends Reducer<IntWritable, Text, IntWritable, Text> {
        private Text geoDoc = new Text();

        public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            Text outValue = new Text();
            System.out.println("KmeansCluster.ClassifyReducer.reduce");
            final int classNumber = key.get();
            System.out.println("Classifying on key: " + classNumber);

            for (Text value : values) {
                GeoDocument geoDocument = new GeoDocument(value.toString());
                geoDoc.set(geoDocument.toString());
                context.write(key, geoDoc);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String inputFile = args[0];
        String outputFile = args[1];
        String centroidFile = args[2];
        System.out.println("InputFile: " + inputFile);
        System.out.println("OutputFile: " + outputFile);
        System.out.println("CentroidFile: " + centroidFile);

        Configuration conf = new Configuration();
        conf.set(CENTROID_FILE, centroidFile);

        Job job = new Job(conf, "K-Means Cluster");
        job.setJarByClass(ClassifyByCentroid.class);
        job.setMapperClass(ClassifyMapper.class);
        job.setReducerClass(ClassifyReducer.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(inputFile));
        FileOutputFormat.setOutputPath(job, new Path(outputFile));
        boolean result =  job.waitForCompletion(true);
    }
}
