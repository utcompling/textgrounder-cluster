package opennlp.textgroundercluster.tfidf;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class DocumentFrequencyCount {
    
    public static class WordCountTokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {
        // Example of input line
        // USER_79321756	47.528139,-122.197916	new:2 boys:1 jk:2 crazy:1 rollin:1 peppers:1 salt:1

        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();
        private Set<String> wordsInDocument;

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

            String line = value.toString();
            wordsInDocument = new HashSet<String>();
            //System.out.print("Line: " + line);
            String[] lineTokens = line.split("\t");
            String[] wordTokens = lineTokens[lineTokens.length -1].split(" ");
            for (String wordCount : wordTokens) {
                final String[] wordCountSplit = wordCount.split(":");
                wordsInDocument.add(wordCountSplit[0]);
            }

            for (String wordType : wordsInDocument) {
                word.set(wordType);
                context.write(word, one);
            }
        }
    }


    public static class IntSumReducer extends Reducer<Text,IntWritable,Text,IntWritable> {

        private IntWritable result = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {

            int sum = 0;
            for (IntWritable val : values)
                sum += val.get();

            result.set(sum);
            context.write(key, result);
        }
    }
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = new Job(conf, "Document Frequency Count");
        job.setJarByClass(DocumentFrequencyCount.class);
        job.setMapperClass(WordCountTokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
    
}
