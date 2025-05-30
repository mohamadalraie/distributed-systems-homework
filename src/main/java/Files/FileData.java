package Files;

import java.io.Serializable;

public class FileData implements Serializable {
    private String filename;
    private byte[] data;


    public FileData(String filename, byte[] data) {
        this.filename = filename;
        this.data = data;

    }

    // Getters and Setters
    public String getFilename() { return filename; }
    public byte[] getData() { return data; }

}