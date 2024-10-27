package tech.octopusdragon.cat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

interface CatRepository extends CrudRepository<Cat, Long>,
        PagingAndSortingRepository<Cat, Long> {
    Cat findByIdAndOwner(Long id, String owner);
    Page<Cat> findByOwner(String owner, PageRequest pageRequest);
    boolean existsByIdAndOwner(Long id, String owner);
}
