package hu.mrolcsi.android.filebrowser.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Matusinka Roland
 * Date: 2015.08.07.
 * Time: 12:43
 */

public class Cache {
    private static Cache ourInstance = new Cache();

    Map<String, Long> sizeCache = new ConcurrentHashMap<>();

    private Cache() {
    }

    public static Cache getInstance() {
        return ourInstance;
    }
}
