package uk.gov.di.ipv.cri.passport.library.service;

import com.nimbusds.jose.JWSObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.passport.library.domain.DcsResponse;
import uk.gov.di.ipv.cri.passport.library.domain.DcsSignedEncryptedResponse;
import uk.gov.di.ipv.cri.passport.library.domain.Gpg45Evidence;
import uk.gov.di.ipv.cri.passport.library.domain.PassportAttributes;
import uk.gov.di.ipv.cri.passport.library.domain.PassportGpg45Score;
import uk.gov.di.ipv.cri.passport.library.exceptions.EmptyDcsResponseException;
import uk.gov.di.ipv.cri.passport.library.persistence.DataStore;
import uk.gov.di.ipv.cri.passport.library.persistence.item.PassportCheckDao;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassportServiceTest {
    public static final String EXPECTED_RESPONSE = "Expected Response";
    public static final String CHECK_PASSPORT_URI = "https://localhost/check/passport";

    @Mock ConfigurationService configurationService;
    @Mock DataStore<PassportCheckDao> dataStore;
    @Mock HttpClient httpClient;
    @Mock JWSObject jwsObject;
    @Mock HttpResponse httpResponse;
    @Mock StatusLine statusLine;
    @Mock HttpEntity entity;

    @Captor ArgumentCaptor<HttpPost> httpPost;

    private PassportService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PassportService(httpClient, configurationService, dataStore);
    }

    @Test
    void shouldPostToDcsEndpoint() throws IOException, EmptyDcsResponseException {
        String expectedPayload = "Test";
        HttpEntity httpEntity = new StringEntity(expectedPayload);
        when(configurationService.getDCSPostUrl()).thenReturn(CHECK_PASSPORT_URI);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(jwsObject.serialize()).thenReturn(expectedPayload);

        DcsSignedEncryptedResponse actualResponse = underTest.dcsPassportCheck(jwsObject);

        verify(httpClient, times(1)).execute(httpPost.capture());

        assertEquals(CHECK_PASSPORT_URI, httpPost.getValue().getURI().toString());
        assertEquals(
                "application/jose", httpPost.getValue().getFirstHeader("content-type").getValue());
        assertEquals(expectedPayload, EntityUtils.toString(httpPost.getValue().getEntity()));

        assertEquals(expectedPayload, actualResponse.getPayload());
    }

    @Test
    void shouldReturnAnErrorWhenDCSRespondsWithNon200() throws IOException {
        String expectedPayload = "Test";
        when(configurationService.getDCSPostUrl()).thenReturn(CHECK_PASSPORT_URI);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(500);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(jwsObject.serialize()).thenReturn(expectedPayload);

        HttpResponseException httpResponseException =
                assertThrows(
                        HttpResponseException.class, () -> underTest.dcsPassportCheck(jwsObject));
        verify(httpClient, times(1)).execute(httpPost.capture());
        assertEquals(
                "status code: 500, reason phrase: DCS responded with an error",
                httpResponseException.getMessage());
        assertEquals(500, httpResponseException.getStatusCode());
    }

    @Test
    void shouldReturnThrowExceptionWhenResponseFromDcsIsEmpty() throws IOException {
        when(configurationService.getDCSPostUrl()).thenReturn(CHECK_PASSPORT_URI);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(null);
        when(jwsObject.serialize()).thenReturn("Test");
        EmptyDcsResponseException emptyDcsResponseException =
                assertThrows(
                        EmptyDcsResponseException.class,
                        () -> {
                            underTest.dcsPassportCheck(jwsObject);
                        });
        assertEquals("Response from DCS is empty", emptyDcsResponseException.getMessage());
    }

    @Test
    void shouldCreateDcsResponseInDataStore() {
        UUID correlationId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        DcsResponse validDcsResponse = new DcsResponse(correlationId, requestId, false, true, null);
        PassportAttributes passportAttributes =
                new PassportAttributes(
                        "PASSPORT_NUMBER",
                        "SURNAME",
                        new String[] {"FORENAMES"},
                        LocalDate.now(),
                        LocalDate.now());
        PassportGpg45Score gpg45Score = new PassportGpg45Score(new Gpg45Evidence(4, 4));
        PassportCheckDao dcsResponse = new PassportCheckDao("UUID", passportAttributes, gpg45Score);
        underTest.persistDcsResponse(dcsResponse);
        verify(dataStore).create(dcsResponse);
    }
}
