package tech.octopusdragon.cat;

import org.springframework.data.annotation.Id;

public record Cat(@Id Long id, String name, Integer age, Sex sex, String owner) {
}
