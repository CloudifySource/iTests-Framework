package iTests.framework.testng.report;

import iTests.framework.utils.LogUtils;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;

/**
 * User: nirb
 * Date: 7/24/13
 */
public class CustomFileListener implements FileListener {

    private boolean isProcessUp = true;

    @Override
    public void fileCreated(FileChangeEvent event) throws Exception {
        LogUtils.log("file created");
        isProcessUp = true;
    }

    @Override
    public void fileDeleted(FileChangeEvent event) throws Exception {
        LogUtils.log("file deleted");
    }

    @Override
    public void fileChanged(FileChangeEvent event) throws Exception {
        LogUtils.log("file changed");
        isProcessUp = true;
    }

    public boolean isProcessUp() {
        return isProcessUp;
    }

    public void setProcessUp(boolean processUp) {
        isProcessUp = processUp;
    }
}
