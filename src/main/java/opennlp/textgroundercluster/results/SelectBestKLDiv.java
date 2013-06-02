package opennlp.textgroundercluster.results;

import org.apache.hadoop.fs.Path;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectBestKLDiv {
    public static void main(String[] args) throws Exception {
        final String directory = args[0];
        File dir = new File(directory);
        String outFile = directory + Path.SEPARATOR + "out";
        String keyFile = directory + Path.SEPARATOR + "key";
        String repFile = directory + Path.SEPARATOR + "rep";
        String[] children = dir.list();

        if(children == null) throw new FileNotFoundException("Directory has no content.");

        Map<Integer, BufferedReader> klFileMap = new HashMap<Integer, BufferedReader>();
        Map<Integer, BufferedReader> distanceFileMap = new HashMap<Integer, BufferedReader>();
        Map<Integer, List<Double>> allDistanceMap = new HashMap<Integer, List<Double>>();
        Map<Integer, List<Double>> bestDistanceMap = new HashMap<Integer, List<Double>>();

        for (String child : children) {
            //System.out.println(child);
            if(child.contains("kl-div")) {
                int mid= child.lastIndexOf(".");
                BufferedReader bufferedReader = new BufferedReader(new FileReader(child));
                final int cluster = Integer.parseInt(child.substring(mid + 1, child.length()));
                klFileMap.put(cluster, bufferedReader);
                allDistanceMap.put(cluster, new ArrayList<Double>());
                bestDistanceMap.put(cluster, new ArrayList<Double>());
            }
            if(child.contains("distance")) {
                int mid= child.lastIndexOf(".");
                BufferedReader bufferedReader = new BufferedReader(new FileReader(child));
                distanceFileMap.put(Integer.parseInt(child.substring(mid+1, child.length())), bufferedReader);
            }
        }

        FileWriter outFileWriter = new FileWriter(outFile);
        FileWriter keyFileWriter = new FileWriter(keyFile);
        String line="";
        do {
            Double minKlDiv = Double.MAX_VALUE;
            Integer minKey = null;
            for (Integer key : klFileMap.keySet()){
                line = klFileMap.get(key).readLine();
                if (line == null) continue;
                Double klDiv = Double.parseDouble(line);
                if (klDiv < minKlDiv) {
                    minKlDiv = klDiv;
                    minKey = key;
                }
            }
            for (Integer key : distanceFileMap.keySet()) {
                line = distanceFileMap.get(key).readLine();
                if (line == null) continue;
                Double value = Double.parseDouble(line);
                allDistanceMap.get(key).add(value);
                if(key == minKey) {
                    outFileWriter.write(line + "\n");
                    keyFileWriter.write(key + "\n");
                    //TODO Include error analysis of distance by key
                    bestDistanceMap.get(key).add(value);
                }
            }
        } while(line != null);
        outFileWriter.close();
        keyFileWriter.close();
        for(BufferedReader reader : klFileMap.values()) {
            reader.close();
        }
        for(BufferedReader reader : distanceFileMap.values()) {
            reader.close();
        }

        FileWriter repFileWriter = new FileWriter(repFile);
        for (Integer key : distanceFileMap.keySet()) {
            List<Double> allDistances = allDistanceMap.get(key);
            List<Double> bestDistances = bestDistanceMap.get(key);
            
            
            Double sum = 0.0;
            Double sumSq = 0.0;
            for (Double value : allDistances) {
                sum += value;
                sumSq += value * value;
            }
            Integer numValues = allDistances.size();
            Double mean = sum / numValues;
            Double var = Math.sqrt(sumSq);
            
            String out = "all:\t" + key + "\t" + mean + "\t" + var + "\n";
            repFileWriter.write(out);
            
            sum = 0.0;
            sumSq = 0.0;
            for (Double value : bestDistances) {
                sum += value;
                sumSq += value * value;
            }
            
            numValues = bestDistances.size();
            mean = sum / numValues;
            var = Math.sqrt(sumSq);
            
            out = "best:\t" + key + "\t" + mean + "\t" + var + "\n";
            repFileWriter.write(out);
        }
        
        repFileWriter.close();
    }
}
