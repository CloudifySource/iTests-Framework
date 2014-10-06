package iTests.framework.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by yohana on 8/14/14.
 */
public class HTTPUtils {
    public enum PARAMS {
        RESPONSE_CODE,
        RESPONSE_BODY,
        COOKIES
    }
/*
Commented by Yohana. Reason: Not tested thus might not work as expected!
    public static boolean isAlive(String url) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            // note : you may also need
            //        HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection con =
                    (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("HEAD");
            con.setConnectTimeout(5000);
            con.connect();
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isURLAvailable(URL url) throws Exception {
        HttpClient client = new DefaultHttpClient();

        HttpGet get = new HttpGet(url.toURI());
        try {
            LogUtils.log("Executing HttpGet to url " + url);
            HttpResponse execute = client.execute(get);
            if (execute.getStatusLine().getStatusCode() == HttpServletResponse.SC_NOT_FOUND) {
                LogUtils.log("Http request failed");
                return false;
            } else {
                LogUtils.log("HttpHead returned successfully");
                return true;
            }
        }
        catch (Exception e) {
            LogUtils.log("Caught exception while executing HttpGet : " + e.getMessage());
            return false;
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }*/

    public static class HTTPSession {
        BasicCookieStore cookieStore = new BasicCookieStore();

        public HashMap<PARAMS, String> post(HTTPPostRequest postRequest) throws IOException {
            HashMap<PARAMS, String> result = postRequest.post(cookieStore);
            return result;
        }
        public HashMap<PARAMS, String> get(HTTPGetRequest getRequest) throws IOException {
            HashMap<PARAMS, String> result = getRequest.get(cookieStore);
            return result;
        }

    }
    public static class HTTPPostRequest {
        private final String _urlAsString;
        private final List<BasicNameValuePair> postParams;

        public HTTPPostRequest (String urlAsString) {
            _urlAsString = urlAsString;
            postParams = new ArrayList<BasicNameValuePair>();
        }

        public HTTPPostRequest withParameter(String key, String value) {
            postParams.add(new BasicNameValuePair(key, value));
            return this;
        }

        protected HashMap<PARAMS, String> post(CookieStore cookieStore) throws IOException {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            httpClient.setCookieStore(cookieStore);
            HttpPost httpPost = new HttpPost(_urlAsString);
            httpPost.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));

            httpClient.setRedirectStrategy(new DefaultRedirectStrategy());
            httpPost.getParams().setParameter("http.protocol.handle-redirects",true);

            HttpResponse response = httpClient.execute(httpPost);

            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");
            HashMap<PARAMS, String> result = new HashMap<PARAMS, String>();
            result.put(PARAMS.RESPONSE_BODY, responseString);
            result.put(PARAMS.RESPONSE_CODE, String.valueOf(response.getStatusLine().getStatusCode()));
            Iterator<Cookie> it = cookieStore.getCookies().iterator();
            String cookies ="";
            while (it.hasNext()) {
                Cookie c = it.next();
                cookies += c.getName()+"="+c.getValue();
                if (it.hasNext()) {
                    cookies += ";";
                }
            }
            result.put(PARAMS.COOKIES, cookies);
            return result;
        }
    }

    public static class HTTPGetRequest {
        private final String _urlAsString;

        public HTTPGetRequest (String urlAsString) {
            _urlAsString = urlAsString;
        }

        protected HashMap<PARAMS, String> get(CookieStore cookieStore) throws IOException {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            httpClient.setCookieStore(cookieStore);
            HttpGet httpGet = new HttpGet(_urlAsString);

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");
            HashMap<PARAMS, String> result = new HashMap<PARAMS, String>();
            result.put(PARAMS.RESPONSE_BODY, responseString);
            result.put(PARAMS.RESPONSE_CODE, String.valueOf(response.getStatusLine().getStatusCode()));
            Iterator<Cookie> it = cookieStore.getCookies().iterator();
            String cookies ="";
     /*       String urlString = _urlAsString;
            if (urlString.lastIndexOf('/') == urlString.length()-1) {
                urlString = urlString.substring(0, urlString.length() -1);
            }*/
            URL url = new URL(/*urlString*/_urlAsString);
            while (it.hasNext()) {
                Cookie c = it.next();
                if (c.getDomain().equals(url.getHost()) /*&& c.getPath().equals(url.getPath())*/) {
                    cookies += c.getName() + "=" + c.getValue();
                    if (it.hasNext()) {
                        cookies += ";";
                    }
                }
            }
            result.put(PARAMS.COOKIES, cookies);
            return result;
        }
    }

}
