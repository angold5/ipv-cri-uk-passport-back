package uk.gov.di.ipv.cri.passport.integrationtest;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.passport.library.domain.DcsResponse;
import uk.gov.di.ipv.cri.passport.library.domain.Gpg45Evidence;
import uk.gov.di.ipv.cri.passport.library.domain.PassportAttributes;
import uk.gov.di.ipv.cri.passport.library.domain.PassportGpg45Score;
import uk.gov.di.ipv.cri.passport.library.persistence.DataStore;
import uk.gov.di.ipv.cri.passport.library.persistence.item.PassportCheckDao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateStorePassportCheckIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateStorePassportCheckIT.class);
    private static final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String DCS_RESPONSE_TABLE_NAME = "dcs-response-integration-test";
    private static final String RESOURCE_ID_PARAM = "resourceId";
    private static final String ATTRIBUTES_PARAM = "attributes";
    private static final String GPG45_SCORE_PARAM = "gpg45Score";
    private static final DataStore<PassportCheckDao> dcsResponseDataStore =
            new DataStore<>(
                    DCS_RESPONSE_TABLE_NAME, PassportCheckDao.class, DataStore.getClient(null));

    private static final List<String> createdItemIds = new ArrayList<>();

    private static final AmazonDynamoDB independentClient =
            AmazonDynamoDBClient.builder().withRegion("eu-west-2").build();

    private static final DynamoDB testClient = new DynamoDB(independentClient);
    private static final Table tableTestHarness = testClient.getTable(DCS_RESPONSE_TABLE_NAME);

    @AfterAll
    public static void deleteTestItems() {
        for (String id : createdItemIds) {
            try {
                tableTestHarness.deleteItem(new KeyAttribute(RESOURCE_ID_PARAM, id));
            } catch (Exception e) {
                LOGGER.warn(
                        String.format(
                                "Failed to delete test data with %s of %s", RESOURCE_ID_PARAM, id));
            }
        }
    }

    @Test
    void shouldPutPassportCheckIntoTable() throws JsonProcessingException {
        PassportCheckDao passportCheckDao = createPassportCheckDao();

        dcsResponseDataStore.create(passportCheckDao);

        Item savedPassportCheck =
                tableTestHarness.getItem(RESOURCE_ID_PARAM, passportCheckDao.getResourceId());

        assertEquals(passportCheckDao.getResourceId(), savedPassportCheck.get(RESOURCE_ID_PARAM));

        String attributesJson =
                objectMapper.writeValueAsString(savedPassportCheck.get(ATTRIBUTES_PARAM));
        PassportAttributes savedPassportAttributes =
                objectMapper.readValue(attributesJson, PassportAttributes.class);
        assertEquals(
                passportCheckDao.getAttributes().toString(), savedPassportAttributes.toString());

        String gpg45ScoreJson =
                objectMapper.writeValueAsString(savedPassportCheck.get(GPG45_SCORE_PARAM));
        PassportGpg45Score savedPassportGpg45Score =
                objectMapper.readValue(gpg45ScoreJson, PassportGpg45Score.class);
        assertEquals(
                passportCheckDao.getGpg45Score().toString(), savedPassportGpg45Score.toString());
    }

    @Test
    void shouldGetPassportCheckDaoFromTable() throws JsonProcessingException {
        PassportCheckDao passportCheckDao = createPassportCheckDao();
        Item item = Item.fromJSON(objectMapper.writeValueAsString(passportCheckDao));
        tableTestHarness.putItem(item);

        PassportCheckDao result = dcsResponseDataStore.getItem(passportCheckDao.getResourceId());

        assertEquals(passportCheckDao.getResourceId(), result.getResourceId());
        assertEquals(
                passportCheckDao.getAttributes().toString(), result.getAttributes().toString());
        assertEquals(
                passportCheckDao.getGpg45Score().toString(), result.getGpg45Score().toString());
    }

    private PassportCheckDao createPassportCheckDao() {
        String resourceId = UUID.randomUUID().toString();
        DcsResponse dcsResponse =
                new DcsResponse(UUID.randomUUID(), UUID.randomUUID(), false, true, null);
        PassportAttributes passportAttributes =
                new PassportAttributes(
                        "passport-number",
                        "surname",
                        List.of("family-name"),
                        LocalDate.of(1900, 1, 1),
                        LocalDate.of(2025, 2, 2));
        passportAttributes.setDcsResponse(dcsResponse);
        PassportGpg45Score passportGpg45Score = new PassportGpg45Score(new Gpg45Evidence(5, 5));
        createdItemIds.add(resourceId);

        return new PassportCheckDao(resourceId, passportAttributes, passportGpg45Score);
    }
}
