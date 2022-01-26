package uk.gov.di.ipv.cri.passport.library.persistence.item;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import uk.gov.di.ipv.cri.passport.library.domain.PassportFormRequest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

public class PassportFormRequestTypeConverter implements AttributeConverter<PassportFormRequest> {

    @Override
    public AttributeValue transformFrom(PassportFormRequest input) {

        Map<String, AttributeValue> attributeValueMap =
                Map.of(
                        "passportNumber",
                                AttributeValue.builder().s(input.getPassportNumber()).build(),
                        "surname", AttributeValue.builder().s(input.getSurname()).build(),
                        "forenames",
                                AttributeValue.builder()
                                        .s(Arrays.toString(input.getForenames()))
                                        .build(),
                        "dateOfBirth",
                                AttributeValue.builder()
                                        .s(input.getDateOfBirth().toString())
                                        .build(),
                        "expiryDate",
                                AttributeValue.builder()
                                        .s(input.getExpiryDate().toString())
                                        .build());

        return AttributeValue.builder().m(attributeValueMap).build();
    }

    @Override
    public PassportFormRequest transformTo(AttributeValue input) {
        Map<String, AttributeValue> attributeMap = input.m();
        return new PassportFormRequest(
                attributeMap.get("passportNumber").s(),
                attributeMap.get("surname").s(),
                attributeMap.get("forenames").ss().toArray(new String[0]),
                LocalDate.parse(attributeMap.get("dateOfBirth").s()),
                LocalDate.parse(attributeMap.get("expiryDate").s()));
    }

    @Override
    public EnhancedType<PassportFormRequest> type() {
        return EnhancedType.of(PassportFormRequest.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.M;
    }
}