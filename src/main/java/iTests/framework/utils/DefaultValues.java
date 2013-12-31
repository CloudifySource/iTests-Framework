package iTests.framework.utils;

import iTests.framework.tools.SGTestHelper;
import org.apache.commons.lang.SystemUtils;

import java.util.MissingResourceException;

/**
 * User: eliranm
 * Date: 12/25/13
 * Time: 5:35 PM
 */
public abstract class DefaultValues {

    public static enum Arch {
        _32("32"),
        _64("64");

        private String val;

        private Arch(String val) {
            this.val = val;
        }

        public static Arch get(String archStr) {
            for (Arch arch : Arch.values()) {
                if (archStr.contains(arch.val)) {
                    return arch;
                }
            }
            return null;
        }
    }


    private static DefaultValues instance = null;

    public static DefaultValues get() {
        if (instance == null) {
            switch (Arch.get(SystemUtils.OS_ARCH)) {
                case _32:
                    if (SystemUtils.IS_OS_WINDOWS) {
                        instance = new Windows32();
                    } else if (SystemUtils.IS_OS_LINUX) {
                        instance = new Linux32();
                    } else if (SystemUtils.IS_OS_MAC) {
                        instance = new Mac32();
                    }
                    break;
                case _64:
                    if (SystemUtils.IS_OS_WINDOWS) {
                        instance = new Windows64();
                    } else if (SystemUtils.IS_OS_LINUX) {
                        instance = new Linux64();
                    } else if (SystemUtils.IS_OS_MAC) {
                        instance = new Mac64();
                    }
                    break;
                default:
                    if (SystemUtils.IS_OS_WINDOWS) {
                        instance = new Windows32();
                    } else if (SystemUtils.IS_OS_LINUX) {
                        instance = new Linux32();
                    } else if (SystemUtils.IS_OS_MAC) {
                        instance = new Mac32();
                    }
                    break;
            }
        }

        if (instance == null) {
            throw new RuntimeException("could not determine operating system type or architecture, default values object is null");
        }

        LogUtils.log(String.format("DefaultValues instance is [%s]", instance));
        return instance;
    }


    public abstract String getChromeDriverPath();

    public abstract String getIEDriverPath();


    public static class Windows32 extends DefaultValues {

        @Override
        public String getChromeDriverPath() {
            return SGTestHelper.getSGTestRootDir() + "/src/main/resources/webui/chromedriver.exe";
        }

        @Override
        public String getIEDriverPath() {
            return SGTestHelper.getSGTestRootDir() + "/src/main/resources/webui/IEDriverServer.exe";
        }
    }

    public static class Linux32 extends DefaultValues {

        @Override
        public String getChromeDriverPath() {
            throw new MissingResourceException(
                    "we don't have a chrome driver for Linux 32 yet. please download one and return its path from this method if you wish to run tests on this operating system.", null, null);
        }

        @Override
        public String getIEDriverPath() {
            throw new UnsupportedOperationException("running tests on Internet Explorer is not supported on Linux systems!");
        }
    }

    public static class Mac32 extends DefaultValues {

        @Override
        public String getChromeDriverPath() {
            throw new MissingResourceException(
                    "we don't have a chrome driver for Mac 32 yet. please download one and return its path from this method if you wish to run tests on this operating system.", null, null);
        }

        @Override
        public String getIEDriverPath() {
            throw new UnsupportedOperationException("running tests on Internet Explorer is not supported on Mac systems!");
        }
    }

    public static class Windows64 extends DefaultValues {

        @Override
        public String getChromeDriverPath() {
            throw new MissingResourceException(
                    "we don't have a chrome driver for Windows 64 yet. please download one and return its path from this method if you wish to run tests on this operating system.", null, null);
        }

        @Override
        public String getIEDriverPath() {
            throw new MissingResourceException(
                    "we don't have an IE driver for Windows 64 yet. please download one and return its path from this method if you wish to run tests on this operating system.", null, null);
        }
    }

    public static class Linux64 extends DefaultValues {

        @Override
        public String getChromeDriverPath() {
            return SGTestHelper.getSGTestRootDir() + "/src/main/resources/webui/chromedriver_linux_64";
        }

        @Override
        public String getIEDriverPath() {
            throw new UnsupportedOperationException("running tests on Internet Explorer is not supported on Linux systems!");
        }
    }

    public static class Mac64 extends DefaultValues {

        @Override
        public String getChromeDriverPath() {
            throw new MissingResourceException(
                    "we don't have a chrome driver for Mac 64 yet. please download one and return its path from this method if you wish to run tests on this operating system.", null, null);
        }

        @Override
        public String getIEDriverPath() {
            throw new UnsupportedOperationException("running tests on Internet Explorer is not supported on Mac systems!");
        }
    }

}
