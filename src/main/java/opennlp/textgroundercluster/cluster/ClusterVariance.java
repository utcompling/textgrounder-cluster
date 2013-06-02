package opennlp.textgroundercluster.cluster;

import opennlp.textgroundercluster.data.GeoDocument;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
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

public class ClusterVariance {
    private static String CENTROID_FILE = "CENTROID FILE";

    public static class ClusterVarianceMapper extends Mapper<Object, Text, IntWritable, DoubleWritable> {

        private IntWritable outKey = new IntWritable();
        private DoubleWritable outValue = new DoubleWritable();

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            GeoDocument geoDocument = new GeoDocument(line);
            for (Integer word : geoDocument.getWordValueMap().keySet()) {
                outKey.set(word);
                outValue.set(geoDocument.getWordValueMap().get(word));
                context.write(outKey, outValue);
            }
        }
    }

    public static class ClusterVarianceReducer extends Reducer<IntWritable, DoubleWritable, DoubleWritable, IntWritable>  {
        private GeoDocument centroid;
        private DoubleWritable outKey = new DoubleWritable();

        @Override
        protected void setup(org.apache.hadoop.mapreduce.Reducer<IntWritable, DoubleWritable, DoubleWritable, IntWritable>.Context context) throws java.io.IOException, java.lang.InterruptedException {
            String centroidInFile = context.getConfiguration().get(CENTROID_FILE);
            System.out.println("Reading centroids from file: " + centroidInFile);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(centroidInFile));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                centroid = new GeoDocument(line);
            }
            //System.out.println("Found " + centroids.size() + " centroids.");
            bufferedReader.close();
        }
        
        public void reduce(IntWritable key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
            String word = key.toString();
            Double variance = 0.0;
            Double n = 0.0;
            for (DoubleWritable tfidf : values) {
                final float value = centroid.getWordValueMap().get(word);
                final double error = tfidf.get();
                variance += (value - error) * (value - error);
                n++;
            }
            variance = variance/n;
            outKey.set(variance);
            context.write(outKey, key);
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

        Job job = new Job(conf, "Variance");
        job.setJarByClass(KMeansClusterText.class);
        job.setMapperClass(ClusterVarianceMapper.class);
        job.setReducerClass(ClusterVarianceReducer.class);
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(DoubleWritable.class);
        job.setOutputKeyClass(DoubleWritable.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(inputFile));
        FileOutputFormat.setOutputPath(job, new Path(outputFile));
        boolean result =  job.waitForCompletion(true);
    }
}
