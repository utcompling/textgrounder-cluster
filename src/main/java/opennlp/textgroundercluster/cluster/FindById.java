package opennlp.textgroundercluster.cluster;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: eskiles
 * Date: 2/20/12
 * Time: 8:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class FindById {

    public static void main(String[] args) throws Exception {
        //Configuration conf = new Configuration();
        final String inFile = args[0];
        final String outFile = args[1];
        final String idFile = args[2];

        Set<String> idSet = new HashSet<String>();
        readSourceIds(idFile, idSet);
        System.out.println("Read source file with size: " + idSet.size());
        extractIdsFromFile(inFile, outFile, idSet);


    }

    private static void readSourceIds(String sourceFile, Set<String> idSet) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(sourceFile));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                idSet.add(line.trim());
            }
            bufferedReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void extractIdsFromFile(String inFile, String outFile, Set<String> idSet) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(inFile));
            FileWriter fileWriter = new FileWriter(outFile);

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                String id = line.split("\t")[0];
                if(idSet.contains(id))
                fileWriter.write(line + "\n");
            }
            bufferedReader.close();
            fileWriter.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }


}
