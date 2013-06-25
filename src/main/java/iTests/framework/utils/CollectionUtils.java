package iTests.framework.utils;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: guym
 * Date: 6/25/13
 * Time: 2:41 PM
 */
public class CollectionUtils extends org.apache.commons.collections.CollectionUtils {

    // exists in apache collection utils 3.2. We use previous version.
    static public boolean isEmpty( Collection collection ){
        return size( collection ) == 0;
    }
}
