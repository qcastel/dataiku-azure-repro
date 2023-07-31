package dev.dataiku.azuread.demo;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.okhttp.OkHttpAsyncHttpClientBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.httpcore.HttpClients;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.DirectoryObjectCollectionPage;
import com.microsoft.graph.requests.DirectoryObjectCollectionRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class ReproSecurityGroupIssue {

    public static void main(String[] args) throws Exception {

        String tenant = args[0];
        String clientId = args[1];
        String clientSecret = args[2];
        String userEmail = args[3];

        TokenCredential tokenCredential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenant)
                .httpClient(getAzureHttpClient())
                .build();
        final TokenCredentialAuthProvider tokenCredentialAuthProvider = new TokenCredentialAuthProvider(Arrays.asList("https://graph.microsoft.com/.default"), tokenCredential);

        GraphServiceClient<Request> graphClient = GraphServiceClient.builder()
                .httpClient(getOkHttpClient(tokenCredentialAuthProvider))
                .buildClient();

        List<User> currentPage = graphClient.users().buildRequest()
                .filter("mail eq '" + userEmail + "'")
                .expand("transitiveMemberOf")
                .get().getCurrentPage();
        if (currentPage.size() == 0) {
            System.out.println("No user found");
            return;
        } else if (currentPage.size() > 1) {
            System.out.println("More than one user found matching the email '" + userEmail + "'");
            return;
        }
        User user = currentPage.get(0);
        System.out.println("User found! displayName = " + user.displayName);

        DirectoryObjectCollectionPage initialPage = user.transitiveMemberOf;
        do {
            System.out.println("Groups:");
            initialPage.getCurrentPage().stream()
                    .filter(d -> d.oDataType.equals("#microsoft.graph.group"))
                    .map(d -> ((Group) d).displayName)
                    .forEach(g -> System.out.println("- " + g));
            DirectoryObjectCollectionRequestBuilder nextPage = initialPage.getNextPage();
            initialPage = nextPage == null ? null : nextPage.buildRequest().get();
        } while (initialPage != null);
    }
    static TrustManager TRUST_ALL_CERTS = new X509TrustManager() {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] {};
        }
    };

    public static HttpClient getAzureHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[] { TRUST_ALL_CERTS }, new java.security.SecureRandom());
        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) TRUST_ALL_CERTS)
                .build();
        return new OkHttpAsyncHttpClientBuilder(client)
                .build();
    }

    public static OkHttpClient getOkHttpClient(TokenCredentialAuthProvider tokenCredentialAuthProvider) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[] { TRUST_ALL_CERTS }, new java.security.SecureRandom());
        return HttpClients.createDefault(tokenCredentialAuthProvider)
                .newBuilder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) TRUST_ALL_CERTS)
                .build();
    }
}
