package tech.octopusdragon.cat;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
public class CatJsonTest {

    @Autowired
    private JacksonTester<Cat> json;

    @Autowired
    private JacksonTester<Cat[]> jsonList;

    private Cat[] cats;

    @BeforeEach
    void setUp() {
        cats = Arrays.array(
                new Cat(99L, "Fluffy", 5, Sex.MALE, "Sarah"),
                new Cat(100L, "Oliver", 12, Sex.MALE, "Sarah"),
                new Cat(101L, "Luna", 7, Sex.FEMALE, "Sarah")
        );
    }

    @Test
    void catSerializationTest() throws IOException {
        Cat cat = cats[0];
        assertThat(json.write(cat)).isStrictlyEqualToJson("single.json");
        assertThat(json.write(cat)).hasJsonPathNumberValue("@.id");
        assertThat(json.write(cat)).extractingJsonPathNumberValue("@.id").isEqualTo(99);
        assertThat(json.write(cat)).hasJsonPathStringValue("@.name");
        assertThat(json.write(cat)).extractingJsonPathStringValue("@.name").isEqualTo("Fluffy");
        assertThat(json.write(cat)).hasJsonPathNumberValue("@.age");
        assertThat(json.write(cat)).extractingJsonPathNumberValue("@.age").isEqualTo(5);
        assertThat(json.write(cat)).hasJsonPathStringValue("@.sex");
        assertThat(json.write(cat)).extractingJsonPathStringValue("@.sex").isEqualTo(Sex.MALE.toString());
    }

    @Test
    void catDeserializationTest() throws IOException {
        String expected = """
                {
                    "id": 99,
                    "name": "Fluffy",
                    "age": 5,
                    "sex": "MALE",
                    "owner": "Sarah"
                }
                """;
        Cat actual = new Cat(99L, "Fluffy", 5, Sex.MALE, "Sarah");
        assertThat(json.parse(expected)).isEqualTo(actual);
        assertThat(json.parseObject(expected).id()).isEqualTo(99);
        assertThat(json.parseObject(expected).name()).isEqualTo("Fluffy");
        assertThat(json.parseObject(expected).sex()).isEqualTo(Sex.MALE);
    }

    @Test
    void catListSerializationTest() throws IOException {
        assertThat(jsonList.write(cats)).isStrictlyEqualToJson("list.json");
    }

    @Test
    void catListDeserializationTest() throws IOException {
        String expected = """
                [
                    {"id":  99, "name": "Fluffy", "age": 5, "sex": "MALE", "owner": "Sarah"},
                    {"id":  100, "name": "Oliver", "age": 12, "sex": "MALE", "owner": "Sarah"},
                    {"id":  101, "name": "Luna", "age": 7, "sex": "FEMALE", "owner": "Sarah"}
                ]
                """;
        assertThat(jsonList.parse(expected)).isEqualTo(cats);
    }
}
