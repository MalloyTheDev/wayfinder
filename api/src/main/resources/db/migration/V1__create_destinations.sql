-- Destination catalog.
--
-- The CHECK constraints here mirror the invariants in the Destination record. The domain
-- enforces them so an invalid object cannot be constructed; the database enforces them so
-- an invalid row cannot exist regardless of what wrote it, including a migration, a fixture
-- or a hand-run statement. Two acceptance criteria are at stake: AC-01.3, that the fields a
-- traveller needs are present, and AC-01.4, that a summary cannot run long enough to break
-- a layout.
--
-- Enum-like columns are TEXT with a CHECK rather than a Postgres ENUM type or an ordinal.
-- An ordinal would silently reinterpret every existing row if a value were ever inserted in
-- the middle of the Java enum, and a Postgres ENUM is awkward to extend inside a migration.

CREATE TABLE destinations (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug          TEXT NOT NULL,
    name          TEXT NOT NULL,
    region        TEXT NOT NULL,
    vacation_type TEXT NOT NULL,
    price_band    TEXT NOT NULL,
    summary       TEXT NOT NULL,

    CONSTRAINT destinations_slug_unique UNIQUE (slug),

    -- Lowercase words joined by single hyphens, matching the pattern the domain applies.
    CONSTRAINT destinations_slug_format
        CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),

    -- AC-01.3: the fields a traveller needs in order to choose must carry actual content.
    CONSTRAINT destinations_name_present
        CHECK (btrim(name) <> ''),

    -- AC-01.4: summary length bounded at both ends, 1 to 140 characters inclusive.
    CONSTRAINT destinations_summary_length
        CHECK (char_length(btrim(summary)) BETWEEN 1 AND 140),

    CONSTRAINT destinations_region_known
        CHECK (region IN ('NORTH_AMERICA', 'SOUTH_AMERICA', 'EUROPE', 'AFRICA', 'ASIA', 'OCEANIA')),

    CONSTRAINT destinations_vacation_type_known
        CHECK (vacation_type IN ('WELLNESS', 'BEACH', 'ADVENTURE', 'CITY', 'NATURE', 'CULTURAL')),

    CONSTRAINT destinations_price_band_known
        CHECK (price_band IN ('BUDGET', 'MODERATE', 'PREMIUM', 'LUXURY'))
);

-- US-04 filters on vacation type, and the seeded catalog is small enough that this index
-- will not be used yet. It is here so the query shape is indexed from the start rather
-- than being noticed as a problem once the catalog grows.
CREATE INDEX destinations_vacation_type_idx ON destinations (vacation_type);
