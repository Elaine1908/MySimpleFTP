package server.core.transmit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FileMeta {

    public enum TYPE {
        FILE,
        DIRECTORY
    }

    public long size;
    public String filename;
    public TYPE type;

    public FileMeta(int size, String filename, TYPE type) {
        this.size = size;
        this.filename = filename;
        this.type = type;
    }
}
