package io.quarkus.it.panache;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.DisabledOnNativeImage;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Test various Panache operations running in Quarkus
 */
@QuarkusTest
public class PanacheFunctionalityTest {

    /**
     * Tests that direct use of the entity in the test class does not break transformation
     *
     * see https://github.com/quarkusio/quarkus/issues/1724
     */
    @SuppressWarnings("unused")
    Person p = new Person();

    @Test
    public void testPanacheFunctionality() throws Exception {
        RestAssured.when().get("/test/model-dao").then().body(is("OK"));
        RestAssured.when().get("/test/model").then().body(is("OK"));
        RestAssured.when().get("/test/accessors").then().body(is("OK"));

        RestAssured.when().get("/test/model1").then().body(is("OK"));
        RestAssured.when().get("/test/model2").then().body(is("OK"));
        RestAssured.when().get("/test/model3").then().body(is("OK"));
    }

    @Test
    public void testPanacheSerialisation() {
        RestAssured.given().accept(ContentType.JSON)
                .when().get("/test/ignored-properties")
                .then()
                .body(is("{\"id\":666,\"dogs\":[],\"name\":\"Eddie\",\"serialisationTrick\":1,\"status\":\"DECEASED\"}"));
        RestAssured.given().accept(ContentType.XML)
                .when().get("/test/ignored-properties")
                .then().body(is(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><person><id>666</id><name>Eddie</name><serialisationTrick>1</serialisationTrick><status>DECEASED</status></person>"));
    }

    @DisabledOnNativeImage
    @Test
    public void testPanacheInTest() {
        Assertions.assertEquals(0, Person.count());
    }

    @Test
    public void testBug5274() {
        RestAssured.when().get("/test/5274").then().body(is("OK"));
    }

    @Test
    public void testBug5885() {
        RestAssured.when().get("/test/5885").then().body(is("OK"));
    }

    @Test
    public void testJaxbAnnotationTransfer() {
        RestAssured.when()
                .get("/test/testJaxbAnnotationTransfer")
                .then()
                .body(is("OK"));
    }

    /**
     * _PanacheEntityBase_ has the method _isPersistent_. This method is used by Jackson to serialize the attribute *peristent*
     * in the JSON which is not intended. This test ensures that the attribute *persistent* is not generated when using Jackson.
     * 
     * This test does not interact with the Quarkus application itself. It is just using the Jackson ObjectMapper with a
     * PanacheEntity. Thus this test is disabled in native mode. The test code runs the JVM and not native.
     */
    @DisabledOnNativeImage
    @Test
    public void jacksonDeserializationIgnoresPersistentAttribute() throws JsonProcessingException {
        // set Up
        Person person = new Person();
        person.name = "max";
        // do
        ObjectMapper objectMapper = new ObjectMapper();
        // make sure the Jaxb module is loaded
        objectMapper.findAndRegisterModules();
        String personAsString = objectMapper.writeValueAsString(person);
        // check 
        // hence no 'persistence'-attribute
        assertEquals(
                "{\"id\":null,\"name\":\"max\",\"uniqueName\":null,\"address\":null,\"status\":null,\"dogs\":[],\"serialisationTrick\":1}",
                personAsString);
    }

    /**
     * This test is disabled in native mode as there is no interaction with the quarkus integration test endpoint.
     */
    @DisabledOnNativeImage
    @Test
    public void jaxbDeserializationHasAllFields() throws JsonProcessingException, JAXBException {
        // set Up
        Person person = new Person();
        person.name = "max";
        // do
        JAXBContext jaxbContext = JAXBContext.newInstance(Person.class);

        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(person, sw);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<person><name>max</name><serialisationTrick>1</serialisationTrick></person>",
                sw.toString());
    }

    /**
     * This test is disabled in native mode as there is no interaction with the quarkus integration test endpoint.
     */
    @DisabledOnNativeImage
    @Test
    public void jsonbDeserializationHasAllFields() throws JsonProcessingException {
        // set Up
        Person person = new Person();
        person.name = "max";
        // do

        Jsonb jsonb = JsonbBuilder.create();
        String json = jsonb.toJson(person);
        assertEquals(
                "{\"dogs\":[],\"name\":\"max\",\"serialisationTrick\":1}",
                json);
    }

    @Test
    public void testBug7721() {
        RestAssured.when().get("/test/7721").then().body(is("OK"));
    }
}
