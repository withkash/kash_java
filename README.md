# Kash API Java Client

This is the KASH API client for the the Server facing API.

Create a client connected to the test environment:

    Client client = Client.createTestClient("<your secret key>");

Create an authorization for payment:

    Authorization authorization = null;

    try {
        authorization = client.createAuthorization("83ada43e-ae0c-42a2-98dc-9bc87123d276", new BigDecimal("20.00"));
    } catch (NotSufficientFundsException e) {
        e.printStackTrace();
    } catch (RelinkRequiredException e) {
        e.printStackTrace();
    } catch (UnexpectedErrorException e) {
        e.printStackTrace();
    }

Clear an authorization:

    try {
        client.removeAuthorization(authorization.authorizationId);
    } catch (UnexpectedErrorException e) {
        e.printStackTrace();
    }

Create a transaction:

    Transaction transaction = null;
    try {
        transaction = client.createTransaction(authorization.authorizationId, new BigDecimal("20.00"));
    } catch (UnexpectedErrorException e) {
        e.printStackTrace();
    } catch (NotSufficientFundsException e) {
        e.printStackTrace();
    }
