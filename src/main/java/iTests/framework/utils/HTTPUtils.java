package iTests.framework.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
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

        public HTTPResponse post(HTTPPostRequest postRequest) throws IOException {
            HTTPResponse result = postRequest.post(cookieStore);
            return result;
        }

        public HTTPResponse put(HTTPPutRequest putRequest) throws IOException {
            HTTPResponse result = putRequest.put(cookieStore);
            return result;
        }

        public HTTPResponse get(HTTPGetRequest getRequest) throws IOException {
            HTTPResponse result = getRequest.get(cookieStore);
            return result;
        }
        public HTTPResponse delete(HTTPDeleteRequest deleteRequest) throws IOException {
            return deleteRequest.get(cookieStore);
        }

    }

    public static class HTTPResponse {
        private HashMap<PARAMS, String> response;
        protected HTTPResponse() {
            response = new HashMap<PARAMS, String>();
        }

        public int getStatusCode() {
            return Integer.valueOf(response.get(PARAMS.RESPONSE_CODE));
        }

        public String getBody() {
            return response.get(PARAMS.RESPONSE_BODY);
        }

        public String getCookies() {
            return response.get(PARAMS.COOKIES);
        }

        public String get(PARAMS param) {
            return response.get(param);
        }

        public void set(PARAMS param, String value) {
            response.put(param, value);
        }

        @Override
        public String toString() {
            return response.toString();
        }
    }


    public static class HTTPPostRequest {
        private final String _urlAsString;
        private final List<BasicNameValuePair> postParams;
        private String jsonBody;

        public HTTPPostRequest (String urlAsString) {
            _urlAsString = urlAsString;
            postParams = new ArrayList<BasicNameValuePair>();
        }

        public HTTPPostRequest withParameter(String key, String value) {
            postParams.add(new BasicNameValuePair(key, value));
            return this;
        }

        public HTTPPostRequest withJSONBody(String jsonBody) {
            this.jsonBody = jsonBody;
            return this;
        }

        protected HTTPResponse post(CookieStore cookieStore) throws IOException {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            httpClient.setCookieStore(cookieStore);
            HttpPost httpPost = new HttpPost(_urlAsString);
            if (jsonBody != null) {
                httpPost.setEntity(new StringEntity(jsonBody, "UTF-8"));
                httpPost.setHeader("Content-type", "application/json; charset=UTF-8");
            } else {
                httpPost.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));
            }

            httpClient.setRedirectStrategy(new DefaultRedirectStrategy());
            httpPost.getParams().setParameter("http.protocol.handle-redirects",true);

            HttpResponse response = httpClient.execute(httpPost);

            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");
            HTTPResponse result = new HTTPResponse();
            result.set(PARAMS.RESPONSE_BODY, responseString);
            result.set(PARAMS.RESPONSE_CODE, String.valueOf(response.getStatusLine().getStatusCode()));
            Iterator<Cookie> it = cookieStore.getCookies().iterator();
            String cookies ="";
            while (it.hasNext()) {
                Cookie c = it.next();
                cookies += c.getName()+"="+c.getValue();
                if (it.hasNext()) {
                    cookies += ";";
                }
            }
            result.set(PARAMS.COOKIES, cookies);
            return result;
        }
    }

    public static class HTTPGetRequest {
        private final String _urlAsString;

        public HTTPGetRequest (String urlAsString) {
            _urlAsString = urlAsString;
        }

        protected HTTPResponse get(CookieStore cookieStore) throws IOException {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            httpClient.setCookieStore(cookieStore);
            HttpGet httpGet = new HttpGet(_urlAsString);

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");
            HTTPResponse result = new HTTPResponse();
            result.set(PARAMS.RESPONSE_BODY, responseString);
            result.set(PARAMS.RESPONSE_CODE, String.valueOf(response.getStatusLine().getStatusCode()));
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
            result.set(PARAMS.COOKIES, cookies);
            return result;
        }
    }


    public static class HTTPDeleteRequest {
        private final String _urlAsString;

        public HTTPDeleteRequest (String urlAsString) {
            _urlAsString = urlAsString;
        }

        protected HTTPResponse get(CookieStore cookieStore) throws IOException {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            httpClient.setCookieStore(cookieStore);
            HttpDelete httpGet = new HttpDelete(_urlAsString);

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");
            HTTPResponse result = new HTTPResponse();
            result.set(PARAMS.RESPONSE_BODY, responseString);
            result.set(PARAMS.RESPONSE_CODE, String.valueOf(response.getStatusLine().getStatusCode()));
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
            result.set(PARAMS.COOKIES, cookies);
            return result;
        }
    }

    public static class HTTPPutRequest {
        private final String _urlAsString;
        private final List<BasicNameValuePair> postParams;
        private String jsonBody;

        public HTTPPutRequest (String urlAsString) {
            _urlAsString = urlAsString;
            postParams = new ArrayList<BasicNameValuePair>();
        }

        public HTTPPutRequest withParameter(String key, String value) {
            postParams.add(new BasicNameValuePair(key, value));
            return this;
        }

        public HTTPPutRequest withJSONBody(String jsonBody) {
            this.jsonBody = jsonBody;
            return this;
        }

        protected HTTPResponse put(CookieStore cookieStore) throws IOException {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            httpClient.setCookieStore(cookieStore);
            HttpPut httpPut = new HttpPut(_urlAsString);
            if (jsonBody != null) {
                httpPut.setEntity(new StringEntity(jsonBody, "UTF-8"));
                httpPut.setHeader("Content-type", "application/json; charset=UTF-8");
            } else {
                httpPut.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));
            }

            httpClient.setRedirectStrategy(new DefaultRedirectStrategy());
            httpPut.getParams().setParameter("http.protocol.handle-redirects",true);

            HttpResponse response = httpClient.execute(httpPut);

            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");
            HTTPResponse result = new HTTPResponse();
            result.set(PARAMS.RESPONSE_BODY, responseString);
            result.set(PARAMS.RESPONSE_CODE, String.valueOf(response.getStatusLine().getStatusCode()));
            Iterator<Cookie> it = cookieStore.getCookies().iterator();
            String cookies ="";
            while (it.hasNext()) {
                Cookie c = it.next();
                cookies += c.getName()+"="+c.getValue();
                if (it.hasNext()) {
                    cookies += ";";
                }
            }
            result.set(PARAMS.COOKIES, cookies);
            return result;
        }
    }
}
