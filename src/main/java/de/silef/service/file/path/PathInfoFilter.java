package de.silef.service.file.path;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by sebastian on 24.09.16.
 */
public interface PathInfoFilter {

    boolean isValidPathInfo(PathInfo pathInfo);
}
