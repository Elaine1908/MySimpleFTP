package server.core.transmit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PartMeta {

    public enum TYPE {
        FILE,
        DIRECTORY
    }

    public long size;
    public String filename;
    public TYPE type;
    public int partID;

    public PartMeta(long size, String filename, TYPE type, int partID) {
        this.size = size;
        this.filename = filename;
        this.type = type;
        this.partID = partID;
    }
}
