package cz.uhk.ppro.horyavoda.application.service;

import cz.uhk.ppro.horyavoda.domain.exception.EntityAlreadyExistsException;
import cz.uhk.ppro.horyavoda.domain.exception.EntityNotFoundException;
import cz.uhk.ppro.horyavoda.domain.model.Customer;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Evidence zákazníků půjčovny. */
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<Customer> listCustomers(String search) {
        return customerRepository.search(search);
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Zákazník s ID " + customerId + " nebyl nalezen."));
    }

    /** Založení zákazníka; číslo dokladu totožnosti je unikátní a ukládá se velkými písmeny. */
    public Customer createCustomer(String firstName, String lastName, String phone, String idDocumentNumber) {
        String normalizedDocument = idDocumentNumber.strip().toUpperCase(Locale.ROOT);
        if (customerRepository.existsByIdDocumentNumber(normalizedDocument)) {
            throw new EntityAlreadyExistsException("Zákazník s dokladem '" + normalizedDocument + "' již v systému existuje.");
        }
        Customer customer = new Customer(firstName.strip(), lastName.strip(), phone.strip(), normalizedDocument);
        return customerRepository.save(customer);
    }
}
