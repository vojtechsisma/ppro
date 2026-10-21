package cz.uhk.ppro.horyavoda.presentation.api;

import cz.uhk.ppro.horyavoda.application.service.CustomerService;
import cz.uhk.ppro.horyavoda.presentation.dto.CustomerCreateRequest;
import cz.uhk.ppro.horyavoda.presentation.dto.CustomerResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Evidence zákazníků – všechny operace vyžadují přihlášení. */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /** Seznam zákazníků s volitelným vyhledáním podle jména, příjmení či telefonu. */
    @GetMapping
    public List<CustomerResponse> listCustomers(@RequestParam(name = "search", required = false) String search) {
        return customerService.listCustomers(search).stream().map(CustomerResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        return CustomerResponse.from(customerService.createCustomer(
                request.firstName(), request.lastName(), request.phone(), request.idDocumentNumber()));
    }

    @GetMapping("/{customerId}")
    public CustomerResponse getCustomer(@PathVariable long customerId) {
        return CustomerResponse.from(customerService.getCustomer(customerId));
    }
}
