package com.enterprise.ca.repository.spec;

import com.enterprise.ca.entity.Client;
import org.springframework.data.jpa.domain.Specification;

public class ClientSpecifications {

    public static Specification<Client> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank()) ? null :
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Client> typeEquals(String type) {
        return (root, query, cb) -> (type == null || type.isBlank()) ? null :
                cb.equal(root.get("clientType"), Client.ClientType.valueOf(type));
    }

    public static Specification<Client> activeEquals(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }
}
