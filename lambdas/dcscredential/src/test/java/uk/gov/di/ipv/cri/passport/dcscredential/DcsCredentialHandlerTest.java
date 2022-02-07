package uk.gov.di.ipv.cri.passport.dcscredential;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.BearerTokenError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.passport.dcscredential.domain.PassportCredentialIssuerResponse;
import uk.gov.di.ipv.cri.passport.library.domain.DcsResponse;
import uk.gov.di.ipv.cri.passport.library.domain.PassportAttributes;
import uk.gov.di.ipv.cri.passport.library.persistence.item.PassportCheckDao;
import uk.gov.di.ipv.cri.passport.library.service.AccessTokenService;
import uk.gov.di.ipv.cri.passport.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.passport.library.service.DcsCredentialService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DcsCredentialHandlerTest {

    private static final String TEST_RESOURCE_ID = UUID.randomUUID().toString();
    public static final String PASSPORT_NUMBER = "1234567890";
    public static final String SURNAME = "Tattsyrup";
    public static final String[] FORENAMES = {"Tubbs"};
    public static final String DATE_OF_BIRTH = "1984-09-28";
    public static final String EXPIRY_DATE = "2024-09-03";

    @Mock private Context mockContext;

    @Mock private DcsCredentialService mockDcsCredentialService;

    @Mock private AccessTokenService mockAccessTokenService;

    @Mock private ConfigurationService mockConfigurationService;

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private DcsCredentialHandler dcsCredentialHandler;
    private PassportCheckDao dcsCredential;
    private Map<String, String> responseBody;

    private final DcsResponse validDcsResponse =
            new DcsResponse(UUID.randomUUID(), UUID.randomUUID(), false, true, null);

    private final PassportAttributes attributes =
            new PassportAttributes(
                    PASSPORT_NUMBER,
                    SURNAME,
                    FORENAMES,
                    LocalDate.parse(DATE_OF_BIRTH),
                    LocalDate.parse(EXPIRY_DATE));

    @BeforeEach
    void setUp() {
        attributes.setDcsResponse(validDcsResponse);
        dcsCredential =
                new PassportCheckDao(TEST_RESOURCE_ID, attributes);
        responseBody = new HashMap<>();

        dcsCredentialHandler =
                new DcsCredentialHandler(mockDcsCredentialService, mockAccessTokenService, mockConfigurationService);
    }

    @Test
    void shouldReturn200OnSuccessfulDcsCredentialRequest() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        Map<String, String> headers =
                Collections.singletonMap("Authorization", accessToken.toAuthorizationHeader());
        event.setHeaders(headers);

        when(mockAccessTokenService.getResourceIdByAccessToken(anyString()))
                .thenReturn(TEST_RESOURCE_ID);
        when(mockDcsCredentialService.getDcsCredential(anyString())).thenReturn(dcsCredential);

        APIGatewayProxyResponseEvent response =
                dcsCredentialHandler.handleRequest(event, mockContext);

        assertEquals(200, response.getStatusCode());
    }

    @Test
    void shouldReturnCredentialsOnSuccessfulDcsCredentialRequest() throws JsonProcessingException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        Map<String, String> headers =
                Collections.singletonMap("Authorization", accessToken.toAuthorizationHeader());
        event.setHeaders(headers);

        when(mockAccessTokenService.getResourceIdByAccessToken(anyString()))
                .thenReturn(TEST_RESOURCE_ID);
        when(mockDcsCredentialService.getDcsCredential(anyString())).thenReturn(dcsCredential);

        APIGatewayProxyResponseEvent response =
                dcsCredentialHandler.handleRequest(event, mockContext);
        PassportCredentialIssuerResponse responseBody =
                objectMapper.readValue(response.getBody(), PassportCredentialIssuerResponse.class);

        assertEquals(dcsCredential.getResourceId(), responseBody.getResourceId());
        assertEquals(dcsCredential.getAttributes().getSurname(), responseBody.getAttributes().getNames().getFamilyName());
        assertEquals(dcsCredential.getAttributes().getForenames()[0], responseBody.getAttributes().getNames().getGivenNames()[0]);
        assertEquals(dcsCredential.getAttributes().getPassportNumber(), responseBody.getAttributes().getPassportNumber());
        assertEquals(dcsCredential.getAttributes().getDateOfBirth(), responseBody.getAttributes().getDateOfBirth());
        assertEquals(dcsCredential.getAttributes().getExpiryDate(), responseBody.getAttributes().getExpiryDate());
        assertEquals(dcsCredential.getAttributes().getRequestId(), responseBody.getAttributes().getRequestId());
        assertEquals(dcsCredential.getAttributes().getCorrelationId(), responseBody.getAttributes().getCorrelationId());
        assertEquals(dcsCredential.getAttributes().getDcsResponse().getRequestId(), responseBody.getAttributes().getDcsResponse().getRequestId());
    }

    @Test
    void shouldReturnErrorResponseWhenTokenIsNull() throws JsonProcessingException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> headers = Collections.singletonMap("Authorization", null);
        event.setHeaders(headers);

        APIGatewayProxyResponseEvent response =
                dcsCredentialHandler.handleRequest(event, mockContext);
        responseBody = objectMapper.readValue(response.getBody(), new TypeReference<>() {});

        assertEquals(BearerTokenError.MISSING_TOKEN.getHTTPStatusCode(), response.getStatusCode());
        assertEquals(BearerTokenError.MISSING_TOKEN.getCode(), responseBody.get("error"));
        assertEquals(
                BearerTokenError.MISSING_TOKEN.getDescription(),
                responseBody.get("error_description"));
    }

    @Test
    void shouldReturnErrorResponseWhenTokenIsMissingBearerPrefix() throws JsonProcessingException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> headers = Collections.singletonMap("Authorization", "11111111");
        event.setHeaders(headers);

        APIGatewayProxyResponseEvent response =
                dcsCredentialHandler.handleRequest(event, mockContext);
        responseBody = objectMapper.readValue(response.getBody(), new TypeReference<>() {});

        assertEquals(
                BearerTokenError.INVALID_REQUEST.getHTTPStatusCode(), response.getStatusCode());
        assertEquals(BearerTokenError.INVALID_REQUEST.getCode(), responseBody.get("error"));
        assertEquals(
                BearerTokenError.INVALID_REQUEST.getDescription(),
                responseBody.get("error_description"));
    }

    @Test
    void shouldReturnErrorResponseWhenTokenIsMissing() throws JsonProcessingException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();

        APIGatewayProxyResponseEvent response =
                dcsCredentialHandler.handleRequest(event, mockContext);
        responseBody = objectMapper.readValue(response.getBody(), new TypeReference<>() {});

        assertEquals(BearerTokenError.MISSING_TOKEN.getHTTPStatusCode(), response.getStatusCode());
        assertEquals(BearerTokenError.MISSING_TOKEN.getCode(), responseBody.get("error"));
        assertEquals(
                BearerTokenError.MISSING_TOKEN.getDescription(),
                responseBody.get("error_description"));
    }

    @Test
    void shouldReturnErrorResponseWhenInvalidAccessTokenProvided() throws JsonProcessingException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        Map<String, String> headers =
                Collections.singletonMap("Authorization", accessToken.toAuthorizationHeader());
        event.setHeaders(headers);

        when(mockAccessTokenService.getResourceIdByAccessToken(anyString())).thenReturn(null);

        APIGatewayProxyResponseEvent response =
                dcsCredentialHandler.handleRequest(event, mockContext);
        Map<String, Object> responseBody =
                objectMapper.readValue(response.getBody(), new TypeReference<>() {});

        assertEquals(403, response.getStatusCode());
        assertEquals(OAuth2Error.ACCESS_DENIED.getCode(), responseBody.get("error"));
        assertEquals(
                OAuth2Error.ACCESS_DENIED
                        .appendDescription(
                                " - The supplied access token was not found in the database")
                        .getDescription(),
                responseBody.get("error_description"));
    }
}
