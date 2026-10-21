package cz.uhk.ppro.horyavoda.infrastructure.persistence;

import cz.uhk.ppro.horyavoda.domain.model.Customer;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Locale;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    boolean existsByIdDocumentNumber(String idDocumentNumber);

    /** Vyhledání zákazníků podle jména, příjmení či telefonu (null/prázdné = všichni). */
    default List<Customer> search(String searchText) {
        Specification<Customer> specification = Specification.unrestricted();
        if (searchText != null && !searchText.isBlank()) {
            String pattern = "%" + searchText.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern)
            ));
        }
        return findAll(specification, Sort.by("lastName", "firstName"));
    }
}
