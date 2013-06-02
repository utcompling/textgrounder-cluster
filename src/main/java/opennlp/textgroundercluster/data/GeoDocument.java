package opennlp.textgroundercluster.data;

import gnu.trove.iterator.TObjectFloatIterator;
import gnu.trove.map.hash.TObjectFloatHashMap;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;


/**
 * Custom object for Geo-located documents
 */
public class GeoDocument implements Writable {
    private String id = "id";
    private String geo = "geo";
    private Double latitude = 0.0;
    private Double longitude = 0.0;
    private int classNumber;

    public TObjectFloatHashMap<Integer> getWordValueMap() {
        return wordValueMap;
    }

    private TObjectFloatHashMap<Integer> wordValueMap;
    private Double norm;

    public GeoDocument() {
        wordValueMap = new TObjectFloatHashMap<Integer>();
    }

    public GeoDocument(String id, String geo, TObjectFloatHashMap<Integer> wordValueMap) {
        this.id = id;
        this.geo = geo;
        parseGeoCoordinates(geo);
        this.wordValueMap = wordValueMap;
    }
    public GeoDocument(int classNumber, String id, String geo, TObjectFloatHashMap<Integer> wordValueMap) {
        this.classNumber = classNumber;
        this.id = id;
        this.geo = geo;
        parseGeoCoordinates(geo);
        this.wordValueMap = wordValueMap;
    }

    private void parseGeoCoordinates(String geo) {
        //String[] parts = geo.split(",");
        //latitude = Double.parseDouble(parts[0]);
        //longitude = Double.parseDouble(parts[1]);
    }

    public GeoDocument(String line) {
        String[] parts = line.split("\t");
        wordValueMap = new  TObjectFloatHashMap<Integer>();

        if(parts.length == 3) {
            id = parts[0].trim();
            geo = parts[1].trim();
            parseGeoCoordinates(geo);
            String[] wordValues = parts[2].split(" ");
            for (String wordValue : wordValues) {
                String[] pair = wordValue.split(":");
                final float value = Float.parseFloat(pair[1]);
                wordValueMap.put(Integer.parseInt(pair[0]), value);
            }
        } else {
            classNumber = Integer.parseInt(parts[0].trim());
            id = parts[1].trim();
            geo = parts[2].trim();
            parseGeoCoordinates(geo);
            String[] wordValues = parts[3].split(" ");
            for (String wordValue : wordValues) {
                String[] pair = wordValue.split(":");
                final float value = Float.parseFloat(pair[1]);
                wordValueMap.put(Integer.parseInt(pair[0]), value);
            }
        }
        computeNorm();
    }

    public void add(GeoDocument otherDocument) {
        for (Integer word : otherDocument.wordValueMap.keySet()) {
            if(this.wordValueMap.containsKey(word)) {
                this.wordValueMap.put(word, this.wordValueMap.get(word) + otherDocument.wordValueMap.get(word)); 
            } else {
                this.wordValueMap.put(word, otherDocument.wordValueMap.get(word));
            }
        }
    }

    public void set(String id, String geo, TObjectFloatHashMap<Integer> wordValueMap) {
        this.id = id;
        this.geo = geo;
        parseGeoCoordinates(geo);
        this.wordValueMap = wordValueMap;
    }

    public void write(DataOutput dataOutput) throws IOException {
        System.out.println("Writing id: " + id);
        dataOutput.writeInt(classNumber);
        dataOutput.writeUTF(id);
        dataOutput.writeUTF(geo);

        // Then write out each key/value pair.
        for (TObjectFloatIterator<Integer> it = wordValueMap.iterator(); it.hasNext();) {
            it.advance();
            dataOutput.write(it.key());
            dataOutput.writeFloat(it.value());
        }
    }

    public void readFields(DataInput dataInput) throws IOException {
        int numEntries = dataInput.readInt();

        System.out.println("Entries: " + numEntries);
        if (numEntries < 4)
            return;

        classNumber = dataInput.readInt();
        id = dataInput.readUTF();
        geo = dataInput.readUTF();
        parseGeoCoordinates(geo);

        for (int i = 2; i < numEntries; i++) {
            String k = dataInput.readUTF();
            float v = dataInput.readFloat();
            wordValueMap.put(Integer.parseInt(k), v);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        //stringBuilder.append(classNumber);
        //stringBuilder.append("\t");
        stringBuilder.append(id);
        stringBuilder.append("\t");
        stringBuilder.append(geo);
        stringBuilder.append("\t");
        for (Integer key : wordValueMap.keySet()) {
            stringBuilder.append(key);
            stringBuilder.append(":");
            stringBuilder.append(wordValueMap.get(key));
            stringBuilder.append(" ");
        }
        return stringBuilder.toString();
    }

    public int compareTo(Object o) {
        if(!(o instanceof GeoDocument)) {
            return 0;
        }
        GeoDocument other = (GeoDocument) o;
        return this.id.compareTo(other.id);
    }

    public int getClassNumber() {
        return classNumber;
    }

    public void setClassNumber(int classNumber) {
        this.classNumber = classNumber;
    }

    public double getNorm() {
        return norm;
    }
    
    public void computeNorm() {
        Double n = 0.0;
        for (Integer word : this.wordValueMap.keySet()) {
            final float value = this.wordValueMap.get(word);
            n += value*value;
        }
        norm = Math.sqrt(n);
    }

    public String getId() {
        return this.id;
    }
 
    public GeoDocument computeClosest(List<GeoDocument> geoDocuments) {
        //System.out.println("Computing closest: " + this.getId() + " and " + geoDocuments.size() + " others");
        GeoDocument closestDocument = null;
        Double closestDistance = 0.0;
        for(GeoDocument otherDocument : geoDocuments) {
            //System.out.println("Other doc: " + otherDocument.getId());
            double distance = similarity(otherDocument);
            //System.out.println("Distance: " + distance);
            if (distance > closestDistance) {
                closestDistance = distance;
                closestDocument = otherDocument;
            }
        }
        //System.out.println("Closest: " + closestDocument.getId());
        return closestDocument;
    }

    public double similarity(GeoDocument otherDocument) {
        double similarity = 0.0;
        //System.out.println("Computing similarity: " + this.wordValueMap.keySet().size() + " : " + otherDocument.wordValueMap.keySet().size());
        for (Integer word : this.wordValueMap.keySet()) {
            similarity += this.wordValueMap.get(word) * otherDocument.wordValueMap.get(word);
        }
        //System.out.println("Similarity after summing wij*dij: " + similarity);
        //System.out.println("this norm: " + this.getNorm());
        //System.out.println("other norm: " + otherDocument.getNorm());
        similarity = similarity / (this.getNorm() * otherDocument.getNorm());
        //Double distance =  computeDistanceTo(otherDocument);
        
        //System.out.println("Word similarity: " + similarity + " : Distance: " + distance);
        return similarity;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    private double computeDistanceTo(GeoDocument otherDocument) {
        System.out.println("Latitude: " + latitude + ": Longitude: " + longitude + ": Latitude: " + otherDocument.getLatitude() + ": Longitude: " + otherDocument.getLongitude());
        return Math.acos( Math.sin(latitude) * Math.sin(otherDocument.getLatitude())
                        + Math.cos(latitude) * Math.cos(otherDocument.getLatitude()) * Math.cos(otherDocument.getLongitude() - longitude));
    }

    public void apply(String line) {
        String[] parts = line.split("\t");
        wordValueMap = new  TObjectFloatHashMap<Integer>();

        if(parts.length == 3) {
            id = parts[0].trim();
            geo = parts[1].trim();
            parseGeoCoordinates(geo);
            String[] wordValues = parts[2].split(" ");
            for (String wordValue : wordValues) {
                String[] pair = wordValue.split(":");
                final float value = Float.parseFloat(pair[1]);
                wordValueMap.put(Integer.parseInt(pair[0]), value);
            }
        } else {
            classNumber = Integer.parseInt(parts[0].trim());
            id = parts[1].trim();
            geo = parts[2].trim();
            parseGeoCoordinates(geo);
            String[] wordValues = parts[3].split(" ");
            for (String wordValue : wordValues) {
                String[] pair = wordValue.split(":");
                final float value = Float.parseFloat(pair[1]);
                wordValueMap.put(Integer.parseInt(pair[0]), value);
            }
        }
        computeNorm();
    }
}
