package tech.octopusdragon.cat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/cats")
public class CatController {
    private final CatRepository catRepository;

    private CatController(CatRepository catRepository) {
        this.catRepository = catRepository;
    }

    @GetMapping()
    private ResponseEntity<List<Cat>> findAll(Pageable pageable, Principal principal) {
        Page<Cat> page = catRepository.findByOwner(principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "age"))
                )
        );
        return ResponseEntity.ok(page.getContent());
    }

    @GetMapping("/{requestedId}")
    private ResponseEntity<Cat> findById(@PathVariable Long requestedId, Principal principal) {
        Cat cat = findCat(requestedId, principal);
        if (cat != null) {
            return ResponseEntity.ok(cat);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    private ResponseEntity<Void> createCat(@RequestBody Cat newCat, UriComponentsBuilder ucb, Principal principal) {
        Cat catWithOwner = new Cat(null, newCat.name(), newCat.age(), newCat.sex(), principal.getName());
        Cat savedCat = catRepository.save(catWithOwner);
        URI locationOfNewCat = ucb
                .path("cats/{id}")
                .buildAndExpand(savedCat.id())
                .toUri();
        return ResponseEntity.created(locationOfNewCat).build();
    }

    @PutMapping("/{requestedId}")
    private ResponseEntity<Void> putCat(@PathVariable Long requestedId, @RequestBody Cat catUpdate, Principal principal) {
        Cat cat = findCat(requestedId, principal);
        if (cat != null) {
            Cat updatedCat = new Cat(cat.id(), catUpdate.name(), catUpdate.age(), catUpdate.sex(), principal.getName());
            catRepository.save(updatedCat);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    private ResponseEntity<Void> deleteCat(@PathVariable Long id, Principal principal) {
        if (catRepository.existsByIdAndOwner(id, principal.getName())) {
            catRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private Cat findCat(Long requestedId, Principal principal) {
        return catRepository.findByIdAndOwner(requestedId, principal.getName());
    }
}
