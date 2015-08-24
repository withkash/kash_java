package com.withkash.kash;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.withkash.kash.exceptions.NotSufficientFundsException;
import com.withkash.kash.exceptions.RelinkRequiredException;
import com.withkash.kash.exceptions.UnexpectedErrorException;
import com.withkash.kash.model.Authorization;
import com.withkash.kash.model.Transaction;
import org.joda.time.DateTime;
import org.json.JSONException;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Kash Corp. info@withkash.com
 *
 * Kash API Client
 * <P>
 * Visit https://docs.withkash.com for more information.
 * </P>
 */
public class Client {

    public static final String KASH_PROD_API = "https://api.withkash.com/v1";
    public static final String KASH_TEST_API = "https://api-test.withkash.com/v1";
    public static final BigDecimal ONE_HUNDRED = BigDecimal.TEN.multiply(BigDecimal.TEN);

    private String mBaseUrl;
    private String mSecretKey;

    /**
     * Create a test Client object configured to use the Test Kash environment.
     * @param secretKey
     * @return
     */
    public static Client createTestClient(String secretKey) {

        URL aURL = null;
        try {
            aURL = new URL(KASH_TEST_API);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Check KASH_TEST_API url");
        }
        return new Client(secretKey, aURL);
    }

    /**
     * Create a Kash API Client configured to use the Prod Kash environment.
     * @param secretKey
     * @return
     */

    public static Client createClient(String secretKey) {

        URL aURL = null;
        try {
            aURL = new URL(KASH_PROD_API);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Check KASH_PROD_API url");
        }

        return new Client(secretKey, aURL);
    }

    /**
     * Create a Kash API Client configured to use a custom environment.
     * @param secretKey
     * @param url
     * @return
     * @throws MalformedURLException
     */
    public static Client createCustomClient(String secretKey, String url) throws MalformedURLException {

        URL aURL = new URL(url);
        return new Client(secretKey, aURL);
    }

    /**
     * Create a Kash Client.
     * @param secretKey Your secret/server key.
     * @param url URL pointing to the environment you wish to use.
     */
    private Client(String secretKey, URL url) {
        mSecretKey = secretKey;
        setBaseUrl(url);
    }

    private void setBaseUrl(URL url) {
        mBaseUrl = url.getProtocol() + "://" + url.getAuthority() + url.getFile();
    }

    /**
     * Create an Authorization for a given customer. See http://docs.withkash.com/docs/authorize-amount
     * @param customerId
     * @param amount
     * @return
     * @throws NotSufficientFundsException
     * @throws RelinkRequiredException
     * @throws UnexpectedErrorException
     */
    public Authorization createAuthorization(String customerId, BigDecimal amount) throws NotSufficientFundsException, RelinkRequiredException, UnexpectedErrorException {

        String requestUrl = mBaseUrl + "/authorizations";

        System.out.println(amount.multiply(ONE_HUNDRED).setScale(0).toPlainString());

        HttpResponse<JsonNode> jsonResponse = null;
        try {
            jsonResponse = Unirest.post(requestUrl)
                    .basicAuth(mSecretKey, "")
                    .header("accept", "application/json")
                    .field("customer_id", customerId)
                    .field("amount", amount.multiply(ONE_HUNDRED).setScale(0).toPlainString())
                    .asJson();
        } catch (UnirestException e) {
            throw new UnexpectedErrorException(e);
        }

        Authorization authorization = null;
        if (jsonResponse.getStatus() == 200) {
            JsonNode body = jsonResponse.getBody();
            org.json.JSONObject object = body.getObject();
            String authorizationId = object.getString("authorization_id");
            String validateUntilString = object.getString("valid_until");
            DateTime validUntil = new DateTime(validateUntilString);
            authorization = new Authorization(authorizationId, validUntil);
        }
        else if (jsonResponse.getStatus() == 402) {
            try {
                String details = jsonResponse.getBody().getObject().getString("error");
                throw new NotSufficientFundsException(details);
            } catch (JSONException e) {
                //couldn't get at the error message...
                throw new NotSufficientFundsException();
            }
        }
        else if (jsonResponse.getStatus() == 410) {
            try {
                String details = jsonResponse.getBody().getObject().getString("error");
                throw new RelinkRequiredException(details);
            } catch (JSONException e) {
                //couldn't get at the error message...
                throw new RelinkRequiredException();
            }
        }
        else if (jsonResponse.getStatus() == 500) {
            handle500Error(jsonResponse);
        }
        else {
            throw new UnexpectedErrorException(jsonResponse.getStatusText());
        }
        return authorization;
    }

    private void handle500Error(HttpResponse<JsonNode> jsonResponse) throws UnexpectedErrorException {
        try {
            String details = jsonResponse.getBody().getObject().getString("error");
            throw new UnexpectedErrorException(details);
        } catch (JSONException e) {
            //couldn't get at the error message...
            throw new UnexpectedErrorException();
        }
    }

    /**
     * Cancel an Authorization for a given customer. See http://docs.withkash.com/docs/cancel-an-authorization
     * @param authorizationId
     * @throws UnexpectedErrorException
     */
    public void removeAuthorization(String authorizationId) throws UnexpectedErrorException {
        String requestUrl = mBaseUrl + "/authorization/" + authorizationId;

        HttpResponse<JsonNode> jsonResponse = null;
        try {
            jsonResponse = Unirest.delete(requestUrl)
                    .basicAuth(mSecretKey, "")
                    .header("accept", "application/json")
                    .asJson();
        } catch (UnirestException e) {
            throw new UnexpectedErrorException(e);
        }

        if (jsonResponse.getStatus() == 200) {
            return;
        }
        else if (jsonResponse.getStatus() == 500) {
            handle500Error(jsonResponse);
        }
        else {
            throw new UnexpectedErrorException("" + jsonResponse.getStatus() + " - " + jsonResponse.getStatusText());
        }

    }

    /**
     * Create a Transaction given an Authorization. See http://docs.withkash.com/docs/create-transaction
     * @param authorizationId
     * @param amount
     * @return
     * @throws UnexpectedErrorException
     */
    public Transaction createTransaction(String authorizationId, BigDecimal amount) throws UnexpectedErrorException, NotSufficientFundsException {
        String requestUrl = mBaseUrl + "/transactions";
        HttpResponse<JsonNode> jsonResponse = null;

        try {
            jsonResponse = Unirest.post(requestUrl)
                    .basicAuth(mSecretKey, "")
                    .header("accept", "application/json")
                    .field("authorization_id", authorizationId)
                    .field("amount", amount.multiply(ONE_HUNDRED).setScale(0).toPlainString())
                    .asJson();
        } catch (UnirestException e) {
            throw new UnexpectedErrorException(e);
        }

        Transaction transaction = null;
        if (jsonResponse.getStatus() == 200) {
            JsonNode body = jsonResponse.getBody();
            org.json.JSONObject object = body.getObject();
            String transactionId = object.getString("transaction_id");
            transaction = new Transaction(transactionId);
        }
        else if (jsonResponse.getStatus() == 402) {
            try {
                String details = jsonResponse.getBody().getObject().getString("error");
                throw new NotSufficientFundsException(details);
            } catch (JSONException e) {
                //couldn't get at the error message...
                throw new NotSufficientFundsException();
            }
        }
        else if (jsonResponse.getStatus() == 500) {
            handle500Error(jsonResponse);
        }
        else {
            throw new UnexpectedErrorException("" + jsonResponse.getStatus() + " - " + jsonResponse.getStatusText());
        }
        return transaction;
    }


}
