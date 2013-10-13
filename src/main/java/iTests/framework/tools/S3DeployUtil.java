package iTests.framework.tools;

import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.s3.S3Client;
import org.jclouds.s3.domain.AccessControlList;
import org.jclouds.s3.domain.CannedAccessPolicy;

public class S3DeployUtil {

    protected static final String CREDENTIALS_FOLDER =System.getProperty("iTests.credentialsFolder",SGTestHelper.getSGTestRootDir() + "/src/main/resources/credentials");
    protected static final int ITESTS_LOG_EXPIRATION_IN_DAYS = Integer.getInteger("iTests.logExpirationInDays", 10);
    private static final String S3_PROPERTIES = CREDENTIALS_FOLDER + "/s3.properties";

    public static void uploadLogFile(File source, String buildNumber, String suiteName, String testName){
        try {
        	Properties props = getS3Properties();
            String container =  props.getProperty("container");
            String user =  props.getProperty("user");
            String key =  props.getProperty("key");
            String target = buildNumber + "/" + suiteName + "/" + testName;
            BlobStoreContext context = ContextBuilder.newBuilder("s3")
                    .credentials(user, key)
                    .buildView(BlobStoreContext.class);

            S3Client client = ContextBuilder.newBuilder("s3")
                    .credentials(user, key)
                    .buildApi(S3Client.class);

            BlobStore store = context.getBlobStore();

            uploadLogFile(source, target, container, client, store);
            context.close();

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    
    private static void uploadLogFile(File source, String target, String container, S3Client client, BlobStore store){
        if (source.isDirectory()){
            for (File f : source.listFiles()){
                uploadLogFile(new File(source.getPath() + "/" + f.getName()), target + "/" + f.getName(), container, client, store);
            }
        }
        else{

            LogUtils.log("Processing " + source + ", upload size is: " + (source).length() + ". Target: " + target);
            Date expires = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(ITESTS_LOG_EXPIRATION_IN_DAYS));
            try {
                Blob blob = store.blobBuilder(target)
                        .payload(source)
                        .build();
                blob.getMetadata().getContentMetadata().setExpires(expires);
                store.putBlob(container, blob);
                LogUtils.log("Upload of " + source + " was ended successfully");
                String ownerId = client.getObjectACL(container, target).getOwner().getId();
                client.putObjectACL(container, target,
                        AccessControlList.fromCannedAccessPolicy(CannedAccessPolicy.PUBLIC_READ, ownerId));
            }
            catch (Exception e){
                LogUtils.log("Upload of Log file: " + source + " Failed!",e);
            }
        }
    }


    private static Properties getS3Properties() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(S3_PROPERTIES));
        } catch (IOException e) {
            throw new RuntimeException("failed to read " + S3_PROPERTIES + " file - " + e, e);
        }

        return properties;
    }


}
