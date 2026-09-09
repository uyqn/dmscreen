package no.uyqn.dmscreen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.modulith.ApplicationModule;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
@ApplicationModule(allowedDependencies = {"account", "campaign", "character", "dice", "rules", "session"})
public @interface AllDomainModules {}
