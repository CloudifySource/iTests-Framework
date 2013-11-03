package iTests.framework.testng.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TestConfiguration {

    public enum VM {
        UNIX,
        WINDOWS,
        MAC,
        ALL
    }

    VM[] os() default iTests.framework.testng.annotations.TestConfiguration.VM.ALL;

    public enum PROTOCOL {
        IPv6_Only,
        IPv4_Only,
        ALL
    }

    PROTOCOL internetProtocol() default iTests.framework.testng.annotations.TestConfiguration.PROTOCOL.ALL;

    public enum CLOUD {
        EC2,
        HP,
        BYON,
        RACKSPACE,
        ALL
    }

    CLOUD[] clouds() default iTests.framework.testng.annotations.TestConfiguration.CLOUD.ALL;


}
