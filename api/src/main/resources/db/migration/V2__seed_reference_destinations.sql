-- The reference dataset.
--
-- Every threshold in docs/backlog.md is stated against this data, so it is versioned as a
-- migration rather than loaded by a fixture: a latency budget or a ranking invariant only
-- means something if the data behind it is the same in every run and on every machine.
--
-- These are real destinations rather than placeholder rows, because US-03 scores on region,
-- vacation type and price band, and a ranking tested against uniform synthetic data proves
-- very little about a ranking.
--
-- 30 destinations, covering all 6 vacation types and all 6 regions. Wellness carries the
-- largest share because the originating project pivoted the entire product to detox and
-- wellness travel mid-sprint, which is the change the sprint retrospective examines.

INSERT INTO destinations (slug, name, region, vacation_type, price_band, summary) VALUES

-- Wellness (8)
('kyoto-mindfulness-retreat', 'Kyoto Mindfulness Retreat', 'ASIA', 'WELLNESS', 'PREMIUM',
 'Temple stays, morning zazen and tea ceremony in a quiet corner of the old capital.'),
('blue-lagoon-thermal-spa', 'Blue Lagoon Thermal Spa', 'EUROPE', 'WELLNESS', 'PREMIUM',
 'Geothermal pools under open sky, with silica mud treatments and long unhurried soaks.'),
('tulum-yoga-sanctuary', 'Tulum Yoga Sanctuary', 'NORTH_AMERICA', 'WELLNESS', 'MODERATE',
 'Beachfront yoga shalas, cenote swims and a signal that is weak on purpose.'),
('rishikesh-ashram-stay', 'Rishikesh Ashram Stay', 'ASIA', 'WELLNESS', 'BUDGET',
 'Daily asana by the Ganges, simple vegetarian meals and evening aarti on the ghats.'),
('baden-baden-thermal-baths', 'Baden-Baden Thermal Baths', 'EUROPE', 'WELLNESS', 'LUXURY',
 'Nineteenth century bathhouses, Black Forest walks and a very long spa tradition.'),
('hakone-hot-springs', 'Hakone Hot Springs', 'ASIA', 'WELLNESS', 'PREMIUM',
 'Open-air onsen facing Mount Fuji, with ryokan dinners and cedar forest trails.'),
('byron-bay-wellness-week', 'Byron Bay Wellness Week', 'OCEANIA', 'WELLNESS', 'MODERATE',
 'Sunrise beach walks, breathwork sessions and a slower pace on the far east coast.'),
('sedona-desert-reset', 'Sedona Desert Reset', 'NORTH_AMERICA', 'WELLNESS', 'MODERATE',
 'Red rock hikes, guided meditation at dawn and clear desert nights for stargazing.'),

-- Beach (5)
('maldives-atoll-escape', 'Maldives Atoll Escape', 'ASIA', 'BEACH', 'LUXURY',
 'Overwater villas, reef snorkelling straight off the deck and almost no horizon.'),
('algarve-coast', 'Algarve Coast', 'EUROPE', 'BEACH', 'MODERATE',
 'Golden cliffs, sheltered coves and seafood grilled within sight of the boats.'),
('zanzibar-spice-coast', 'Zanzibar Spice Coast', 'AFRICA', 'BEACH', 'MODERATE',
 'White sand, dhow sailing at dusk and a Stone Town market heavy with cloves.'),
('gold-coast-surf', 'Gold Coast Surf', 'OCEANIA', 'BEACH', 'MODERATE',
 'Consistent breaks, surf schools for beginners and a long run of patrolled beaches.'),
('rio-beaches', 'Copacabana and Ipanema', 'SOUTH_AMERICA', 'BEACH', 'MODERATE',
 'Two famous beaches, a mountain backdrop and a city that treats sand as public space.'),

-- Adventure (5)
('patagonia-trek', 'Patagonia Trekking', 'SOUTH_AMERICA', 'ADVENTURE', 'PREMIUM',
 'Granite towers, glacier crossings and weather that changes four times before lunch.'),
('iceland-ring-road', 'Iceland Ring Road', 'EUROPE', 'ADVENTURE', 'PREMIUM',
 'Waterfalls, lava fields and black sand, on one road that returns you where you began.'),
('annapurna-circuit', 'Annapurna Circuit', 'ASIA', 'ADVENTURE', 'BUDGET',
 'Teahouse trekking over a high pass, through rice terraces and into thin dry air.'),
('queenstown-adrenaline', 'Queenstown Adrenaline', 'OCEANIA', 'ADVENTURE', 'PREMIUM',
 'Bungee, jet boats and alpine trails packed into one small lakeside town.'),
('moab-canyon-country', 'Moab Canyon Country', 'NORTH_AMERICA', 'ADVENTURE', 'MODERATE',
 'Slickrock biking, two national parks and canyons that glow at the end of the day.'),

-- City (5)
('lisbon-city-break', 'Lisbon City Break', 'EUROPE', 'CITY', 'MODERATE',
 'Tiled facades, trams up steep hills and dinner that starts late by design.'),
('tokyo-neighbourhoods', 'Tokyo by Neighbourhood', 'ASIA', 'CITY', 'PREMIUM',
 'Distinct districts, transit that always works and the best late food anywhere.'),
('mexico-city-centro', 'Mexico City Centro', 'NORTH_AMERICA', 'CITY', 'BUDGET',
 'Murals, markets and a historic centre built on a drained lake bed.'),
('cape-town-city-bowl', 'Cape Town City Bowl', 'AFRICA', 'CITY', 'MODERATE',
 'A city under a flat-topped mountain, with beaches and vineyards inside an hour.'),
('buenos-aires-barrios', 'Buenos Aires Barrios', 'SOUTH_AMERICA', 'CITY', 'BUDGET',
 'Grand avenues, late dinners and tango that is danced rather than performed.'),

-- Nature (4)
('serengeti-migration', 'Serengeti Migration', 'AFRICA', 'NATURE', 'LUXURY',
 'Follow the herds across open grassland, with river crossings and very early starts.'),
('banff-lakes', 'Banff Lakes', 'NORTH_AMERICA', 'NATURE', 'PREMIUM',
 'Glacial lakes that really are that colour, ringed by larch and walkable peaks.'),
('azores-sao-miguel', 'Azores, Sao Miguel', 'EUROPE', 'NATURE', 'MODERATE',
 'Crater lakes, hot springs and hydrangea hedges along most of the island roads.'),
('borneo-rainforest', 'Borneo Rainforest', 'ASIA', 'NATURE', 'PREMIUM',
 'River journeys into old-growth forest, with orangutans and nights loud with insects.'),

-- Cultural (3)
('andalusia-moorish-trail', 'Andalusian Moorish Trail', 'EUROPE', 'CULTURAL', 'MODERATE',
 'The Alhambra, Cordoba''s mosque-cathedral and Seville, across a few hours of rail.'),
('peru-sacred-valley', 'Peru Sacred Valley', 'SOUTH_AMERICA', 'CULTURAL', 'PREMIUM',
 'Inca terraces, highland markets and Machu Picchu reached on foot or by train.'),
('marrakech-medina', 'Marrakech Medina', 'AFRICA', 'CULTURAL', 'BUDGET',
 'A walled old city of souks and riads, with the Atlas mountains an hour away.');
