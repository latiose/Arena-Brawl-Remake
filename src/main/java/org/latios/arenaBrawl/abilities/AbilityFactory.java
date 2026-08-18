package org.latios.arenaBrawl.abilities;

@FunctionalInterface
public interface AbilityFactory {
    Ability create(AbilityDependencies deps);
}