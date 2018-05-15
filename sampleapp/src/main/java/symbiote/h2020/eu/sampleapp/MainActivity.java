package symbiote.h2020.eu.sampleapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.JsonProcessingException;
import eu.h2020.symbiote.security.ClientSecurityHandlerFactory;
import eu.h2020.symbiote.security.commons.Certificate;
import eu.h2020.symbiote.security.commons.Token;
import eu.h2020.symbiote.security.commons.credentials.AuthorizationCredentials;
import eu.h2020.symbiote.security.commons.exceptions.custom.AAMException;
import eu.h2020.symbiote.security.commons.exceptions.custom.JWTCreationException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.security.communication.AAMClient;
import eu.h2020.symbiote.security.communication.IAAMClient;
import eu.h2020.symbiote.security.communication.payloads.AAM;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.ISecurityHandler;
import eu.h2020.symbiote.security.helpers.MutualAuthenticationHelper;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

  public static final String TAG = "MainActivity";
  private final String AAMServerAddress = "https://symbiote-open.man.poznan.pl/coreInterface/";
  private String keyStorePassword = "KEYSTORE_PASSWORD";
  private String icomUsername = "USERNAME";
  private String icomPassword = "PASSWORD";
  private String platformId = "SymbIoTe_Core_AAM";
  private String clientId = "1ef55ca2-206a-11e8-b467-0ed5f89f718b";
  private String keyStoreFilename = "/keystore.jks";
  private ISecurityHandler clientSH = null;

  static {
    Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    //don't put the network job to background like this in your app
    // - this is only an example :)
    new Thread(new Runnable() {
      @Override public void run() {
        //1. getting guest token
        //getGuestTokenAndHeaders();
        //2. getting security request
        getSecurityRequest();
      }
    }).start();
  }

  public void getSecurityRequest() {
    // Initializing application security handler
    String keyStorePath =
        getApplicationContext().getFilesDir().getAbsolutePath() + keyStoreFilename;

    try {
      clientSH = ClientSecurityHandlerFactory.getSecurityHandler(AAMServerAddress, keyStorePath,
          keyStorePassword);
    } catch (SecurityHandlerException e) {
      e.printStackTrace();
    }
    // examples how to retrieve AAM instances
    AAM coreAAM = clientSH.getCoreAAMInstance();
    AAM platform1 = null;
    try {
      platform1 = clientSH.getAvailableAAMs().get(platformId);
    } catch (SecurityHandlerException e) {
      e.printStackTrace();
    }

    // Acquiring application certificate, this operation needs the user password
    try {
      Certificate clientCertificate =
          clientSH.getCertificate(platform1, icomUsername, icomPassword, clientId);
    } catch (SecurityHandlerException e) {
      e.printStackTrace();
    }

    // Acquiring HOME token from platform1 AAM
    Token token = null;
    try {
      token = clientSH.login(platform1);
    } catch (SecurityHandlerException e) {
      e.printStackTrace();
    } catch (ValidationException e) {
      e.printStackTrace();
    }

    // preparing the security request using the credentials the actor has from platform 1
    Set<AuthorizationCredentials> authorizationCredentialsSet = new HashSet<>();
    // please note that from now on we don't need the password and only the the client certificate and matching private key.
    authorizationCredentialsSet.add(new AuthorizationCredentials(token, platform1,
        clientSH.getAcquiredCredentials().get(platform1.getAamInstanceId()).homeCredentials));
    try {
      SecurityRequest securityRequest =
          MutualAuthenticationHelper.getSecurityRequest(authorizationCredentialsSet, false);
      //Simple HTTP Request to test
      String x_auth_1 = "{\"token\":\"["
          + token.getToken()
          + "]\", "
          + "\"authenticationChallenge\":\"\", "
          + "\"clientCertificate\":\"\", "
          + "\"clientCertificateSigningAAMCertificate\":\"\", "
          + "\"foreignTokenIssuingAAMCertificate\":\"\"}";
      RequestQueue queue = Volley.newRequestQueue(this);
      JsonObjectRequest stringRequest =
          new JsonObjectRequest(Request.Method.GET, AAMServerAddress + "query", null,
              new Response.Listener<JSONObject>() {
                @Override public void onResponse(JSONObject response) {
                  //verifying the service response
                  //once again - don't start the background job in your production app :)
                  new Thread(new Runnable() {
                    @Override public void run() {
                      Log.d(TAG, response.toString());
                      try {
                        String serviceResponse = response.getString("serviceResponse");
                        if (serviceResponse != null) {
                          boolean result =
                              MutualAuthenticationHelper.isServiceResponseVerified(serviceResponse,
                                  clientSH.getComponentCertificate("search", platformId));
                          if (result) Log.d(TAG, "ServiceResponse verified!");
                        }
                      } catch (JSONException e) {
                        e.printStackTrace();
                      } catch (CertificateException e) {
                        e.printStackTrace();
                      } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                      } catch (SecurityHandlerException e) {
                        e.printStackTrace();
                      }
                    }
                    //
                  }).start();
                }
              }, new Response.ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
              Log.d(TAG, "That didn't work!");
            }
          }) {
            @Override public Map<String, String> getHeaders() throws AuthFailureError {
              HashMap<String, String> headers = new HashMap<>();
              headers.put("x-auth-timestamp", String.valueOf(securityRequest.getTimestamp()));
              headers.put("x-auth-size", String.valueOf(1));
              headers.put("x-auth-1", x_auth_1);
              return headers;
            }
          };

      queue.add(stringRequest);
    } catch (NoSuchAlgorithmException e)

    {
      e.printStackTrace();
    }
  }

  public void getGuestTokenAndHeaders() {
    //this code is from the readme.md ss github
    // creating REST client communicating with SymbIoTe Authorization Services
    // AAMServerAddress can be acquired from SymbIoTe web page
    IAAMClient restClient = new AAMClient(AAMServerAddress);

    // acquiring Guest Token
    String guestToken = null;
    try {
      guestToken = restClient.getGuestToken();
      Log.d(MainActivity.class.getSimpleName(), String.format("Got token: %s", guestToken));
    } catch (JWTCreationException e) {
      e.printStackTrace();
    } catch (AAMException e) {
      e.printStackTrace();
    }

    // creating securityRequest using guest Token
    SecurityRequest securityRequest = new SecurityRequest(guestToken);

    // converting the prepared request into communication ready HTTP headers.
    Map<String, String> securityHeaders = new HashMap<>();
    try {
      securityHeaders = securityRequest.getSecurityRequestHeaderParams();
      Log.d(MainActivity.class.getSimpleName(),
          String.format("Got headers: %s", securityHeaders.values().toString()));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }
}
