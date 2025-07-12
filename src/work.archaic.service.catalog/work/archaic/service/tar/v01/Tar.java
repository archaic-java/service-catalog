package work.archaic.service.tar.v01;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface Tar {

    void createTar(File baseDir, List<File> files, OutputStream out) throws TarWriteException;

    void createTar(File file, OutputStream out) throws TarWriteException;

    void extractTar(InputStream in, File outputDir) throws TarReadException;

    List<String> listEntries(InputStream in) throws TarReadException;
}

