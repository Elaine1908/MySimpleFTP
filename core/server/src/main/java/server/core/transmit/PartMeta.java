package server.core.transmit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PartMeta {

    public enum Compressed {
        COMPRESSED,
        NOT_COMPRESSED
    }

    public long size;
    public String filename;
    public Compressed compressed;
    public int partID;

    public PartMeta(long size, String filename, Compressed compressed, int partID) {
        this.size = size;
        this.filename = filename;
        this.compressed = compressed;
        this.partID = partID;
    }
}
