package tech.octopusdragon.cat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CatApplicationTests {
    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void shouldReturnACatWhenDataIsSaved() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .getForEntity("/cats/99", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        Number id = documentContext.read("$.id");
        String name = documentContext.read("$.name");
        Number age = documentContext.read("$.age");
        Sex sex = Sex.valueOf(documentContext.read("$.sex"));
        assertThat(id).isEqualTo(99);
        assertThat(name).isEqualTo("Fluffy");
        assertThat(age).isEqualTo(5);
        assertThat(sex).isEqualTo(Sex.MALE);
    }

    @Test
    void shouldNotReturnACatWithAnUnknownId() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .getForEntity("/cats/1000", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isBlank();
    }

    @Test
    @DirtiesContext
    void shouldCreateANewCat() {
        Cat newCat = new Cat(null, "Mittens", 2, Sex.FEMALE, null);
        ResponseEntity<Void> createResponse = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .postForEntity("/cats", newCat, Void.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        URI locationOfNewCat = createResponse.getHeaders().getLocation();
        ResponseEntity<String> getResponse = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .getForEntity(locationOfNewCat, String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
        Number id = documentContext.read("$.id");
        String name = documentContext.read("$.name");
        Number age = documentContext.read("$.age");
        Sex sex = Sex.valueOf(documentContext.read("$.sex"));

        assertThat(id).isNotNull();
        assertThat(name).isEqualTo("Mittens");
        assertThat(age).isEqualTo(2);
        assertThat(sex).isEqualTo(Sex.FEMALE);
    }

    @Test
    void shouldReturnAllCatsWhenListIsRequested() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .getForEntity("/cats", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        int catCount = documentContext.read("$.length()");
        assertThat(catCount).isEqualTo(3);

        JSONArray ids = documentContext.read("$..id");
        assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

        JSONArray names = documentContext.read("$..name");
        assertThat(names).containsExactlyInAnyOrder("Fluffy", "Oliver", "Luna");

        JSONArray ages = documentContext.read("$..age");
        assertThat(ages).containsExactlyInAnyOrder(5, 12, 7);

        JSONArray sexes = documentContext.read("$..sex");
        assertThat(sexes).containsExactlyInAnyOrder(Sex.MALE.toString(), Sex.MALE.toString(), Sex.FEMALE.toString());
    }

    @Test
    void shouldReturnAPageOfCats() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .getForEntity("/cats?page=0&size=1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(1);
    }

    @Test
    void shouldReturnASortedPageOfCats() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .getForEntity("/cats?page=0&size=1&sort=age,desc", String.class);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray read = documentContext.read("$[*]");
        assertThat(read.size()).isEqualTo(1);

        String name = documentContext.read("$[0].name");
        assertThat(name).isEqualTo("Oliver");

        int age = documentContext.read("$[0].age");
        assertThat(age).isEqualTo(12);

        Sex sex = Sex.valueOf(documentContext.read("$[0].sex"));
        assertThat(sex).isEqualTo(Sex.MALE);
    }

    @Test
    void shouldReturnASortedPageOfCatsWithNoParametersAndUseDefaultValues() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .getForEntity("/cats", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(3);

        JSONArray names = documentContext.read("$..name");
        assertThat(names).containsExactly("Fluffy", "Luna", "Oliver");

        JSONArray ages = documentContext.read("$..age");
        assertThat(ages).containsExactlyInAnyOrder(5, 7, 12);

        JSONArray sexes = documentContext.read("$..sex");
        assertThat(sexes).containsExactlyInAnyOrder(Sex.MALE.toString(), Sex.FEMALE.toString(), Sex.MALE.toString());
    }

    @Test
    void shouldRejectUsersWhoAreNotCatOwners() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Hank", "qrs456")
                .getForEntity("/cats/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldNotAllowAccessToCatsTheyDoNotOwn() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .getForEntity("/cats/102", String.class);   // Kumar's data
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DirtiesContext
    void shouldUpdateAnExistingCat() {
        Cat catUpdate = new Cat(null, "Fluffy", 6, Sex.MALE, null);
        HttpEntity<Cat> request = new HttpEntity<>(catUpdate);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .exchange("/cats/99", HttpMethod.PUT, request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .getForEntity("/cats/99", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
        Number id = documentContext.read("$.id");
        String name = documentContext.read("$.name");
        Number age = documentContext.read("$.age");
        Sex sex = Sex.valueOf(documentContext.read("$.sex"));
        assertThat(id).isEqualTo(99);
        assertThat(name).isEqualTo("Fluffy");
        assertThat(age).isEqualTo(6);
        assertThat(sex).isEqualTo(Sex.MALE);
    }

    @Test
    void shouldNotUpdateACatThatDoesNotExist() {
        Cat unknownCat = new Cat(null, "Fluffy", 6, Sex.MALE, null);
        HttpEntity<Cat> request = new HttpEntity<>(unknownCat);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .exchange("/cats/99999", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotUpdateACatThatIsOwnedBySomeoneElse() {
        Cat kumarsCat = new Cat(null, "Pumpkin", 11, Sex.FEMALE, null);
        HttpEntity<Cat> request = new HttpEntity<>(kumarsCat);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .exchange("/cats/102", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DirtiesContext
    void shouldDeleteAnExistingCat() {
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .exchange("/cats/99", HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .getForEntity("/cats/99", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotDeleteACatThatDoesNotExist() {
        ResponseEntity<Void> deleteResponse = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .exchange("/cats/99999", HttpMethod.DELETE, null, Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotAllowDeletionOfCatsTheyDoNotOwn() {
        ResponseEntity<Void> deleteResponse = restTemplate
                .withBasicAuth("Sarah", "abc123")
                .exchange("/cats/102", HttpMethod.DELETE, null, Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> getResponse = restTemplate
                .withBasicAuth("Kumar", "xyz789")
                .getForEntity("/cats/102", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}
