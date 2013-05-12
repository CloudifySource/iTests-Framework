package iTests.framework.utils;

import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.openspaces.jee.sessions.jetty.SessionData;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class WebUtils {

    /***
     *  Used to get the session attributeMap which is protected
     */
    public static ConcurrentHashMap<String, Object> getSessionDataAttributeMap(
            SessionData data) throws Exception {

        Class clazz = data.getClass();
        Class[] paramTypes = new Class[] {};
        Method m = clazz.getDeclaredMethod("getAttributeMap", paramTypes);
        m.setAccessible(true);
        ConcurrentHashMap<String, Object> result = 
            (ConcurrentHashMap<String, Object>) m.invoke(data, new Object[] {});

        return result;
    }
    
    /***
     * used to get the message from an object using reflection
     */
    public static String getMessage(Object message) throws Exception {
        Class clazz = message.getClass();
        Class[] paramTypes = new Class[] {};
        Method m = clazz.getDeclaredMethod("getMessage", paramTypes);
        String result = (String) m.invoke(message, new Object[] {});
        
        return result;
    }

    public static void repetitiveAssertWebUrlAvailable(final String applicationUrl, long timeout, TimeUnit timeUnit) throws KeyManagementException, NoSuchAlgorithmException {
        final HttpClient client = new DefaultHttpClient();
        trustAllCertificates(client);

        AssertUtils.repetitiveAssertTrue("Cannot access " + applicationUrl, new RepetitiveConditionProvider() {

            @Override
            public boolean getCondition() {
                try {
                    try {
                        final HttpGet get = new HttpGet(new URL(applicationUrl).toURI());

                        final HttpResponse response = client.execute(get);
                        if (response.getStatusLine().getStatusCode() != 200) {
                            LogUtils.log("Failed to access " + applicationUrl + " response:" + response.getStatusLine().getReasonPhrase() + " status code:" + response.getStatusLine().getStatusCode());
                        }
                        return true;

                    } finally {
                        client.getConnectionManager().shutdown();
                    }
                } catch (final Exception e) {
                    LogUtils.log("Failed to access " + applicationUrl, e);
                    return false;
                }
            }
        }, timeUnit.toMillis(timeout));
    }

    public static boolean isURLAvailable(URL url) throws Exception {
        HttpClient client = new DefaultHttpClient();
        trustAllCertificates(client);

        // Do not use HEAD here! The spring org.cloudifysource.quality.iTests.framework we use does not like it.
        HttpGet get = new HttpGet(url.toURI());
        try {
        	LogUtils.log("Executing HttpHead to url " + url);
            client.execute(get);
            LogUtils.log("HttpHead returned succesfully");
            return true;
        }
        catch (Exception e) {
        	LogUtils.log("Caught exception while exectuing HttpHead");
        	return false;
        }
            finally {
            LogUtils.log("closing connection manager");
            client.getConnectionManager().shutdown();
            LogUtils.log("connection manager closed");
        }
    }

    private static void trustAllCertificates(HttpClient client) throws NoSuchAlgorithmException, KeyManagementException {

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {

                    public java.security.cert.X509Certificate[] getAcceptedIssuers()
                    {
                        return null;
                    }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
                    {
                        //No need to implement.
                    }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
                    {
                        //No need to implement.
                    }
                }
        };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());

        SSLSocketFactory socketFactory = new SSLSocketFactory(sc, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        Scheme sch = new Scheme("https", 443, socketFactory);
        client.getConnectionManager().getSchemeRegistry().register(sch);

    }

    public static boolean waitForHost(String url, int timeout) throws Exception {
        InetAddress address = InetAddress.getByName(url);
        return address.isReachable(timeout);
    }

    /**
     *
     * @return the content of the given url or an empty if not found
     * Use #getURLContent(URL) or repetitiveAssertWebUrlAvailable(URL) instead
     */
    @Deprecated
    public static String getURLContentSwallowExceptions(URL url) throws Exception {
        HttpClient client = new DefaultHttpClient();
        trustAllCertificates(client);
        HttpGet get = new HttpGet(url.toURI());
        try {
            return client.execute(get, new BasicResponseHandler());
        } catch (Exception e) {
            return "";
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
     * @param url
     * @return the content of the given url or an empty if not found
     * @throws Exception
     */
    public static String getURLContent(URL url) throws Exception {
        HttpClient client = new DefaultHttpClient();
        trustAllCertificates(client);
        HttpGet get = new HttpGet(url.toURI());
        try {
            return client.execute(get, new BasicResponseHandler());
        } catch (Exception e) {
            return "";
        } finally {
            client.getConnectionManager().shutdown();
        }        
    }

    public static String getURLContent(URL url, String username, String password) throws ClientProtocolException, IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException {
        DefaultHttpClient client = new DefaultHttpClient();
        trustAllCertificates(client);
        client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
                new UsernamePasswordCredentials(username, password));

        HttpGet get = new HttpGet(url.toURI());
        try {
            return client.execute(get, new BasicResponseHandler());
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }
}
