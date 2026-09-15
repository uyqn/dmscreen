package no.uyqn.dmscreen.dice.internal;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface RollRepository extends CrudRepository<Roll, UUID> {}
