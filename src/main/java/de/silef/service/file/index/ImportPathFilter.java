package de.silef.service.file.index;

import java.nio.file.Path;

/**
 * Created by sebastian on 24.09.16.
 */
public interface ImportPathFilter {

    boolean importPath(Path path);
}
