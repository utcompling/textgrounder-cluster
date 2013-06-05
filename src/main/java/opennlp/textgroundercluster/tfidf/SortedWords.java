package fogbow.eskiles;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: eskiles
 * Date: 2/12/12
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class SortedWords {

    public static class WordCountMapper extends Mapper<Object, Text, Text, DoubleWritable> {
        DoubleWritable outValue = new DoubleWritable();
        Text outKey = new Text();

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] items = line.split("\t");
            String[] wordValuePairs = items[2].split(" ");
            for (String wordValuePair : wordValuePairs) {
                String wordValue[] = wordValuePair.split(":");
                String word = wordValue[0];
                Double val = Double.parseDouble(wordValue[1]);
                outKey.set(word);
                outValue.set(val);
                context.write(outKey, outValue);
            }
        }
    }

    public static class WordCountReducer extends Reducer<Text, DoubleWritable, DoubleWritable, Text> {
        private DoubleWritable outKey = new DoubleWritable();

        public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
            Double result = 0.0;
            for (DoubleWritable value : values) {
                result += value.get();
            }
            outKey.set(result);
            context.write(outKey, key);
        }
    }

    public static class WordSortMapper extends Mapper<Object, Text, DoubleWritable, Text> {
        DoubleWritable outKey = new DoubleWritable();
        Text outValue = new Text();

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] items = line.split("\t");
            if(items.length != 2) return;
            outKey.set(Double.parseDouble(items[0]));
            outValue.set(items[1]);
            context.write(outKey, outValue);
        }
    }

    public static class WordSortReducer extends Reducer<DoubleWritable, Text, Text, DoubleWritable> {
        private Text outKey = new Text();

        public void reduce(DoubleWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text value : values) {
                context.write(value, key);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        //TODO read in the centroid file and sort on lowest variance
        String inputFile = args[0];
        String outputFile = args[1];
        String tempFile = inputFile + ".temp";
        System.out.println("InputFile: " + inputFile);
        System.out.println("OutputFile: " + outputFile);

        Configuration conf = new Configuration();

        Job job = new Job(conf, "Word Count");
        job.setJarByClass(KMeansClusterText.class);
        job.setMapperClass(WordCountMapper.class);
        job.setReducerClass(WordCountReducer.class);
        job.setOutputKeyClass(DoubleWritable.class);
        job.setOutputValueClass(Text.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(DoubleWritable.class);
        FileInputFormat.addInputPath(job, new Path(inputFile));
        FileOutputFormat.setOutputPath(job, new Path(tempFile));
        boolean result =  job.waitForCompletion(true);
        
        Job job2 = new Job(conf, "Sorted Word Count");
        job2.setJarByClass(KMeansClusterText.class);
        job2.setMapperClass(WordSortMapper.class);
        job2.setReducerClass(WordSortReducer.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(DoubleWritable.class);
        job2.setMapOutputKeyClass(DoubleWritable.class);
        job2.setMapOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job2, new Path(tempFile));
        FileOutputFormat.setOutputPath(job2, new Path(outputFile));
        result =  job2.waitForCompletion(true);
    }
}
