package cz.uhk.ppro.horyavoda.domain.model;

/** Stav opotřebení fyzického kusu vybavení. */
public enum WearCondition {
    /** Nový kus. */
    NEW,
    /** Výborný stav (minimální opotřebení). */
    EXCELLENT,
    /** Běžný dobrý stav. */
    GOOD,
    /** Znatelné opotřebení (škrábance). */
    FAIR,
    /** Poškozeno (vyžaduje servis). */
    DAMAGED
}
