# Pokyny pro AI agenty (AGENTS.md)

Tento dokument definuje závazná pravidla, architektonické mantinely, pracovní postupy a doménová specifika pro všechny AI agenty pracující v tomto repozitáři.

---

## 1. Kontext projektu

* **Název projektu:** Informační systém pro půjčovnu sportovního vybavení (Hory a voda)
* **Akademický kontext:** Semestrální projekt předmětu **Pokročilé programování (PPRO)**, Fakulta informatiky a managementu, Univerzita Hradec Králové (ZS 2026/2027).
* **Zadání:** Varianta D – Půjčovna vybavení (klient Martin Řehák).
* **Primární zdroj pravdy:** [`README.md`](./README.md) – obsahuje kompletní specifikaci zadání, funkční požadavky, obchodní pravidla i protokol architektonických rozhodnutí.

---

## 2. Závazná architektonická pravidla (PPRO Standard)

Každý agent je povinen striktně dodržovat následující standardy:

1. **Třívrstvá architektura se závislostmi jedním směrem:**
   * **Prezentační vrstva (UI / API Controllers):** Přijímá vstupy, validuje formát požadavků, volá aplikační služby a vrací DTO/odpovědi. Nesmí obsahovat doménovou logiku ani volat databázi napřímo.
   * **Aplikační a doménová vrstva (Services, Domain Model):** Obsahuje jádro byznys logiky, doménové entity, stavové automaty a vynucuje obchodní pravidla. Je nezávislá na konkrétní databázi či UI frameworku.
   * **Infrastrukturní a datová vrstva (Repositories, DB, ORM):** Implementuje přístup k relační databázi, externím službám a souborovému systému.
   * *Závislosti směřují výhradně shora dolů (Prezentace -> Doména/Aplikace -> Infrastruktura), nikdy opačně.*

2. **Databáze a migrace:**
   * Jako databáze se používá relační DB (PostgreSQL).
   * Schéma databáze se **nikdy neupravuje ručně** – jakákoli změna modelu musí být realizována jako verzovaná kódová migrace.
   * Databázový model musí obsahovat minimálně 5 doménových entit a alespoň jednu vazbu M:N (v našem případě `Rental` ↔ `EquipmentItem` přes `RentalItem`).

3. **Orchestrace kontejnerů:**
   * Celý systém musí být reprodukovatelně sestavitelný a spustitelný jediným příkazem:
     ```bash
     docker compose up
     ```
   * Služby v `docker-compose.yml` musí mít definovány `healthchecks` a správné pořadí spouštění (`depends_on`).

4. **Integrita a bezpečnost dat:**
   * V repozitáři se **nesmí objevit žádná reálná osobní data** (používají se výhradně syntetické seed fixtures).
   * Žádné citlivé přihlašovací údaje nesmí být hardcodované v kódu. Konfigurace se předává pomocí environmentálních proměnných (`.env.example` je verzován, `.env` ignorován).

---

## 3. Klíčová obchodní a doménová pravidla (Domain Invariants)

Při implementaci jakékoli funkce musí agent garantovat následující invarianty:

1. **Zákaz překrytí (No Double Booking):**
   * Jeden fyzický kus vybavení (`EquipmentItem`) **nesmí být v časovém překryvu** ve dvou aktivních výpůjčkách nebo rezervacích.
2. **Servisní blokace:**
   * Kus ve stavu `IN_SERVICE` nesmí jít půjčit ani rezervovat na dobu servisního zásahu.
3. **Položkové částečné vracení a penále:**
   * Výpůjčka se skládá z položek (`RentalItem`). Každá položka má samostatný čas vrácení.
   * Penále za pozdní vrácení se počítá **pouze za dny prodlení konkrétního opožděného kusu**, nikoli za celou výpůjčku.
4. **Rezervace typu vs. konkrétního kusu (Kde se návrh láme):**
   * Standardní rezervace rezervuje **kapacitní kvótu** v dané kategorii a velikostní specifikaci.
   * Konkrétní kus s inventárním číslem se přiřazuje při výdeji u pultu (převod rezervace na výpůjčku).
5. **Snapshot sjednaných cen:**
   * Cena z ceníku se při sjednání výpůjčky/rezervace fixuje do položky (`RentalItem.unit_daily_price`). Změna ceníku během sezóny nesmí ovlivnit stávající smlouvy.
6. **Nemazatelná historie:**
   * Historie výpůjček a zákazníků se fyzicky nemaže (soft-delete / historické auditní záznamy).

---

## 4. Pokyny pro úpravy kódu a dokumentace

### 4.1 Správa sekce Rozhodnutí v README.md
* **Povinnost:** Pokud agent provede zásadní technické, architektonické nebo procesní rozhodnutí (např. volba frameworku, úprava schématu, nový stavový přechod), **musí toto rozhodnutí zapsat do tabulky v sekci 4.2 v [`README.md`](./README.md)**.
* Zápis musí obsahovat: Datum, oblast/téma, stručné rozhodnutí + odůvodnění a stav.

### 4.2 Git a konvence commitů
* **Striktní zákaz pushování:** Agent **nesmí za žádných okolností provádět `git push`** do vzdáleného repozitáře bez výslovného a explicitního pokynu od vývojáře. Veškeré změny a commity zůstávají výhradně lokální.
* Používat konvenci **Conventional Commits**:
  * `feat:` nová funkcionalita
  * `fix:` oprava chyby
  * `docs:` úpravy dokumentace (včetně README a AGENTS.md)
  * `test:` přidání či úprava testů
  * `refactor:` úprava kódu bez změny chování
  * `chore:` údržba konfigurací, build nástrojů
* Commity provádět v logických krocích s výstižnou zprávou v češtině nebo angličtině.

### 4.3 Testování
* Každé nové obchodní pravidlo (kolize termínů, výpočet penále, expirace rezervace) musí být ověřeno automatizovanými testy.
* Testy se spouštějí lokálně i v CI prostředí.

### 4.4 Styl kódu a čistota
* Dodržovat standardy zvoleného programovacího jazyka (linting, formátování).
* Striktně typovat (TypeScript / Python type hints / PHP strictly typed dle zvoleného backendu).
* Preferovat explicitní kód před „magií“; psát samo-vysvětlující názvy metod a proměnných.
* Zachovávat komentáře a dokumentační bloky u klíčových doménových rozhraní.

---

## 5. Kontrolní seznam před dokončením úkolu (Definition of Done)

Před odevzdáním jakékoli práce se ujisti, že:
- [ ] Kód splňuje třívrstvé uspořádání a respektuje směr závislostí.
- [ ] Všechna relevantní obchodní pravidla jsou ošetřena validacemi na doménové vrstvě.
- [ ] Jsou napsány / aktualizovány automatizované testy a všechny procházejí.
- [ ] Změny datového modelu mají odpovídající migrační skript.
- [ ] Pokud došlo k rozhodnutí, je zapsáno v [`README.md`](./README.md).
- [ ] Git commit odpovídá specifikaci Conventional Commits.
- [ ] Kód neobsahuje žádné hardcodované přihlašovací údaje ani reálné osobní údaje.
